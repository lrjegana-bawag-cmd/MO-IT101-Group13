MO-IT101-Group13

MotorPH Payroll System Code

[Team Details and Contribution]

Jennifer T. Egana-Bawag
* Responsible for creating the repository and consistently updating the Project Plan to ensure the team stays on track.

Catherine Castro
* Responsible for the core Java programming, implementing the payroll logic, CSV data parsing, and deduction formulas.

Smith Gregorio
* Served as the Code Reviewer to ensure accuracy and handled the final deployment/upload of the code to GitHub.

Project Overview

The **MotorPH Payroll System** is a Java-based application designed to automate salary calculations, statutory deductions, and tax computations. The system follows a **Procedural Programming** approach, focusing on a structured sequence of methods to process employee data accurately.

### Key Features:

* **Automated Data Parsing:** Uses procedural methods to read and extract employee records from CSV files.
* **Statutory Deductions:** Accurate logic for SSS, PhilHealth, and Pag-IBIG based on the **MotorPH Mandated Government Deductions**.
* **Progressive Tax Computation:** Implements the latest BIR withholding tax brackets using conditional logic.
* **Dynamic Validation:** Future-proofed year validation using `java.time.Year` to ensure long-term system reliability.
* **Refined Documentation:** Includes advanced inline comments explaining the "Why" behind every mathematical formula.

### Folder Structure:

- `src/Resources/`: Contains the official Employee and Attendance CSV databases.
- `src/motorph/payroll/system/code/`: Contains the Java source code following the required package hierarchy.
- `docs/monitoring/`: Contains the `progress_log.txt` for activity tracking.

How to Run the Project

1. **Clone the Repository:** `git clone https://github.com/lrjegana-bawag-cmd/MO-IT101-Group13.git`
2. **Open in IDE:** Import the project into NetBeans or your preferred Java IDE.
3. **Verify Paths:** Ensure the `Resources` folder (Capital 'R') is inside the `src` directory.
4. **Execute:** Run the `MotorPHPayrollSystemCode.java` file.

[Project Plan Link]

https://docs.google.com/spreadsheets/d/1OE-tjR3Vai38DAVIfPA3zwBzqeFtq6temDUgEKHwnaw/edit?usp=sharing
