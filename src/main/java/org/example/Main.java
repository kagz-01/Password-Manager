package org.example;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Use SwingUtilities to ensure thread safety for GUI creation
        SwingUtilities.invokeLater(() -> {
            LoginForm loginForm = new LoginForm();
            loginForm.setVisible(true); // Make the login form visible
        });
    }
}
