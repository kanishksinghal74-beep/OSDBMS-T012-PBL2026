package com.smartwarehouse;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.sql.Timestamp;

public class OrderTrackingFrame extends JFrame {

    private final OrderDAO orderDAO = new OrderDAO();
    private final int userId;

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a");

    private final DefaultTableModel model =
            new DefaultTableModel(
                    new String[]{
                            "Order ID",
                            "Total",
                            "Payment",
                            "Status",
                            "Order Date"
                    },
                    0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JTable table = new JTable(model);
    private final JTextArea details = new JTextArea();
    private final JLabel status =
            new JLabel("Loading your orders...");

    private int trackingOrderId = -1;
    private String detailsMode = "";

    private final Timer refreshTimer =
            new Timer(5000, e -> loadOrders(true));

    public OrderTrackingFrame(int userId) {

        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID.");
        }

        this.userId = userId;

        setTitle("Smart Warehouse - My Orders");
        setSize(950, 650);
        setMinimumSize(new Dimension(750, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        build();
        loadOrders(false);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                refreshTimer.stop();
            }
        });

        refreshTimer.start();
    }

    // =========================================
    // FORMAT ORDER DATE IN INDIA TIMEZONE
    // =========================================

    private String formatOrderDate(Timestamp timestamp) {

        if (timestamp == null) {
            return "";
        }

        /*
         * IMPORTANT:
         * This conversion assumes the Timestamp represents UTC.
         * If the database already stores Indian local time,
         * use timestamp.toLocalDateTime() directly instead.
         */
        LocalDateTime utcDateTime = LocalDateTime.ofInstant(
                timestamp.toInstant(),
                ZoneId.of("UTC")
        );

        return utcDateTime
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(INDIA_ZONE)
                .format(DATE_FORMATTER);
    }

    // =========================================
    // BUILD UI
    // =========================================

    private void build() {

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel header = new JPanel(new BorderLayout(10, 10));

        JLabel title = new JLabel("MY ORDERS & ORDER TRACKING");
        title.setFont(new Font("Arial", Font.BOLD, 22));

        JButton refresh = new JButton("REFRESH");

        header.add(title, BorderLayout.WEST);
        header.add(refresh, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);

        root.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton track = new JButton("TRACK SELECTED ORDER");
        JButton items = new JButton("VIEW ORDER ITEMS");

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionPanel.add(track);
        actionPanel.add(items);

        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setRows(8);
        details.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.add(actionPanel, BorderLayout.NORTH);
        bottom.add(new JScrollPane(details), BorderLayout.CENTER);
        bottom.add(status, BorderLayout.SOUTH);

        root.add(bottom, BorderLayout.SOUTH);

        refresh.addActionListener(e -> loadOrders(true));
        track.addActionListener(e -> trackOrder());
        items.addActionListener(e -> showOrderItems());

        setContentPane(root);
    }

    // =========================================
    // LOAD LOGGED-IN USER'S ORDERS
    // =========================================

    private void loadOrders(boolean preserveDetails) {

        int previouslySelectedOrderId = getSelectedOrderId();

        model.setRowCount(0);

        try {
            List<Order> orders =
                    orderDAO.getOrdersByUser(userId);

            for (Order order : orders) {

                if (order.getUserId() != userId) {
                    continue;
                }

                model.addRow(new Object[]{
                        order.getOrderId(),
                        String.format(
                                java.util.Locale.US,
                                "₹%.2f",
                                order.getTotalAmount()
                        ),
                        order.getPaymentMethod(),
                        order.getStatus(),
                        formatOrderDate(order.getOrderDate())
                });
            }

            if (previouslySelectedOrderId > 0) {
                selectOrderInTable(previouslySelectedOrderId);
            }

            status.setText(
                    "My Orders: " + model.getRowCount()
                    + " | Auto-refresh: every 5 seconds"
            );

            if (!preserveDetails) {
                trackingOrderId = -1;
                detailsMode = "";
                details.setText("");
            } else if ("tracking".equals(detailsMode)
                    && trackingOrderId > 0) {
                refreshTrackingDetails();
            }

        } catch (Exception ex) {
            status.setText("Error loading your orders.");

            if (!preserveDetails || details.getText().isEmpty()) {
                details.setText(
                        "Database Error:\n" + ex.getMessage()
                );
            }
        }
    }

    // =========================================
    // GET SELECTED ORDER ID
    // =========================================

    private int getSelectedOrderId() {

        int viewRow = table.getSelectedRow();

        if (viewRow < 0) {
            return -1;
        }

        int modelRow = table.convertRowIndexToModel(viewRow);

        return Integer.parseInt(
                model.getValueAt(modelRow, 0).toString()
        );
    }

    // =========================================
    // RESTORE TABLE SELECTION
    // =========================================

    private void selectOrderInTable(int orderId) {

        for (int modelRow = 0;
             modelRow < model.getRowCount();
             modelRow++) {

            int currentId = Integer.parseInt(
                    model.getValueAt(modelRow, 0).toString()
            );

            if (currentId == orderId) {
                int viewRow = table.convertRowIndexToView(modelRow);

                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                }

                return;
            }
        }
    }

    // =========================================
    // VERIFY ORDER OWNERSHIP
    // =========================================

    private Order getAuthorizedOrder(int orderId)
            throws java.sql.SQLException {

        Order order = orderDAO.getOrder(orderId);

        if (order == null || order.getUserId() != userId) {
            return null;
        }

        return order;
    }

    // =========================================
    // TRACK SELECTED ORDER
    // =========================================

    private void trackOrder() {

        int orderId = getSelectedOrderId();

        if (orderId < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select an order first.",
                    "Order Tracking",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        trackingOrderId = orderId;
        detailsMode = "tracking";

        refreshTrackingDetails();
    }

    // =========================================
    // REFRESH TRACKING DETAILS
    // =========================================

    private void refreshTrackingDetails() {

        if (trackingOrderId <= 0) {
            return;
        }

        try {
            Order order = getAuthorizedOrder(trackingOrderId);

            if (order == null) {
                trackingOrderId = -1;
                detailsMode = "";
                details.setText(
                        "This order was not found or is not available."
                );
                return;
            }

            String current = order.getStatus();

            if (current == null || current.trim().isEmpty()) {
                current = "UNKNOWN";
            }

            current = current.trim();

            StringBuilder tracking = new StringBuilder();

            tracking.append("ORDER TRACKING\n\n");
            tracking.append("Order ID: ")
                    .append(order.getOrderId()).append("\n");
            tracking.append("Order Date: ")
                    .append(formatOrderDate(order.getOrderDate()))
                    .append("\n\n");
            tracking.append("STATUS TIMELINE\n\n");

            String[] stages = {
                    "PLACED",
                    "PROCESSING",
                    "PICKING",
                    "PACKING",
                    "PACKED",
                    "ROBOT ASSIGNED",
                    "OUT FOR DELIVERY",
                    "DELIVERED",
                    "COMPLETED"
            };

            for (String stage : stages) {
                tracking.append(getStatusLine(stage, current));
            }

            tracking.append("\nCurrent Status: ")
                    .append(current);

            details.setText(tracking.toString());
            details.setCaretPosition(0);

        } catch (Exception ex) {
            details.setText(
                    "Tracking Error:\n" + ex.getMessage()
            );
        }
    }

    // =========================================
    // STATUS TIMELINE
    // =========================================

    private String getStatusLine(String step, String current) {

        if (step.equalsIgnoreCase(current)) {
            return "▶ " + step + "  <-- CURRENT\n";
        }

        if (isCompleted(step, current)) {
            return "✓ " + step + "\n";
        }

        return "○ " + step + "\n";
    }

    private boolean isCompleted(String step, String current) {

        String[] stages = {
                "PLACED",
                "PROCESSING",
                "PICKING",
                "PACKING",
                "PACKED",
                "ROBOT ASSIGNED",
                "OUT FOR DELIVERY",
                "DELIVERED",
                "COMPLETED"
        };

        int stepIndex = -1;
        int currentIndex = -1;

        for (int i = 0; i < stages.length; i++) {

            if (stages[i].equalsIgnoreCase(step)) {
                stepIndex = i;
            }

            if (stages[i].equalsIgnoreCase(current)) {
                currentIndex = i;
            }
        }

        return stepIndex >= 0
                && currentIndex >= 0
                && stepIndex < currentIndex;
    }

    // =========================================
    // VIEW ORDER ITEMS
    // =========================================

    private void showOrderItems() {

        int orderId = getSelectedOrderId();

        if (orderId < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select an order first.",
                    "Order Items",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            Order order = getAuthorizedOrder(orderId);

            if (order == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "This order is not available to your account.",
                        "Access Denied",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            List<String> items = orderDAO.getOrderItems(orderId);

            trackingOrderId = -1;
            detailsMode = "items";

            if (items.isEmpty()) {
                details.setText(
                        "No items found for Order ID " + orderId
                );
                return;
            }

            StringBuilder text = new StringBuilder();

            text.append("ORDER ITEMS\n\n");
            text.append("Order ID: ").append(orderId).append("\n\n");

            for (String item : items) {
                text.append("• ").append(item).append("\n");
            }

            details.setText(text.toString());
            details.setCaretPosition(0);

        } catch (Exception ex) {
            details.setText(
                    "Error loading order items:\n" + ex.getMessage()
            );
        }
    }
}