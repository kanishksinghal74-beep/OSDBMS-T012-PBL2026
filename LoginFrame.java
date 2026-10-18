
package com.smartwarehouse;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginFrame extends JFrame {

    // =====================================================
    // FONTS
    // =====================================================

    private static final Font TITLE_FONT =
            new Font("Arial", Font.BOLD, 30);

    private static final Font SUBTITLE_FONT =
            new Font("Arial", Font.BOLD, 18);

    private static final Font BUTTON_FONT =
            new Font("Arial", Font.BOLD, 16);

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public LoginFrame() {

        setTitle("Smart Warehouse Management System");
        setSize(850, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        buildMainUI();
    }

    // =====================================================
    // MAIN UI WITH WAREHOUSE BACKGROUND
    // =====================================================

    private void buildMainUI() {

        JPanel main = new JPanel(new BorderLayout()) {

            private final Image backgroundImage =
                    loadBackgroundImage();

            @Override
            protected void paintComponent(Graphics g) {

                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC
                );

                g2.setRenderingHint(
                        RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY
                );

                if (backgroundImage != null) {

                    g2.drawImage(
                            backgroundImage,
                            0, 0,
                            getWidth(), getHeight(),
                            this
                    );

                } else {

                    g2.setPaint(
                            new GradientPaint(
                                    0, 0,
                                    new Color(10, 25, 45),
                                    getWidth(), getHeight(),
                                    new Color(35, 65, 90)
                            )
                    );

                    g2.fillRect(
                            0, 0,
                            getWidth(), getHeight()
                    );
                }

                // Transparent dark overlay
                g2.setColor(new Color(0, 0, 0, 115));

                g2.fillRect(
                        0, 0,
                        getWidth(), getHeight()
                );

                g2.dispose();
            }
        };

        JPanel center = new JPanel();

        center.setLayout(
                new BoxLayout(center, BoxLayout.Y_AXIS)
        );

        center.setOpaque(false);

        center.setBorder(
                BorderFactory.createEmptyBorder(
                        90, 100, 90, 100
                )
        );

        // TITLE

        JLabel title = new JLabel(
                "SMART WAREHOUSE MANAGEMENT",
                SwingConstants.CENTER
        );

        title.setFont(TITLE_FONT);
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // SUBTITLE

        JLabel subtitle = new JLabel(
                "SELECT LOGIN",
                SwingConstants.CENTER
        );

        subtitle.setFont(SUBTITLE_FONT);
        subtitle.setForeground(Color.WHITE);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // USER LOGIN BUTTON

        JButton userButton =
                createLargeButton("USER LOGIN");

        userButton.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        // ADMIN LOGIN BUTTON

        JButton adminButton =
                createLargeButton("ADMIN LOGIN");

        adminButton.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        // BUTTON STYLING

        styleLoginButton(userButton);
        styleLoginButton(adminButton);

        // ADD COMPONENTS

        center.add(title);
        center.add(Box.createVerticalStrut(20));

        center.add(subtitle);
        center.add(Box.createVerticalStrut(55));

        center.add(userButton);
        center.add(Box.createVerticalStrut(25));

        center.add(adminButton);

        main.add(center, BorderLayout.CENTER);

        // BUTTON EVENTS

        userButton.addActionListener(
                e -> showUserLogin()
        );

        adminButton.addActionListener(
                e -> showAdminLogin()
        );

        setContentPane(main);
    }

    // =====================================================
    // LOAD BACKGROUND IMAGE
    // =====================================================

    private Image loadBackgroundImage() {

        try {

            java.net.URL imageURL =
                    getClass().getResource(
                            "/images/warehouse_bg.png"
                    );

            if (imageURL != null) {

                return new ImageIcon(imageURL).getImage();

            } else {

                System.err.println(
                        "Warehouse background image not found!"
                );

                System.err.println(
                        "Expected: src/main/resources/images/warehouse_bg.png"
                );
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return null;
    }

    // =====================================================
    // LARGE BUTTON
    // =====================================================

    private JButton createLargeButton(String text) {

        JButton button = new JButton(text);

        button.setFont(BUTTON_FONT);

        button.setPreferredSize(
                new Dimension(360, 65)
        );

        button.setMaximumSize(
                new Dimension(360, 65)
        );

        button.setMinimumSize(
                new Dimension(360, 65)
        );

        button.setFocusPainted(false);
        button.setOpaque(true);

        button.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        return button;
    }

    // =====================================================
    // BUTTON STYLING - BLACK TEXT
    // =====================================================

    private void styleLoginButton(JButton button) {

        // Light blue background for readable black text
        button.setBackground(
                new Color(180, 225, 245)
        );

        // USER LOGIN and ADMIN LOGIN text in black
        button.setForeground(Color.BLACK);

        button.setFont(
                new Font("Arial", Font.BOLD, 18)
        );

        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);

        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(80, 190, 255), 2
                        ),
                        BorderFactory.createEmptyBorder(
                                8, 20, 8, 20
                        )
                )
        );

        button.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );
    }

    // =====================================================
    // USER LOGIN DIALOG
    // =====================================================

    private void showUserLogin() {

        JDialog dialog =
                new JDialog(this, "User Login", true);

        dialog.setSize(500, 490);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        25, 35, 25, 35
                )
        );

        GridBagConstraints g = new GridBagConstraints();

        g.insets = new Insets(10, 10, 10, 10);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel(
                "USER LOGIN",
                SwingConstants.CENTER
        );

        title.setFont(
                new Font("Arial", Font.BOLD, 24)
        );

        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;

        panel.add(title, g);

        // Username

        g.gridy++;
        g.gridwidth = 1;
        g.gridx = 0;

        panel.add(new JLabel("Username"), g);

        JTextField usernameField = new JTextField(18);

        g.gridx = 1;

        panel.add(usernameField, g);

        // Password

        g.gridy++;
        g.gridx = 0;

        panel.add(new JLabel("Password"), g);

        JPasswordField passwordField =
                new JPasswordField(18);

        g.gridx = 1;

        panel.add(passwordField, g);

        // Email

        g.gridy++;
        g.gridx = 0;

        panel.add(new JLabel("Email"), g);

        JTextField emailField = new JTextField(18);

        g.gridx = 1;

        panel.add(emailField, g);

        // Login button

        JButton login =
                new JButton("LOGIN & SEND OTP");

        login.setFont(BUTTON_FONT);

        g.gridy++;
        g.gridx = 0;
        g.gridwidth = 2;

        panel.add(login, g);

        // Registration button

        JButton register =
                new JButton("REGISTER NEW USER");

        register.setFont(
                new Font("Arial", Font.PLAIN, 14)
        );

        g.gridy++;

        panel.add(register, g);

        // Back button

        JButton back = new JButton("BACK");

        g.gridy++;

        panel.add(back, g);

        // Login event

        login.addActionListener(e -> {

            String user =
                    usernameField.getText().trim();

            String pass =
                    new String(passwordField.getPassword());

            String email =
                    emailField.getText().trim();

            if (user.isEmpty()
                    || pass.isEmpty()
                    || email.isEmpty()) {

                JOptionPane.showMessageDialog(
                        dialog,
                        "Please enter Username, Password and Email.",
                        "Login",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            if (!isValidEmail(email)) {

                JOptionPane.showMessageDialog(
                        dialog,
                        "Please enter a valid email address.",
                        "Invalid Email",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            loginUserWithOtp(
                    user, pass, email, dialog
            );
        });

        passwordField.addActionListener(
                e -> login.doClick()
        );

        register.addActionListener(
                e -> showRegistration()
        );

        back.addActionListener(
                e -> dialog.dispose()
        );

        dialog.add(panel);
        dialog.setVisible(true);
    }

    // =====================================================
    // EMAIL VALIDATION
    // =====================================================

    private boolean isValidEmail(String email) {

        return email.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        );
    }

    // =====================================================
    // USER LOGIN + OTP
    // =====================================================

    private void loginUserWithOtp(
            String username,
            String password,
            String enteredEmail,
            JDialog loginDialog
    ) {

        String sql =
                "SELECT user_id, username, email " +
                "FROM users " +
                "WHERE username = ? " +
                "AND password = ? " +
                "AND role = 'USER'";

        try (
                Connection conn =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        conn.prepareStatement(sql)
        ) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {

                    JOptionPane.showMessageDialog(
                            loginDialog,
                            "Invalid username or password.",
                            "Login Failed",
                            JOptionPane.WARNING_MESSAGE
                    );

                    return;
                }

                int userId = rs.getInt("user_id");

                String name =
                        rs.getString("username");

                String dbEmail =
                        rs.getString("email");

                if (dbEmail == null
                        || !dbEmail.trim().equalsIgnoreCase(
                                enteredEmail.trim()
                        )) {

                    JOptionPane.showMessageDialog(
                            loginDialog,
                            "Email does not match our records.\n"
                                    + "Please enter your registered email.",
                            "Email Mismatch",
                            JOptionPane.ERROR_MESSAGE
                    );

                    return;
                }

                String otp = EmailSender.generateOtp();

                boolean sent =
                        EmailSender.sendOtpEmail(
                                enteredEmail.trim(), otp
                        );

                if (!sent) {

                    JOptionPane.showMessageDialog(
                            loginDialog,
                            "OTP send nahi hua.\n"
                                    + "Email configuration check karein.",
                            "Email Error",
                            JOptionPane.ERROR_MESSAGE
                    );

                    return;
                }

                JOptionPane.showMessageDialog(
                        loginDialog,
                        "OTP sent successfully to:\n"
                                + enteredEmail,
                        "OTP Sent",
                        JOptionPane.INFORMATION_MESSAGE
                );

                showOtpDialog(
                        loginDialog,
                        userId,
                        name,
                        enteredEmail.trim(),
                        otp
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    loginDialog,
                    "Database Error:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // =====================================================
    // OTP VERIFICATION DIALOG
    // =====================================================

    private void showOtpDialog(
            JDialog loginDialog,
            int userId,
            String name,
            String email,
            String otp
    ) {

        JDialog otpDialog =
                new JDialog(
                        loginDialog,
                        "OTP Verification",
                        true
                );

        otpDialog.setSize(430, 320);
        otpDialog.setLocationRelativeTo(loginDialog);
        otpDialog.setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        20, 30, 20, 30
                )
        );

        GridBagConstraints g = new GridBagConstraints();

        g.insets = new Insets(10, 10, 10, 10);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel(
                "OTP VERIFICATION",
                SwingConstants.CENTER
        );

        title.setFont(
                new Font("Arial", Font.BOLD, 20)
        );

        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;

        panel.add(title, g);

        JLabel info = new JLabel(
                "<html><center>OTP sent to:<br>"
                        + "<b>" + email + "</b><br>"
                        + "<small>Check your inbox / spam</small>"
                        + "</center></html>",
                SwingConstants.CENTER
        );

        g.gridy++;

        panel.add(info, g);

        g.gridy++;
        g.gridwidth = 1;
        g.gridx = 0;

        panel.add(new JLabel("Enter OTP"), g);

        JTextField otpField = new JTextField(10);

        otpField.setFont(
                new Font("Arial", Font.BOLD, 22)
        );

        otpField.setHorizontalAlignment(
                JTextField.CENTER
        );

        g.gridx = 1;

        panel.add(otpField, g);

        JButton verify =
                new JButton("VERIFY & LOGIN");

        verify.setFont(BUTTON_FONT);

        g.gridy++;
        g.gridx = 0;
        g.gridwidth = 2;

        panel.add(verify, g);

        JButton resend = new JButton("RESEND OTP");

        g.gridy++;

        panel.add(resend, g);

        final String[] currentOtp = {otp};

        verify.addActionListener(e -> {

            String entered =
                    otpField.getText().trim();

            if (entered.isEmpty()) {

                JOptionPane.showMessageDialog(
                        otpDialog,
                        "Please enter the OTP.",
                        "OTP Required",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            if (entered.equals(currentOtp[0])) {

                otpDialog.dispose();
                loginDialog.dispose();
                dispose();

                new UserDashboard(
                        userId, name
                ).setVisible(true);

            } else {

                JOptionPane.showMessageDialog(
                        otpDialog,
                        "Incorrect OTP. Please try again.",
                        "Invalid OTP",
                        JOptionPane.ERROR_MESSAGE
                );

                otpField.setText("");
                otpField.requestFocus();
            }
        });

        otpField.addActionListener(
                e -> verify.doClick()
        );

        resend.addActionListener(e -> {

            String newOtp =
                    EmailSender.generateOtp();

            boolean sent =
                    EmailSender.sendOtpEmail(
                            email, newOtp
                    );

            if (sent) {

                currentOtp[0] = newOtp;

                otpField.setText("");

                JOptionPane.showMessageDialog(
                        otpDialog,
                        "New OTP sent to:\n" + email,
                        "OTP Resent",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else {

                JOptionPane.showMessageDialog(
                        otpDialog,
                        "OTP resend failed.\n"
                                + "Check email configuration.",
                        "Email Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        otpDialog.add(panel);
        otpDialog.setVisible(true);
    }

    // =====================================================
    // ADMIN LOGIN DIALOG
    // =====================================================

    private void showAdminLogin() {

        JDialog dialog =
                new JDialog(this, "Admin Login", true);

        dialog.setSize(500, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        25, 35, 25, 35
                )
        );

        GridBagConstraints g = new GridBagConstraints();

        g.insets = new Insets(10, 10, 10, 10);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel(
                "ADMIN LOGIN",
                SwingConstants.CENTER
        );

        title.setFont(
                new Font("Arial", Font.BOLD, 24)
        );

        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;

        panel.add(title, g);

        g.gridy++;
        g.gridwidth = 1;
        g.gridx = 0;

        panel.add(new JLabel("Admin Username"), g);

        JTextField usernameField = new JTextField(18);

        g.gridx = 1;

        panel.add(usernameField, g);

        g.gridy++;
        g.gridx = 0;

        panel.add(new JLabel("Password"), g);

        JPasswordField passwordField =
                new JPasswordField(18);

        g.gridx = 1;

        panel.add(passwordField, g);

        JButton login = new JButton("ADMIN LOGIN");

        login.setFont(BUTTON_FONT);

        g.gridy++;
        g.gridx = 0;
        g.gridwidth = 2;

        panel.add(login, g);

        JButton back = new JButton("BACK");

        g.gridy++;

        panel.add(back, g);

        login.addActionListener(e -> {

            String user =
                    usernameField.getText().trim();

            String pass =
                    new String(passwordField.getPassword());

            if (user.isEmpty() || pass.isEmpty()) {

                JOptionPane.showMessageDialog(
                        dialog,
                        "Please enter admin username and password.",
                        "Admin Login",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            loginAdmin(user, pass, dialog);
        });

        passwordField.addActionListener(
                e -> login.doClick()
        );

        back.addActionListener(
                e -> dialog.dispose()
        );

        dialog.add(panel);
        dialog.setVisible(true);
    }

    // =====================================================
    // ADMIN LOGIN DATABASE
    // =====================================================

    private void loginAdmin(
            String username,
            String password,
            JDialog dialog
    ) {

        String sql =
                "SELECT user_id FROM users " +
                "WHERE username = ? " +
                "AND password = ? " +
                "AND role = 'ADMIN'";

        try (
                Connection conn =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        conn.prepareStatement(sql)
        ) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {

                    JOptionPane.showMessageDialog(
                            dialog,
                            "Invalid admin username or password.",
                            "Admin Login Failed",
                            JOptionPane.WARNING_MESSAGE
                    );

                    return;
                }

                dialog.dispose();
                dispose();

                new AdminDashboard().setVisible(true);
            }

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    dialog,
                    "Database Error:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // =====================================================
    // REGISTRATION DIALOG
    // =====================================================

    private void showRegistration() {

        JDialog dialog =
                new JDialog(
                        this,
                        "User Registration",
                        true
                );

        dialog.setSize(520, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        25, 35, 25, 35
                )
        );

        GridBagConstraints g = new GridBagConstraints();

        g.insets = new Insets(9, 9, 9, 9);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel(
                "CREATE USER ACCOUNT",
                SwingConstants.CENTER
        );

        title.setFont(
                new Font("Arial", Font.BOLD, 23)
        );

        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;

        panel.add(title, g);

        // Full name

        g.gridy++;
        g.gridwidth = 1;
        g.gridx = 0;

        panel.add(new JLabel("Full Name"), g);

        JTextField nameField = new JTextField(18);

        g.gridx = 1;

        panel.add(nameField, g);

        // Username

        g.gridy++;
        g.gridx = 0;

        panel.add(new JLabel("Username"), g);

        JTextField usernameField = new JTextField(18);

        g.gridx = 1;

        panel.add(usernameField, g);

        // Email

        g.gridy++;
        g.gridx = 0;

        panel.add(new JLabel("Email"), g);

        JTextField emailField = new JTextField(18);

        g.gridx = 1;

        panel.add(emailField, g);

        // Password

        g.gridy++;
        g.gridx = 0;

        panel.add(new JLabel("Password"), g);

        JPasswordField passwordField =
                new JPasswordField(18);

        g.gridx = 1;

        panel.add(passwordField, g);

        // Confirm password

        g.gridy++;
        g.gridx = 0;

        panel.add(new JLabel("Confirm Password"), g);

        JPasswordField confirmField =
                new JPasswordField(18);

        g.gridx = 1;

        panel.add(confirmField, g);

        JButton register =
                new JButton("CREATE ACCOUNT");

        register.setFont(BUTTON_FONT);

        g.gridy++;
        g.gridx = 0;
        g.gridwidth = 2;

        panel.add(register, g);

        JButton cancel = new JButton("CANCEL");

        g.gridy++;

        panel.add(cancel, g);

        register.addActionListener(e -> {

            String fullName =
                    nameField.getText().trim();

            String user =
                    usernameField.getText().trim();

            String email =
                    emailField.getText().trim();

            String pass =
                    new String(passwordField.getPassword());

            String confirmPass =
                    new String(confirmField.getPassword());

            if (fullName.isEmpty()
                    || user.isEmpty()
                    || email.isEmpty()
                    || pass.isEmpty()
                    || confirmPass.isEmpty()) {

                JOptionPane.showMessageDialog(
                        dialog,
                        "Please fill all fields.",
                        "Registration",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            if (!isValidEmail(email)) {

                JOptionPane.showMessageDialog(
                        dialog,
                        "Please enter a valid email address.",
                        "Invalid Email",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            if (!pass.equals(confirmPass)) {

                JOptionPane.showMessageDialog(
                        dialog,
                        "Passwords do not match.",
                        "Registration",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            if (pass.length() < 4) {

                JOptionPane.showMessageDialog(
                        dialog,
                        "Password must be at least 4 characters.",
                        "Registration",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            registerUser(
                    fullName,
                    user,
                    email,
                    pass,
                    dialog
            );
        });

        cancel.addActionListener(
                e -> dialog.dispose()
        );

        dialog.add(panel);
        dialog.setVisible(true);
    }

    // =====================================================
    // REGISTER USER DATABASE
    // =====================================================

    private void registerUser(
            String name,
            String username,
            String email,
            String password,
            JDialog dialog
    ) {

        String checkSql =
                "SELECT user_id FROM users " +
                "WHERE username = ?";

        String insertSql =
                "INSERT INTO users " +
                "(full_name, username, email, password, role) " +
                "VALUES (?, ?, ?, ?, 'USER')";

        try (
                Connection conn =
                        DBConnection.getConnection()
        ) {

            // Check username

            try (
                    PreparedStatement check =
                            conn.prepareStatement(checkSql)
            ) {

                check.setString(1, username);

                try (ResultSet rs = check.executeQuery()) {

                    if (rs.next()) {

                        JOptionPane.showMessageDialog(
                                dialog,
                                "Username already exists.",
                                "Registration Failed",
                                JOptionPane.WARNING_MESSAGE
                        );

                        return;
                    }
                }
            }

            // Insert user

            try (
                    PreparedStatement insert =
                            conn.prepareStatement(insertSql)
            ) {

                insert.setString(1, name);
                insert.setString(2, username);
                insert.setString(3, email);
                insert.setString(4, password);

                insert.executeUpdate();
            }

            // Welcome email

            EmailSender.sendWelcomeEmail(email, name);

            JOptionPane.showMessageDialog(
                    dialog,
                    "Account created successfully!\n"
                            + "Welcome email sent to: "
                            + email + "\n"
                            + "You can now login.",
                    "Registration Successful",
                    JOptionPane.INFORMATION_MESSAGE
            );

            dialog.dispose();

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    dialog,
                    "Registration Error:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // =====================================================
    // MAIN
    // =====================================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(
                () -> new LoginFrame().setVisible(true)
        );
    }
}
