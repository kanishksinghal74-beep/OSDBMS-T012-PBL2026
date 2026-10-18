package com.smartwarehouse;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.UUID;

public class PaymentFrame extends JFrame {

    public interface PaymentListener {
        boolean onPaymentConfirmed(
                String paymentMethod,
                String transactionId
        );
    }

    private final String paymentMethod;
    private final double total;
    private final PaymentListener listener;

    private final Color navy = new Color(19, 35, 69);
    private final Color blue = new Color(40, 100, 210);
    private final Color background = new Color(245, 247, 251);

    private final JPanel methodPanel = new JPanel(new BorderLayout(10, 10));
    private final JButton payButton = new JButton();
    private final JLabel message = new JLabel(" ");
    private boolean processing = false;

    private JTextField upiField;
    private JTextField cardNameField;
    private JTextField cardNumberField;
    private JTextField expiryField;
    private JPasswordField cvvField;
    private JComboBox<String> bankBox;

    public PaymentFrame(
            String paymentMethod,
            double total,
            PaymentListener listener
    ) {
        this.paymentMethod = paymentMethod;
        this.total = total;
        this.listener = listener;

        setTitle("Smart Warehouse | Secure Demo Checkout");
        setSize(760, 610);
        setMinimumSize(new Dimension(680, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(18, 18));
        root.setBackground(background);
        root.setBorder(new EmptyBorder(22, 26, 22, 26));

        JPanel header = new JPanel(new BorderLayout(5, 8));
        header.setBackground(navy);
        header.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("SMART WAREHOUSE");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 23));

        JLabel subtitle = new JLabel(
                "DEMO PAYMENT GATEWAY  |  No real money is transferred"
        );
        subtitle.setForeground(new Color(220, 230, 245));
        subtitle.setFont(new Font("Arial", Font.PLAIN, 12));

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(Box.createVerticalStrut(6));
        heading.add(subtitle);
        header.add(heading, BorderLayout.CENTER);

        JPanel summary = new JPanel(new BorderLayout(8, 8));
        summary.setBackground(Color.WHITE);
        summary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 236)),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel orderLabel = new JLabel("ORDER TOTAL");
        orderLabel.setForeground(Color.GRAY);
        orderLabel.setFont(new Font("Arial", Font.BOLD, 12));

        JLabel amount = new JLabel(String.format("\u20B9 %.2f", total));
        amount.setForeground(navy);
        amount.setFont(new Font("Arial", Font.BOLD, 26));

        JPanel amountPanel = new JPanel(new BorderLayout());
        amountPanel.setOpaque(false);
        amountPanel.add(orderLabel, BorderLayout.NORTH);
        amountPanel.add(amount, BorderLayout.SOUTH);

        JLabel demoBadge = new JLabel("  DEMO MODE  ");
        demoBadge.setOpaque(true);
        demoBadge.setBackground(new Color(255, 239, 204));
        demoBadge.setForeground(new Color(125, 75, 0));
        demoBadge.setFont(new Font("Arial", Font.BOLD, 12));

        summary.add(amountPanel, BorderLayout.CENTER);
        summary.add(demoBadge, BorderLayout.EAST);

        methodPanel.setBackground(Color.WHITE);
        methodPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 236)),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JLabel methodTitle = new JLabel(
                "Payment method: " + paymentMethod
        );
        methodTitle.setFont(new Font("Arial", Font.BOLD, 17));
        methodTitle.setForeground(navy);

        JPanel methodContent = new JPanel();
        methodContent.setLayout(new BoxLayout(methodContent, BoxLayout.Y_AXIS));
        methodContent.setBackground(Color.WHITE);
        methodContent.setBorder(new EmptyBorder(16, 0, 0, 0));

        if ("UPI".equalsIgnoreCase(paymentMethod)) {
            buildUPI(methodContent);
        } else if ("Debit Card".equalsIgnoreCase(paymentMethod)
                || "Credit Card".equalsIgnoreCase(paymentMethod)) {
            buildCard(methodContent);
        } else if ("Net Banking".equalsIgnoreCase(paymentMethod)) {
            buildNetBanking(methodContent);
        } else {
            JLabel cod = new JLabel(
                    "<html><b>Cash on Delivery</b><br><br>" +
                    "Payment will be collected at delivery.<br>" +
                    "No online payment will be processed.</html>"
            );
            methodContent.add(cod);
        }

        methodPanel.add(methodTitle, BorderLayout.NORTH);
        methodPanel.add(methodContent, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(10, 10));
        footer.setOpaque(false);

        message.setForeground(new Color(170, 45, 45));
        message.setFont(new Font("Arial", Font.PLAIN, 12));

        JButton cancel = new JButton("CANCEL");
        cancel.addActionListener(e -> dispose());

        payButton.setText(
                "Cash on Delivery".equalsIgnoreCase(paymentMethod)
                        ? "PLACE COD ORDER"
                        : "PAY NOW  \u2022  DEMO"
        );
        payButton.setFont(new Font("Arial", Font.BOLD, 14));
        payButton.setForeground(Color.WHITE);
        payButton.setBackground(blue);
        payButton.setFocusPainted(false);
        payButton.setBorder(new EmptyBorder(12, 20, 12, 20));
        payButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        payButton.addActionListener(e -> processPayment());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(payButton);

        footer.add(message, BorderLayout.CENTER);
        footer.add(buttons, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(14, 14));
        center.setOpaque(false);
        center.add(summary, BorderLayout.NORTH);
        center.add(methodPanel, BorderLayout.CENTER);

        root.add(header, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void buildUPI(JPanel panel) {
        JLabel note = new JLabel(
                "<html>Enter a demo UPI ID. Do not enter a real UPI PIN<br>" +
                "or any banking credentials.</html>"
        );

        upiField = new JTextField("demo@upi", 24);
        upiField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JPanel qr = new JPanel(new GridLayout(9, 9, 2, 2));
        qr.setBackground(Color.WHITE);
        qr.setPreferredSize(new Dimension(126, 126));
        qr.setMaximumSize(new Dimension(126, 126));
        qr.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Decorative QR-like pattern; it is NOT a scannable payment QR.
        java.util.Random random = new java.util.Random(42);
        for (int i = 0; i < 81; i++) {
            JPanel cell = new JPanel();
            cell.setBackground(random.nextBoolean()
                    ? Color.BLACK : Color.WHITE);
            qr.add(cell);
        }

        panel.add(note);
        panel.add(Box.createVerticalStrut(12));
        panel.add(new JLabel("Demo UPI ID"));
        panel.add(Box.createVerticalStrut(5));
        panel.add(upiField);
        panel.add(Box.createVerticalStrut(12));
        panel.add(new JLabel("Illustrative QR (not scannable)"));
        panel.add(Box.createVerticalStrut(6));
        panel.add(qr);
    }

    private void buildCard(JPanel panel) {
        JLabel warning = new JLabel(
                "<html>DEMO ONLY: Use dummy values. Never enter real card details.<br>" +
                "The values are not sent to a payment provider.</html>"
        );
        warning.setForeground(new Color(160, 70, 30));

        cardNameField = addField(panel, "Cardholder name", "Demo User");
        cardNumberField = addField(panel, "Dummy card number", "4111111111111111");
        expiryField = addField(panel, "Expiry (MM/YY)", "12/30");

        JLabel cvvLabel = new JLabel("Dummy CVV");
        cvvField = new JPasswordField("123", 18);
        cvvField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        panel.add(Box.createVerticalStrut(8));
        panel.add(cvvLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(cvvField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(warning);
    }

    private JTextField addField(
            JPanel panel,
            String label,
            String defaultValue
    ) {
        panel.add(new JLabel(label));
        panel.add(Box.createVerticalStrut(4));

        JTextField field = new JTextField(defaultValue, 25);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        panel.add(field);
        panel.add(Box.createVerticalStrut(9));
        return field;
    }

    private void buildNetBanking(JPanel panel) {
        panel.add(new JLabel("Choose a demo bank"));
        panel.add(Box.createVerticalStrut(8));

        bankBox = new JComboBox<>(new String[]{
                "Demo Bank of India",
                "Demo National Bank",
                "Demo City Bank",
                "Demo Cooperative Bank"
        });
        bankBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        panel.add(bankBox);
        panel.add(Box.createVerticalStrut(12));
        panel.add(new JLabel(
                "No real banking login or password is required."
        ));
    }

    private boolean validateDemoInputs() {
        if (total <= 0) {
            message.setText("Invalid order total.");
            return false;
        }

        if ("UPI".equalsIgnoreCase(paymentMethod)) {
            String upi = upiField.getText().trim();
            if (!upi.matches("[A-Za-z0-9._-]+@[A-Za-z0-9.-]+")) {
                message.setText("Enter a valid demo UPI ID, e.g. demo@upi.");
                return false;
            }
        }

        if ("Debit Card".equalsIgnoreCase(paymentMethod)
                || "Credit Card".equalsIgnoreCase(paymentMethod)) {
            String name = cardNameField.getText().trim();
            String number = cardNumberField.getText().replaceAll("\\s+", "");
            String expiry = expiryField.getText().trim();
            String cvv = new String(cvvField.getPassword()).trim();

            if (name.isEmpty()
                    || !number.matches("[0-9]{12,19}")
                    || !expiry.matches("(0[1-9]|1[0-2])/[0-9]{2}")
                    || !cvv.matches("[0-9]{3,4}")) {
                message.setText("Enter valid dummy card fields to continue.");
                return false;
            }
        }

        return true;
    }

    private void processPayment() {
        if (processing) {
            return;
        }

        if (!"Cash on Delivery".equalsIgnoreCase(paymentMethod)
                && !validateDemoInputs()) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "This is a DEMO transaction. No money will be transferred.\n" +
                "Continue with the order?",
                "Confirm Demo Payment",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        processing = true;
        payButton.setEnabled(false);
        payButton.setText("PROCESSING...");
        message.setForeground(blue);
        message.setText("Processing demo transaction...");

        Timer timer = new Timer(1200, e -> {
            ((Timer) e.getSource()).stop();

            String transactionId =
                    "DEMO-" + UUID.randomUUID()
                            .toString().substring(0, 8).toUpperCase();

            try {
                boolean saved = listener.onPaymentConfirmed(
                        paymentMethod,
                        transactionId
                );

                if (saved) {
                    JOptionPane.showMessageDialog(
                            this,
                            "ORDER CONFIRMED\n\n" +
                            "Demo Transaction ID: " + transactionId + "\n" +
                            "Amount: " + String.format("\u20B9 %.2f", total) +
                            "\n\nNo real money was transferred.",
                            "Smart Warehouse | Success",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    dispose();
                } else {
                    showFailure("The order could not be saved. Your cart was not cleared.");
                }
            } catch (Exception ex) {
                showFailure("Order failed: " + ex.getMessage());
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void showFailure(String text) {
        processing = false;
        payButton.setEnabled(true);
        payButton.setText(
                "Cash on Delivery".equalsIgnoreCase(paymentMethod)
                        ? "PLACE COD ORDER"
                        : "PAY NOW  \u2022  DEMO"
        );
        message.setForeground(new Color(170, 45, 45));
        message.setText(text);

        JOptionPane.showMessageDialog(
                this,
                text,
                "Order Not Completed",
                JOptionPane.ERROR_MESSAGE
        );
    }
}