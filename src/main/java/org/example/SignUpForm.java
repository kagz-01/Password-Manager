package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

public class SignUpForm extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    private static final String DB_URL = "jdbc:mariadb://localhost:3306/PASSWORD_MANAGER_APP";
    private static final String DB_USERNAME = "kagz03";
    private static final String DB_PASSWORD = "kennytelo";

    public SignUpForm() {
        setTitle("Sign Up Form");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create panel for components
        JPanel panel = new JPanel(new GridBagLayout());
        add(panel);

        // Place components on the panel
        placeComponents(panel);
    }

    private void placeComponents(JPanel panel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel welcomeLabel = new JLabel("<html>WELCOME TO KAGZ PASSWORD MANAGER<br>" +
                "Please insert the following information</html>");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(welcomeLabel, gbc);

        gbc.gridwidth = 1;

        JLabel userLabel = new JLabel("New Username:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        panel.add(userLabel, gbc);

        usernameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        panel.add(usernameField, gbc);

        JLabel passwordLabel = new JLabel("Password:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END;
        panel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        panel.add(passwordField, gbc);

        JButton submitButton = new JButton("Submit");
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.LINE_START;
        panel.add(submitButton, gbc);

        submitButton.addActionListener(e -> handleSignUp());
    }

    private boolean validatePasswordSecurity(String password) {
        String upperCasePattern = ".*[A-Z].*";
        String lowerCasePattern = ".*[a-z].*";
        String digitPattern = ".*[0-9].*";
        String specialCharPattern = ".*[!@#$%^&*(),.?\":{}|<>].*";

        return password.length() >= 8 &&
                password.matches(upperCasePattern) &&
                password.matches(lowerCasePattern) &&
                password.matches(digitPattern) &&
                password.matches(specialCharPattern);
    }

    private void handleSignUp() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (!validatePasswordSecurity(password)) {
            JOptionPane.showMessageDialog(this, "Password must contain at least 8 characters, " +
                    "including uppercase, lowercase, digits, and special characters.", "Weak Password", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
            String checkQuery = "SELECT * FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "Oops!! There exists a similar username... Please be more creative!!");
                    return;
                }
            }

            String insertQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, password); // Ideally, hash the password

                insertStmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Sign-up successful! You can now log in.");
                new LoginForm().setVisible(true);
                dispose();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database connection error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SignUpForm form = new SignUpForm();
            form.setVisible(true);
        });
    }
}
