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

    // File path constants for Employee and Attendance data records.
    private static final String EMPLOYEE_FILE = "src/Resources/MotorPH_Employee Data - Employee Details.csv";
    private static final String ATTENDANCE_FILE = "src/Resources/MotorPH Attendance Data - Attendance Record.csv";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // --- 1. LOGIN SYSTEM --- // asking to input username (employee or payroll staff)
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        /// Validates credentials; terminates if incorrect
        if (!password.equals("12345") || (!username.equals("employee") && !username.equals("payroll_staff"))) {
            System.out.println("Incorrect username and/or password. Program terminated.");
            return;
        }
        // Configure the payroll period based on user-defined year and month range for flexibility.
        System.out.println("\n--- Payroll Configuration ---");
        System.out.print("Enter Year (e.g., 2024): "); // Prompt the user for the target fiscal year.
        int year = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter Start Month (1-12): "); // Define the beginning of the payroll calculation period (1-12).
        int startMonth = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter End Month (1-12): "); // Define the end of the payroll calculation period (1-12)
        int endMonth = Integer.parseInt(scanner.nextLine());
        
        // Main application loop to handle user interaction based on role (Employee or Payroll Staff).
        while (true) {
            // Dynamically display menu options depending on the logged-in user's role.
            System.out.println("1. " + (username.equals("employee") ? "Enter your employee number" : "Process Payroll"));
            System.out.println("2. Exit");
            System.out.print("Choice: ");
            String choice = scanner.nextLine();

            if (choice.equals("2"))break;
            if (choice.equals("1")) {
                // Provide additional processing options for payroll staff.
                if (username.equals("payroll_staff")) {
                    System.out.println("\nPayroll Staff Options:");
                    System.out.println("1. Process One Employee");
                    System.out.println("2. Process All Employees");
                    System.out.println("3. Back to Main Menu");
                    System.out.print("Choice: ");
                    String subChoice = scanner.nextLine();
                    
                    if (subChoice.equals("1")) {
                        processOneEmployee(scanner, year, startMonth, endMonth);
                    } else if (subChoice.equals("2")) {
                        processAllEmployees(year, startMonth, endMonth);
                    }
                } else {
                    // Proceed with individual employee record lookup
                    processOneEmployee(scanner, year, startMonth, endMonth);
                }
            }
        }
    }
    /**
    * Searches for a specific employee by ID and displays their payroll 
    * for the defined month range.
    */
    public static void processOneEmployee(Scanner scanner, int year, int startMonth, int endMonth) {
        System.out.print("Enter Employee Number: ");
        String employeeId = scanner.nextLine();
        
        String[] employeeData = findEmployee(employeeId);
        if (employeeData == null) {
            System.out.println("Error: Employee number " + employeeId + " not found.");
            return;
        }
        displayPayrollForMonths(employeeData, year, startMonth, endMonth);
    }
    /**
    * Iterates through the entire employee database to generate and display 
    * payroll records for every employee found in the system.
    */
    public static void processAllEmployees(int year, int startMonth, int endMonth) {
        try (BufferedReader reader = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {
            reader.readLine(); // Skip header
            String line;
            // Skip the CSV header row to avoid processing column titles as data
            while ((line = reader.readLine()) != null) { 
                String[] employeeData = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); 
                // Trigger the calculation and display logic for the current employee record
                displayPayrollForMonths(employeeData, year, startMonth, endMonth);
                System.out.println("-------------------------------------------------------");
            }
        } catch (Exception e) {
            System.out.println("Error processing all employees: " + e.getMessage());
        }
    }
    /**
    * Calculates and displays the payroll breakdown for a specific employee
    * across a given range of months.
    */
    public static void displayPayrollForMonths(String[] employeeData, int year, int start, int end) {
        String employeeId = employeeData[0].replace("\"", "").trim();
        System.out.println("\nEmployee #: " + employeeId);
        System.out.println("Employee Name: " + employeeData[1].replace("\"", "") + " " + employeeData[2].replace("\"", ""));
        System.out.println("Birthday: " + employeeData[3].replace("\"", ""));
        // Hourly rate parsed from column 18
        double hourlyRate = Double.parseDouble(employeeData[18].replace("\"", "").trim());
        
        // Initialize the year-month context for the current iteration
        for (int m = start; m <= end; m++) {
            java.time.YearMonth ym = java.time.YearMonth.of(year, m);
            String monthName = ym.getMonth().toString();
            
            // Calculate hours worked for the 1st cutoff (1st to 15th of the month)
            double firstCutoffHours = getHours(employeeId, ym.atDay(1), ym.atDay(15));
            // Calculate hours worked for the 2nd cutoff (16th to the last day of the month)
            double secondCutoffHours = getHours(employeeId, ym.atDay(16), ym.atEndOfMonth());
            // Compute gross salaries based on the hourly rate and total hours per cutoff
            double firstCutoffGross = firstCutoffHours * hourlyRate;
            double secondCutoffGross = secondCutoffHours * hourlyRate;
            double monthlyGrossSalary = firstCutoffGross + secondCutoffGross;

            // Compute statutory deductions based on the monthly gross salary.
            double sss = computeSSS(monthlyGrossSalary);// SSS based on monthly total
            double philHealth = (monthlyGrossSalary * 0.03) / 2;// PhilHealth (3% rate divided by 2)
            double pagIbig = (monthlyGrossSalary > 1500) ? 100 : monthlyGrossSalary * 0.01;// Pag-IBIG logic
            double tax = computeTax(monthlyGrossSalary - (sss + philHealth + pagIbig)); 
            double totalDeductions = sss + philHealth + pagIbig + tax;// Total combined deductions
            /**
            * Note: Per MotorPH policy, the 1st cutoff (1st–15th) is paid in full gross.
            * All monthly statutory deductions (SSS, PhilHealth, Pag-IBIG, and Tax)
            * are applied solely to the 2nd cutoff (16th–30th/31st).
            */
            // Display First Cutoff results
            System.out.println("\nCutoff Date: " + monthName + " 1 to 15");
            System.out.println("Total Hours Worked : " + firstCutoffHours);
            System.out.println("Gross Salary: " + firstCutoffGross);
            System.out.println("Net Salary: " + firstCutoffGross); // No deductions on 1st cutoff
            // --- 2nd CUTOFF DISPLAY ---
            System.out.println("\nCutoff Date: " + monthName + " 16 to " + ym.lengthOfMonth());
            System.out.println("Total Hours Worked : " + secondCutoffHours);
            System.out.println("Gross Salary: " + secondCutoffGross);
            
            System.out.println("Each Deductions:");
            System.out.println("    SSS: " + sss);
            System.out.println("    PhilHealth: " + philHealth);
            System.out.println("    Pag-IBIG: " + pagIbig);
            System.out.println("    Tax: " + tax);
            System.out.println("Total Deductions: " + totalDeductions);
            System.out.println("Net Salary: " + (secondCutoffGross - totalDeductions)); 
            // Horizontal Divider every month
            System.out.println("-----------------------------------------------------------------------");
        }
    }

    // Calculates the total hours worked for a specific employee within a given date range
    //Includes logic for shift capping, grace periods, and mandatory break deductions
    public static double getHours(String employeeId, LocalDate startDate, LocalDate endDate) {
        double totalHoursWorked = 0;
        // Define date and time formats to match the CSV source data
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("[H:mm:ss][H:mm]"); 

        try (BufferedReader br = new BufferedReader(new FileReader(ATTENDANCE_FILE))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                // Use regex to split CSV columns while preserving data inside quotes
                String[] attendanceRecord = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                // Filter records by Employee ID and Date Range
                if (attendanceRecord[0].replace("\"", "").trim().equals(employeeId)) {
                    LocalDate recordDate = LocalDate.parse(attendanceRecord[3].replace("\"", "").trim(), dateFormatter);
                    
                    if (!recordDate.isBefore(startDate) && !recordDate.isAfter(endDate)) {
                        LocalTime timeIn = LocalTime.parse(attendanceRecord[4].replace("\"", "").trim(), timeFormatter);
                        LocalTime timeOut = LocalTime.parse(attendanceRecord[5].replace("\"", "").trim(), timeFormatter);

                        // Apply company shift rules (e.g., grace period and fixed start times)
                        LocalTime actualIn = timeIn;
                        // Apply grace period: If arrival is between 8:00 AM and 8:10 AM, 
                        // it is still considered an 8:00 AM start
                        if (timeIn.isBefore(LocalTime.of(8, 0))) {
                            actualIn = LocalTime.of(8, 0);
                        } else if (timeIn.isAfter(LocalTime.of(8, 0)) && timeIn.isBefore(LocalTime.of(8, 11))) {
                            actualIn = LocalTime.of(8, 0); // Grace period
                        }
                        // Shift Capping: Standard shift ends at 5:00 PM (17:00)
                        LocalTime actualOut = timeOut.isAfter(LocalTime.of(17, 0)) ? LocalTime.of(17, 0) : timeOut;

                        if (actualOut.isAfter(actualIn)) {
                            long minutesWorked = Duration.between(actualIn, actualOut).toMinutes();
                            //Subtract 1 hour (60 mins) for lunch
                            if (minutesWorked > 60) {
                                minutesWorked -= 60;
                            } else {
                                minutesWorked = 0;
                            }
                            totalHoursWorked += minutesWorked / 60.0;
                        }
                    }
                }
            }
        } catch (Exception e) {
            //Log specific error details instead of using empty catch blocks
            System.out.println("Error calculating hours for ID " + employeeId + ": " + e.getMessage());
        }
        return totalHoursWorked;
    }
    //Searches the employee database for a record matching the provided ID.
    //Returns a String array of employee data if found, or null if no match exists.
    public static String[] findEmployee(String id) {
        try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] employeeData = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                // Compare the ID column (index 0) with the target ID after cleaning quotes and spaces
                if (employeeData[0].replace("\"", "").trim().equals(id)) {
                    return employeeData; // Employee record found
                }
            }
        } catch (Exception e) {
            // Log an error message to the console if file reading or parsing fails
            System.out.println("Error finding employee record: " + e.getMessage());
        }
        return null;
    }

    // Determines the SSS contribution amount based on the employee's monthly gross salary
    // Uses a predefined table of salary brackets and corresponding contribution values
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
                return sssTable1[1]; //Return the fixed contribution for this bracket
            }
        }

    return 1125.00; // salary above 24,750
}
    // Calculates the monthly withholding tax based on the employee's taxable income
    // Applies a tiered tax bracket system to determine the correct tax due
    public static double computeTax(double taxableIncome) {
        if (taxableIncome <= 20832) return 0; // No tax for low income
        else if (taxableIncome < 33333) return (taxableIncome - 20833) * 0.20; // 20% bracket
        else if (taxableIncome < 66667) return 2500 + (taxableIncome - 33333) * 0.25; // 25% bracket
        else if (taxableIncome < 166667) return 10833 + (taxableIncome - 66667) * 0.30; // 30% bracket
        else if (taxableIncome < 666667) return 40833.33 + (taxableIncome - 166667) * 0.32; // 32% bracket
        else return 200833.33 + (taxableIncome - 666667) * 0.35; // 35% bracket
    }
}