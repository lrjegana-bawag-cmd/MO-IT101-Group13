/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
/*
 * Project: MotorPH Payroll System
 * MO-IT101-Group13
 * Authors: Jennifer T. Egana-Bawag, Catherine Castro, Smith Gregorio
 * Description: This program processes employee payroll for MotorPH by reading
 * employee and attendance data from CSV files, computing worked hours,
 * calculating statutory deductions (SSS, PhilHealth, Pag-IBIG, and tax),
 * and displaying payroll results per cutoff period.
 */
package motorph.payroll.system.code;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class MotorPHPayrollSystemCode {

    // Relative paths ensure portability; the code will run as long as the 
    // folder structure is maintained on any machine.
    private static final String EMPLOYEE_FILE = "src/Resources/MotorPH_Employee Data - Employee Details.csv";
    private static final String ATTENDANCE_FILE = "src/Resources/MotorPH Attendance Data - Attendance Record.csv";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Security Layer: Separated to keep the payroll logic clean and focused.
        if (!authenticateUser(scanner)) {
            System.out.println("Incorrect username and/or password. Program terminated.");
            return;
        }

        // Configuration: User defines the processing scope.
        int year = getValidYear(scanner);
        int startMonth = getValidMonth(scanner, "Enter Start Month (1-12): ");
        int endMonth = getValidMonth(scanner, "Enter End Month (1-12): ");

        boolean isRunning = true;
        while (isRunning) {
            System.out.println("\n[1] Process Payroll");
            System.out.println("[2] Exit");
            System.out.print("Choice: ");
            String choice = scanner.nextLine();

            if (choice.equals("2")) {
                isRunning = false;
            } else if (choice.equals("1")) {
                handlePayrollSelection(scanner, year, startMonth, endMonth);
            } else {
                System.out.println("Invalid selection. Please try again.");
            }
        }
        
        // Scanner is closed only once at the end of the program to prevent StreamClosed exceptions.
        scanner.close();
    }

    // --- MODULAR METHODS: AUTHENTICATION & NAVIGATION ---

    public static boolean authenticateUser(Scanner scanner) {
        System.out.print("Enter username: ");
        String user = scanner.nextLine();
        System.out.print("Enter password: ");
        String pass = scanner.nextLine();
        // Simple credential check; can be expanded to check a CSV file if needed.
        return (pass.equals("12345") && (user.equals("employee") || user.equals("payroll_staff")));
    }

    public static void handlePayrollSelection(Scanner scanner, int year, int startMonth, int endMonth) {
        System.out.println("\n1. Process One Employee\n2. Process All Employees\n3. Back");
        System.out.print("Choice: ");
        String sub = scanner.nextLine();

        if (sub.equals("1")) {
            System.out.print("Enter Employee Number: ");
            String empId = scanner.nextLine();
            String[] empData = findEmployeeById(empId);
            if (empData != null) {
                displayPayrollReport(empData, year, startMonth, endMonth);
            } else {
                System.out.println("Error: Employee ID not found in database.");
            }
        } else if (sub.equals("2")) {
            processAllEmployees(year, startMonth, endMonth);
        } else {
            System.out.println("Invalid selection. Please try again.");
        }
    }

    // --- ALGORITHM: PAYROLL COMPUTATION ---

    public static void displayPayrollReport(String[] empData, int year, int startMonth, int endMonth) {
        /* * DATA MAPPING (From MotorPH CSV Schema):
         * [0] ID, [1] First Name, [2] Last Name, [3] Birthday, [18] Hourly Rate.
         * We strip quotes to ensure numeric parsing doesn't fail on Excel-exported data.
         */
        String id = empData[0].replace("\"", "").trim();
        String name = empData[2].replace("\"", "") + " " + empData[1].replace("\"", "");
        String birthday = empData[3].replace("\"", "");
        double hourlyRate = Double.parseDouble(empData[18].replace("\"", "").trim());

        System.out.println("\nEmployee #: " + id);
        System.out.println("Employee Name: " + name);
        System.out.println("Birthday: " + birthday);

        for (int month = startMonth; month <= endMonth; month++) {
            YearMonth ym = YearMonth.of(year, month);
            
            // Calculating by cutoff ensures alignment with semi-monthly salary
            double firstCutoffHours = getHoursFromCSV(id, ym.atDay(1), ym.atDay(15));
            double secondCutoffHours = getHoursFromCSV(id, ym.atDay(16), ym.atEndOfMonth());

            /// DATA EXISTENCE CHECK: Informing the user if no logs exist for the period 
            // instead of displaying misleading zero-value payrolls.
            if (firstCutoffHours == 0 && secondCutoffHours == 0) {
                System.out.println("\nNo attendance records found for " + ym.getMonth() + " " + year);
                System.out.println("-------------------------------------------------------");
                continue; // Skip calculation for months with no data.
            }

            double firstCutoffGross = firstCutoffHours * hourlyRate;
            double secondCutoffGross = secondCutoffHours * hourlyRate;
            double monthlyGross = firstCutoffGross + secondCutoffGross;

            // Statutory deductions are always based on the TOTAL monthly gross as per MototPH requirements
            double sss = computeSSS(monthlyGross);
            double philHealth = computePhilHealth(monthlyGross);
            double pagIbig = computePagIbig(monthlyGross);
            
            // Taxable Income Rule: Deductions are subtracted FIRST before calculating withholding tax.
            double taxable = monthlyGross - (sss + philHealth + pagIbig);
            double tax = computeTax(taxable);
            double totalDeductions = sss + philHealth + pagIbig + tax;
            /**
            * Note: Per MotorPH policy, the 1st cutoff (1st–15th) is paid in full gross.
            * All monthly statutory deductions (SSS, PhilHealth, Pag-IBIG, and Tax)
            * are applied solely to the 2nd cutoff (16th–30th/31st).
            */
            // OUTPUT: Concatenating raw doubles to maintain full precision (No Rounding).
            System.out.println("\nCutoff Date: " + ym.getMonth() + " 1 to 15");
            System.out.println("Total Hours Worked : " + firstCutoffHours);
            System.out.println("Gross Salary: " + firstCutoffGross);
            System.out.println("Net Salary: " + firstCutoffGross);

            System.out.println("\nCutoff Date: " + ym.getMonth() + " 16 to " + ym.lengthOfMonth());
            System.out.println("Total Hours Worked : " + secondCutoffHours);
            System.out.println("Gross Salary: " + secondCutoffGross);
            System.out.println("Each Deductions:");
            System.out.println("    SSS: " + sss);
            System.out.println("    PhilHealth: " + philHealth);
            System.out.println("    Pag-IBIG: " + pagIbig);
            System.out.println("    Tax: " + tax);
            System.out.println("Total Deductions: " + totalDeductions);
            System.out.println("Net Salary: " + (secondCutoffGross - totalDeductions));
            System.out.println("-------------------------------------------------------");
        }
    }

    public static double computePhilHealth(double gross) {
        // Rule: 3% premium split equally between employer and employee.
        // Why: Following MotorPH Mandated Government Deductions, the 1.5% 
        // employee share is capped between 150.00 and 900.00.
        double share = (gross * 0.03) / 2;
        // Floor and Ceiling Rule: Ensuring contributions stay within legal bounds (150-900).
        if (share < 150) return 150.0;
        if (share > 900) return 900.0;
        return share;
    }

    public static double computePagIbig(double gross) {
        // Rule: 1% for 1000-1500, 2% for >1500. Capped at 100 max contribution.
        double rate = (gross > 1500) ? 0.02 : (gross >= 1000) ? 0.01 : 0;
        double amount = gross * rate;
        // Cap Rule: Monthly contribution cannot exceed 100 per policy.
        return (amount > 100) ? 100.0 : amount;
    }

    public static double getHoursFromCSV(String empId, LocalDate start, LocalDate end) {
        double totalHrs = 0;
        DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        DateTimeFormatter tFmt = DateTimeFormatter.ofPattern("[H:mm:ss][H:mm]");

        try (BufferedReader br = new BufferedReader(new FileReader(ATTENDANCE_FILE))) {
            br.readLine(); 
            String line;
            while ((line = br.readLine()) != null) {
                // Regex: Correctly handles commas found within quoted CSV strings.
                String[] row = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (row[0].replace("\"", "").trim().equals(empId)) {
                    LocalDate date = LocalDate.parse(row[3].replace("\"", "").trim(), dFmt);
                    if (!date.isBefore(start) && !date.isAfter(end)) {
                        LocalTime tIn = LocalTime.parse(row[4].replace("\"", "").trim(), tFmt);
                        LocalTime tOut = LocalTime.parse(row[5].replace("\"", "").trim(), tFmt);

                        // Rule: 10-minute grace period (Arrival until 8:10 is 8:00 start).
                        LocalTime adjIn = (tIn.isBefore(LocalTime.of(8, 11))) ? LocalTime.of(8, 0) : tIn;
                        // Shift Cap: Unauthorized overtime (beyond 5 PM) is excluded from auto-payroll.
                        LocalTime adjOut = (tOut.isAfter(LocalTime.of(17, 0))) ? LocalTime.of(17, 0) : tOut;

                        if (adjOut.isAfter(adjIn)) {
                            long mins = Duration.between(adjIn, adjOut).toMinutes();
                            // Rule: Mandatory 1-hour lunch break deduction for full shifts.
                            mins = (mins > 60) ? mins - 60 : 0;
                            totalHrs += mins / 60.0;
                        }
                    }
                }
            }
        } catch (Exception e) { /* Errors are silent but could be logged */ }
        return totalHrs;
    }

    // --- DATA ACCESS & TABLES ---

    public static String[] findEmployeeById(String id) {
        try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {
            br.readLine(); String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (data[0].replace("\"", "").trim().equals(id)) return data;
            }
        } catch (Exception e) { }
        return null;
    }

    public static void processAllEmployees(int year, int startMonth, int endMonth) {
        try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {
            br.readLine(); String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                displayPayrollReport(data, year, startMonth, endMonth);
            }
        } catch (Exception e) {
            System.out.println("Error processing all employees: " + e.getMessage());
        }
    }

    /**
     * SSS Contribution Table (revised)
     * Why: Array-based lookup is used for O(1) calculation speed. 
     * The brackets follow MotorPH Mandated Government Deductions to ensure 
     * accurate employee-share contributions based on monthly gross.
     */
    public static double computeSSS(double salary) {
        double[][] sssTable = { {3250, 135}, {3750, 157.5}, {4250, 180}, {4750, 202.5}, {5250, 225}, {5750, 247.5}, {6250, 270}, {6750, 292.5}, {7250, 315}, {7750, 337.5}, {8250, 360}, {8750, 382.5}, {9250, 405}, {9750, 427.5}, {10250, 450}, {10750, 472.5}, {11250, 495}, {11750, 517.5}, {12250, 540}, {12750, 562.5}, {13250, 585}, {13750, 607.5}, {14250, 630}, {14750, 652.5}, {15250, 675}, {15750, 697.5}, {16250, 720}, {16750, 742.5}, {17250, 765}, {17750, 787.5}, {18250, 810}, {18750, 832.5}, {19250, 855}, {19750, 877.5}, {20250, 900}, {20750, 922.5}, {21250, 945}, {21750, 967.5}, {22250, 990}, {22750, 1012.5}, {23250, 1035}, {23750, 1057.5}, {24250, 1080}, {24750, 1102.5} };
        for (double[] row : sssTable) { if (salary <= row[0]) return row[1]; }
        return 1125.0; 
    }

    public static double computeTax(double taxable) {
        /** * Logic: Progressive Tax Schedule based on MotorPH Mandated Government Deductions.
         * Why: Tax is applied ONLY to the Taxable Income (Gross minus SSS, PhilHealth, Pag-IBIG).
         */
        // Income below 20,833 is non-taxable.
        if (taxable <= 20832) return 0;
        // Bracket 2: 20% tax on the excess over 20,833.
        else if (taxable < 33333) return (taxable - 20833) * 0.20;
        // Bracket 3: Fixed 2,500 base tax plus 25% on the excess over 33,333.
        else if (taxable < 66667) return 2500 + (taxable - 33333) * 0.25;
        // Bracket 4: Fixed 10,833 base tax plus 30% on the excess over 66,667.
        else if (taxable < 166667) return 10833 + (taxable - 66667) * 0.30;
        // Bracket 5 & 6: High-income brackets with 32% to 35% progressive rates.
        else if (taxable < 666667) return 40833.33 + (taxable - 166667) * 0.32;
        else return 200833.33 + (taxable - 666667) * 0.35;
    }

    // --- HELPERS: ERROR HANDLING & DYNAMIC VALIDATION ---

    private static int getValidYear(Scanner sc) {
        while (true) {
            try {
                System.out.print("Enter Year: ");
                int yr = Integer.parseInt(sc.nextLine());
                int currentYear = Year.now().getValue();
                // Future dates are invalid as attendance logs don't exist yet.
                if (yr >= 1900 && yr <= currentYear) {
                    return yr;
                } else {
                    System.out.println("Error: Year must be between 1900 and " + currentYear);
                }
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    private static int getValidMonth(Scanner sc, String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                int month = Integer.parseInt(sc.nextLine());
                if (month >= 1 && month <= 12) return month;
                System.out.println("Error: Month must be between 1 and 12.");
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }
}