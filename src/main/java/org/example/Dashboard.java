package org.example;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.sql.*;

public class Dashboard extends JFrame {
    private static final String DB_URL = "jdbc:mariadb://localhost:3306/PASSWORD_MANAGER_APP";
    private static final String DB_USERNAME = "kagz03";
    private static final String DB_PASSWORD = "kennytelo";
    private String loggedInUser; // Store the currently logged-in user

    public Dashboard(String loggedInUser) {
        this.loggedInUser = loggedInUser; // Assign logged-in user
        setTitle("Password Manager - Welcome, " + loggedInUser);
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        add(panel);
        placeComponents(panel);
    }

    private void placeComponents(JPanel panel) {
        panel.setLayout(new GridLayout(3, 1)); // Set layout to a grid for vertical stacking

        JButton addButton = new JButton("Add New Password");
        addButton.addActionListener(e -> addNewPassword());

        JButton viewButton = new JButton("View Existing Passwords");
        viewButton.addActionListener(e -> viewPasswords());

        JButton updateButton = new JButton("Update Password");
        updateButton.addActionListener(e -> updatePassword());

        panel.add(addButton);
        panel.add(viewButton);
        panel.add(updateButton);
    }

    private void addNewPassword() {
        String website = JOptionPane.showInputDialog(this, "Enter App Name:");
        if (website == null || website.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "App Name cannot be empty!", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String username = JOptionPane.showInputDialog(this, "Enter Username:");
        if (username == null || username.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username cannot be empty!", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String password = JOptionPane.showInputDialog(this, "Enter Password:");
        if (password == null || !isPasswordStrong(password)) {
            JOptionPane.showMessageDialog(this, "Password must meet the required strength criteria.", "Weak Password", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
            String query = "INSERT INTO Passwords (app_name, username, password, user) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, website);
                stmt.setString(2, username);
                stmt.setString(3, password);
                stmt.setString(4, loggedInUser); // Associate password with the logged-in user
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Password added successfully!");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding password: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewPasswords() {
        JFrame viewFrame = new JFrame("View Passwords");
        viewFrame.setSize(500, 400);
        viewFrame.setLocationRelativeTo(null);

        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("App Name");
        model.addColumn("Username");
        model.addColumn("Password");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
            String query = "SELECT app_name, username, password FROM Passwords WHERE user = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, loggedInUser);
                ResultSet rs = stmt.executeQuery();

                if (!rs.isBeforeFirst()) { // Check if no records exist
                    JOptionPane.showMessageDialog(this, "You don't have any saved passwords. Please create one.", "No Records Found", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getString("app_name"),
                            rs.getString("username"),
                            rs.getString("password")
                    });
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error retrieving passwords: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTable passwordTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(passwordTable);

        JTextField searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(200, 30));
        JLabel searchLabel = new JLabel("Search: ");

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTable();
            }

            private void filterTable() {
                String searchTerm = searchField.getText().toLowerCase();
                TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
                passwordTable.setRowSorter(sorter);

                if (searchTerm.trim().isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchTerm));
                }
            }
        });

        JPanel searchPanel = new JPanel();
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);

        viewFrame.add(searchPanel, BorderLayout.NORTH);
        viewFrame.add(scrollPane, BorderLayout.CENTER);
        viewFrame.setVisible(true);
    }

    private void updatePassword() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
            // Fetch app names associated with the logged-in user
            String fetchAppsQuery = "SELECT app_name FROM Passwords WHERE user = ?";
            try (PreparedStatement fetchStmt = conn.prepareStatement(fetchAppsQuery)) {
                fetchStmt.setString(1, loggedInUser);
                ResultSet rs = fetchStmt.executeQuery();

                // Populate the JComboBox with app names
                DefaultComboBoxModel<String> appModel = new DefaultComboBoxModel<>();
                while (rs.next()) {
                    appModel.addElement(rs.getString("app_name"));
                }

                if (appModel.getSize() == 0) {
                    JOptionPane.showMessageDialog(this, "No passwords found to update.", "No Records Found", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                JComboBox<String> appDropdown = new JComboBox<>(appModel);
                int result = JOptionPane.showConfirmDialog(this, appDropdown, "Select App to Update", JOptionPane.OK_CANCEL_OPTION);

                if (result == JOptionPane.OK_OPTION) {
                    String selectedApp = (String) appDropdown.getSelectedItem();

                    // Fetch current username and password for the selected app
                    String fetchDetailsQuery = "SELECT username, password FROM Passwords WHERE app_name = ? AND user = ?";
                    try (PreparedStatement detailsStmt = conn.prepareStatement(fetchDetailsQuery)) {
                        detailsStmt.setString(1, selectedApp);
                        detailsStmt.setString(2, loggedInUser);
                        ResultSet detailsRs = detailsStmt.executeQuery();

                        if (detailsRs.next()) {
                            String currentUsername = detailsRs.getString("username");
                            String currentPassword = detailsRs.getString("password");

                            // Prompt user for new details
                            String newUsername = JOptionPane.showInputDialog(this, "Enter new Username:", currentUsername);
                            String newPassword = JOptionPane.showInputDialog(this, "Enter new Password:", currentPassword);

                            if (newUsername == null || newUsername.trim().isEmpty() || newPassword == null || !isPasswordStrong(newPassword)) {
                                JOptionPane.showMessageDialog(this, "Invalid username or password.", "Update Error", JOptionPane.ERROR_MESSAGE);
                                return;
                            }

                            // Update the password in the database
                            String updateQuery = "UPDATE Passwords SET username = ?, password = ? WHERE app_name = ? AND user = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                                updateStmt.setString(1, newUsername);
                                updateStmt.setString(2, newPassword);
                                updateStmt.setString(3, selectedApp);
                                updateStmt.setString(4, loggedInUser);

                                updateStmt.executeUpdate();
                                JOptionPane.showMessageDialog(this, "Password updated successfully!");
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating password: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private boolean isPasswordStrong(String password) {
        return password.length() >= 8; // Add your own password strength logic
    }
}
