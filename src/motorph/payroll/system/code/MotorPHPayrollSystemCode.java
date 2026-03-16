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

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        
        // --- 1. LOGIN SYSTEM --- // asking to input username (employee or payroll staff)
        System.out.print("Enter username: ");
        String user = sc.nextLine();
        System.out.print("Enter password: ");
        String pass = sc.nextLine();
        // Validates credentials; terminates if incorrect
        if (!pass.equals("12345") || (!user.equals("employee") && !user.equals("payroll_staff"))) {
            System.out.println("Incorrect username and/or password. Program terminated.");
            return;
        }

        // --- 2. MAIN MENU LOOP ---
        while (true) {
            System.out.println("\n1. " + (user.equals("employee") ? "Enter your employee number" : "Process Payroll"));
            System.out.println("2. Exit the program");
            System.out.print("choice: ");
            String choice = sc.nextLine();

            if (choice.equals("2")) break;
            if (choice.equals("1")) {
                if (user.equals("payroll_staff")) {
                    System.out.println("\n1. One employee\n2. All employees\n3. Exit the program");
                    System.out.print("choice: ");
                    String sub = sc.nextLine();
                    if (sub.equals("3")) break;
                    if (sub.equals("2")) { processAllEmployees(); continue; }
                }
                processOneEmployee(sc);
            }
        }
    }
    // Handles payroll calculation for a single employee ID
    public static void processOneEmployee(Scanner sc) {
        System.out.print("Enter Employee #: ");
        String inputId = sc.nextLine();
        // File paths for employee details and attendance records
        String empFile = "src/resources/MotorPH_Employee Data - Employee Details.csv";
        String attFile = "src/resources/MotorPH Attendance Data - Attendance Record.csv";
        // Retrieve employee data from CSV
        String[] empData = findEmployee(inputId, empFile);
        if (empData == null) {
            System.out.println("Employee number does not exist.");
            return;
        }

        displayPayrollForMonths(empData, attFile);
    }
    // Loops through all employees in the CSV file
    public static void processAllEmployees() {
        String empFile = "src/resources/MotorPH_Employee Data - Employee Details.csv";
        String attFile = "src/resources/MotorPH Attendance Data - Attendance Record.csv";
        
        try (BufferedReader br = new BufferedReader(new FileReader(empFile))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                // Splits the line into an array using a comma as a delimiter, but only if the comma is NOT inside double quotes
                String[] empData = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                displayPayrollForMonths(empData, attFile);
                System.out.println("------------------------------------------------");
            }
        } catch (Exception e) { }
    }
    // Main calculation logic for monthly payroll and deductions
    public static void displayPayrollForMonths(String[] empData, String attFile) {
        String id = empData[0].replace("\"", "").trim();
        System.out.println("\nEmployee #: " + id);
        System.out.println("Employee Name: " + empData[2].replace("\"", "") + " " + empData[1].replace("\"", ""));
        System.out.println("Birthday: " + empData[3].replace("\"", ""));
        // Hourly rate parsed from column 18
        double rate = Double.parseDouble(empData[18].replace("\"", "").trim());

        // Process months from June to December
        for (int m = 6; m <= 12; m++) {
            YearMonth ym = YearMonth.of(2024, m);
            
            // First Cutoff: 1st to 15th of the month
            double h1 = getHours(id, ym.atDay(1), ym.atDay(15), attFile);
            double g1 = h1 * rate;

            // Second Cutoff: 16th to end of the month
            double h2 = getHours(id, ym.atDay(16), ym.atEndOfMonth(), attFile);
            double g2 = h2 * rate;

            // --- MONTHLY DEDUCTIONS ---
            double totalG = g1 + g2;// Total Monthly Gross
            double sss = computeSSS(totalG); // SSS based on monthly total
            double ph = (totalG * 0.03) / 2; // PhilHealth (3% rate divided by 2)
            double pi = (totalG > 1500) ? 100 : totalG * 0.01; // Pag-IBIG logic
            //// Tax computed after subtracting statutory deductions
            double tax = computeTax(totalG - (sss + ph + pi)); 
            double totalD = sss + ph + pi + tax;// Total combined deductions
            
            /**
            * Note: Per MotorPH policy, the 1st cutoff (1st–15th) is paid in full gross.
            * All monthly statutory deductions (SSS, PhilHealth, Pag-IBIG, and Tax)
            * are applied solely to the 2nd cutoff (16th–30th/31st).
            */
            
            // Display First Cutoff results
            String month = ym.getMonth().toString();
            System.out.println("\nCutoff Date: " + month + " 1 to 15");
            System.out.println("Total Hours Worked : " + h1);
            System.out.println("Gross Salary: " + g1);
            System.out.println("Net Salary: " + g1); // No deductions on 1st cutoff

            // --- 2nd CUTOFF DISPLAY (June 16 to End) ---
            System.out.println("\nCutoff Date: " + month + " 16 to " + ym.lengthOfMonth());
            System.out.println("Total Hours Worked : " + h2); // No rounding
            System.out.println("Gross Salary: " + g2); 
            
            System.out.println("Each Deductions:");
            System.out.println("    SSS: " + sss); // Indented with 4 spaces
            System.out.println("    PhilHealth: " + ph);
            System.out.println("    Pag-IBIG: " + pi);
            System.out.println("    Tax: " + tax);
            System.out.println("Total Deductions: " + totalD);
            System.out.println("Net Salary: " + (g2 - totalD)); // Deductions taken from 2nd cutoff

            // Horizontal Divider every month
            System.out.println("-----------------------------------------------------------------------");
        }
    }
    // Calculates hours worked with grace period and shift capping
    public static double getHours(String id, LocalDate start, LocalDate end, String path) {
    double total = 0;
    DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    DateTimeFormatter tFmt = DateTimeFormatter.ofPattern("[H:mm:ss][H:mm]"); 

    try (BufferedReader br = new BufferedReader(new FileReader(path))) {
        br.readLine(); // Skip header
        String line;
        while ((line = br.readLine()) != null) {
            String[] d = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            if (d[0].replace("\"", "").trim().equals(id)) {
                LocalDate date = LocalDate.parse(d[3].replace("\"", "").trim(), dFmt);
                
                // Filter records within the specified cutoff dates
                if (!date.isBefore(start) && !date.isAfter(end)) {
                    LocalTime in = LocalTime.parse(d[4].replace("\"", "").trim(), tFmt);
                    LocalTime out = LocalTime.parse(d[5].replace("\"", "").trim(), tFmt);

                    // --- SHIFT LOGIC (8:00 AM - 5:00 PM) ---
                    LocalTime actualIn = in;
                    // 10-minute Grace Period (8:01-8:10 counted as 8:00)
                    if (in.isBefore(LocalTime.of(8, 0))) {
                        actualIn = LocalTime.of(8, 0);
                    } else if (in.isAfter(LocalTime.of(8, 0)) && in.isBefore(LocalTime.of(8, 11))) {
                        actualIn = LocalTime.of(8, 0); // Early clock-in starts at 8:00
                    }

                    // Capping Log Out at 5:00 PM (17:00)
                    LocalTime actualOut = out.isAfter(LocalTime.of(17, 0)) ? LocalTime.of(17, 0) : out;

                    if (actualOut.isAfter(actualIn)) {
                        long minutes = Duration.between(actualIn, actualOut).toMinutes();
                        
                        // --- 2. LUNCH BREAK DEDUCTION ---
                        // Mandatory 1-hour lunch deduction
                        if (minutes > 60) { 
                            minutes -= 60;
                        } else {
                            minutes = 0;
                        }
                        // Convert to decimal hours; no rounding applied
                        total += minutes / 60.0;
                    }
                }
            }
        }
    } catch (Exception e) { }
    return total;
}
    // Searches for employee details by ID in the CSV
    public static String[] findEmployee(String id, String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                
                String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (data[0].replace("\"", "").trim().equals(id)) return data;
            }
        } catch (Exception e) { }
        return null;
    }

    // --- Complete SSS range using if, else if ---
    public static double computeSSS(double salary) {

    double[][] sssTable = {
        {3250, 135.00},
        {3750, 157.50},
        {4250, 180.00},
        {4750, 202.50},
        {5250, 225.00},
        {5750, 247.50},
        {6250, 270.00},
        {6750, 292.50},
        {7250, 315.00},
        {7750, 337.50},
        {8250, 360.00},
        {8750, 382.50},
        {9250, 405.00},
        {9750, 427.50},
        {10250, 450.00},
        {10750, 472.50},
        {11250, 495.00},
        {11750, 517.50},
        {12250, 540.00},
        {12750, 562.50},
        {13250, 585.00},
        {13750, 607.50},
        {14250, 630.00},
        {14750, 652.50},
        {15250, 675.00},
        {15750, 697.50},
        {16250, 720.00},
        {16750, 742.50},
        {17250, 765.00},
        {17750, 787.50},
        {18250, 810.00},
        {18750, 832.50},
        {19250, 855.00},
        {19750, 877.50},
        {20250, 900.00},
        {20750, 922.50},
        {21250, 945.00},
        {21750, 967.50},
        {22250, 990.00},
        {22750, 1012.50},
        {23250, 1035.00},
        {23750, 1057.50},
        {24250, 1080.00},
        {24750, 1102.50}
    };

        for (double[] sssTable1 : sssTable) {
            if (salary <= sssTable1[0]) {
                return sssTable1[1];
            }
        }

    return 1125.00; // salary above 24,750
}
    // tax range computation 
    public static double computeTax(double taxableIncome) {
        if (taxableIncome <= 20832) return 0; // No tax for low income
        else if (taxableIncome < 33333) return (taxableIncome - 20833) * 0.20; // 20% bracket
        else if (taxableIncome < 66667) return 2500 + (taxableIncome - 33333) * 0.25; // 25% bracket
        else if (taxableIncome < 166667) return 10833 + (taxableIncome - 66667) * 0.30; // 30% bracket
        else if (taxableIncome < 666667) return 40833.33 + (taxableIncome - 166667) * 0.32; // 32% bracket
        else return 200833.33 + (taxableIncome - 666667) * 0.35; // 35% bracket
    }
}
