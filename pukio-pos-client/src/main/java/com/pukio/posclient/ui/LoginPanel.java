package com.pukio.posclient.ui;

import com.pukio.posclient.client.AppServerClient;
import com.pukio.posclient.client.SessionContext;
import com.pukio.posclient.dto.LoginResponse;
import com.pukio.posclient.ui.common.SwingWorkerTask;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Login panel for user authentication.
 * 
 * TASK-E2-26e
 */
public class LoginPanel extends JPanel {

    private final AppServerClient client;
    private final MainFrame mainFrame;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JProgressBar progressBar;
    private JLabel errorLabel;

    public LoginPanel(AppServerClient client, MainFrame mainFrame) {
        this.client = client;
        this.mainFrame = mainFrame;

        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout()); // Center the form

        // Create form panel with MigLayout
        JPanel formPanel = new JPanel(new MigLayout("wrap 2, gap 10", "[right]10[300,fill]", "[]10[]10[]10[]10[]"));
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Iniciar Sesión - Pukio POS"),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // Username field
        formPanel.add(new JLabel("Usuario:"));
        usernameField = new JTextField();
        usernameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        formPanel.add(usernameField);

        // Password field
        formPanel.add(new JLabel("Contraseña:"));
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        formPanel.add(passwordField);

        // Login button
        formPanel.add(new JLabel("")); // Empty cell
        loginButton = new JButton("Iniciar Sesión");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginButton.setPreferredSize(new Dimension(200, 35));
        loginButton.addActionListener(e -> performLogin());
        formPanel.add(loginButton, "split 2");

        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(100, 35));
        progressBar.setVisible(false);
        formPanel.add(progressBar);

        // Error label (initially hidden)
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        formPanel.add(errorLabel, "span 2, center");

        // Add form panel to center
        add(formPanel);

        // Enter key triggers login
        passwordField.addActionListener(e -> performLogin());
    }

    /**
     * Perform login operation.
     */
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        // Validate inputs
        if (username.isEmpty() || password.isEmpty()) {
            showError("Por favor ingrese usuario y contraseña");
            return;
        }

        // Clear previous error
        errorLabel.setText(" ");

        // Execute login in background
        SwingWorkerTask.execute(
            () -> client.login(username, password),
            this::handleLoginSuccess,
            this::handleLoginError,
            loginButton,
            progressBar
        );
    }

    /**
     * Handle successful login.
     */
    private void handleLoginSuccess(LoginResponse response) {
        // Set session context
        SessionContext.setSession(
            response.getToken(),
            response.getUserId(),
            response.getUsername(),
            response.getRole(),
            response.getStoreId(),
            response.getStoreName(),
            response.getShiftId()
        );

        // Update main frame
        mainFrame.onLoginSuccess();

        // Clear password field
        passwordField.setText("");
        errorLabel.setText(" ");
    }

    /**
     * Handle login error.
     */
    private void handleLoginError(Exception e) {
        showError("Error de autenticación: " + e.getMessage());
    }

    /**
     * Show error message.
     */
    private void showError(String message) {
        errorLabel.setText(message);
    }

    /**
     * Reset the form.
     */
    public void reset() {
        usernameField.setText("");
        passwordField.setText("");
        errorLabel.setText(" ");
    }
}
