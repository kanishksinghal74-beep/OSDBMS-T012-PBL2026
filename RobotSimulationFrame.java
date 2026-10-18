
package com.smartwarehouse;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RobotSimulationFrame extends JFrame {

    // =========================================================
    // THEME
    // =========================================================

    private static final Color BG = new Color(243, 246, 251);
    private static final Color CARD = Color.WHITE;
    private static final Color NAVY = new Color(20, 34, 62);
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color GREEN = new Color(5, 150, 105);
    private static final Color ORANGE = new Color(217, 119, 6);
    private static final Color RED = new Color(220, 38, 38);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color BORDER = new Color(226, 232, 240);

    private static final Font TITLE_FONT =
            new Font("Segoe UI", Font.BOLD, 25);

    private static final Font HEADING_FONT =
            new Font("Segoe UI", Font.BOLD, 15);

    private static final Font BODY_FONT =
            new Font("Segoe UI", Font.PLAIN, 13);

    private static final Font VALUE_FONT =
            new Font("Segoe UI", Font.BOLD, 16);

    // =========================================================
    // ANIMATION
    // 120 ms per pixel = approximately 89 seconds
    // for a 740-pixel journey.
    // =========================================================

    private int x = 80;
    private final int robotY = 325;

    private final Timer timer;

    private boolean running = false;
    private boolean deliveryStarted = false;
    private boolean deliveryCompleted = false;

    private long lastTelemetryUpdate = 0;

    private static final int START_X = 80;
    private static final int END_X = 820;
    private static final int STEP = 1;
    private static final int TIMER_DELAY_MS = 1000;

    // =========================================================
    // ORDER DATA
    // =========================================================

    private int orderId = -1;
    private String productName = "";
    private int quantity = 0;
    private String location = "";
    private String currentStatus = "WAITING";

    // =========================================================
    // DATABASE
    // =========================================================

    private final OrderDAO orderDAO = new OrderDAO();
    private final RobotDAO robotDAO = new RobotDAO();

    private int assignedRobotId = -1;

    // =========================================================
    // UI COMPONENTS
    // =========================================================

    private JLabel orderValue;
    private JLabel productValue;
    private JLabel quantityValue;
    private JLabel locationValue;
    private JLabel statusValue;
    private JLabel robotValue;
    private JLabel batteryValue;
    private JLabel positionValue;
    private JLabel connectionValue;
    private JLabel stageValue;

    private JProgressBar progressBar;

    private JButton startButton;
    private JButton stopButton;
    private JButton completeButton;

    private WarehousePanel warehousePanel;

    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    public RobotSimulationFrame() {

        setTitle("Smart Warehouse | Autonomous Robot Control");
        setSize(1180, 760);
        setMinimumSize(new Dimension(980, 650));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        buildUI();

        timer = new Timer(TIMER_DELAY_MS, e -> moveRobot());
        timer.setCoalesce(true);

        updateDashboard();
    }

    public RobotSimulationFrame(int orderId) {
        this();

        this.orderId = orderId;
        loadOrderData();
    }

    // =========================================================
    // BUILD UI
    // =========================================================

    private void buildUI() {

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);

        root.add(createHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(18, 20, 18, 20));

        content.add(createSummaryCards(), BorderLayout.NORTH);
        content.add(createMainContent(), BorderLayout.CENTER);
        content.add(createControlPanel(), BorderLayout.SOUTH);

        root.add(content, BorderLayout.CENTER);

        setContentPane(root);
    }

    // =========================================================
    // HEADER
    // =========================================================

    private JPanel createHeader() {

        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(18, 24, 18, 24));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("SMART WAREHOUSE");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 23));

        JLabel subtitle = new JLabel(
                "AUTONOMOUS MOBILE ROBOT  /  CONTROL CENTER"
        );
        subtitle.setForeground(new Color(190, 205, 228));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        titles.add(title);
        titles.add(Box.createVerticalStrut(5));
        titles.add(subtitle);

        JPanel connection = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        connection.setOpaque(false);

        connectionValue = new JLabel("● SYSTEM READY");
        connectionValue.setForeground(new Color(110, 231, 183));
        connectionValue.setFont(new Font("Segoe UI", Font.BOLD, 12));

        connection.add(connectionValue);

        header.add(titles, BorderLayout.CENTER);
        header.add(connection, BorderLayout.EAST);

        return header;
    }

    // =========================================================
    // SUMMARY CARDS
    // =========================================================

    private JPanel createSummaryCards() {

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setOpaque(false);

        cards.add(createMetricCard(
                "ORDER ID", "—", BLUE, v -> orderValue = v
        ));

        cards.add(createMetricCard(
                "ROBOT STATUS", "WAITING", ORANGE, v -> statusValue = v
        ));

        cards.add(createMetricCard(
                "BATTERY LEVEL", "100%", GREEN, v -> batteryValue = v
        ));

        cards.add(createMetricCard(
                "TRAVEL PROGRESS", "0%", BLUE, v -> positionValue = v
        ));

        return cards;
    }

    private JPanel createMetricCard(
            String heading,
            String initialValue,
            Color accent,
            java.util.function.Consumer<JLabel> register) {

        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(15, 16, 15, 16)
        ));

        JLabel headingLabel = new JLabel(heading);
        headingLabel.setForeground(MUTED);
        headingLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));

        JLabel valueLabel = new JLabel(initialValue);
        valueLabel.setForeground(accent);
        valueLabel.setFont(VALUE_FONT);

        register.accept(valueLabel);

        card.add(headingLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    // =========================================================
    // MAIN CONTENT
    // =========================================================

    private JPanel createMainContent() {

        JPanel main = new JPanel(new BorderLayout(16, 0));
        main.setOpaque(false);

        JPanel simulationCard = createCard();
        simulationCard.setLayout(new BorderLayout(0, 12));

        JPanel simulationHeader = new JPanel(new BorderLayout());
        simulationHeader.setOpaque(false);

        JLabel simulationTitle = new JLabel("Warehouse Floor");
        simulationTitle.setFont(HEADING_FONT);
        simulationTitle.setForeground(NAVY);

        JLabel simulationSubtitle = new JLabel(
                "Live robot movement and operational stages"
        );
        simulationSubtitle.setFont(BODY_FONT);
        simulationSubtitle.setForeground(MUTED);

        JPanel simHeading = new JPanel();
        simHeading.setOpaque(false);
        simHeading.setLayout(new BoxLayout(simHeading, BoxLayout.Y_AXIS));
        simHeading.add(simulationTitle);
        simHeading.add(Box.createVerticalStrut(4));
        simHeading.add(simulationSubtitle);

        simulationHeader.add(simHeading, BorderLayout.WEST);

        warehousePanel = new WarehousePanel();
        warehousePanel.setPreferredSize(new Dimension(650, 390));

        simulationCard.add(simulationHeader, BorderLayout.NORTH);
        simulationCard.add(warehousePanel, BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(270, 0));
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        right.add(createOrderCard());
        right.add(Box.createVerticalStrut(14));
        right.add(createStageCard());
        right.add(Box.createVerticalStrut(14));
        right.add(createTelemetryCard());

        main.add(simulationCard, BorderLayout.CENTER);
        main.add(right, BorderLayout.EAST);

        return main;
    }

    private JPanel createCard() {

        JPanel card = new JPanel();
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(18, 18, 18, 18)
        ));

        return card;
    }

    // =========================================================
    // ORDER DETAILS CARD
    // =========================================================

    private JPanel createOrderCard() {

        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("ORDER DETAILS");
        heading.setFont(HEADING_FONT);
        heading.setForeground(NAVY);

        card.add(heading);
        card.add(Box.createVerticalStrut(15));

        productValue = addDetail(card, "PRODUCT", "Not selected");
        quantityValue = addDetail(card, "QUANTITY", "—");
        locationValue = addDetail(card, "PICKUP LOCATION", "Not assigned");
        robotValue = addDetail(card, "ASSIGNED ROBOT", "Unassigned");

        return card;
    }

    private JLabel addDetail(
            JPanel parent,
            String heading,
            String initialValue) {

        JLabel label = new JLabel(heading);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setForeground(MUTED);

        JLabel value = new JLabel(
                "<html><div style='width:205px'>"
                        + escapeHtml(initialValue)
                        + "</div></html>"
        );

        value.setFont(BODY_FONT);
        value.setForeground(NAVY);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);

        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        parent.add(label);
        parent.add(Box.createVerticalStrut(4));
        parent.add(value);
        parent.add(Box.createVerticalStrut(12));

        return value;
    }

    private String escapeHtml(String value) {

        if (value == null) {
            return "—";
        }

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // =========================================================
    // WORKFLOW STAGE CARD
    // =========================================================

    private JPanel createStageCard() {

        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("WORKFLOW");
        heading.setFont(HEADING_FONT);
        heading.setForeground(NAVY);

        stageValue = new JLabel(
                "<html>1. Pickup<br><br>"
                        + "2. Packing<br><br>"
                        + "3. Dispatch</html>"
        );

        stageValue.setFont(BODY_FONT);
        stageValue.setForeground(MUTED);

        card.add(heading);
        card.add(Box.createVerticalStrut(12));
        card.add(stageValue);

        return card;
    }

    // =========================================================
    // TELEMETRY CARD
    // =========================================================

    private JPanel createTelemetryCard() {

        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("MOVEMENT TELEMETRY");
        heading.setFont(HEADING_FONT);
        heading.setForeground(NAVY);

        JLabel progressLabel = new JLabel("Journey completion");
        progressLabel.setFont(BODY_FONT);
        progressLabel.setForeground(MUTED);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(BLUE);
        progressBar.setBackground(new Color(226, 232, 240));
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 11));

        card.add(heading);
        card.add(Box.createVerticalStrut(12));
        card.add(progressLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(progressBar);

        return card;
    }

    // =========================================================
    // CONTROL PANEL
    // =========================================================

    private JPanel createControlPanel() {

        JPanel panel = new JPanel(new BorderLayout(12, 10));
        panel.setBackground(CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("ROBOT CONTROLS");
        heading.setForeground(NAVY);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JLabel hint = new JLabel("Use START to begin the demonstration.");
        hint.setForeground(MUTED);
        hint.setFont(BODY_FONT);

        info.add(heading);
        info.add(Box.createVerticalStrut(5));
        info.add(hint);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        startButton = createButton("START ROBOT", BLUE);
        stopButton = createButton("STOP", RED);
        completeButton = createButton("COMPLETE DELIVERY", GREEN);

        startButton.addActionListener(e -> startRobot());
        stopButton.addActionListener(e -> stopRobot());
        completeButton.addActionListener(e -> completeDelivery());

        buttons.add(startButton);
        buttons.add(stopButton);
        buttons.add(completeButton);

        panel.add(info, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.EAST);

        return panel;
    }

    private JButton createButton(String text, Color color) {

        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(12, 16, 12, 16));

        return button;
    }

    // =========================================================
    // LOAD ORDER DATA
    // =========================================================

    private void loadOrderData() {

        if (orderId <= 0) {
            setStatus("WAITING");
            updateDashboard();
            return;
        }

        try {

            Order order = orderDAO.getOrder(orderId);

            if (order == null) {
                showError("Order not found.");
                return;
            }

            String status = order.getStatus();

            if (!"PACKED".equalsIgnoreCase(status)
                    && !"ROBOT ASSIGNED".equalsIgnoreCase(status)) {

                showError("Order must be PACKED before robot dispatch.");
                return;
            }

            String sql =
                    "SELECT p.product_name, oi.quantity, " +
                    "p.block_name, p.rack, p.shelf, p.bin_code " +
                    "FROM order_items oi " +
                    "JOIN products p ON oi.product_id=p.product_id " +
                    "WHERE oi.order_id=?";

            try (Connection c = DBConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setInt(1, orderId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        showError("No order items found.");
                        return;
                    }

                    productName = rs.getString("product_name");
                    quantity = rs.getInt("quantity");

                    location = "Block " + rs.getString("block_name")
                            + " / Rack " + rs.getString("rack")
                            + " / Shelf " + rs.getString("shelf")
                            + " / Bin " + rs.getString("bin_code");
                }
            }

            updateDashboard();

        } catch (Exception e) {
            showError("Unable to load order: " + e.getMessage());
        }
    }

    // =========================================================
    // UPDATE DASHBOARD
    // =========================================================

    private void updateDashboard() {

        if (orderValue != null) {
            orderValue.setText(orderId > 0 ? "#" + orderId : "—");
        }

        if (productValue != null) {
            productValue.setText(
                    "<html><div style='width:205px'>"
                            + escapeHtml(productName.isEmpty()
                                    ? "Not selected" : productName)
                            + "</div></html>"
            );
        }

        if (quantityValue != null) {
            quantityValue.setText(String.valueOf(quantity));
        }

        if (locationValue != null) {
            locationValue.setText(
                    "<html><div style='width:205px'>"
                            + escapeHtml(location.isEmpty()
                                    ? "Not assigned" : location)
                            + "</div></html>"
            );
        }

        if (statusValue != null) {
            statusValue.setText(currentStatus);
        }

        if (robotValue != null) {
            robotValue.setText(
                    assignedRobotId > 0
                            ? "AGV-" + assignedRobotId
                            : "Unassigned"
            );
        }

        if (positionValue != null) {
            int progress = Math.max(0,
                    Math.min(100,
                            (x - START_X) * 100 / (END_X - START_X)));

            positionValue.setText(progress + "%");

            if (progressBar != null) {
                progressBar.setValue(progress);
            }
        }

        if (warehousePanel != null) {
            warehousePanel.repaint();
        }
    }

    private void setStatus(String status) {

        currentStatus = status;

        if (statusValue != null) {
            statusValue.setText(status);
        }

        if (warehousePanel != null) {
            warehousePanel.repaint();
        }
    }

    // =========================================================
    // START ROBOT
    // =========================================================

    private void startRobot() {

        if (running) {
            return;
        }

        if (orderId <= 0 || productName.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Open a valid PACKED order before starting.",
                    "Order Required",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {

            Order order = orderDAO.getOrder(orderId);

            if (order == null) {
                showError("Order not found.");
                return;
            }

            String status = order.getStatus();

            if (!"PACKED".equalsIgnoreCase(status)
                    && !"ROBOT ASSIGNED".equalsIgnoreCase(status)) {

                showError("Current status: " + status
                        + "\nRequired status: PACKED");
                return;
            }

            // Assign an available AGV if the order has none.
            assignedRobotId = robotDAO.getRobotIdByOrder(orderId);

            if (assignedRobotId <= 0) {

                assignedRobotId =
                        robotDAO.assignAvailableRobot(orderId);

                if (assignedRobotId <= 0) {
                    showError("No available robot in the fleet.");
                    return;
                }
            }

            if ("PACKED".equalsIgnoreCase(status)) {
                orderDAO.updateStatus(orderId, "ROBOT ASSIGNED");
            }

            deliveryStarted = false;
            deliveryCompleted = false;
            x = START_X;

            setStatus("ROBOT ASSIGNED");

            running = true;
            updateDashboard();

            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            completeButton.setEnabled(false);

            timer.start();

        } catch (Exception e) {

            showError("Unable to start robot: " + e.getMessage());
        }
    }

    // =========================================================
    // STOP ROBOT
    // =========================================================

    private void stopRobot() {

        if (!running) {
            return;
        }

        running = false;
        timer.stop();

        setStatus("STOPPED");

        startButton.setEnabled(true);
        stopButton.setEnabled(false);

        connectionValue.setText("● PAUSED");

        updateDashboard();
    }

    // =========================================================
    // ROBOT MOVEMENT
    // =========================================================

    private void moveRobot() {

        if (!running) {
            return;
        }

        x = Math.min(END_X, x + STEP);

        // PICKUP / COLLECTION
        if (x < 350) {
            setStatus("PICKING");
            updateStage("1. PICKUP  •  IN PROGRESS",
                    "2. PACKING", "3. DISPATCH");
        }

        // PACKING
        else if (x < 550) {
            setStatus("PACKING");
            updateStage("1. PICKUP  •  COMPLETED",
                    "2. PACKING  •  IN PROGRESS", "3. DISPATCH");
        }

        // MOVING TO DISPATCH
        else if (x < END_X) {

            if (!deliveryStarted) {

                deliveryStarted = true;

                try {
                    orderDAO.updateStatus(orderId, "OUT FOR DELIVERY");
                } catch (Exception e) {
                    connectionValue.setText("● STATUS UPDATE ERROR");
                }
            }

            setStatus("OUT FOR DELIVERY");

            updateStage("1. PICKUP  •  COMPLETED",
                    "2. PACKING  •  COMPLETED",
                    "3. DISPATCH  •  IN PROGRESS");
        }

        // DELIVERY FINISHED
        if (x >= END_X) {

            x = END_X;
            running = false;
            timer.stop();
            deliveryCompleted = true;

            setStatus("DELIVERED");

            updateStage("1. PICKUP  •  COMPLETED",
                    "2. PACKING  •  COMPLETED",
                    "3. DISPATCH  •  COMPLETED");

            try {
                orderDAO.updateStatus(orderId, "DELIVERED");
            } catch (Exception e) {
                connectionValue.setText("● DATABASE UPDATE ERROR");
            }

            startButton.setEnabled(false);
            stopButton.setEnabled(false);
            completeButton.setEnabled(true);

            connectionValue.setText("● DELIVERY ARRIVED");

            JOptionPane.showMessageDialog(
                    this,
                    "Delivery simulation finished.\n\n"
                            + "Order ID: " + orderId
                            + "\nProduct: " + productName
                            + "\nQuantity: " + quantity,
                    "Delivery Arrived",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }

        // Periodically persist simulated location and battery.
        long now = System.currentTimeMillis();

        if (assignedRobotId > 0
                && now - lastTelemetryUpdate >= 2000) {

            lastTelemetryUpdate = now;

            int battery = Math.max(
                    0,
                    100 - (x - START_X) * 20 / (END_X - START_X)
            );

            try {
                robotDAO.updateTelemetry(
                        assignedRobotId,
                        "Simulation X=" + x,
                        battery
                );

                if (batteryValue != null) {
                    batteryValue.setText(battery + "%");
                }

            } catch (Exception e) {
                connectionValue.setText("● TELEMETRY UNAVAILABLE");
            }
        }

        updateDashboard();
    }

    private void updateStage(String first, String second, String third) {

        if (stageValue != null) {
            stageValue.setText(
                    "<html>"
                            + escapeHtml(first) + "<br><br>"
                            + escapeHtml(second) + "<br><br>"
                            + escapeHtml(third)
                            + "</html>"
            );
        }
    }

    // =========================================================
    // COMPLETE DELIVERY
    // =========================================================

    private void completeDelivery() {

        if (!deliveryCompleted
                || !"DELIVERED".equalsIgnoreCase(currentStatus)) {

            JOptionPane.showMessageDialog(
                    this,
                    "Wait until the robot reaches the dispatch point.",
                    "Delivery Incomplete",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        try {

            boolean updated =
                    orderDAO.updateStatus(orderId, "COMPLETED");

            if (!updated) {
                showError("Order status could not be updated.");
                return;
            }

            // Release the robot only after successful order completion.
            if (assignedRobotId > 0) {
                robotDAO.releaseRobot(assignedRobotId);
            }

            setStatus("COMPLETED");

            connectionValue.setText("● ORDER COMPLETED");

            completeButton.setEnabled(false);

            JOptionPane.showMessageDialog(
                    this,
                    "Order completed successfully.\n"
                            + "Order ID: " + orderId,
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception e) {

            showError("Unable to complete delivery: " + e.getMessage());
        }
    }

    // =========================================================
    // ERROR DIALOG
    // =========================================================

    private void showError(String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "Smart Warehouse",
                JOptionPane.WARNING_MESSAGE
        );
    }

    // =========================================================
    // WAREHOUSE FLOOR RENDERING
    // =========================================================

    private class WarehousePanel extends JPanel {

        WarehousePanel() {
            setBackground(new Color(248, 250, 253));
            setBorder(BorderFactory.createLineBorder(BORDER));
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            try {

                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                int width = getWidth();
                int height = getHeight();

                // GRID
                g2.setColor(new Color(232, 238, 246));

                for (int gx = 0; gx < width; gx += 35) {
                    g2.drawLine(gx, 0, gx, height);
                }

                for (int gy = 0; gy < height; gy += 35) {
                    g2.drawLine(0, gy, width, gy);
                }

                // WAREHOUSE BLOCKS
                drawZone(g2, 35, 45, 165, 105,
                        "BLOCK A", "RACKS A1 - A2",
                        new Color(219, 234, 254));

                drawZone(g2, 240, 45, 165, 105,
                        "BLOCK B", "RACKS B1 - B2",
                        new Color(224, 231, 255));

                // PICKUP STATION
                drawZone(g2, 25, 220, 165, 90,
                        "PICKUP STATION", "COLLECTION",
                        new Color(209, 250, 229));

                // PACKING STATION
                drawZone(g2, Math.max(215, width / 2 - 80),
                        220, 160, 90,
                        "PACKING", "PROCESSING",
                        new Color(254, 243, 199));

                // DISPATCH STATION
                drawZone(g2, Math.max(400, width - 190),
                        220, 165, 90,
                        "DISPATCH", "DELIVERY GATE",
                        new Color(219, 234, 254));

                // ROUTE
                int pathY = Math.min(height - 70, robotY + 20);

                g2.setStroke(new BasicStroke(
                        3,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND,
                        0,
                        new float[]{7, 7},
                        0
                ));

                g2.setColor(new Color(96, 165, 250));

                g2.drawLine(START_X, pathY, END_X, pathY);

                // ROBOT BODY
                int robotX = Math.min(
                        Math.max(15, x),
                        Math.max(15, width - 55)
                );

                g2.setColor(new Color(37, 99, 235, 35));
                g2.fillOval(robotX - 10, pathY - 30, 60, 60);

                g2.setColor(NAVY);

                RoundRectangle2D body = new RoundRectangle2D.Double(
                        robotX, pathY - 20, 42, 40, 10, 10
                );

                g2.fill(body);

                g2.setColor(new Color(56, 189, 248));
                g2.fillRoundRect(
                        robotX + 6, pathY - 14, 30, 10, 5, 5
                );

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));

                g2.drawString(
                        "AGV",
                        robotX + 10,
                        pathY + 10
                );

                // CURRENT STATUS
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.setColor(NAVY);

                g2.drawString(
                        "ROBOT: " + currentStatus,
                        18,
                        height - 18
                );

            } finally {
                g2.dispose();
            }
        }

        private void drawZone(
                Graphics2D g2,
                int x,
                int y,
                int w,
                int h,
                String title,
                String subtitle,
                Color color) {

            g2.setColor(color);
            g2.fillRoundRect(x, y, w, h, 12, 12);

            g2.setColor(new Color(203, 213, 225));
            g2.drawRoundRect(x, y, w, h, 12, 12);

            g2.setColor(NAVY);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.drawString(title, x + 12, y + 27);

            g2.setColor(MUTED);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.drawString(subtitle, x + 12, y + 46);
        }
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            RobotSimulationFrame frame =
                    new RobotSimulationFrame();

            frame.setVisible(true);
        });
    }
}
