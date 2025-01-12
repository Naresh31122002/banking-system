import java.sql.*;
import java.util.Scanner;

// MySQL Connector
class MySQLConnector {
    private static final String URL = "jdbc:mysql://localhost:3306/LoanManagement";
    private static final String USER = "root";  // Replace with your MySQL username
    private static final String PASSWORD = "NareshR31#";  // Replace with your MySQL password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}

// Account Management Class
class AccountManager {
    public void createAccount() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Name: ");
        String name = scanner.nextLine();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();
        System.out.print("Enter DOB (YYYY-MM-DD): ");
        String dob = scanner.nextLine();
        System.out.print("Enter Phone Number: ");
        String phone = scanner.nextLine();
        System.out.print("Enter Address: ");
        String address = scanner.nextLine();
        System.out.println("Select Account Type: 1. Saving 2. Current");
        int accountTypeChoice = scanner.nextInt();
        String accountType = (accountTypeChoice == 1) ? "saving" : "current";

        String query = "INSERT INTO accounts (name, password, dob, phone, address, account_type) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = MySQLConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setString(2, password);
            pstmt.setString(3, dob);
            pstmt.setString(4, phone);
            pstmt.setString(5, address);
            pstmt.setString(6, accountType);
            pstmt.executeUpdate();
            System.out.println("Account created successfully!");
        } catch (SQLException e) {
            System.out.println("Error creating account: " + e.getMessage());
        }
    }

    public boolean login() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Username: ");
        String username = scanner.nextLine();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        String query = "SELECT account_number FROM accounts WHERE name = ? AND password = ?";
        try (Connection conn = MySQLConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Login successful. Account Number: " + rs.getInt("account_number"));
                    return true;
                } else {
                    System.out.println("Invalid username or password.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error during login: " + e.getMessage());
        }
        return false;
    }
}

// Loan Management Class
class LoanManager {
    public void applyForLoan(int accountNumber) {
        Scanner scanner = new Scanner(System.in);

        // Ask for the user's credit score
        System.out.print("Enter your credit score: ");
        int creditScore = scanner.nextInt();

        // Check if credit score is less than 750
        if (creditScore < 750) {
            System.out.println("Your credit score is " + creditScore + ". Loan application rejected.");
            return;
        }

        // Proceed with loan application if credit score is sufficient
        System.out.println("Select Loan Type: 1. Vehicle 2. Education 3. Personal 4. Home");
        int loanTypeChoice = scanner.nextInt();
        String loanType = switch (loanTypeChoice) {
            case 1 -> "vehicle";
            case 2 -> "education";
            case 3 -> "personal";
            case 4 -> "home";
            default -> throw new IllegalArgumentException("Invalid loan type.");
        };

        System.out.print("Enter Loan Amount: ");
        double loanAmount = scanner.nextDouble();

        String query = "INSERT INTO loans (account_number, loan_type, loan_amount, interest, status) VALUES (?, ?, ?, ?, 'pending')";
        try (Connection conn = MySQLConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, accountNumber);
            pstmt.setString(2, loanType);
            pstmt.setDouble(3, loanAmount);
            pstmt.setDouble(4, 0.0);  // Initial interest is set to 0.0
            pstmt.executeUpdate();
            System.out.println("Loan request submitted successfully.");
        } catch (SQLException e) {
            System.out.println("Error applying for loan: " + e.getMessage());
        }
    }

    public void selectInterestRate() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Loan ID to update interest: ");
        int loanId = scanner.nextInt();

        // Fetch the loan details
        String query = "SELECT loan_amount, interest FROM loans WHERE loan_id = ?";
        try (Connection conn = MySQLConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, loanId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    double loanAmount = rs.getDouble("loan_amount");
                    double interestRate = 0;
                    System.out.println("Select Interest Rate Type:");
                    System.out.println("1. Fixed Rate");
                    System.out.println("2. Variable Rate");
                    System.out.print("Enter your choice: ");
                    int choice = scanner.nextInt();

                    switch (choice) {
                        case 1 -> interestRate = loanAmount * 0.05;  // Fixed rate is 5% (0.05)
                        case 2 -> {
                            System.out.print("Enter the Variable Interest Rate (in percentage): ");
                            interestRate = loanAmount * (scanner.nextDouble() / 100);  // Convert to decimal
                        }
                        default -> System.out.println("Invalid choice.");
                    }

                    // Update interest in the loan
                    updateLoanInterestRate(loanId, interestRate);
                } else {
                    System.out.println("Loan ID not found.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error updating loan interest rate: " + e.getMessage());
        }
    }

    // Helper method to update interest rate in the loans table
    private void updateLoanInterestRate(int loanId, double interestRate) {
        String query = "UPDATE loans SET interest = ? WHERE loan_id = ?";
        try (Connection conn = MySQLConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDouble(1, interestRate);
            pstmt.setInt(2, loanId);
            pstmt.executeUpdate();
            System.out.println("Loan interest rate updated successfully.");
        } catch (SQLException e) {
            System.out.println("Error updating loan interest rate: " + e.getMessage());
        }
    }

    public void viewUserLoans() {
        String query = "SELECT * FROM loans";
        try (Connection conn = MySQLConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                System.out.printf("Loan ID: %d, Account: %d, Type: %s, Amount: %.2f, Interest: %.2f, Status: %s%n",
                        rs.getInt("loan_id"), rs.getInt("account_number"), rs.getString("loan_type"),
                        rs.getDouble("loan_amount"), rs.getDouble("interest"), rs.getString("status"));
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving loans: " + e.getMessage());
        }
    }

    public void approveLoan() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Loan ID: ");
        int loanId = scanner.nextInt();
        System.out.print("Enter Approval Status (approved/rejected): ");
        String status = scanner.next();

        String query = "UPDATE loans SET status = ? WHERE loan_id = ?";
        try (Connection conn = MySQLConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, loanId);
            pstmt.executeUpdate();
            System.out.println("Loan status updated successfully.");
        } catch (SQLException e) {
            System.out.println("Error updating loan status: " + e.getMessage());
        }
    }
}

// Transaction History Class
class TransactionHistory {
    public void viewTransactionHistory(int accountNumber) {
        String query = "SELECT * FROM loans WHERE account_number = ?";
        try (Connection conn = MySQLConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, accountNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("Loan ID: %d, Type: %s, Amount: %.2f, Interest: %.2f, Status: %s%n",
                            rs.getInt("loan_id"), rs.getString("loan_type"),
                            rs.getDouble("loan_amount"), rs.getDouble("interest"), rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving transaction history: " + e.getMessage());
        }
    }
}

// Main Program Class
public class LoanManagementSystem {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        AccountManager accountManager = new AccountManager();
        LoanManager loanManager = new LoanManager();
        TransactionHistory transactionHistory = new TransactionHistory();

        while (true) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1. Bank Manager");
            System.out.println("2. User");
            System.out.println("3. Exit");
            System.out.print("Select an option: ");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1 -> {
                    boolean exitAdminMenu = false;
                    while (!exitAdminMenu) {
                        System.out.println("\n--- Manager Menu ---");
                        System.out.println("1. View All User Loans");
                        System.out.println("2. Set Interest Rate");
                        System.out.println("3. Approve Loan");
                        System.out.println("4. Back to Main Menu");
                        System.out.print("Select an option: ");
                        int adminChoice = scanner.nextInt();
                        switch (adminChoice) {
                            case 1 -> loanManager.viewUserLoans();
                            case 2 -> loanManager.selectInterestRate();
                            case 3 -> loanManager.approveLoan();
                            case 4 -> exitAdminMenu = true;
                            default -> System.out.println("Invalid choice.");
                        }
                    }
                }
                case 2 -> {
                    boolean exitUserMenu = false;
                    boolean loggedIn = false;
                    int accountNumber = -1;
                    while (!exitUserMenu) {
                        System.out.println("\n--- User Menu ---");
                        System.out.println("1. Create Account");
                        System.out.println("2. Login");
                        System.out.println("3. Apply for Loan");
                        System.out.println("4. View Loan History");
                        System.out.println("5. Back to Main Menu");
                        System.out.print("Select an option: ");
                        int userChoice = scanner.nextInt();
                        switch (userChoice) {
                            case 1 -> accountManager.createAccount();
                            case 2 -> {
                                loggedIn = accountManager.login();
                                if (loggedIn) {
                                    System.out.print("Enter your account number: ");
                                    accountNumber = scanner.nextInt();
                                }
                            }
                            case 3 -> {
                                if (loggedIn) loanManager.applyForLoan(accountNumber);
                                else System.out.println("Please login first.");
                            }
                            case 4 -> {
                                if (loggedIn) transactionHistory.viewTransactionHistory(accountNumber);
                                else System.out.println("Please login first.");
                            }
                            case 5 -> exitUserMenu = true;
                            default -> System.out.println("Invalid choice.");
                        }
                    }
                }
                case 3 -> {
                    System.out.println("Exiting the system. Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
}
