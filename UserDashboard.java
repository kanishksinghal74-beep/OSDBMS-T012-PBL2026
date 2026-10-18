
package com.smartwarehouse;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

public class UserDashboard extends JFrame {

    // ================= USER =================

    private final int userId;
    private final String userName;

    // ================= DAO =================

    private final ProductDAO productDAO = new ProductDAO();

    // ================= CART =================

    private final CartFrame cartFrame;

    // ================= COLORS =================

    private static final Color BG = new Color(3, 15, 31);
    private static final Color SIDEBAR = new Color(5, 23, 44);
    private static final Color PANEL = new Color(9, 30, 54);
    private static final Color CARD = new Color(11, 35, 62);
    private static final Color BLUE = new Color(0, 125, 255);
    private static final Color CYAN = new Color(0, 210, 255);
    private static final Color WHITE = new Color(238, 246, 255);
    private static final Color MUTED = new Color(157, 183, 211);
    private static final Color GREEN = new Color(0, 220, 160);
    private static final Color RED = new Color(255, 90, 100);

    // ================= UI =================

    private final JTextField searchField = new JTextField(22);

    private final JLabel statusLabel =
            new JLabel("Ready to explore the warehouse");

    private final JLabel cartCountLabel = new JLabel(" ");

    private final JLabel productCountLabel =
            new JLabel("0 products");

    private final JPanel productsPanel =
            new JPanel(new GridLayout(0, 3, 16, 16));

    private final JPanel mainContent =
            new JPanel(new BorderLayout(16, 16));

    private final JPanel sidebar = new JPanel();

    private String selectedCategory = "All";

    private List<Product> allProducts = new ArrayList<>();

    // ================= CONSTRUCTOR =================

    public UserDashboard(int userId, String name) {

        this.userId = userId;

        this.userName =
                (name == null || name.trim().isEmpty())
                        ? "User"
                        : name.trim();

        this.cartFrame = new CartFrame(userId);

        setTitle("Smart Warehouse | User Dashboard");
        setSize(1450, 900);
        setMinimumSize(new Dimension(1050, 700));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        buildDashboard();
        refreshProducts();
    }

    // ================= DASHBOARD =================

    private void buildDashboard() {

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        root.add(createHeader(), BorderLayout.NORTH);
        root.add(createSidebar(), BorderLayout.WEST);

        mainContent.setBackground(BG);
        mainContent.setBorder(
                new EmptyBorder(22, 22, 18, 22)
        );

        mainContent.add(
                createWelcomeSection(),
                BorderLayout.NORTH
        );

        mainContent.add(
                createProductSection(),
                BorderLayout.CENTER
        );

        mainContent.add(
                createFooter(),
                BorderLayout.SOUTH
        );

        root.add(mainContent, BorderLayout.CENTER);

        setContentPane(root);
        applyButtonStyleToTree(root);
    }

    // ================= HEADER =================

    private JPanel createHeader() {

        JPanel header = new JPanel(new BorderLayout(20, 0));

        header.setBackground(SIDEBAR);

        header.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(20, 65, 105)),
                        new EmptyBorder(15, 22, 15, 22)
                )
        );

        // BRAND

        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);

        JLabel logo = new JLabel("🐉");
        logo.setFont(
                new Font("Segoe UI Emoji", Font.PLAIN, 32)
        );

        JPanel brandText = new JPanel();

        brandText.setOpaque(false);
        brandText.setLayout(
                new BoxLayout(brandText, BoxLayout.Y_AXIS)
        );

        JLabel title = new JLabel("SMART WAREHOUSE");
        title.setForeground(WHITE);
        title.setFont(
                new Font("Segoe UI", Font.BOLD, 19)
        );

        JLabel subtitle = new JLabel("MANAGEMENT SYSTEM");
        subtitle.setForeground(CYAN);
        subtitle.setFont(
                new Font("Segoe UI", Font.PLAIN, 11)
        );

        brandText.add(title);
        brandText.add(subtitle);

        brand.add(logo, BorderLayout.WEST);
        brand.add(brandText, BorderLayout.CENTER);

        // SEARCH

        JPanel searchBox = new JPanel(
                new BorderLayout(8, 0)
        );

        searchBox.setOpaque(false);

        searchField.setBackground(PANEL);
        searchField.setForeground(WHITE);
        searchField.setCaretColor(CYAN);

        searchField.setFont(
                new Font("Segoe UI", Font.PLAIN, 14)
        );

        searchField.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(35, 85, 130)),
                        new EmptyBorder(10, 12, 10, 12)
                )
        );

        searchField.setToolTipText(
                "Search by product name or category"
        );

        JButton searchButton = createButton("SEARCH");

        searchButton.addActionListener(
                e -> searchProducts()
        );

        searchField.addActionListener(
                e -> searchProducts()
        );

        searchBox.add(
                searchField,
                BorderLayout.CENTER
        );

        searchBox.add(
                searchButton,
                BorderLayout.EAST
        );

        // CART

        JButton cartButton = createButton("🛒 VIEW CART");

        cartButton.addActionListener(
                e -> openCart()
        );

        // LOGOUT

        JButton logoutButton = createWhiteButton("LOGOUT");

        logoutButton.addActionListener(
                e -> logout()
        );

        // RIGHT PANEL

        JPanel right = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 12, 0)
        );

        right.setOpaque(false);

        right.add(cartButton);
        right.add(logoutButton);
        right.add(createUserLabel());

        header.add(brand, BorderLayout.WEST);
        header.add(searchBox, BorderLayout.CENTER);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    // ================= LOGOUT =================

    private void logout() {

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {

            cartFrame.dispose();
            dispose();

            // Assumes LoginFrame has a no-argument constructor.
            new LoginFrame().setVisible(true);
        }
    }

    // ================= USER LABEL =================

    private JLabel createUserLabel() {

        JLabel user = new JLabel(
                "<html><b>" + escapeHtml(userName)
                        + "</b><br><font color='#9DB7D3'>"
                        + "Warehouse User</font></html>"
        );

        user.setForeground(WHITE);

        user.setFont(
                new Font("Segoe UI", Font.PLAIN, 13)
        );

        user.setBorder(
                new EmptyBorder(0, 8, 0, 0)
        );

        return user;
    }

    // ================= SIDEBAR =================

    private JPanel createSidebar() {

        sidebar.setLayout(
                new BoxLayout(sidebar, BoxLayout.Y_AXIS)
        );

        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(215, 0));

        sidebar.setBorder(
                new EmptyBorder(24, 12, 20, 12)
        );

        addSidebarButton("⌂   Home", () -> {
            selectedCategory = "All";
            searchField.setText("");
            displayProducts(allProducts);
        });

        addSidebarButton("▦   Browse Products", () -> {
            selectedCategory = "All";
            searchField.setText("");
            displayProducts(allProducts);
        });

        addSidebarButton("🛒   My Cart", this::openCart);

        addSidebarButton(
                "▣   Order Tracking",
                this::openOrderTracking
        );

        addSidebarButton(
                "▤   My Orders",
                this::openOrderTracking
        );

        addSidebarButton(
                "♙   My Profile",
                this::showProfile
        );

        sidebar.add(Box.createVerticalGlue());

        JLabel dragon = new JLabel("🐉");

        dragon.setFont(
                new Font("Segoe UI Emoji", Font.PLAIN, 42)
        );

        dragon.setForeground(CYAN);
        dragon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tagline = new JLabel(
                "FAST  •  SMART  •  AUTOMATED"
        );

        tagline.setForeground(MUTED);
        tagline.setFont(
                new Font("Segoe UI", Font.BOLD, 9)
        );

        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        sidebar.add(dragon);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(tagline);
        sidebar.add(Box.createVerticalStrut(15));

        return sidebar;
    }

    private void addSidebarButton(
            String text,
            Runnable action
    ) {

        JButton button = createWhiteButton(text);

        button.setHorizontalAlignment(
                SwingConstants.LEFT
        );

        button.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 46)
        );

        button.setPreferredSize(
                new Dimension(190, 46)
        );

        button.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        button.setBorder(
                new EmptyBorder(10, 12, 10, 8)
        );

        button.setFont(
                new Font("Segoe UI", Font.PLAIN, 13)
        );

        button.addActionListener(
                e -> action.run()
        );

        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(7));
    }

    // ================= WELCOME SECTION =================

    private JPanel createWelcomeSection() {

        JPanel section = new JPanel(
                new BorderLayout(10, 12)
        );

        section.setOpaque(false);

        section.setBorder(
                new EmptyBorder(0, 0, 8, 0)
        );

        JPanel textPanel = new JPanel();

        textPanel.setOpaque(false);

        textPanel.setLayout(
                new BoxLayout(textPanel, BoxLayout.Y_AXIS)
        );

        JLabel welcome = new JLabel(
                "Welcome Back, " + escapeHtml(userName) + "!"
        );

        welcome.setForeground(WHITE);

        welcome.setFont(
                new Font("Segoe UI", Font.BOLD, 26)
        );

        JLabel description = new JLabel(
                "Discover products and manage your warehouse orders."
        );

        description.setForeground(MUTED);

        description.setFont(
                new Font("Segoe UI", Font.PLAIN, 14)
        );

        textPanel.add(welcome);
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(description);

        JPanel stats = new JPanel(
                new GridLayout(1, 3, 12, 0)
        );

        stats.setOpaque(false);

        stats.add(
                createStatCard(
                        "PRODUCTS",
                        "Browse Catalog",
                        BLUE
                )
        );

        stats.add(
                createStatCard(
                        "YOUR CART",
                        "Ready to Shop",
                        GREEN
                )
        );

        stats.add(
                createStatCard(
                        "ORDERS",
                        "Track Deliveries",
                        new Color(145, 95, 255)
                )
        );

        section.add(textPanel, BorderLayout.NORTH);
        section.add(stats, BorderLayout.CENTER);

        return section;
    }

    private JPanel createStatCard(
            String heading,
            String description,
            Color accent
    ) {

        JPanel card = new JPanel();

        card.setLayout(
                new BoxLayout(card, BoxLayout.Y_AXIS)
        );

        card.setBackground(PANEL);

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(27, 68, 105)),
                        new EmptyBorder(13, 15, 13, 15)
                )
        );

        JLabel h = new JLabel(heading);

        h.setForeground(accent);

        h.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );

        JLabel d = new JLabel(description);

        d.setForeground(WHITE);

        d.setFont(
                new Font("Segoe UI", Font.BOLD, 15)
        );

        card.add(h);
        card.add(Box.createVerticalStrut(7));
        card.add(d);

        return card;
    }

    // ================= PRODUCT SECTION =================

    private JPanel createProductSection() {

        JPanel section = new JPanel(
                new BorderLayout(0, 12)
        );

        section.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout());

        top.setOpaque(false);

        JLabel heading = new JLabel("AVAILABLE PRODUCTS");

        heading.setForeground(WHITE);

        heading.setFont(
                new Font("Segoe UI", Font.BOLD, 20)
        );

        top.add(heading, BorderLayout.WEST);
        top.add(productCountLabel, BorderLayout.EAST);

        productCountLabel.setForeground(CYAN);

        productCountLabel.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );

        JPanel categoryPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 0)
        );

        categoryPanel.setOpaque(false);

        String[] categories = {
                "All",
                "Electronics",
                "Home & Kitchen",
                "Fashion",
                "Sports",
                "Books",
                "Others"
        };

        for (String category : categories) {

            JButton button = createWhiteButton(category);

            button.addActionListener(e -> {
                selectedCategory = category;
                filterProducts();
            });

            categoryPanel.add(button);
        }

        JPanel headingArea = new JPanel();

        headingArea.setOpaque(false);

        headingArea.setLayout(
                new BoxLayout(headingArea, BoxLayout.Y_AXIS)
        );

        headingArea.add(top);
        headingArea.add(Box.createVerticalStrut(13));
        headingArea.add(categoryPanel);

        productsPanel.setBackground(BG);

        productsPanel.setBorder(
                new EmptyBorder(2, 2, 10, 2)
        );

        JScrollPane scroll = new JScrollPane(productsPanel);

        scroll.setBorder(
                BorderFactory.createEmptyBorder()
        );

        scroll.getViewport().setBackground(BG);

        scroll.getVerticalScrollBar().setUnitIncrement(18);

        section.add(headingArea, BorderLayout.NORTH);
        section.add(scroll, BorderLayout.CENTER);

        return section;
    }

    // ================= LOAD PRODUCTS =================

    private void refreshProducts() {

        try {

            List<Product> products = productDAO.getProducts();

            allProducts = new ArrayList<>(products);

            filterProducts();

            statusLabel.setText(
                    "Products loaded successfully: "
                            + allProducts.size()
            );

        } catch (Exception e) {

            statusLabel.setText("Unable to load products.");

            JOptionPane.showMessageDialog(
                    this,
                    "Could not load products.\n" + e.getMessage(),
                    "Product Loading Error",
                    JOptionPane.ERROR_MESSAGE
            );

            e.printStackTrace();
        }
    }

    // ================= SEARCH AND FILTER =================

    private void searchProducts() {
        filterProducts();
    }

    private void filterProducts() {

        String keyword = searchField.getText()
                .trim()
                .toLowerCase(Locale.ROOT);

        List<Product> filtered = new ArrayList<>();

        for (Product product : allProducts) {

            String name = product.name == null
                    ? ""
                    : product.name.toLowerCase(Locale.ROOT);

            String category = product.category == null
                    ? ""
                    : product.category.toLowerCase(Locale.ROOT);

            boolean matchesSearch =
                    name.contains(keyword)
                            || category.contains(keyword)
                            || String.valueOf(product.id)
                            .contains(keyword);

            boolean matchesCategory =
                    selectedCategory.equalsIgnoreCase("All")
                            || category.equalsIgnoreCase(
                                    selectedCategory
                            );

            if (matchesSearch && matchesCategory) {
                filtered.add(product);
            }
        }

        displayProducts(filtered);
    }

    private void displayProducts(List<Product> products) {

        productsPanel.removeAll();

        if (products.isEmpty()) {

            productsPanel.setLayout(
                    new BorderLayout()
            );

            JLabel empty = new JLabel(
                    "No products found. Try another search.",
                    SwingConstants.CENTER
            );

            empty.setForeground(MUTED);

            empty.setFont(
                    new Font("Segoe UI", Font.PLAIN, 16)
            );

            productsPanel.add(
                    empty,
                    BorderLayout.CENTER
            );

        } else {

            productsPanel.setLayout(
                    new GridLayout(0, 3, 16, 16)
            );

            for (Product product : products) {
                productsPanel.add(
                        createProductCard(product)
                );
            }
        }

        productCountLabel.setText(
                products.size() + " products"
        );

        productsPanel.revalidate();
        productsPanel.repaint();
    }

    // ================= PRODUCT CARD =================

    private JPanel createProductCard(Product product) {

        JPanel card = new JPanel(
                new BorderLayout(8, 10)
        );

        card.setBackground(CARD);

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(29, 78, 120)),
                        new EmptyBorder(12, 12, 12, 12)
                )
        );

        // PRODUCT IMAGE

        JLabel imageLabel = new JLabel(
                "",
                SwingConstants.CENTER
        );

        imageLabel.setPreferredSize(
                new Dimension(180, 140)
        );

        ImageIcon image = getProductImage(product.id);

        if (image != null) {

            imageLabel.setIcon(image);

        } else {

            imageLabel.setText("PRODUCT");
            imageLabel.setForeground(CYAN);

            imageLabel.setFont(
                    new Font("Segoe UI", Font.BOLD, 18)
            );
        }

        // INFORMATION

        JPanel information = new JPanel();

        information.setOpaque(false);

        information.setLayout(
                new BoxLayout(information, BoxLayout.Y_AXIS)
        );

        JLabel name = new JLabel(
                escapeHtml(
                        product.name == null
                                ? "Product"
                                : product.name
                )
        );

        name.setForeground(WHITE);

        name.setFont(
                new Font("Segoe UI", Font.BOLD, 16)
        );

        JLabel category = new JLabel(
                product.category == null
                        ? "General"
                        : escapeHtml(product.category)
        );

        category.setForeground(MUTED);

        category.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );

        JLabel price = new JLabel(
                String.format(
                        Locale.US,
                        "₹ %.2f",
                        product.price
                )
        );

        price.setForeground(CYAN);

        price.setFont(
                new Font("Segoe UI", Font.BOLD, 19)
        );

        JLabel stock = new JLabel(
                product.stock > 0
                        ? "● In Stock: " + product.stock
                        : "● Out of Stock"
        );

        stock.setForeground(
                product.stock > 0 ? GREEN : RED
        );

        stock.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );

        information.add(name);
        information.add(Box.createVerticalStrut(7));
        information.add(category);
        information.add(Box.createVerticalStrut(9));
        information.add(price);
        information.add(Box.createVerticalStrut(7));
        information.add(stock);

        // BUTTONS

        JButton details = createWhiteButton("DETAILS");

        details.addActionListener(
                e -> showProductDetails(product)
        );

        JButton add = createWhiteButton("ADD TO CART");

        add.setEnabled(product.stock > 0);

        add.addActionListener(
                e -> addToCart(product)
        );

        JPanel buttons = new JPanel(
                new GridLayout(1, 2, 8, 0)
        );

        buttons.setOpaque(false);

        buttons.add(details);
        buttons.add(add);

        JPanel center = new JPanel(
                new BorderLayout(0, 10)
        );

        center.setOpaque(false);

        center.add(imageLabel, BorderLayout.NORTH);
        center.add(information, BorderLayout.CENTER);

        card.add(center, BorderLayout.CENTER);
        card.add(buttons, BorderLayout.SOUTH);

        return card;
    }

   
  
    // ================= PRODUCT IMAGE =================

  
private ImageIcon getProductImage(int productId) {

    String[] extensions = {".png", ".jpg", ".jpeg"};

    for (String extension : extensions) {

        String path = "/Images/product_" + productId + extension;

        java.net.URL url = getClass().getResource(path);

        if (url != null) {

            ImageIcon icon = loadScaledImage(url);

            if (icon != null) {
                System.out.println(
                        "[IMAGE LOADED] Product " + productId
                                + " -> " + path
                );

                return icon;
            }
        }
    }

    System.out.println(
            "[IMAGE NOT FOUND] Product ID: " + productId
    );

    return null;
}

private ImageIcon loadScaledImage(java.net.URL url) {

    ImageIcon original = new ImageIcon(url);

    if (original.getIconWidth() <= 0
            || original.getIconHeight() <= 0) {
        return null;
    }

    Image scaled = original.getImage().getScaledInstance(
            180,
            130,
            Image.SCALE_SMOOTH
    );

    return new ImageIcon(scaled);
}




    // ================= PRODUCT DETAILS =================

    private void showProductDetails(Product product) {

        JPanel panel = new JPanel();

        panel.setBackground(PANEL);

        panel.setLayout(
                new BoxLayout(panel, BoxLayout.Y_AXIS)
        );

        panel.setBorder(
                new EmptyBorder(15, 15, 15, 15)
        );

        JLabel name = new JLabel(
                "Product: "
                        + (product.name == null
                        ? ""
                        : escapeHtml(product.name))
        );

        JLabel id = new JLabel(
                "Product ID: " + product.id
        );

        JLabel category = new JLabel(
                "Category: "
                        + (product.category == null
                        ? "General"
                        : escapeHtml(product.category))
        );

        JLabel price = new JLabel(
                String.format(
                        Locale.US,
                        "Price: ₹ %.2f",
                        product.price
                )
        );

        JLabel stock = new JLabel(
                "Available Stock: " + product.stock
        );

        JLabel note = new JLabel(
                product.stock > 0
                        ? "Available to add to cart."
                        : "Currently out of stock."
        );

        for (JLabel label : new JLabel[]{
                name, id, category, price, stock, note
        }) {

            label.setForeground(WHITE);

            label.setFont(
                    new Font("Segoe UI", Font.PLAIN, 14)
            );

            label.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            panel.add(label);
            panel.add(Box.createVerticalStrut(10));
        }

        JOptionPane.showMessageDialog(
                this,
                panel,
                "Product Details",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // ================= ADD TO CART =================

    private void addToCart(Product product) {

        if (product.stock <= 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "This product is out of stock.",
                    "Out of Stock",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        try {

            cartFrame.addProduct(
                    product.id,
                    product.name,
                    product.price,
                    product.stock
            );

            statusLabel.setText(
                    product.name + " added to cart."
            );

            cartFrame.setVisible(true);
            cartFrame.toFront();

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to add product to cart.\n"
                            + e.getMessage(),
                    "Cart Error",
                    JOptionPane.ERROR_MESSAGE
            );

            e.printStackTrace();
        }
    }

    // ================= CART =================

    private void openCart() {

        cartFrame.setVisible(true);
        cartFrame.toFront();
    }

    // ================= ORDER TRACKING =================

    private void openOrderTracking() {

        try {

            new OrderTrackingFrame(userId).setVisible(true);

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to open order tracking.\n"
                            + e.getMessage(),
                    "Order Tracking",
                    JOptionPane.ERROR_MESSAGE
            );

            e.printStackTrace();
        }
    }

    // ================= USER PROFILE =================

    private void showProfile() {

        JOptionPane.showMessageDialog(
                this,
                "User Name: " + userName
                        + "\nUser ID: " + userId
                        + "\nAccount: Active",
                "My Profile",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // ================= FOOTER =================

    private JPanel createFooter() {

        JPanel footer = new JPanel(
                new BorderLayout()
        );

        footer.setOpaque(false);

        footer.setBorder(
                new EmptyBorder(10, 0, 0, 0)
        );

        statusLabel.setForeground(MUTED);

        statusLabel.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );

        JLabel tagline = new JLabel(
                "EFFICIENCY  •  ACCURACY  •  AUTOMATION"
        );

        tagline.setForeground(CYAN);

        tagline.setFont(
                new Font("Segoe UI", Font.BOLD, 10)
        );

        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(tagline, BorderLayout.EAST);

        return footer;
    }

    // ================= BLUE BUTTON =================

    private JButton createButton(String text) {

        JButton button = new JButton(text);

        button.setBackground(BLUE);
        button.setForeground(Color.WHITE);

        button.setFont(
                new Font("Segoe UI", Font.BOLD, 11)
        );

        button.setFocusPainted(false);
        button.setBorder(
                new EmptyBorder(10, 12, 10, 12)
        );

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        button.setOpaque(true);
        button.setContentAreaFilled(true);

        button.addMouseListener(
                new java.awt.event.MouseAdapter() {

                    @Override
                    public void mouseEntered(
                            java.awt.event.MouseEvent e
                    ) {
                        if (button.isEnabled()) {
                            button.setBackground(
                                    new Color(0, 155, 255)
                            );
                        }
                    }

                    @Override
                    public void mouseExited(
                            java.awt.event.MouseEvent e
                    ) {
                        button.setBackground(BLUE);
                    }
                }
        );

        return button;
    }

    // ================= WHITE BUTTON / BLACK TEXT =================

    private JButton createWhiteButton(String text) {

        JButton button = new JButton(text);

        button.setBackground(Color.WHITE);
        button.setForeground(Color.BLACK);

        button.setFont(
                new Font("Segoe UI", Font.BOLD, 11)
        );

        button.setFocusPainted(false);

        button.setBorder(
                new LineBorder(
                        new Color(210, 220, 230),
                        1
                )
        );

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        button.setOpaque(true);
        button.setContentAreaFilled(true);

        button.addMouseListener(
                new java.awt.event.MouseAdapter() {

                    @Override
                    public void mouseEntered(
                            java.awt.event.MouseEvent e
                    ) {
                        if (button.isEnabled()) {
                            button.setBackground(
                                    new Color(225, 240, 255)
                            );

                            button.setForeground(Color.BLACK);
                        }
                    }

                    @Override
                    public void mouseExited(
                            java.awt.event.MouseEvent e
                    ) {
                        button.setBackground(Color.WHITE);
                        button.setForeground(Color.BLACK);
                    }
                }
        );

        return button;
    }

    // ================= BUTTON STYLING =================

    private void applyButtonStyleToTree(
            Container container
    ) {

        for (Component component : container.getComponents()) {

            if (component instanceof JButton) {

                JButton button = (JButton) component;

                button.setFocusPainted(false);
            }

            if (component instanceof Container) {

                applyButtonStyleToTree(
                        (Container) component
                );
            }
        }
    }

    // ================= HTML SAFETY =================

    private String escapeHtml(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
