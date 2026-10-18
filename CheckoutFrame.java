package com.smartwarehouse;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CheckoutFrame extends JFrame {

    private final CartFrame cartFrame;
    private final int userId;
    private final OrderDAO orderDAO = new OrderDAO();

    private final JTextArea address = new JTextArea(4, 30);

    private final JComboBox<String> payment = new JComboBox<>(
            new String[]{
                    "UPI",
                    "Debit Card",
                    "Credit Card",
                    "Net Banking",
                    "Cash on Delivery"
            }
    );

    private final JComboBox<String> deliveryType = new JComboBox<>(
            new String[]{"Normal", "Urgent"}
    );

    private final JLabel totalLabel = new JLabel("Total: ₹0.00");

    private final Color navy = new Color(19, 35, 69);
    private final Color blue = new Color(40, 100, 210);

    private boolean orderInProgress = false;

    public CheckoutFrame(CartFrame cartFrame, int userId) {
        this.cartFrame = cartFrame;
        this.userId = userId;

        setTitle("Smart Warehouse | Checkout");
        setSize(720, 570);
        setMinimumSize(new Dimension(650, 520));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        build();
        updateTotal();
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout(18, 18));
        root.setBackground(new Color(245, 247, 251));
        root.setBorder(new EmptyBorder(22, 24, 22, 24));

        // HEADER
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(navy);
        header.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("SMART WAREHOUSE");
        title.setFont(new Font("Arial", Font.BOLD, 23));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("CHECKOUT  |  Review your order");
        subtitle.setForeground(new Color(215, 225, 240));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(title);
        heading.add(Box.createVerticalStrut(6));
        heading.add(subtitle);

        header.add(heading, BorderLayout.CENTER);

        // FORM
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 8, 10, 8);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        address.setLineWrap(true);
        address.setWrapStyleWord(true);
        address.setFont(new Font("Arial", Font.PLAIN, 14));

        JScrollPane addressScroll = new JScrollPane(address);
        addressScroll.setPreferredSize(new Dimension(340, 90));

        addFormRow(form, g, 0, "Delivery Address", addressScroll);
        addFormRow(form, g, 1, "Payment Method", payment);
        addFormRow(form, g, 2, "Delivery Type", deliveryType);

        totalLabel.setFont(new Font("Arial", Font.BOLD, 23));
        totalLabel.setForeground(navy);
        addFormRow(form, g, 3, "Amount Payable", totalLabel);

        JLabel notice = new JLabel(
                "<html><font color='#666666'>" +
                "Online payments use demo simulation only. " +
                "No actual money is transferred.</font></html>"
        );
        g.gridx = 0;
        g.gridy = 4;
        g.gridwidth = 2;
        form.add(notice, g);

        // BUTTONS
        JButton cancel = new JButton("CANCEL");
        JButton continueButton = new JButton("CONTINUE TO PAYMENT");

        continueButton.setFont(new Font("Arial", Font.BOLD, 13));
        continueButton.setForeground(Color.WHITE);
        continueButton.setBackground(blue);
        continueButton.setFocusPainted(false);
        continueButton.setBorder(new EmptyBorder(12, 18, 12, 18));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);
        bottom.add(cancel);
        bottom.add(continueButton);

        cancel.addActionListener(e -> dispose());
        continueButton.addActionListener(e -> openPaymentGateway(continueButton));

        root.add(header, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void addFormRow(
            JPanel form,
            GridBagConstraints g,
            int row,
            String label,
            Component component
    ) {
        g.gridwidth = 1;
        g.weightx = 0;

        g.gridx = 0;
        g.gridy = row;
        form.add(new JLabel(label), g);

        g.gridx = 1;
        g.weightx = 1;
        form.add(component, g);
    }

    private void updateTotal() {
        totalLabel.setText(
                String.format("\u20B9 %.2f", cartFrame.getGrandTotal())
        );
    }

    private void openPaymentGateway(JButton continueButton) {
        if (orderInProgress) {
            return;
        }

        String deliveryAddress = address.getText().trim();

        if (deliveryAddress.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter your delivery address.",
                    "Address Required",
                    JOptionPane.WARNING_MESSAGE
            );
            address.requestFocus();
            return;
        }

        if (userId <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Invalid user account.",
                    "User Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (cartFrame.cart == null || cartFrame.cart.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Your cart is empty.",
                    "Empty Cart",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        double total = cartFrame.getGrandTotal();

        if (total <= 0 || Double.isNaN(total)
                || Double.isInfinite(total)) {
            JOptionPane.showMessageDialog(
                    this,
                    "The order total is invalid.",
                    "Invalid Total",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        String method = (String) payment.getSelectedItem();
        String delivery = (String) deliveryType.getSelectedItem();

        if (method == null || delivery == null) {
            return;
        }

        // Prevent repeated checkout windows from this frame.
        continueButton.setEnabled(false);

        PaymentFrame paymentFrame = new PaymentFrame(
                method,
                total,
                (paymentMethod, transactionId) -> saveOrder(
                        deliveryAddress,
                        paymentMethod,
                        delivery,
                        total,
                        transactionId
                )
        );

        paymentFrame.setVisible(true);
        paymentFrame.setAlwaysOnTop(false);

        // Re-enable checkout if the payment window is cancelled.
        paymentFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                continueButton.setEnabled(true);
            }
        });
    }

    private boolean saveOrder(
            String deliveryAddress,
            String paymentMethod,
            String delivery,
            double total,
            String transactionId
    ) {
        if (orderInProgress) {
            return false;
        }

        if (cartFrame.cart == null || cartFrame.cart.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Your cart is empty. No order was created.",
                    "Order Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }

        orderInProgress = true;

        try {
            // Uses the method signature from your existing OrderDAO.
            int orderId = orderDAO.createOrder(
                    userId,
                    deliveryAddress,
                    total,
                    paymentMethod,
                    delivery,
                    cartFrame.cart
            );

            if (orderId <= 0) {
                throw new IllegalStateException(
                        "The database did not return a valid order ID."
                );
            }

            // Only clear the cart after createOrder returns successfully.
            cartFrame.cart.clear();
            cartFrame.refreshCart();

            String paymentStatus =
                    "Cash on Delivery".equalsIgnoreCase(paymentMethod)
                            ? "PAYMENT DUE ON DELIVERY"
                            : "DEMO PAYMENT CONFIRMED";

            JOptionPane.showMessageDialog(
                    this,
                    "Order ID: " + orderId +
                    "\nUser ID: " + userId +
                    "\nTotal: " + String.format("\u20B9 %.2f", total) +
                    "\nPayment: " + paymentMethod +
                    "\nPayment Status: " + paymentStatus +
                    "\nDelivery: " + delivery +
                    "\n\nDemo Transaction ID: " + transactionId +
                    "\n\nDelivery Address:\n" + deliveryAddress,
                    "Order Saved Successfully",
                    JOptionPane.INFORMATION_MESSAGE
            );

            dispose();
            return true;

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "The order could not be saved.\n" +
                    "Your cart has not been cleared by this error handler.\n\n" +
                    ex.getMessage(),
                    "Order Error",
                    JOptionPane.ERROR_MESSAGE
            );

            ex.printStackTrace();
            return false;

        } finally {
            orderInProgress = false;
        }
    }
}