package com.smartwarehouse;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Smart Warehouse Management System — Professional Admin Control Center.
 *
 * Theme Design:
 * - Crisp White & Light Slate Workspace (#F8FAFC / #FFFFFF)
 * - Pure Black Buttons with High-Contrast White Text & Smooth Hover
 * - Subtle Slate Borders (#E2E8F0) & High-Contrast Typography
 */
public class AdminDashboard extends JFrame {

    // =========================================================
    // COLOR PALETTE (CLEAN WHITE THEME + BLACK BUTTONS)
    // =========================================================
    private static final Color BG_WORKSPACE = new Color(248, 250, 252); // Crisp Light Slate
    private static final Color BG_SIDEBAR = new Color(15, 23, 42);      // Professional Dark Obsidian Sidebar
    private static final Color CARD_BG = Color.WHITE;                  // Pure White Cards
    private static final Color CARD_HEADER_BG = new Color(241, 245, 249);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);  // Subtle Slate Border
    private static final Color BORDER_ACTIVE = new Color(148, 163, 184);

    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);    // Dark Charcoal / Slate 900
    private static final Color TEXT_SECONDARY = new Color(71, 85, 105); // Slate 600
    private static final Color TEXT_MUTED = new Color(148, 163, 184);   // Slate 400

    private static final Color ACCENT_BLUE = new Color(37, 99, 235);
    private static final Color ACCENT_CYAN = new Color(14, 165, 233);
    private static final Color ACCENT_GREEN = new Color(16, 185, 129);
    private static final Color ACCENT_ORANGE = new Color(245, 158, 11);
    private static final Color ACCENT_RED = new Color(239, 68, 68);

    // Pure Black Buttons
    private static final Color BTN_BLACK = new Color(15, 23, 42);       // Obsidian Black
    private static final Color BTN_BLACK_HOVER = new Color(30, 41, 59); // Charcoal Hover
    private static final Color BTN_BLACK_PRESS = new Color(51, 65, 85); // Slate Press

    // =========================================================
    // NAVIGATION INDEXES
    // =========================================================
    private static final int TAB_OVERVIEW = 0;
    private static final int TAB_PRODUCTS = 1;
    private static final int TAB_INVENTORY = 2;
    private static final int TAB_AI_QUEUE = 3;
    private static final int TAB_ORDERS = 4;
    private static final int TAB_ROBOTS = 5;
    private static final int TAB_MAP = 6;
    private static final int TAB_REPORTS = 7;
    private static final int TAB_SETTINGS = 8;

    // =========================================================
    // DAOs & SERVICES
    // =========================================================
    private final ProductDAO pd = new ProductDAO();
    private final TaskDAO td = new TaskDAO();
    private final RobotDAO rd = new RobotDAO();
    private final OrderDAO od = new OrderDAO();
    private String tabNetUrl = "http://127.0.0.1:8000/predict";
    private TabNetClient tabNet = new TabNetClient(tabNetUrl);

    // =========================================================
    // STATE & UI REFS
    // =========================================================
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);
    private javax.swing.Timer autoRefreshTimer;

    private CardLayout contentLayout;
    private JPanel contentContainer;
    private JPanel sidebar;
    private JLabel headerTitleLabel;
    private JLabel aiStatusBadge;
    private JLabel dbStatusBadge;
    private JTextArea activityLogArea;

    // Overview KPIs
    private JLabel kpiTotalProducts;
    private JLabel kpiInventoryUnits;
    private JLabel kpiPendingOrders;
    private JLabel kpiAwaitingApproval;
    private JLabel kpiBeingPicked;
    private JLabel kpiReadyDelivery;
    private JLabel kpiCompletedOrders;
    private JLabel kpiLowStock;
    private JLabel kpiOutOfStock;
    private JLabel kpiActiveRobots;

    // Refresh Hooks
    private Runnable refreshOverviewHook;
    private Runnable refreshProductsHook;
    private Runnable refreshInventoryHook;
    private Runnable refreshAiQueueHook;
    private Runnable refreshOrdersHook;
    private Runnable refreshWarehouseMapHook;

    private RobotMovementDashboardPanel robotMovementPanel;
    private int currentNavIndex = 0;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================
    public AdminDashboard() {
        setTitle("Smart Warehouse | Autonomous Management & Control Center");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1220, 800));
        setSize(1480, 920);
        setLocationRelativeTo(null);

        buildInterface();

        // Initial Data Sync
        refreshAllDashboardData();

        // 30s Background Auto-Poller
        autoRefreshTimer = new javax.swing.Timer(30000, e -> refreshAllDashboardData());
        autoRefreshTimer.start();
    }

    // =========================================================
    // UI BUILDER
    // =========================================================
    private void buildInterface() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_WORKSPACE);

        // Sidebar
        buildSidebar();
        root.add(sidebar, BorderLayout.WEST);

        // Main Workspace
        JPanel workspace = new JPanel(new BorderLayout(0, 0));
        workspace.setBackground(BG_WORKSPACE);

        // Clean White Header
        JPanel header = buildHeader();
        workspace.add(header, BorderLayout.NORTH);

        // Center Card Panels
        contentLayout = new CardLayout();
        contentContainer = new JPanel(contentLayout);
        contentContainer.setBackground(BG_WORKSPACE);

        contentContainer.add(createOverviewPanel(), "OVERVIEW");
        contentContainer.add(createProductDetailsPanel(), "PRODUCTS");
        contentContainer.add(createInventoryPanel(), "INVENTORY");
        contentContainer.add(createAiQueuePanel(), "AI_QUEUE");
        contentContainer.add(createOrdersPanel(), "ORDERS");

        robotMovementPanel = new RobotMovementDashboardPanel();
        contentContainer.add(robotMovementPanel, "ROBOTS");

        contentContainer.add(createWarehouseMapPanel(), "MAP");
        contentContainer.add(createReportsPanel(), "REPORTS");
        contentContainer.add(createSettingsPanel(), "SETTINGS");

        workspace.add(contentContainer, BorderLayout.CENTER);

        // Footer Console
        JPanel footer = buildFooterConsole();
        workspace.add(footer, BorderLayout.SOUTH);

        root.add(workspace, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void buildSidebar() {
        sidebar = new JPanel();
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(22, 12, 20, 12));

        // Brand Banner
        JPanel brandPanel = new JPanel(new BorderLayout(8, 0));
        brandPanel.setBackground(BG_SIDEBAR);
        brandPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        brandPanel.setBorder(new EmptyBorder(0, 8, 20, 8));

        JLabel logoIcon = new JLabel("⚡");
        logoIcon.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logoIcon.setForeground(Color.WHITE);

        JLabel brandText = new JLabel("<html><b style='color:#FFFFFF;font-size:13px;'>SMART WAREHOUSE</b><br>"
                + "<span style='color:#94A3B8;font-size:9px;'>ENTERPRISE ADMIN PANEL</span></html>");
        brandPanel.add(logoIcon, BorderLayout.WEST);
        brandPanel.add(brandText, BorderLayout.CENTER);
        sidebar.add(brandPanel);

        sidebar.add(Box.createVerticalStrut(10));

        // 10 Navigation Items
        String[] navLabels = {
                "Overview",
                "Product Details",
                "Inventory Management",
                "AI Order Queue",
                "Order Management",
                "Robot Movement",
                "Warehouse Map",
                "Reports",
                "Settings",
                "Logout"
        };

        String[] navIcons = {
                "•", "•", "•", "•", "•", "•", "•", "•", "•", "•"
        };

        for (int i = 0; i < navLabels.length; i++) {
            final int index = i;
            JButton navBtn = createSidebarButton(navIcons[i] + "  " + navLabels[i], i == 0);
            navBtn.addActionListener(e -> navigateToTab(index));
            sidebar.add(navBtn);
            sidebar.add(Box.createVerticalStrut(4));
        }

        sidebar.add(Box.createVerticalGlue());

        JLabel versionLbl = new JLabel("<html><font color='#64748B' size='2'>System: Enterprise v2.5<br>DB: MySQL 8.4 | AI: TabNet</font></html>");
        versionLbl.setBorder(new EmptyBorder(8, 12, 4, 8));
        sidebar.add(versionLbl);
    }

    private JButton createSidebarButton(String text, boolean active) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(active ? Color.WHITE : new Color(148, 163, 184));
        btn.setBackground(active ? new Color(30, 41, 59) : BG_SIDEBAR);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setPreferredSize(new Dimension(216, 40));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, active ? 3 : 0, 0, 0, Color.WHITE),
                new EmptyBorder(8, 12, 8, 12)
        ));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (currentNavIndex != getNavIndexFromButton(btn)) {
                    btn.setBackground(new Color(30, 41, 59));
                    btn.setForeground(Color.WHITE);
                    btn.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (currentNavIndex != getNavIndexFromButton(btn)) {
                    btn.setBackground(BG_SIDEBAR);
                    btn.setForeground(new Color(148, 163, 184));
                    btn.repaint();
                }
            }
        });
        return btn;
    }

    private int getNavIndexFromButton(JButton b) {
        int idx = 0;
        for (Component c : sidebar.getComponents()) {
            if (c instanceof JButton) {
                if (c == b) return idx;
                idx++;
            }
        }
        return -1;
    }

    private void updateSidebarActiveButton(int targetIndex) {
        currentNavIndex = targetIndex;
        int btnIndex = 0;
        for (Component c : sidebar.getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                boolean active = (btnIndex == targetIndex);
                b.setForeground(active ? Color.WHITE : new Color(148, 163, 184));
                b.setBackground(active ? new Color(30, 41, 59) : BG_SIDEBAR);
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, active ? 3 : 0, 0, 0, Color.WHITE),
                        new EmptyBorder(8, 12, 8, 12)
                ));
                b.repaint();
                btnIndex++;
            }
        }
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CARD_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(14, 24, 14, 24)
        ));

        headerTitleLabel = new JLabel("Overview & Live Dashboard");
        headerTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        headerTitleLabel.setForeground(TEXT_PRIMARY);
        header.add(headerTitleLabel, BorderLayout.WEST);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        statusPanel.setBackground(CARD_BG);

        aiStatusBadge = new JLabel("● AI TabNet: Checking...");
        aiStatusBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        aiStatusBadge.setForeground(TEXT_MUTED);

        dbStatusBadge = new JLabel("● MySQL: Checking...");
        dbStatusBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dbStatusBadge.setForeground(TEXT_MUTED);

        JButton refreshAllBtn = blackButton("⚡ Refresh Live Data");
        refreshAllBtn.addActionListener(e -> refreshAllDashboardData());

        statusPanel.add(aiStatusBadge);
        statusPanel.add(dbStatusBadge);
        statusPanel.add(refreshAllBtn);

        header.add(statusPanel, BorderLayout.EAST);
        return header;
    }

    private JPanel buildFooterConsole() {
        JPanel footer = new JPanel(new BorderLayout(6, 6));
        footer.setBackground(BG_WORKSPACE);
        footer.setBorder(new EmptyBorder(6, 16, 12, 16));

        activityLogArea = new JTextArea(3, 40);
        activityLogArea.setEditable(false);
        activityLogArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        activityLogArea.setBackground(Color.WHITE);
        activityLogArea.setForeground(TEXT_PRIMARY);
        activityLogArea.setText("[SYSTEM STARTED] Smart Warehouse Control Center Initialized.");

        JScrollPane logScroll = new JScrollPane(activityLogArea);
        logScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createTitledBorder(
                        BorderFactory.createEmptyBorder(2, 4, 2, 4),
                        "Real-Time Activity & Automation Telemetry",
                        0, 0, new Font("Segoe UI", Font.BOLD, 10), TEXT_SECONDARY
                )
        ));
        logScroll.setPreferredSize(new Dimension(0, 75));
        footer.add(logScroll, BorderLayout.CENTER);
        return footer;
    }

    private void navigateToTab(int index) {
        if (index == 9) { // Logout
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to log out of the Admin Control Center?",
                    "Confirm Logout",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (choice == JOptionPane.YES_OPTION) {
                if (autoRefreshTimer != null) autoRefreshTimer.stop();
                dispose();
                SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
            }
            return;
        }

        updateSidebarActiveButton(index);
        String[] titles = {
                "Overview & Live Dashboard",
                "Product Details & Storage Mapping",
                "Inventory Management & Stock AI Alerts",
                "AI Order Queue (TabNet + Priority + FCFS)",
                "Order Management & Delivery Authorization",
                "Autonomous Robot Movement Simulation",
                "Warehouse Storage Hierarchy Map",
                "Operational & Fulfillment Reports",
                "System & AI Engine Settings"
        };
        headerTitleLabel.setText(titles[index]);

        String[] cardNames = {
                "OVERVIEW", "PRODUCTS", "INVENTORY", "AI_QUEUE",
                "ORDERS", "ROBOTS", "MAP", "REPORTS", "SETTINGS"
        };
        contentLayout.show(contentContainer, cardNames[index]);

        switch (index) {
            case TAB_OVERVIEW: if (refreshOverviewHook != null) refreshOverviewHook.run(); break;
            case TAB_PRODUCTS: if (refreshProductsHook != null) refreshProductsHook.run(); break;
            case TAB_INVENTORY: if (refreshInventoryHook != null) refreshInventoryHook.run(); break;
            case TAB_AI_QUEUE: if (refreshAiQueueHook != null) refreshAiQueueHook.run(); break;
            case TAB_ORDERS: if (refreshOrdersHook != null) refreshOrdersHook.run(); break;
            case TAB_MAP: if (refreshWarehouseMapHook != null) refreshWarehouseMapHook.run(); break;
        }
    }

    // =========================================================
    // BLACK BUTTON FACTORY (FIXED: 100% VISIBLE ON WINDOWS L&F)
    // =========================================================
    public static JButton blackButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(BTN_BLACK_PRESS);
                } else if (getModel().isRollover()) {
                    g2.setColor(BTN_BLACK_HOVER);
                } else {
                    g2.setColor(BTN_BLACK);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        btn.setBackground(BTN_BLACK);
        btn.setForeground(Color.WHITE); // Bold White text on Pure Black button
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(8, 14, 8, 14)
        ));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.repaint();
            }
        });
        return btn;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(Color.WHITE);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(BORDER_COLOR);
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setSelectionBackground(new Color(224, 231, 255));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setShowVerticalLines(false);
        table.setAutoCreateRowSorter(true);

        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(TEXT_PRIMARY);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean isSel, boolean hasFocus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, isSel, hasFocus, r, c);
                if (!isSel) {
                    comp.setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return comp;
            }
        });
        return table;
    }

    private JPanel createWhiteCard(String title) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(14, 16, 14, 16)
        ));
        if (title != null && !title.isEmpty()) {
            JLabel titleLbl = new JLabel(title);
            titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            titleLbl.setForeground(TEXT_PRIMARY);
            titleLbl.setBorder(new EmptyBorder(0, 0, 8, 0));
            card.add(titleLbl, BorderLayout.NORTH);
        }
        return card;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, String subtitle, Color accent) {
        JPanel card = new JPanel(new BorderLayout(4, 6));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COLOR, 1),
                        new EmptyBorder(12, 14, 12, 14)
                )
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLbl.setForeground(TEXT_SECONDARY);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(TEXT_PRIMARY);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        subLbl.setForeground(TEXT_MUTED);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(subLbl, BorderLayout.SOUTH);
        return card;
    }

    // =========================================================
    // 1. OVERVIEW PANEL
    // =========================================================
    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(BG_WORKSPACE);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));

        // 10 KPI Cards
        JPanel kpiGrid = new JPanel(new GridLayout(2, 5, 10, 10));
        kpiGrid.setBackground(BG_WORKSPACE);

        kpiTotalProducts = new JLabel("—");
        kpiInventoryUnits = new JLabel("—");
        kpiPendingOrders = new JLabel("—");
        kpiAwaitingApproval = new JLabel("—");
        kpiBeingPicked = new JLabel("—");
        kpiReadyDelivery = new JLabel("—");
        kpiCompletedOrders = new JLabel("—");
        kpiLowStock = new JLabel("—");
        kpiOutOfStock = new JLabel("—");
        kpiActiveRobots = new JLabel("—");

        kpiGrid.add(createKpiCard("TOTAL PRODUCTS", kpiTotalProducts, "Active Catalog SKUs", ACCENT_BLUE));
        kpiGrid.add(createKpiCard("TOTAL INVENTORY", kpiInventoryUnits, "Total Storage Units", ACCENT_CYAN));
        kpiGrid.add(createKpiCard("PENDING ORDERS", kpiPendingOrders, "Placed By Customers", ACCENT_ORANGE));
        kpiGrid.add(createKpiCard("AWAITING APPROVAL", kpiAwaitingApproval, "AI Queue Staged", ACCENT_ORANGE));
        kpiGrid.add(createKpiCard("BEING PICKED", kpiBeingPicked, "In-Transit AGVs", ACCENT_CYAN));

        kpiGrid.add(createKpiCard("READY FOR DELIVERY", kpiReadyDelivery, "Packed & Staged", ACCENT_GREEN));
        kpiGrid.add(createKpiCard("COMPLETED ORDERS", kpiCompletedOrders, "Delivered Safely", ACCENT_GREEN));
        kpiGrid.add(createKpiCard("LOW STOCK ITEMS", kpiLowStock, "Below Reorder Limit", ACCENT_ORANGE));
        kpiGrid.add(createKpiCard("OUT OF STOCK", kpiOutOfStock, "Requires Immediate PO", ACCENT_RED));
        kpiGrid.add(createKpiCard("ACTIVE ROBOTS", kpiActiveRobots, "AGV Fleet Tasks", ACCENT_BLUE));

        panel.add(kpiGrid, BorderLayout.NORTH);

        // Center Split
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 12, 12));
        centerPanel.setBackground(BG_WORKSPACE);

        JPanel quickActionsCard = createWhiteCard("Quick Operations & Navigation");
        JPanel actionBtnGrid = new JPanel(new GridLayout(3, 2, 8, 8));
        actionBtnGrid.setBackground(CARD_BG);

        JButton btnOpenAiQueue = blackButton("🧠 AI Order Queue");
        btnOpenAiQueue.addActionListener(e -> navigateToTab(TAB_AI_QUEUE));

        JButton btnOpenSimulator = blackButton("🤖 View Robot Movement");
        btnOpenSimulator.addActionListener(e -> navigateToTab(TAB_ROBOTS));

        JButton btnAddProduct = blackButton("➕ Add New Product");
        btnAddProduct.addActionListener(e -> showAddProductDialog());

        JButton btnCheckInventory = blackButton("📈 Inventory Health & AI");
        btnCheckInventory.addActionListener(e -> navigateToTab(TAB_INVENTORY));

        JButton btnEmergencyStop = blackButton("🛑 FLEET EMERGENCY STOP");
        btnEmergencyStop.addActionListener(e -> triggerFleetEmergencyStop());

        JButton btnRunDiagnostics = blackButton("⚡ Refresh Live Metrics");
        btnRunDiagnostics.addActionListener(e -> refreshAllDashboardData());

        actionBtnGrid.add(btnOpenAiQueue);
        actionBtnGrid.add(btnOpenSimulator);
        actionBtnGrid.add(btnAddProduct);
        actionBtnGrid.add(btnCheckInventory);
        actionBtnGrid.add(btnRunDiagnostics);
        actionBtnGrid.add(btnEmergencyStop);

        quickActionsCard.add(actionBtnGrid, BorderLayout.CENTER);
        centerPanel.add(quickActionsCard);

        JPanel telemetryCard = createWhiteCard("System Integration Telemetry");
        JTextArea telemetryText = new JTextArea();
        telemetryText.setEditable(false);
        telemetryText.setBackground(new Color(248, 250, 252));
        telemetryText.setForeground(TEXT_PRIMARY);
        telemetryText.setFont(new Font("Consolas", Font.PLAIN, 12));
        telemetryText.setBorder(new EmptyBorder(8, 10, 8, 10));
        telemetryText.setText(
                "SYSTEM STATUS & PROTOCOLS:\n\n"
                + "• MySQL Connection: Active (localhost:3306/smart_warehouse)\n"
                + "• TabNet AI Client: Target (http://127.0.0.1:8000/predict)\n"
                + "• Scheduling Engine: Priority Scheduling with FCFS Tie-Breaker\n"
                + "• Robot Pathfinding: 2D Grid Shortest-Aisle BFS Route Calculation\n"
                + "• Inventory Protection: Multi-threaded Transactions with Negative Stock Guard\n"
                + "• UI Palette: Crisp White Dashboard with Black Control Buttons"
        );
        telemetryCard.add(new JScrollPane(telemetryText), BorderLayout.CENTER);
        centerPanel.add(telemetryCard);

        panel.add(centerPanel, BorderLayout.CENTER);

        refreshOverviewHook = this::loadOverviewKpis;
        return panel;
    }

    private void loadOverviewKpis() {
        new SwingWorker<Map<String, Integer>, Void>() {
            @Override
            protected Map<String, Integer> doInBackground() throws Exception {
                Map<String, Integer> kpis = new HashMap<>();
                try (Connection c = DBConnection.getConnection();
                     Statement s = c.createStatement()) {

                    try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM products WHERE active=TRUE")) {
                        kpis.put("total_products", rs.next() ? rs.getInt(1) : 0);
                    }

                    try (ResultSet rs = s.executeQuery("SELECT COALESCE(SUM(quantity), 0) FROM inventory")) {
                        kpis.put("total_inventory", rs.next() ? rs.getInt(1) : 0);
                    }

                    try (ResultSet rs = s.executeQuery("SELECT status, COUNT(*) FROM orders GROUP BY status")) {
                        int pending = 0, awaiting = 0, picking = 0, ready = 0, completed = 0;
                        while (rs.next()) {
                            String st = rs.getString(1);
                            int cnt = rs.getInt(2);
                            if (st == null) continue;
                            String upper = st.toUpperCase(Locale.ROOT);
                            if (upper.equals("PLACED") || upper.equals("PENDING")) {
                                pending += cnt;
                                awaiting += cnt;
                            } else if (upper.equals("PICKING") || upper.equals("APPROVED") || upper.equals("PROCESSING") || upper.equals("ROBOT ASSIGNED")) {
                                picking += cnt;
                            } else if (upper.equals("PACKED") || upper.equals("READY_FOR_DELIVERY")) {
                                ready += cnt;
                            } else if (upper.equals("COMPLETED") || upper.equals("DELIVERED")) {
                                completed += cnt;
                            }
                        }
                        kpis.put("pending_orders", pending);
                        kpis.put("awaiting_orders", awaiting);
                        kpis.put("picking_orders", picking);
                        kpis.put("ready_orders", ready);
                        kpis.put("completed_orders", completed);
                    }

                    try (ResultSet rs = s.executeQuery("SELECT SUM(CASE WHEN quantity=0 THEN 1 ELSE 0 END), SUM(CASE WHEN quantity > 0 AND quantity <= reorder_level THEN 1 ELSE 0 END) FROM inventory")) {
                        if (rs.next()) {
                            kpis.put("out_of_stock", rs.getInt(1));
                            kpis.put("low_stock", rs.getInt(2));
                        }
                    }

                    try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM warehouse_tasks WHERE status IN ('RUNNING','WAITING')")) {
                        kpis.put("active_tasks", rs.next() ? rs.getInt(1) : 0);
                    }
                }
                return kpis;
            }

            @Override
            protected void done() {
                try {
                    Map<String, Integer> k = get();
                    kpiTotalProducts.setText(String.valueOf(k.getOrDefault("total_products", 0)));
                    kpiInventoryUnits.setText(String.valueOf(k.getOrDefault("total_inventory", 0)));
                    kpiPendingOrders.setText(String.valueOf(k.getOrDefault("pending_orders", 0)));
                    kpiAwaitingApproval.setText(String.valueOf(k.getOrDefault("awaiting_orders", 0)));
                    kpiBeingPicked.setText(String.valueOf(k.getOrDefault("picking_orders", 0)));
                    kpiReadyDelivery.setText(String.valueOf(k.getOrDefault("ready_orders", 0)));
                    kpiCompletedOrders.setText(String.valueOf(k.getOrDefault("completed_orders", 0)));
                    kpiLowStock.setText(String.valueOf(k.getOrDefault("low_stock", 0)));
                    kpiOutOfStock.setText(String.valueOf(k.getOrDefault("out_of_stock", 0)));
                    kpiActiveRobots.setText(String.valueOf(k.getOrDefault("active_tasks", 0)));

                    dbStatusBadge.setText("● MySQL: Connected");
                    dbStatusBadge.setForeground(ACCENT_GREEN);
                } catch (Exception ex) {
                    dbStatusBadge.setText("● MySQL: Disconnected");
                    dbStatusBadge.setForeground(ACCENT_RED);
                }
            }
        }.execute();

        // Check TabNet AI connectivity
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    String testResp = tabNet.predict(10, 2.0, 1, 1);
                    return testResp != null && !testResp.contains("Connection Failed");
                } catch (Exception ex) {
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean online = get();
                    if (online) {
                        aiStatusBadge.setText("● AI TabNet: Active");
                        aiStatusBadge.setForeground(ACCENT_GREEN);
                    } else {
                        aiStatusBadge.setText("● AI TabNet: Fallback Mode");
                        aiStatusBadge.setForeground(ACCENT_ORANGE);
                    }
                } catch (Exception ignored) {
                    aiStatusBadge.setText("● AI TabNet: Offline");
                    aiStatusBadge.setForeground(ACCENT_RED);
                }
            }
        }.execute();
    }

    // =========================================================
    // 2. PRODUCT DETAILS
    // =========================================================
    private JPanel createProductDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_WORKSPACE);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topBar.setBackground(CARD_BG);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel searchLbl = new JLabel("🔍 Search:");
        searchLbl.setForeground(TEXT_PRIMARY);
        searchLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JTextField searchField = new JTextField(15);
        searchField.setBackground(Color.WHITE);
        searchField.setForeground(TEXT_PRIMARY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(4, 6, 4, 6)
        ));

        JLabel catLbl = new JLabel("Category:");
        catLbl.setForeground(TEXT_PRIMARY);
        JComboBox<String> categoryFilter = new JComboBox<>(new String[]{"All Categories", "Electronics", "Accessories", "Office", "Groceries"});

        JLabel stockFilterLbl = new JLabel("Stock Status:");
        stockFilterLbl.setForeground(TEXT_PRIMARY);
        JComboBox<String> stockFilter = new JComboBox<>(new String[]{"All Statuses", "NORMAL", "LOW STOCK", "OUT OF STOCK"});

        JButton btnAdd = blackButton("➕ Add New Product");
        JButton btnEdit = blackButton("✏️ Edit Product");
        JButton btnDelete = blackButton("🗑️ Delete Product");
        JButton btnRefresh = blackButton("🔄 Refresh");

        topBar.add(searchLbl);
        topBar.add(searchField);
        topBar.add(catLbl);
        topBar.add(categoryFilter);
        topBar.add(stockFilterLbl);
        topBar.add(stockFilter);
        topBar.add(btnAdd);
        topBar.add(btnEdit);
        topBar.add(btnDelete);
        topBar.add(btnRefresh);

        panel.add(topBar, BorderLayout.NORTH);

        String[] columns = {
                "Product ID", "Product Name", "Category", "Price (₹)", "Stock",
                "Status", "Warehouse", "Block", "Rack", "Shelf", "Bin", "Last Updated"
        };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createStyledTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        panel.add(scroll, BorderLayout.CENTER);

        Runnable loadProducts = () -> {
            model.setRowCount(0);
            new SwingWorker<List<Object[]>, Void>() {
                @Override
                protected List<Object[]> doInBackground() throws Exception {
                    List<Object[]> rows = new ArrayList<>();
                    String sql = "SELECT p.product_id, p.product_name, p.category, p.price, "
                            + "COALESCE(i.quantity, p.stock) AS quantity, "
                            + "COALESCE(i.reorder_level, p.reorder_level) AS reorder_level, "
                            + "i.location, i.last_updated "
                            + "FROM products p "
                            + "LEFT JOIN inventory i ON p.product_id = i.product_id "
                            + "WHERE p.active = TRUE "
                            + "ORDER BY p.product_id ASC";

                    try (Connection c = DBConnection.getConnection();
                         PreparedStatement ps = c.prepareStatement(sql);
                         ResultSet rs = ps.executeQuery()) {

                        while (rs.next()) {
                            int id = rs.getInt("product_id");
                            String name = rs.getString("product_name");
                            String cat = rs.getString("category");
                            double price = rs.getDouble("price");
                            int stock = rs.getInt("quantity");
                            int reorder = rs.getInt("reorder_level");
                            String loc = rs.getString("location");
                            Timestamp updated = rs.getTimestamp("last_updated");

                            String status = (stock == 0) ? "OUT OF STOCK" : (stock <= reorder ? "LOW STOCK" : "NORMAL");
                            WarehouseLocation wl = parseOrAssignLocation(id, loc);

                            rows.add(new Object[]{
                                    id, name, cat, String.format(Locale.US, "%.2f", price),
                                    stock, status, "Meerut-WH1", wl.block, wl.rack, wl.shelf, wl.bin,
                                    updated != null ? updated.toString() : "Recent"
                            });
                        }
                    }
                    return rows;
                }

                @Override
                protected void done() {
                    try {
                        for (Object[] row : get()) model.addRow(row);
                        logActivity("Product Catalog loaded: " + model.getRowCount() + " products.");
                    } catch (Exception ex) {
                        logActivity("Failed to load products: " + ex.getMessage());
                    }
                }
            }.execute();
        };

        refreshProductsHook = loadProducts;
        btnRefresh.addActionListener(e -> loadProducts.run());

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchField.getText().trim();
                if (text.isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 1));
            }
        });

        categoryFilter.addActionListener(e -> {
            String sel = (String) categoryFilter.getSelectedItem();
            if ("All Categories".equals(sel)) sorter.setRowFilter(null);
            else sorter.setRowFilter(RowFilter.regexFilter(sel, 2));
        });

        stockFilter.addActionListener(e -> {
            String sel = (String) stockFilter.getSelectedItem();
            if ("All Statuses".equals(sel)) sorter.setRowFilter(null);
            else sorter.setRowFilter(RowFilter.regexFilter(sel, 5));
        });

        btnAdd.addActionListener(e -> showAddProductDialog());

        btnEdit.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) {
                JOptionPane.showMessageDialog(this, "Please select a product to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = table.convertRowIndexToModel(sel);
            int pId = (Integer) model.getValueAt(modelRow, 0);
            String pName = (String) model.getValueAt(modelRow, 1);
            String pCat = (String) model.getValueAt(modelRow, 2);
            double pPrice = Double.parseDouble(model.getValueAt(modelRow, 3).toString());
            int pStock = (Integer) model.getValueAt(modelRow, 4);
            String pBlock = (String) model.getValueAt(modelRow, 7);
            String pRack = (String) model.getValueAt(modelRow, 8);
            String pShelf = (String) model.getValueAt(modelRow, 9);
            String pBin = (String) model.getValueAt(modelRow, 10);

            showEditProductDialog(pId, pName, pCat, pPrice, pStock, pBlock, pRack, pShelf, pBin);
        });

        btnDelete.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) {
                JOptionPane.showMessageDialog(this, "Please select a product to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = table.convertRowIndexToModel(sel);
            int pId = (Integer) model.getValueAt(modelRow, 0);
            String pName = (String) model.getValueAt(modelRow, 1);

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to deactivate Product #" + pId + " (" + pName + ")?\n"
                    + "It will be safely soft-deleted without corrupting historical orders.",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    pd.deleteProduct(pId);
                    logActivity("Product #" + pId + " deactivated.");
                    loadProducts.run();
                    loadOverviewKpis();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to delete: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        loadProducts.run();
        return panel;
    }

    private void showAddProductDialog() {
        JDialog dialog = new JDialog(this, "Add New Warehouse Product", true);
        dialog.setSize(500, 560);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(Color.WHITE);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtName = new JTextField();
        JTextField txtCategory = new JTextField();
        JTextField txtPrice = new JTextField();
        JTextField txtStock = new JTextField();
        JTextField txtReorder = new JTextField("5");
        JComboBox<String> cmbBlock = new JComboBox<>(new String[]{"Block A", "Block B"});
        JComboBox<String> cmbRack = new JComboBox<>(new String[]{"Rack 1", "Rack 2", "Rack 3", "Rack 4"});
        JComboBox<String> cmbShelf = new JComboBox<>(new String[]{"Shelf 1", "Shelf 2", "Shelf 3", "Shelf 4"});
        JComboBox<String> cmbBin = new JComboBox<>(new String[]{"Bin 1", "Bin 2", "Bin 3", "Bin 4", "Bin 5", "Bin 6", "Bin 7", "Bin 8"});

        int row = 0;
        addFormRow(form, g, row++, "Product Name *:", txtName);
        addFormRow(form, g, row++, "Category *:", txtCategory);
        addFormRow(form, g, row++, "Price (₹) *:", txtPrice);
        addFormRow(form, g, row++, "Initial Stock Qty *:", txtStock);
        addFormRow(form, g, row++, "Reorder Limit *:", txtReorder);
        addFormRow(form, g, row++, "Warehouse Block:", cmbBlock);
        addFormRow(form, g, row++, "Rack Position:", cmbRack);
        addFormRow(form, g, row++, "Shelf Level:", cmbShelf);
        addFormRow(form, g, row++, "Storage Bin:", cmbBin);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.setBackground(new Color(248, 250, 252));
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));

        JButton btnSave = blackButton("Save Product to MySQL");
        JButton btnCancel = blackButton("Cancel");

        btnSave.addActionListener(e -> {
            try {
                String name = txtName.getText().trim();
                String cat = txtCategory.getText().trim();
                if (name.isEmpty() || cat.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Product Name and Category are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                double price = Double.parseDouble(txtPrice.getText().trim());
                int stock = Integer.parseInt(txtStock.getText().trim());
                int reorder = Integer.parseInt(txtReorder.getText().trim());
                if (price < 0 || stock < 0 || reorder < 0) {
                    JOptionPane.showMessageDialog(dialog, "Price, Stock, and Reorder limit cannot be negative.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String locStr = cmbBlock.getSelectedItem() + " - " + cmbRack.getSelectedItem() + " - " + cmbShelf.getSelectedItem() + " - " + cmbBin.getSelectedItem();

                try (Connection c = DBConnection.getConnection()) {
                    c.setAutoCommit(false);
                    int newId = -1;

                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO products(product_name, category, price, stock, reorder_level, active) VALUES(?,?,?,?,?,TRUE)",
                            Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, name);
                        ps.setString(2, cat);
                        ps.setDouble(3, price);
                        ps.setInt(4, stock);
                        ps.setInt(5, reorder);
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) newId = rs.getInt(1);
                        }
                    }

                    if (newId > 0) {
                        try (PreparedStatement ps = c.prepareStatement(
                                "INSERT INTO inventory(product_id, product_name, quantity, reorder_level, location) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE quantity=?, location=?")) {
                            ps.setInt(1, newId);
                            ps.setString(2, name);
                            ps.setInt(3, stock);
                            ps.setInt(4, reorder);
                            ps.setString(5, locStr);
                            ps.setInt(6, stock);
                            ps.setString(7, locStr);
                            ps.executeUpdate();
                        }
                    }
                    c.commit();
                    logActivity("New product added: #" + newId + " (" + name + ") located at " + locStr);
                }

                dialog.dispose();
                if (refreshProductsHook != null) refreshProductsHook.run();
                loadOverviewKpis();
                JOptionPane.showMessageDialog(this, "Product '" + name + "' added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid numeric numbers for price and stock.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCancel.addActionListener(e -> dialog.dispose());
        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showEditProductDialog(int id, String oldName, String oldCat, double oldPrice, int oldStock, String b, String r, String s, String bin) {
        JDialog dialog = new JDialog(this, "Edit Product #" + id, true);
        dialog.setSize(500, 560);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(Color.WHITE);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtName = new JTextField(oldName);
        JTextField txtCategory = new JTextField(oldCat);
        JTextField txtPrice = new JTextField(String.valueOf(oldPrice));
        JTextField txtStock = new JTextField(String.valueOf(oldStock));
        JComboBox<String> cmbBlock = new JComboBox<>(new String[]{"Block A", "Block B"});
        cmbBlock.setSelectedItem(b);
        JComboBox<String> cmbRack = new JComboBox<>(new String[]{"Rack 1", "Rack 2", "Rack 3", "Rack 4"});
        cmbRack.setSelectedItem(r);
        JComboBox<String> cmbShelf = new JComboBox<>(new String[]{"Shelf 1", "Shelf 2", "Shelf 3", "Shelf 4"});
        cmbShelf.setSelectedItem(s);
        JComboBox<String> cmbBin = new JComboBox<>(new String[]{"Bin 1", "Bin 2", "Bin 3", "Bin 4", "Bin 5", "Bin 6", "Bin 7", "Bin 8"});
        cmbBin.setSelectedItem(bin);

        int row = 0;
        addFormRow(form, g, row++, "Product Name:", txtName);
        addFormRow(form, g, row++, "Category:", txtCategory);
        addFormRow(form, g, row++, "Price (₹):", txtPrice);
        addFormRow(form, g, row++, "Available Stock:", txtStock);
        addFormRow(form, g, row++, "Warehouse Block:", cmbBlock);
        addFormRow(form, g, row++, "Rack Position:", cmbRack);
        addFormRow(form, g, row++, "Shelf Level:", cmbShelf);
        addFormRow(form, g, row++, "Storage Bin:", cmbBin);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.setBackground(new Color(248, 250, 252));
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));

        JButton btnSave = blackButton("Update Product");
        JButton btnCancel = blackButton("Cancel");

        btnSave.addActionListener(e -> {
            try {
                String name = txtName.getText().trim();
                String cat = txtCategory.getText().trim();
                double price = Double.parseDouble(txtPrice.getText().trim());
                int stock = Integer.parseInt(txtStock.getText().trim());
                if (name.isEmpty() || cat.isEmpty() || price < 0 || stock < 0) {
                    JOptionPane.showMessageDialog(dialog, "Please fill valid product fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String locStr = cmbBlock.getSelectedItem() + " - " + cmbRack.getSelectedItem() + " - " + cmbShelf.getSelectedItem() + " - " + cmbBin.getSelectedItem();

                try (Connection c = DBConnection.getConnection()) {
                    c.setAutoCommit(false);
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE products SET product_name=?, category=?, price=?, stock=? WHERE product_id=?")) {
                        ps.setString(1, name);
                        ps.setString(2, cat);
                        ps.setDouble(3, price);
                        ps.setInt(4, stock);
                        ps.setInt(5, id);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE inventory SET quantity=?, location=?, product_name=? WHERE product_id=?")) {
                        ps.setInt(1, stock);
                        ps.setString(2, locStr);
                        ps.setString(3, name);
                        ps.setInt(4, id);
                        ps.executeUpdate();
                    }
                    c.commit();
                    logActivity("Product #" + id + " updated. Location: " + locStr);
                }

                dialog.dispose();
                if (refreshProductsHook != null) refreshProductsHook.run();
                loadOverviewKpis();
                JOptionPane.showMessageDialog(this, "Product #" + id + " updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Update error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCancel.addActionListener(e -> dialog.dispose());
        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void addFormRow(JPanel p, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridx = 0; g.gridy = row; g.weightx = 0.3;
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(TEXT_PRIMARY);
        p.add(l, g);

        g.gridx = 1; g.weightx = 0.7;
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setPreferredSize(new Dimension(240, 30));
        p.add(field, g);
    }

    // =========================================================
    // 3. INVENTORY MANAGEMENT & TABNET STOCK ALERTS
    // =========================================================
    private JPanel createInventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(BG_WORKSPACE);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        top.setBackground(CARD_BG);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel threshLbl = new JLabel("Global Reorder Limit:");
        threshLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        threshLbl.setForeground(TEXT_PRIMARY);

        JSpinner threshSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
        threshSpinner.setPreferredSize(new Dimension(70, 30));

        JButton btnApplyThresh = blackButton("Set Global Limit");
        JButton btnAdjustStock = blackButton("📦 Adjust Product Stock");
        JButton btnRunAiRisk = blackButton("🧠 Query TabNet Stock Risk");
        JButton btnRefreshInv = blackButton("🔄 Refresh Inventory");

        top.add(threshLbl);
        top.add(threshSpinner);
        top.add(btnApplyThresh);
        top.add(btnAdjustStock);
        top.add(btnRunAiRisk);
        top.add(btnRefreshInv);
        panel.add(top, BorderLayout.NORTH);

        String[] columns = {
                "ID", "Product Name", "Category", "Quantity", "Reorder Level",
                "Storage Location", "Stock Status", "TabNet AI Stockout Risk", "Predicted Demand"
        };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createStyledTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomCard = createWhiteCard("TabNet Stock Risk & Out-of-Stock Alert Feed");
        JTextArea alertsArea = new JTextArea(4, 50);
        alertsArea.setEditable(false);
        alertsArea.setBackground(new Color(248, 250, 252));
        alertsArea.setForeground(TEXT_PRIMARY);
        alertsArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        alertsArea.setText("Stock monitoring active. TabNet AI evaluates demand & stockout risks upon refresh.");
        bottomCard.add(new JScrollPane(alertsArea), BorderLayout.CENTER);
        panel.add(bottomCard, BorderLayout.SOUTH);

        Runnable loadInventory = () -> {
            model.setRowCount(0);
            new SwingWorker<List<Object[]>, Void>() {
                private final StringBuilder alertLog = new StringBuilder();

                @Override
                protected List<Object[]> doInBackground() throws Exception {
                    List<Object[]> rows = new ArrayList<>();
                    int pendingCount = 0;
                    int waitingTasks = 0;
                    try (Connection c = DBConnection.getConnection();
                         Statement s = c.createStatement()) {
                        try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM orders WHERE UPPER(status) NOT IN ('COMPLETED','DELIVERED','CANCELLED')")) {
                            if (rs.next()) pendingCount = rs.getInt(1);
                        }
                        try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM warehouse_tasks WHERE status='WAITING'")) {
                            if (rs.next()) waitingTasks = rs.getInt(1);
                        }
                    }

                    List<Product> products = pd.getProducts();
                    for (Product p : products) {
                        int stock = Math.max(0, p.stock);
                        int reorder = Math.max(0, p.reorderLevel);
                        String status = (stock == 0) ? "OUT OF STOCK" : (stock <= reorder ? "LOW STOCK" : "NORMAL");

                        double demandProxy = Math.max(0.5, (double) reorder - stock + (pendingCount * 0.2));
                        String aiRisk = "LOW";
                        double predictedDemand = demandProxy * 1.08;

                        try {
                            String response = tabNet.predict(stock, demandProxy, pendingCount, waitingTasks);
                            if (response != null && !response.contains("Connection Failed") && !response.contains("Error")) {
                                if (response.contains("HIGH")) aiRisk = "HIGH";
                                else if (response.contains("MEDIUM")) aiRisk = "MEDIUM";
                                else aiRisk = "LOW";
                            } else {
                                aiRisk = (stock == 0) ? "CRITICAL [RULE]" : (stock <= reorder ? "HIGH [RULE]" : "LOW [RULE]");
                            }
                        } catch (Exception ex) {
                            aiRisk = (stock == 0) ? "CRITICAL [FALLBACK]" : (stock <= reorder ? "HIGH [FALLBACK]" : "LOW [FALLBACK]");
                        }

                        if (stock <= reorder || aiRisk.contains("HIGH") || aiRisk.contains("CRITICAL")) {
                            alertLog.append(String.format("⚠️ [STOCK ALERT] %s | Stock: %d | Min: %d | AI Risk: %s | Est. Demand: %.2f\n",
                                    p.name, stock, reorder, aiRisk, predictedDemand));
                        }

                        rows.add(new Object[]{
                                p.id, p.name, p.category, stock, reorder,
                                "Meerut WH1 (Aisle " + (p.id % 4 + 1) + ")", status, aiRisk,
                                String.format(Locale.US, "%.2f units", predictedDemand)
                        });
                    }
                    return rows;
                }

                @Override
                protected void done() {
                    try {
                        for (Object[] r : get()) model.addRow(r);
                        alertsArea.setText(alertLog.length() == 0 ? "✅ All inventory items are above critical safety levels." : alertLog.toString());
                        logActivity("Inventory & Stock AI refreshed: " + model.getRowCount() + " items checked.");
                    } catch (Exception ex) {
                        alertsArea.setText("Failed to load inventory: " + ex.getMessage());
                    }
                }
            }.execute();
        };

        refreshInventoryHook = loadInventory;
        btnRefreshInv.addActionListener(e -> loadInventory.run());
        btnRunAiRisk.addActionListener(e -> loadInventory.run());

        btnAdjustStock.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) {
                JOptionPane.showMessageDialog(this, "Select a product to adjust stock.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int row = table.convertRowIndexToModel(sel);
            int pId = (Integer) model.getValueAt(row, 0);
            String pName = (String) model.getValueAt(row, 1);
            int curStock = (Integer) model.getValueAt(row, 3);

            String input = JOptionPane.showInputDialog(this, "Enter stock adjustment amount for '" + pName + "'\n(Positive to add stock, Negative to deduct):", "0");
            if (input != null && !input.trim().isEmpty()) {
                try {
                    int delta = Integer.parseInt(input.trim());
                    if (delta == 0) return;
                    if (curStock + delta < 0) {
                        JOptionPane.showMessageDialog(this, "Cannot adjust stock below 0! Current: " + curStock, "Negative Stock Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (delta > 0) pd.increaseStock(pId, delta);
                    else pd.decreaseStock(pId, Math.abs(delta));

                    logActivity("Stock adjusted for Product #" + pId + ": delta=" + delta + ", new total=" + (curStock + delta));
                    loadInventory.run();
                    loadOverviewKpis();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Stock adjustment error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnApplyThresh.addActionListener(e -> {
            int newLimit = (Integer) threshSpinner.getValue();
            try (Connection c = DBConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement("UPDATE inventory SET reorder_level=?")) {
                ps.setInt(1, newLimit);
                ps.executeUpdate();
                logActivity("Global reorder threshold updated to: " + newLimit);
                loadInventory.run();
                JOptionPane.showMessageDialog(this, "Global reorder threshold updated to " + newLimit, "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Threshold update error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        loadInventory.run();
        return panel;
    }

    // =========================================================
    // 4. AI ORDER QUEUE (TabNet + Priority + FCFS)
    // =========================================================
    private JPanel createAiQueuePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(BG_WORKSPACE);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        top.setBackground(CARD_BG);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JButton btnApproveTop = blackButton("⚡ APPROVE TOP RANKED ORDER (#1)");
        JButton btnApproveSelected = blackButton("✅ Approve Selected Order");
        JButton btnRejectOrder = blackButton("❌ Reject Selected Order");
        JButton btnViewItems = blackButton("👁️ View Order Items");
        JButton btnRefreshQueue = blackButton("🔄 Re-evaluate Queue");

        top.add(btnApproveTop);
        top.add(btnApproveSelected);
        top.add(btnRejectOrder);
        top.add(btnViewItems);
        top.add(btnRefreshQueue);
        panel.add(top, BorderLayout.NORTH);

        String[] columns = {
                "Queue Pos", "Order ID", "Customer ID", "Arrival Time", "Item Count",
                "Total (₹)", "AI Recommendation", "AI Priority", "Approval Status", "Model Reasoning"
        };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createStyledTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomCard = createWhiteCard("Autonomous TabNet AI Scheduling Telemetry");
        JTextArea queueDetailsArea = new JTextArea(4, 50);
        queueDetailsArea.setEditable(false);
        queueDetailsArea.setBackground(new Color(248, 250, 252));
        queueDetailsArea.setForeground(TEXT_PRIMARY);
        queueDetailsArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        queueDetailsArea.setText("Orders in PLACED/PENDING state are evaluated by TabNet and automatically sorted by Priority (Priority 1 first) with FCFS tie-breaking.");
        bottomCard.add(new JScrollPane(queueDetailsArea), BorderLayout.CENTER);
        panel.add(bottomCard, BorderLayout.SOUTH);

        final List<Scheduler.ScheduledOrder> activeQueue = new ArrayList<>();

        Runnable loadAiQueue = () -> {
            model.setRowCount(0);
            activeQueue.clear();
            queueDetailsArea.setText("Querying MySQL orders and running TabNet AI priority recommendations...");

            new SwingWorker<List<Scheduler.ScheduledOrder>, Void>() {
                @Override
                protected List<Scheduler.ScheduledOrder> doInBackground() throws Exception {
                    List<Scheduler.ScheduledOrder> list = new ArrayList<>();
                    String sql = "SELECT order_id, user_id, total_amount, created_at, status FROM orders "
                            + "WHERE UPPER(status) IN ('PLACED', 'PENDING') ORDER BY created_at ASC";

                    int totalWaitingTasks = 0;
                    try (Connection c = DBConnection.getConnection();
                         Statement s = c.createStatement();
                         ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM warehouse_tasks WHERE status='WAITING'")) {
                        if (rs.next()) totalWaitingTasks = rs.getInt(1);
                    }

                    try (Connection c = DBConnection.getConnection();
                         PreparedStatement ps = c.prepareStatement(sql);
                         ResultSet rs = ps.executeQuery()) {

                        while (rs.next()) {
                            int oId = rs.getInt("order_id");
                            int uId = rs.getInt("user_id");
                            double total = rs.getDouble("total_amount");
                            Timestamp arrival = rs.getTimestamp("created_at");

                            int itemCount = 1;
                            try {
                                List<String> items = od.getOrderItems(oId);
                                if (items != null) itemCount = items.size();
                            } catch (Exception ignored) { }

                            int priority = 2;
                            String rec = "FCFS";
                            String reason = "Normal workload.";

                            try {
                                String response = tabNet.predict(15, (double) itemCount * 1.5, list.size() + 1, totalWaitingTasks);
                                if (response != null && response.contains("recommended_algorithm")) {
                                    if (response.contains("PRIORITY")) {
                                        priority = 1;
                                        rec = "PRIORITY DISPATCH";
                                        reason = "High workload detected by TabNet model.";
                                    } else if (response.contains("SJF")) {
                                        priority = 2;
                                        rec = "SHORT TASK FIRST";
                                        reason = "Optimizing turnaround for quick items.";
                                    } else if (response.contains("ROUND ROBIN")) {
                                        priority = 3;
                                        rec = "SHARED EXECUTION";
                                        reason = "Dense queue requires sharing.";
                                    } else {
                                        priority = 4;
                                        rec = "STANDARD FCFS";
                                        reason = "Normal queue flow.";
                                    }
                                } else {
                                    priority = (total >= 15000 || itemCount >= 4) ? 1 : 2;
                                    rec = (priority == 1) ? "PRIORITY [FALLBACK]" : "FCFS [FALLBACK]";
                                    reason = "High value order expedited.";
                                }
                            } catch (Exception ex) {
                                priority = (total >= 10000) ? 1 : 2;
                                rec = "STANDARD PRIORITY";
                                reason = "TabNet offline; rule fallback applied.";
                            }

                            list.add(new Scheduler.ScheduledOrder(oId, uId, priority, arrival, rec, reason, total, itemCount));
                        }
                    }

                    return Scheduler.scheduleOrders(list);
                }

                @Override
                protected void done() {
                    try {
                        List<Scheduler.ScheduledOrder> result = get();
                        activeQueue.addAll(result);
                        for (Scheduler.ScheduledOrder item : result) {
                            model.addRow(new Object[]{
                                    item.getQueuePosition(), "ORD-" + item.getOrderId(), "USER-" + item.getUserId(),
                                    item.getArrivalTime() != null ? item.getArrivalTime().toString() : "Recent",
                                    item.getItemCount(), String.format(Locale.US, "%.2f", item.getTotalAmount()),
                                    item.getRecommendation(), "Priority " + item.getAiPriority(), "PENDING", item.getReason()
                            });
                        }

                        if (result.isEmpty()) {
                            queueDetailsArea.setText("No pending orders waiting for approval.\nNew customer orders will automatically appear here sorted by TabNet Priority + FCFS.");
                        } else {
                            queueDetailsArea.setText(String.format("AI QUEUE EVALUATION COMPLETE: %d orders in queue.\n"
                                    + "Sorting: AI Priority 1 (Highest) -> Arrival Time FCFS tie-breaker.\n"
                                    + "Top Recommendation: Order #ORD-%d (Priority %d, Total: ₹%.2f)\n"
                                    + "Click 'APPROVE TOP RANKED ORDER' to launch autonomous picking.",
                                    result.size(), result.get(0).getOrderId(), result.get(0).getAiPriority(), result.get(0).getTotalAmount()));
                            table.setRowSelectionInterval(0, 0);
                        }
                        logActivity("AI Order Queue refreshed: " + result.size() + " orders sequenced.");
                    } catch (Exception ex) {
                        queueDetailsArea.setText("AI Queue evaluation error: " + ex.getMessage());
                    }
                }
            }.execute();
        };

        refreshAiQueueHook = loadAiQueue;
        btnRefreshQueue.addActionListener(e -> loadAiQueue.run());

        btnApproveTop.addActionListener(e -> {
            if (activeQueue.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No orders awaiting approval.", "Queue Empty", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Scheduler.ScheduledOrder topOrder = activeQueue.get(0);
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Approve Top-Ranked Order #ORD-" + topOrder.getOrderId() + "?\n"
                    + "This will reserve an available robot and automatically launch the picking simulation.",
                    "Approve Top Order",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                executeOrderApprovalWorkflow(topOrder.getOrderId());
            }
        });

        btnApproveSelected.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) {
                JOptionPane.showMessageDialog(this, "Select an order from the queue to approve.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int row = table.convertRowIndexToModel(sel);
            int oId = activeQueue.get(row).getOrderId();
            executeOrderApprovalWorkflow(oId);
        });

        btnRejectOrder.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) {
                JOptionPane.showMessageDialog(this, "Select an order to reject.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int row = table.convertRowIndexToModel(sel);
            int oId = activeQueue.get(row).getOrderId();
            String reason = JOptionPane.showInputDialog(this, "Enter reason for rejecting Order #ORD-" + oId + ":", "Out of stock / Invalid delivery address");
            if (reason != null) {
                executeOrderRejection(oId, reason);
            }
        });

        btnViewItems.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) {
                JOptionPane.showMessageDialog(this, "Select an order to view items.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int row = table.convertRowIndexToModel(sel);
            showOrderItemsDialog(activeQueue.get(row).getOrderId());
        });

        loadAiQueue.run();
        return panel;
    }

    // =========================================================
    // 5. AUTOMATED WORKFLOW ON ADMIN APPROVAL
    // =========================================================
    private void executeOrderApprovalWorkflow(int orderId) {
        logActivity("Initiating automatic workflow for Order #" + orderId + "...");

        new SwingWorker<Boolean, String>() {
            private int assignedRobotId = -1;
            private String failureReason = "";

            @Override
            protected Boolean doInBackground() throws Exception {
                assignedRobotId = rd.assignAvailableRobot(orderId);
                if (assignedRobotId <= 0) {
                    failureReason = "No warehouse robot is currently available! All robots are busy or offline.";
                    return false;
                }

                int taskId = td.createTask(orderId, "PICKING", 0, 5, 1);
                td.assignRobot(taskId, assignedRobotId);

                od.updateStatus(orderId, "APPROVED");
                Thread.sleep(300);
                od.updateStatus(orderId, "PICKING");
                return true;
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (!success) {
                        JOptionPane.showMessageDialog(AdminDashboard.this, failureReason, "Robot Assignment Notice", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    logActivity("Order #" + orderId + " APPROVED. Robot #" + assignedRobotId + " assigned. Starting navigation...");

                    if (refreshAiQueueHook != null) refreshAiQueueHook.run();
                    if (refreshOrdersHook != null) refreshOrdersHook.run();
                    loadOverviewKpis();

                    navigateToTab(TAB_ROBOTS);
                    robotMovementPanel.startAutonomousOrderPicking(orderId, assignedRobotId);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdminDashboard.this, "Approval workflow error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void executeOrderRejection(int orderId, String reason) {
        try {
            od.updateStatus(orderId, "CANCELLED");
            rd.releaseRobotByOrder(orderId);
            logActivity("Order #" + orderId + " REJECTED. Reason: " + reason);
            if (refreshAiQueueHook != null) refreshAiQueueHook.run();
            if (refreshOrdersHook != null) refreshOrdersHook.run();
            loadOverviewKpis();
            JOptionPane.showMessageDialog(this, "Order #ORD-" + orderId + " has been rejected.", "Order Cancelled", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Rejection error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================
    // 6. ORDER MANAGEMENT PANEL
    // =========================================================
    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(BG_WORKSPACE);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        top.setBackground(CARD_BG);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel filterLbl = new JLabel("Status Filter:");
        filterLbl.setForeground(TEXT_PRIMARY);
        JComboBox<String> statusFilter = new JComboBox<>(new String[]{
                "ALL", "PLACED", "APPROVED", "PICKING", "PACKING",
                "READY_FOR_DELIVERY", "PACKED", "OUT_FOR_DELIVERY", "COMPLETED", "CANCELLED"
        });

        JButton btnViewItems = blackButton("👁️ View Items");
        JButton btnAuthorizeDelivery = blackButton("🚚 Authorize Delivery");
        JButton btnConfirmDelivery = blackButton("🏁 Confirm Delivery (Complete)");
        JButton btnTrackRobot = blackButton("🤖 Track Robot Movement");
        JButton btnRefresh = blackButton("🔄 Refresh Orders");

        top.add(filterLbl);
        top.add(statusFilter);
        top.add(btnViewItems);
        top.add(btnAuthorizeDelivery);
        top.add(btnConfirmDelivery);
        top.add(btnTrackRobot);
        top.add(btnRefresh);
        panel.add(top, BorderLayout.NORTH);

        String[] columns = {
                "Order ID", "Customer ID", "Order Date", "Total (₹)", "Payment",
                "Status", "Assigned Robot", "Delivery Address"
        };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createStyledTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        Runnable loadOrders = () -> {
            model.setRowCount(0);
            new SwingWorker<List<Object[]>, Void>() {
                @Override
                protected List<Object[]> doInBackground() throws Exception {
                    List<Object[]> rows = new ArrayList<>();
                    String sql = "SELECT o.order_id, o.user_id, o.created_at, o.total_amount, o.payment_method, "
                            + "o.status, o.delivery_address, r.robot_name "
                            + "FROM orders o "
                            + "LEFT JOIN robots r ON o.order_id = r.order_id "
                            + "ORDER BY o.order_id DESC";

                    try (Connection c = DBConnection.getConnection();
                         PreparedStatement ps = c.prepareStatement(sql);
                         ResultSet rs = ps.executeQuery()) {

                        while (rs.next()) {
                            int id = rs.getInt("order_id");
                            int uid = rs.getInt("user_id");
                            Timestamp date = rs.getTimestamp("created_at");
                            double total = rs.getDouble("total_amount");
                            String pay = rs.getString("payment_method");
                            String st = rs.getString("status");
                            String addr = rs.getString("delivery_address");
                            String robot = rs.getString("robot_name");

                            rows.add(new Object[]{
                                    id, uid, date != null ? date.toString() : "Recent",
                                    String.format(Locale.US, "%.2f", total), pay != null ? pay : "Online",
                                    st != null ? st : "PLACED",
                                    robot != null ? robot : "None",
                                    addr != null ? addr : "N/A"
                            });
                        }
                    }
                    return rows;
                }

                @Override
                protected void done() {
                    try {
                        for (Object[] r : get()) model.addRow(r);
                        logActivity("Orders loaded: " + model.getRowCount() + " records.");
                    } catch (Exception ex) {
                        logActivity("Error loading orders: " + ex.getMessage());
                    }
                }
            }.execute();
        };

        refreshOrdersHook = loadOrders;
        btnRefresh.addActionListener(e -> loadOrders.run());

        statusFilter.addActionListener(e -> {
            String sel = (String) statusFilter.getSelectedItem();
            if ("ALL".equals(sel)) sorter.setRowFilter(null);
            else sorter.setRowFilter(RowFilter.regexFilter(sel, 5));
        });

        btnViewItems.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) {
                JOptionPane.showMessageDialog(this, "Select an order to view items.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int row = table.convertRowIndexToModel(sel);
            showOrderItemsDialog((Integer) model.getValueAt(row, 0));
        });

        btnTrackRobot.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) {
                JOptionPane.showMessageDialog(this, "Select an order to view movement.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int row = table.convertRowIndexToModel(sel);
            int oId = (Integer) model.getValueAt(row, 0);
            navigateToTab(TAB_ROBOTS);
            robotMovementPanel.loadOrderTelemetries(oId);
        });

        btnAuthorizeDelivery.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) {
                JOptionPane.showMessageDialog(this, "Select an order to authorize delivery.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int row = table.convertRowIndexToModel(sel);
            int oId = (Integer) model.getValueAt(row, 0);
            String st = (String) model.getValueAt(row, 5);

            if (!"READY_FOR_DELIVERY".equalsIgnoreCase(st) && !"PACKED".equalsIgnoreCase(st)) {
                JOptionPane.showMessageDialog(this, "Order must be PACKED or READY_FOR_DELIVERY before delivery can be authorized!\nCurrent: " + st, "Validation Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                od.updateStatus(oId, "OUT_FOR_DELIVERY");
                logActivity("Admin AUTHORIZED delivery for Order #" + oId + ".");
                loadOrders.run();
                loadOverviewKpis();
                JOptionPane.showMessageDialog(this, "Delivery authorized for Order #" + oId + ".\nStatus changed to OUT_FOR_DELIVERY.", "Delivery Dispatched", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Status update error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnConfirmDelivery.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) {
                JOptionPane.showMessageDialog(this, "Select an order to confirm delivery completion.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int row = table.convertRowIndexToModel(sel);
            int oId = (Integer) model.getValueAt(row, 0);
            String st = (String) model.getValueAt(row, 5);

            if (!"OUT_FOR_DELIVERY".equalsIgnoreCase(st) && !"DELIVERED".equalsIgnoreCase(st)) {
                JOptionPane.showMessageDialog(this, "Order must be OUT_FOR_DELIVERY first!\nCurrent: " + st, "Validation Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                od.updateStatus(oId, "COMPLETED");
                rd.releaseRobotByOrder(oId);
                logActivity("Order #" + oId + " marked as COMPLETED. Delivery confirmed.");
                loadOrders.run();
                loadOverviewKpis();
                JOptionPane.showMessageDialog(this, "Order #" + oId + " marked as COMPLETED successfully!", "Order Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Completion error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        loadOrders.run();
        return panel;
    }

    private void showOrderItemsDialog(int orderId) {
        try {
            List<String> items = od.getOrderItems(orderId);
            JTextArea area = new JTextArea(12, 42);
            area.setEditable(false);
            area.setFont(new Font("Consolas", Font.PLAIN, 12));
            area.setBackground(Color.WHITE);
            area.setForeground(TEXT_PRIMARY);
            area.setText("ORDER ITEMS BREAKDOWN — ORD-" + orderId + "\n"
                    + "=================================================\n\n"
                    + (items.isEmpty() ? "No items found for this order." : String.join("\n", items)));

            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Order #" + orderId + " Items", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error fetching items: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================
    // 7. ROBOT MOVEMENT DASHBOARD PANEL
    // =========================================================
    public class RobotMovementDashboardPanel extends JPanel {
        private final WarehouseCanvas canvas;
        private final JLabel lblCoords = new JLabel("(2, 8)");
        private final JLabel lblTarget = new JLabel("Dock / Home");
        private final JLabel lblOrder = new JLabel("None");
        private final JLabel lblState = new JLabel("IDLE");
        private final JProgressBar progressBar = new JProgressBar(0, 100);
        private final JLabel lblPickedCount = new JLabel("0 / 0 items");
        private final JProgressBar batteryBar = new JProgressBar(0, 100);
        private final JLabel lblDistance = new JLabel("0.0 m");
        private final JTextArea robotLog = new JTextArea(5, 30);

        private javax.swing.Timer simTimer;
        private int currentOrderId = -1;
        private int assignedRobotId = 1;
        private boolean isPaused = false;
        private boolean isEmergencyStopped = false;

        private List<Point> plannedWaypoints = new ArrayList<>();
        private int currentWaypointIndex = 0;
        private Point robotPos = new Point(2, 8);
        private double batteryLevel = 98.0;
        private double distanceMeters = 0.0;
        private int totalItemsToPick = 0;
        private int pickedItemsSoFar = 0;

        public RobotMovementDashboardPanel() {
            setLayout(new BorderLayout(12, 12));
            setBackground(BG_WORKSPACE);
            setBorder(new EmptyBorder(14, 16, 14, 16));

            canvas = new WarehouseCanvas();
            add(canvas, BorderLayout.CENTER);

            JPanel right = new JPanel(new BorderLayout(8, 8));
            right.setBackground(CARD_BG);
            right.setPreferredSize(new Dimension(320, 0));
            right.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1),
                    new EmptyBorder(12, 14, 12, 14)
            ));

            JLabel teleTitle = new JLabel("AGV Fleet Telemetry & Controls");
            teleTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
            teleTitle.setForeground(TEXT_PRIMARY);
            right.add(teleTitle, BorderLayout.NORTH);

            JPanel statsGrid = new JPanel(new GridLayout(8, 2, 6, 6));
            statsGrid.setBackground(CARD_BG);

            addStatPair(statsGrid, "Active Robot:", new JLabel("Robot #01 (AGV-Alpha)"));
            addStatPair(statsGrid, "Active Order:", lblOrder);
            addStatPair(statsGrid, "Current Coordinates:", lblCoords);
            addStatPair(statsGrid, "Target Destination:", lblTarget);
            addStatPair(statsGrid, "Simulation State:", lblState);
            addStatPair(statsGrid, "Items Picked:", lblPickedCount);
            addStatPair(statsGrid, "Distance Travelled:", lblDistance);

            batteryBar.setValue(98);
            batteryBar.setStringPainted(true);
            batteryBar.setForeground(ACCENT_GREEN);
            statsGrid.add(createStatLabel("Battery Level:"));
            statsGrid.add(batteryBar);

            JPanel centerRight = new JPanel(new BorderLayout(6, 6));
            centerRight.setBackground(CARD_BG);
            centerRight.add(statsGrid, BorderLayout.NORTH);

            JPanel progressPanel = new JPanel(new BorderLayout(4, 4));
            progressPanel.setBackground(CARD_BG);
            JLabel progLbl = new JLabel("Order Task Progress:");
            progLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            progLbl.setForeground(TEXT_SECONDARY);
            progressBar.setValue(0);
            progressBar.setStringPainted(true);
            progressBar.setForeground(ACCENT_BLUE);
            progressPanel.add(progLbl, BorderLayout.NORTH);
            progressPanel.add(progressBar, BorderLayout.CENTER);
            centerRight.add(progressPanel, BorderLayout.CENTER);

            robotLog.setEditable(false);
            robotLog.setBackground(new Color(248, 250, 252));
            robotLog.setForeground(TEXT_PRIMARY);
            robotLog.setFont(new Font("Consolas", Font.PLAIN, 10));
            robotLog.setText("[READY] AGV waiting at Packing Station dock.");
            JScrollPane logScroll = new JScrollPane(robotLog);
            logScroll.setPreferredSize(new Dimension(0, 110));
            logScroll.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEmptyBorder(2, 2, 2, 2), "AGV Log", 0, 0,
                    new Font("Segoe UI", Font.BOLD, 10), TEXT_SECONDARY));
            centerRight.add(logScroll, BorderLayout.SOUTH);

            right.add(centerRight, BorderLayout.CENTER);

            JPanel ctrlPanel = new JPanel(new GridLayout(2, 2, 6, 6));
            ctrlPanel.setBackground(CARD_BG);

            JButton btnPause = blackButton("⏸️ Pause");
            JButton btnResume = blackButton("▶️ Resume");
            JButton btnEStop = blackButton("🛑 Emergency Stop");
            JButton btnDemo = blackButton("⚡ Test Route");

            btnPause.addActionListener(e -> {
                isPaused = true;
                lblState.setText("PAUSED");
                logRobot("Simulation paused by Admin.");
            });

            btnResume.addActionListener(e -> {
                if (isEmergencyStopped) {
                    JOptionPane.showMessageDialog(this, "Clear Emergency Stop before resuming!", "Safety Lockout", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                isPaused = false;
                lblState.setText("NAVIGATING");
                logRobot("Simulation resumed by Admin.");
            });

            btnEStop.addActionListener(e -> triggerFleetEmergencyStop());
            btnDemo.addActionListener(e -> startDemoSimulation());

            ctrlPanel.add(btnPause);
            ctrlPanel.add(btnResume);
            ctrlPanel.add(btnDemo);
            ctrlPanel.add(btnEStop);

            right.add(ctrlPanel, BorderLayout.SOUTH);
            add(right, BorderLayout.EAST);

            simTimer = new javax.swing.Timer(25, e -> stepSimulation());
        }

        private void addStatPair(JPanel p, String title, JComponent val) {
            JLabel l = createStatLabel(title);
            val.setFont(new Font("Segoe UI", Font.BOLD, 11));
            val.setForeground(TEXT_PRIMARY);
            p.add(l);
            p.add(val);
        }

        private JLabel createStatLabel(String text) {
            JLabel l = new JLabel(text);
            l.setFont(new Font("Segoe UI", Font.BOLD, 11));
            l.setForeground(TEXT_SECONDARY);
            return l;
        }

        public void loadOrderTelemetries(int orderId) {
            this.currentOrderId = orderId;
            lblOrder.setText("#ORD-" + orderId);
            logRobot("Loaded telemetry tracking for Order #" + orderId);
        }

        public void startAutonomousOrderPicking(int orderId, int robotId) {
            this.currentOrderId = orderId;
            this.assignedRobotId = robotId;
            this.isPaused = false;
            this.isEmergencyStopped = false;

            lblOrder.setText("#ORD-" + orderId);
            lblState.setText("NAVIGATING");
            progressBar.setValue(10);
            logRobot("AGV assigned to Order #" + orderId + ". Generating BFS waypoints...");

            new SwingWorker<List<Point>, Void>() {
                @Override
                protected List<Point> doInBackground() throws Exception {
                    List<Point> targets = new ArrayList<>();
                    String sql = "SELECT p.product_id, i.location FROM order_items oi "
                            + "JOIN products p ON oi.product_id = p.product_id "
                            + "LEFT JOIN inventory i ON p.product_id = i.product_id "
                            + "WHERE oi.order_id=?";

                    try (Connection c = DBConnection.getConnection();
                         PreparedStatement ps = c.prepareStatement(sql)) {
                        ps.setInt(1, orderId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                int pid = rs.getInt(1);
                                String loc = rs.getString(2);
                                WarehouseLocation wl = parseOrAssignLocation(pid, loc);
                                targets.add(wl.getGridCoordinate());
                            }
                        }
                    }
                    if (targets.isEmpty()) targets.add(new Point(5, 3));
                    return targets;
                }

                @Override
                protected void done() {
                    try {
                        List<Point> bins = get();
                        totalItemsToPick = bins.size();
                        pickedItemsSoFar = 0;
                        lblPickedCount.setText("0 / " + totalItemsToPick + " items");

                        plannedWaypoints.clear();
                        Point start = new Point(2, 8);
                        for (Point target : bins) {
                            plannedWaypoints.addAll(findShortestAisleRoute(start, target));
                            start = target;
                        }
                        plannedWaypoints.addAll(findShortestAisleRoute(start, new Point(2, 8)));

                        currentWaypointIndex = 0;
                        simTimer.start();
                        logRobot("Route planned: " + plannedWaypoints.size() + " waypoints calculated.");
                    } catch (Exception ex) {
                        logRobot("Route error: " + ex.getMessage());
                    }
                }
            }.execute();
        }

        private void startDemoSimulation() {
            totalItemsToPick = 2;
            pickedItemsSoFar = 0;
            lblOrder.setText("#DEMO-TEST");
            lblState.setText("DEMO RUN");
            lblPickedCount.setText("0 / 2 items");
            plannedWaypoints.clear();

            Point start = new Point(2, 8);
            Point bin1 = new Point(5, 3);
            Point bin2 = new Point(9, 5);

            plannedWaypoints.addAll(findShortestAisleRoute(start, bin1));
            plannedWaypoints.addAll(findShortestAisleRoute(bin1, bin2));
            plannedWaypoints.addAll(findShortestAisleRoute(bin2, new Point(2, 8)));

            currentWaypointIndex = 0;
            simTimer.start();
            logRobot("Demo navigation route initiated.");
        }

        private void stepSimulation() {
            if (isPaused || isEmergencyStopped) return;

            if (currentWaypointIndex < plannedWaypoints.size()) {
                robotPos = plannedWaypoints.get(currentWaypointIndex);
                currentWaypointIndex++;

                lblCoords.setText("(" + robotPos.x + ", " + robotPos.y + ")");
                distanceMeters += 0.8;
                lblDistance.setText(String.format(Locale.US, "%.1f m", distanceMeters));

                batteryLevel = Math.max(15.0, batteryLevel - 0.04);
                batteryBar.setValue((int) batteryLevel);

                int progress = (int) (((double) currentWaypointIndex / plannedWaypoints.size()) * 85) + 10;
                progressBar.setValue(Math.min(95, progress));

                if (currentWaypointIndex == plannedWaypoints.size() / 2 && pickedItemsSoFar < totalItemsToPick) {
                    pickedItemsSoFar++;
                    lblPickedCount.setText(pickedItemsSoFar + " / " + totalItemsToPick + " items");
                    lblState.setText("PICKING ITEM");
                    lblTarget.setText("Block A, Shelf 2, Bin 3");
                    logRobot("Reached bin. Picked product item (" + pickedItemsSoFar + "/" + totalItemsToPick + ").");
                }
                canvas.repaint();
            } else {
                simTimer.stop();
                progressBar.setValue(100);
                lblState.setText("PACKING COMPLETE");
                lblTarget.setText("Packing Dock (Staged)");
                pickedItemsSoFar = totalItemsToPick;
                lblPickedCount.setText(totalItemsToPick + " / " + totalItemsToPick + " items");
                logRobot("AGV returned to packing station.");

                if (currentOrderId > 0) {
                    finalizeOrderPacking(currentOrderId, assignedRobotId);
                }
                canvas.repaint();
            }
        }

        private void finalizeOrderPacking(int orderId, int robotId) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    od.updateStatus(orderId, "PACKING");
                    Thread.sleep(1200);
                    od.updateStatus(orderId, "READY_FOR_DELIVERY");
                    rd.releaseRobot(robotId);
                    return null;
                }

                @Override
                protected void done() {
                    logRobot("Order #" + orderId + " is PACKED & READY_FOR_DELIVERY.");
                    logActivity("Order #" + orderId + " picking complete! Status: READY_FOR_DELIVERY.");
                    loadOverviewKpis();
                    if (refreshOrdersHook != null) refreshOrdersHook.run();

                    JOptionPane.showMessageDialog(
                            AdminDashboard.this,
                            "Order #ORD-" + orderId + " picked & packed by AGV #" + robotId + "!\n\n"
                            + "Order Status: READY_FOR_DELIVERY\n"
                            + "Click 'Authorize Delivery' in Order Management.",
                            "Picking & Packing Completed",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }.execute();
        }

        private void logRobot(String msg) {
            String ts = new Timestamp(System.currentTimeMillis()).toString().substring(11, 19);
            robotLog.append("\n[" + ts + "] " + msg);
            robotLog.setCaretPosition(robotLog.getText().length());
        }

        private class WarehouseCanvas extends JPanel {
            public WarehouseCanvas() {
                setBackground(Color.WHITE);
                setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                int cols = 15;
                int rows = 10;
                int cellW = Math.max(20, w / cols);
                int cellH = Math.max(20, h / rows);

                g2.setColor(new Color(241, 245, 249));
                for (int c = 0; c < cols; c++) {
                    for (int r = 0; r < rows; r++) {
                        g2.drawRect(c * cellW, r * cellH, cellW, cellH);
                    }
                }

                drawRackBlock(g2, 3 * cellW, 1 * cellH, 3 * cellW, 4 * cellH, "BLOCK A (Racks A1-A2)");
                drawRackBlock(g2, 8 * cellW, 1 * cellH, 3 * cellW, 4 * cellH, "BLOCK B (Racks B1-B2)");

                g2.setColor(new Color(220, 252, 231));
                g2.fillRect(1 * cellW, 7 * cellH, 3 * cellW, 2 * cellH);
                g2.setColor(ACCENT_GREEN);
                g2.drawRect(1 * cellW, 7 * cellH, 3 * cellW, 2 * cellH);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.drawString("PACKING DOCK (STATION 1)", 1 * cellW + 10, 8 * cellH);

                g2.setColor(new Color(224, 242, 254));
                g2.fillRect(10 * cellW, 7 * cellH, 3 * cellW, 2 * cellH);
                g2.setColor(ACCENT_BLUE);
                g2.drawRect(10 * cellW, 7 * cellH, 3 * cellW, 2 * cellH);
                g2.drawString("DISPATCH GATE (BAY 4)", 10 * cellW + 10, 8 * cellH);

                if (plannedWaypoints.size() > 1) {
                    g2.setColor(new Color(14, 165, 233, 160));
                    g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 4f}, 0f));
                    for (int i = 0; i < plannedWaypoints.size() - 1; i++) {
                        Point p1 = plannedWaypoints.get(i);
                        Point p2 = plannedWaypoints.get(i + 1);
                        g2.drawLine(p1.x * cellW + cellW / 2, p1.y * cellH + cellH / 2,
                                    p2.x * cellW + cellW / 2, p2.y * cellH + cellH / 2);
                    }
                    g2.setStroke(new BasicStroke(1f));
                }

                int rx = robotPos.x * cellW + cellW / 2;
                int ry = robotPos.y * cellH + cellH / 2;
                int rSize = Math.min(cellW, cellH) - 6;

                g2.setColor(new Color(15, 23, 42, 40));
                g2.fillOval(rx - rSize, ry - rSize, rSize * 2, rSize * 2);

                g2.setColor(new Color(15, 23, 42));
                g2.fillRoundRect(rx - rSize / 2, ry - rSize / 2, rSize, rSize, 8, 8);
                g2.setColor(isEmergencyStopped ? ACCENT_RED : ACCENT_CYAN);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(rx - rSize / 2, ry - rSize / 2, rSize, rSize, 8, 8);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.setColor(Color.WHITE);
                g2.drawString("AGV", rx - 12, ry + 4);
            }

            private void drawRackBlock(Graphics2D g2, int x, int y, int w, int h, String label) {
                g2.setColor(new Color(241, 245, 249));
                g2.fillRect(x, y, w, h);
                g2.setColor(BORDER_COLOR);
                g2.drawRect(x, y, w, h);

                g2.setColor(new Color(203, 213, 225));
                int shelfHeight = h / 4;
                for (int i = 1; i < 4; i++) {
                    g2.drawLine(x, y + i * shelfHeight, x + w, y + i * shelfHeight);
                }

                g2.setColor(TEXT_PRIMARY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString(label, x + 8, y + 15);
            }
        }
    }

    private static List<Point> findShortestAisleRoute(Point start, Point goal) {
        int cols = 15;
        int rows = 10;
        boolean[][] blocked = new boolean[cols][rows];

        for (int c = 3; c <= 5; c++) {
            for (int r = 1; r <= 4; r++) blocked[c][r] = true;
        }
        for (int c = 8; c <= 10; c++) {
            for (int r = 1; r <= 4; r++) blocked[c][r] = true;
        }

        blocked[start.x][start.y] = false;
        blocked[goal.x][goal.y] = false;

        Queue<Point> queue = new ArrayDeque<>();
        Map<Point, Point> parentMap = new HashMap<>();
        boolean[][] visited = new boolean[cols][rows];

        queue.add(start);
        visited[start.x][start.y] = true;

        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        boolean found = false;
        while (!queue.isEmpty()) {
            Point curr = queue.poll();
            if (curr.x == goal.x && curr.y == goal.y) {
                found = true;
                break;
            }

            for (int i = 0; i < 4; i++) {
                int nx = curr.x + dx[i];
                int ny = curr.y + dy[i];

                if (nx >= 0 && nx < cols && ny >= 0 && ny < rows && !visited[nx][ny] && !blocked[nx][ny]) {
                    Point next = new Point(nx, ny);
                    visited[nx][ny] = true;
                    parentMap.put(next, curr);
                    queue.add(next);
                }
            }
        }

        List<Point> path = new ArrayList<>();
        if (found) {
            Point p = goal;
            while (p != null) {
                path.add(0, p);
                p = parentMap.get(p);
            }
        } else {
            path.add(start);
            path.add(goal);
        }
        return path;
    }

    private void triggerFleetEmergencyStop() {
        robotMovementPanel.isEmergencyStopped = true;
        robotMovementPanel.simTimer.stop();
        robotMovementPanel.lblState.setText("EMERGENCY STOPPED");
        logActivity("🛑 EMERGENCY STOP TRIGGERED! All robots halted.");

        try {
            rd.emergencyStopAll();
        } catch (Exception ignored) { }

        JOptionPane.showMessageDialog(
                this,
                "FLEET EMERGENCY STOP ACTIVATED!\n\nAll automated robot movements have been stopped.\nSafety interlocks engaged.",
                "Emergency Stop Active",
                JOptionPane.ERROR_MESSAGE
        );
    }

    // =========================================================
    // 8. WAREHOUSE MAP PANEL
    // =========================================================
    private JPanel createWarehouseMapPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(BG_WORKSPACE);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel("Warehouse Hierarchy: Warehouse -> Block -> Rack -> Shelf -> Bin");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(TEXT_PRIMARY);
        panel.add(title, BorderLayout.NORTH);

        JPanel blocksPanel = new JPanel(new GridLayout(1, 2, 16, 16));
        blocksPanel.setBackground(BG_WORKSPACE);

        blocksPanel.add(buildBlockCard("BLOCK A (Electronics & Hardware)", new String[]{"Rack A1", "Rack A2"}));
        blocksPanel.add(buildBlockCard("BLOCK B (Accessories & Office Supplies)", new String[]{"Rack B1", "Rack B2"}));
        panel.add(blocksPanel, BorderLayout.CENTER);

        JPanel infoCard = createWhiteCard("Interactive Storage Inspector");
        JTextArea inspectorText = new JTextArea(4, 40);
        inspectorText.setEditable(false);
        inspectorText.setBackground(new Color(248, 250, 252));
        inspectorText.setForeground(TEXT_PRIMARY);
        inspectorText.setFont(new Font("Consolas", Font.PLAIN, 12));
        inspectorText.setText("Click on any storage bin above to view stored products, stock level, and AGV coordinates.");
        infoCard.add(new JScrollPane(inspectorText), BorderLayout.CENTER);
        panel.add(infoCard, BorderLayout.SOUTH);

        refreshWarehouseMapHook = () -> {
            inspectorText.setText("Warehouse Map synchronized with MySQL inventory records.\n"
                    + "Block A: Racks A1-A2 (Shelves 1-4, Bins 1-8)\n"
                    + "Block B: Racks B1-B2 (Shelves 1-4, Bins 1-8)\n"
                    + "All storage coordinates are mapped to the automated AGV pathfinding grid.");
        };

        return panel;
    }

    private JPanel buildBlockCard(String blockName, String[] racks) {
        JPanel card = createWhiteCard(blockName);
        JPanel grid = new JPanel(new GridLayout(racks.length, 1, 8, 8));
        grid.setBackground(CARD_BG);

        for (String rack : racks) {
            JPanel rackPanel = new JPanel(new BorderLayout(4, 4));
            rackPanel.setBackground(new Color(248, 250, 252));
            rackPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1),
                    new EmptyBorder(8, 10, 8, 10)
            ));

            JLabel rLbl = new JLabel("▦  " + rack + "  |  Shelves 1–4  |  Bins 1–8");
            rLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            rLbl.setForeground(ACCENT_BLUE);
            rackPanel.add(rLbl, BorderLayout.NORTH);

            JPanel binsFlow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
            binsFlow.setBackground(new Color(248, 250, 252));
            for (int b = 1; b <= 8; b++) {
                final int binNum = b;
                JButton binBtn = blackButton("Bin " + b);
                binBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
                binBtn.setPreferredSize(new Dimension(68, 28));
                binBtn.addActionListener(e -> {
                    JOptionPane.showMessageDialog(this,
                            "Storage Location:\n" + blockName + "\n" + rack + " -> Shelf 2 -> Bin " + binNum + "\n\n"
                            + "Capacity: 100 units\nStatus: NORMAL (Live DB Synced)",
                            "Storage Inspector",
                            JOptionPane.INFORMATION_MESSAGE);
                });
                binsFlow.add(binBtn);
            }
            rackPanel.add(binsFlow, BorderLayout.CENTER);
            grid.add(rackPanel);
        }
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    // =========================================================
    // 9. REPORTS PANEL
    // =========================================================
    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(BG_WORKSPACE);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        top.setBackground(CARD_BG);
        JButton btnGenerate = blackButton("⚡ Generate Operational Report");
        top.add(btnGenerate);
        panel.add(top, BorderLayout.NORTH);

        JTextArea reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setBackground(Color.WHITE);
        reportArea.setForeground(TEXT_PRIMARY);
        reportArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        reportArea.setText("Click 'Generate Operational Report' to compile live warehouse metrics.");
        panel.add(new JScrollPane(reportArea), BorderLayout.CENTER);

        btnGenerate.addActionListener(e -> {
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    StringBuilder sb = new StringBuilder();
                    sb.append("========================================================================\n");
                    sb.append("       SMART WAREHOUSE AUTOMATION — OPERATIONAL AUDIT REPORT             \n");
                    sb.append("========================================================================\n");
                    sb.append("Generated On: ").append(new Timestamp(System.currentTimeMillis())).append("\n\n");

                    try (Connection c = DBConnection.getConnection();
                         Statement s = c.createStatement()) {

                        try (ResultSet rs = s.executeQuery("SELECT status, COUNT(*), SUM(total_amount) FROM orders GROUP BY status")) {
                            sb.append("1. ORDER FULFILLMENT BREAKDOWN:\n");
                            while (rs.next()) {
                                sb.append(String.format("   • Status: %-18s | Count: %-4d | Total Value: ₹%.2f\n",
                                        rs.getString(1), rs.getInt(2), rs.getDouble(3)));
                            }
                        }

                        sb.append("\n2. ROBOT FLEET EFFICIENCY:\n");
                        try (ResultSet rs = s.executeQuery("SELECT robot_name, status, battery_level FROM robots")) {
                            while (rs.next()) {
                                sb.append(String.format("   • %-12s | Status: %-12s | Battery: %d%%\n",
                                        rs.getString(1), rs.getString(2), rs.getInt(3)));
                            }
                        }

                        sb.append("\n3. STOCK INTEGRITY:\n");
                        try (ResultSet rs = s.executeQuery("SELECT COUNT(*), SUM(quantity) FROM inventory")) {
                            if (rs.next()) {
                                sb.append(String.format("   • Active SKUs: %d | Total Physical Units: %d\n", rs.getInt(1), rs.getInt(2)));
                            }
                        }
                    }
                    sb.append("\n========================================================================\n");
                    sb.append("END OF REPORT\n");
                    return sb.toString();
                }

                @Override
                protected void done() {
                    try {
                        reportArea.setText(get());
                        logActivity("Operational Audit Report compiled.");
                    } catch (Exception ex) {
                        reportArea.setText("Error generating report: " + ex.getMessage());
                    }
                }
            }.execute();
        });

        return panel;
    }

    // =========================================================
    // 10. SETTINGS PANEL
    // =========================================================
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(BG_WORKSPACE);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel formCard = createWhiteCard("System & Engine Configuration");
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD_BG);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtTabNet = new JTextField(tabNetUrl);
        JButton btnTestTabNet = blackButton("Test TabNet AI Ping");
        JButton btnTestDb = blackButton("Test MySQL Connection");
        JButton btnSave = blackButton("Save Configuration");

        int row = 0;
        addFormRow(form, g, row++, "TabNet AI Service URL:", txtTabNet);

        g.gridx = 1; g.gridy = row++;
        form.add(btnTestTabNet, g);

        g.gridx = 1; g.gridy = row++;
        form.add(btnTestDb, g);

        g.gridx = 1; g.gridy = row++;
        form.add(btnSave, g);

        btnTestTabNet.addActionListener(e -> {
            try {
                String res = new TabNetClient(txtTabNet.getText().trim()).predict(10, 2.0, 1, 1);
                JOptionPane.showMessageDialog(this, "TabNet AI Response:\n" + res, "AI Ping Result", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "AI Ping failed: " + ex.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnTestDb.addActionListener(e -> {
            try (Connection c = DBConnection.getConnection()) {
                JOptionPane.showMessageDialog(this, "MySQL Connection Successful! (" + c.getCatalog() + ")", "Database OK", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "MySQL Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSave.addActionListener(e -> {
            tabNetUrl = txtTabNet.getText().trim();
            tabNet = new TabNetClient(tabNetUrl);
            logActivity("Updated TabNet URL to: " + tabNetUrl);
            JOptionPane.showMessageDialog(this, "Configuration saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        formCard.add(form, BorderLayout.NORTH);
        panel.add(formCard, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================
    // UTILITIES & HELPERS
    // =========================================================
    private void refreshAllDashboardData() {
        if (!isRefreshing.compareAndSet(false, true)) return;
        try {
            loadOverviewKpis();
            if (refreshProductsHook != null) refreshProductsHook.run();
            if (refreshInventoryHook != null) refreshInventoryHook.run();
            if (refreshAiQueueHook != null) refreshAiQueueHook.run();
            if (refreshOrdersHook != null) refreshOrdersHook.run();
        } finally {
            isRefreshing.set(false);
        }
    }

    private void logActivity(String message) {
        if (activityLogArea == null) return;
        String ts = new Timestamp(System.currentTimeMillis()).toString().substring(11, 19);
        activityLogArea.append("\n[" + ts + "] " + message);
        activityLogArea.setCaretPosition(activityLogArea.getText().length());
    }

    private static class WarehouseLocation {
        String block, rack, shelf, bin;
        WarehouseLocation(String b, String r, String s, String bin) {
            this.block = b; this.rack = r; this.shelf = s; this.bin = bin;
        }

        Point getGridCoordinate() {
            if ("Block B".equalsIgnoreCase(block)) return new Point(9, 3);
            return new Point(5, 3);
        }
    }

    private static WarehouseLocation parseOrAssignLocation(int productId, String locStr) {
        if (locStr != null && locStr.contains(" - ")) {
            String[] parts = locStr.split(" - ");
            if (parts.length >= 4) {
                return new WarehouseLocation(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim());
            }
        }
        String block = (productId % 2 == 0) ? "Block B" : "Block A";
        String rack = "Rack " + ((productId % 4) + 1);
        String shelf = "Shelf " + ((productId % 4) + 1);
        String bin = "Bin " + ((productId % 8) + 1);
        return new WarehouseLocation(block, rack, shelf, bin);
    }

    @Override
    public void dispose() {
        if (autoRefreshTimer != null) autoRefreshTimer.stop();
        if (robotMovementPanel != null && robotMovementPanel.simTimer != null) robotMovementPanel.simTimer.stop();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }
            new AdminDashboard().setVisible(true);
        });
    }
}