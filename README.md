MO-IT101-Group13

MotorPH Payroll System Code

The MotorPH Payroll System is a simple Java-based tool designed to automate the calculation of employee salaries. It follows a procedural approach, focusing on a step-by-step MotorPH Basic Payroll Process Flow.

[Team Details and Contribution]

Jennifer T. Egana-Bawag

Responsible for creating the repository and consistently updating the Project Plan to ensure the team stays on track.
Catherine Castro

Responsible for the core Java programming, implementing the payroll logic, CSV data parsing, and deduction formulas.
Smith Gregorio

Served as the Code Reviewer to ensure accuracy and handled the final deployment/upload of the code to GitHub.
[Program Details]

Key Features:

Login & Security: Restricts access to authorized employees and payroll staff using a basic username and password check.

Data Processing: Reads employee information and attendance records directly from CSV files.

Work Hour Standardization:

Grace Period: Automatically treats clock-ins between 8:01 AM and 8:10 AM as 8:00 AM.

Shift Capping: Limits work hours to the official 8:00 AM – 5:00 PM shift to prevent unauthorized early-in or late-out pay.

Lunch Deduction: Subtracts exactly 1 hour for the mandatory unpaid lunch break.

Automated Calculations: * Computes Gross Salary based on hourly rates.

Calculates statutory deductions (SSS, PhilHealth, and Pag-IBIG).

Computes Withholding Tax using standard tax brackets.

Payslip Generation: Displays a clear breakdown of earnings and deductions for two cut-offs per month, showing the final Net Salary.
[Project Plan Link]

https://docs.google.com/spreadsheets/d/1OE-tjR3Vai38DAVIfPA3zwBzqeFtq6temDUgEKHwnaw/edit?usp=sharing
