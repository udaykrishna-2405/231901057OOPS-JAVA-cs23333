import javax.swing.*;
import java.awt.event.*;
import java.sql.*;

public class PersonalFinanceTracker extends JFrame {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/PersonalFinanceTracker";
    private static final String DB_USER = "root"; // Replace with your MySQL username
    private static final String DB_PASSWORD = "Password"; // Replace with your MySQL password

    public static void main(String[] args) {
        new LoginRegister();
    }

    // Database connection helper
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // Login and Registration Form
    static class LoginRegister extends JFrame {
        private JTextField txtUsername = new JTextField(20);
        private JPasswordField txtPassword = new JPasswordField(20);
        private JButton btnLogin = new JButton("Login");
        private JButton btnRegister = new JButton("Register");

        public LoginRegister() {
            setTitle("Login/Register");
            setSize(300, 150);
            setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

            add(new JLabel("Username:"));
            add(txtUsername);
            add(new JLabel("Password:"));
            add(txtPassword);
            add(btnLogin);
            add(btnRegister);

            btnLogin.addActionListener(e -> login());
            btnRegister.addActionListener(e -> register());

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setVisible(true);
        }

        private void login() {
            try (Connection conn = getConnection()) {
                String query = "SELECT * FROM Users WHERE username=? AND password=?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, txtUsername.getText());
                stmt.setString(2, new String(txtPassword.getPassword()));
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "Login successful!");
                    new Dashboard(rs.getInt("user_id"));
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid username or password.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void register() {
            try (Connection conn = getConnection()) {
                String query = "INSERT INTO Users (username, password) VALUES (?, ?)";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, txtUsername.getText());
                stmt.setString(2, new String(txtPassword.getPassword()));
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Registration successful!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Main Application Dashboard
    static class Dashboard extends JFrame {
        private int userId;

        public Dashboard(int userId) {
            this.userId = userId;
            setTitle("Dashboard");
            setSize(400, 300);
            setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

            JButton btnTransactions = new JButton("Manage Transactions");
            JButton btnBudget = new JButton("Set Budget");
            JButton btnReports = new JButton("View Reports");

            btnTransactions.addActionListener(e -> new TransactionManager(userId));
            btnBudget.addActionListener(e -> new BudgetManager(userId));
            btnReports.addActionListener(e -> new ViewReport(userId));

            add(btnTransactions);
            add(btnBudget);
            add(btnReports);

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setVisible(true);
        }
    }

    // Transaction Manager
    static class TransactionManager extends JFrame {
        private JComboBox<String> categoryCombo = new JComboBox<>();
        private JTextField txtAmount = new JTextField(10);
        private JButton btnAdd = new JButton("Add Transaction");

        public TransactionManager(int userId) {
            setTitle("Manage Transactions");
            setSize(300, 200);
            setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

            add(new JLabel("Category:"));
            loadCategories();
            add(categoryCombo);
            add(new JLabel("Amount:"));
            add(txtAmount);
            add(btnAdd);

            btnAdd.addActionListener(e -> addTransaction(userId));

            setVisible(true);
        }

        private void loadCategories() {
            try (Connection conn = getConnection()) {
                String query = "SELECT category_name FROM Categories";
                PreparedStatement stmt = conn.prepareStatement(query);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    categoryCombo.addItem(rs.getString("category_name"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void addTransaction(int userId) {
            try (Connection conn = getConnection()) {
                String query = "INSERT INTO Transactions (user_id, category_id, amount, transaction_date) VALUES (?, ?, ?, CURDATE())";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, userId);
                stmt.setInt(2, categoryCombo.getSelectedIndex() + 1); // Assuming categories are sequential
                stmt.setDouble(3, Double.parseDouble(txtAmount.getText()));
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Transaction added!");
                dispose();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Budget Manager
    static class BudgetManager extends JFrame {
        private JTextField txtMonth = new JTextField(10);
        private JTextField txtYear = new JTextField(10);
        private JTextField txtAmount = new JTextField(10);
        private JButton btnSet = new JButton("Set Budget");

        public BudgetManager(int userId) {
            setTitle("Set Monthly Budget");
            setSize(300, 200);
            setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

            add(new JLabel("Month (1-12):"));
            add(txtMonth);
            add(new JLabel("Year:"));
            add(txtYear);
            add(new JLabel("Amount:"));
            add(txtAmount);
            add(btnSet);

            btnSet.addActionListener(e -> setBudget(userId));

            setVisible(true);
        }

        private void setBudget(int userId) {
            try (Connection conn = getConnection()) {
                String query = "INSERT INTO Budgets (user_id, month, year, amount) VALUES (?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, userId);
                stmt.setInt(2, Integer.parseInt(txtMonth.getText()));
                stmt.setInt(3, Integer.parseInt(txtYear.getText()));
                stmt.setDouble(4, Double.parseDouble(txtAmount.getText()));
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Budget set!");
                dispose();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // View Report
    static class ViewReport extends JFrame {
        public ViewReport(int userId) {
            setTitle("View Report");
            setSize(400, 300);
            setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

            JTextArea reportArea = new JTextArea();
            reportArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(reportArea);
            add(scrollPane);

            try (Connection conn = getConnection()) {
                String query = "SELECT C.category_name, T.amount, T.transaction_date " +
                               "FROM Transactions T JOIN Categories C ON T.category_id = C.category_id " +
                               "WHERE T.user_id = ? ORDER BY T.transaction_date DESC";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();

                StringBuilder report = new StringBuilder("Date\tCategory\tAmount\n");
                while (rs.next()) {
                    report.append(rs.getDate("transaction_date")).append("\t")
                          .append(rs.getString("category_name")).append("\t")
                          .append(rs.getDouble("amount")).append("\n");
                }
                reportArea.setText(report.toString());
            } catch (SQLException e) {
                e.printStackTrace();
            }

            setVisible(true);
        }
    }
}