package com.smartwarehouse;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CartFrame extends JFrame {

    // ================= THEME =================

    private final Color navy = new Color(19, 35, 69);
    private final Color blue = new Color(40, 100, 210);
    private final Color background = new Color(245, 247, 251);

    // ================= USER =================

    private final int userId;

    // ================= CART ITEM =================

    static class CartItem {

        int productId;
        String productName;
        double price;
        int quantity;
        int stock;

        CartItem(
                int productId,
                String productName,
                double price,
                int quantity,
                int stock
        ) {
            this.productId = productId;
            this.productName = productName;
            this.price = price;
            this.quantity = quantity;
            this.stock = stock;
        }

        double getTotal() {
            return price * quantity;
        }
    }

    // ================= CART =================

    final List<CartItem> cart = new ArrayList<>();

    // ================= TABLE MODEL =================

    private final DefaultTableModel model =
            new DefaultTableModel(
                    new String[]{
                            "Product ID",
                            "Product",
                            "Price",
                            "Quantity",
                            "Total",
                            "Available Stock"
                    },
                    0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JTable table = new JTable(model);

    // ================= TOTAL =================

    private final JLabel totalLabel = new JLabel("Total: ₹0.00");

    // ================= CONSTRUCTOR =================

    public CartFrame(int userId) {

        this.userId = userId;

        setTitle("Smart Warehouse | Shopping Cart");
        setSize(950, 600);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        build();
        refreshCart();
    }

    // ================= BUILD UI =================

    private void build() {

        JPanel root = new JPanel(new BorderLayout(15, 15));
        root.setBackground(background);
        root.setBorder(new EmptyBorder(18, 18, 18, 18));

        // ================= HEADER =================

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(navy);
        header.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("MY SHOPPING CART");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Review your products before checkout");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitle.setForeground(new Color(215, 225, 240));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(title);
        heading.add(Box.createVerticalStrut(5));
        heading.add(subtitle);

        header.add(heading, BorderLayout.WEST);

        // ================= TABLE =================

        table.setRowHeight(36);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setShowGrid(true);
        table.setGridColor(new Color(225, 230, 238));
        table.setSelectionBackground(new Color(220, 232, 255));
        table.setSelectionForeground(Color.BLACK);

        table.getTableHeader().setFont(
                new Font("Arial", Font.BOLD, 13)
        );

        table.getTableHeader().setBackground(
                new Color(230, 236, 246)
        );

        table.getTableHeader().setForeground(navy);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(
                BorderFactory.createLineBorder(
                        new Color(220, 226, 236)
                )
        );

        // ================= CART CONTROLS =================

        JButton minus = new JButton("−");
        JButton plus = new JButton("+");
        JButton remove = new JButton("REMOVE");
        JButton clear = new JButton("CLEAR CART");

        styleSecondaryButton(minus);
        styleSecondaryButton(plus);
        styleSecondaryButton(remove);
        styleSecondaryButton(clear);

        JPanel controls = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 7, 5)
        );
        controls.setOpaque(false);

        controls.add(minus);
        controls.add(plus);
        controls.add(remove);
        controls.add(clear);

        // ================= TOTAL PANEL =================

        totalLabel.setFont(new Font("Arial", Font.BOLD, 22));
        totalLabel.setForeground(navy);

        JPanel totalPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 10, 8)
        );
        totalPanel.setBackground(Color.WHITE);
        totalPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(220, 226, 236)
                        ),
                        new EmptyBorder(4, 10, 4, 10)
                )
        );

        totalPanel.add(totalLabel);

        // ================= CHECKOUT =================

        JButton checkout = new JButton("PROCEED TO CHECKOUT");

        checkout.setFont(new Font("Arial", Font.BOLD, 14));
        checkout.setForeground(Color.WHITE);
        checkout.setBackground(blue);
        checkout.setFocusPainted(false);
        checkout.setBorder(
                new EmptyBorder(13, 20, 13, 20)
        );
        checkout.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        checkout.setToolTipText(
                "Continue to delivery details and demo payment"
        );

        // ================= BOTTOM =================

        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.setOpaque(false);

        bottom.add(controls, BorderLayout.WEST);
        bottom.add(totalPanel, BorderLayout.CENTER);
        bottom.add(checkout, BorderLayout.EAST);

        // ================= ACTION LISTENERS =================

        minus.addActionListener(e -> decreaseQuantity());

        plus.addActionListener(e -> increaseQuantity());

        remove.addActionListener(e -> removeSelected());

        clear.addActionListener(e -> clearCart());

        checkout.addActionListener(e -> openCheckout());

        // ================= ROOT =================

        root.add(header, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ================= BUTTON STYLE =================

    private void styleSecondaryButton(JButton button) {

        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBackground(Color.WHITE);
        button.setForeground(navy);
        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(205, 214, 228)
                        ),
                        new EmptyBorder(9, 12, 9, 12)
                )
        );
        button.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
    }

    // ================= ADD PRODUCT =================

    public void addProduct(
            int productId,
            String productName,
            double price,
            int stock
    ) {

        if (productId <= 0 || productName == null
                || price < 0 || stock < 0
                || Double.isNaN(price)
                || Double.isInfinite(price)) {

            JOptionPane.showMessageDialog(
                    this,
                    "Invalid product information.",
                    "Product Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (stock <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "This product is out of stock.",
                    "Out of Stock",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        for (CartItem item : cart) {

            if (item.productId == productId) {

                if (item.quantity >= item.stock) {

                    JOptionPane.showMessageDialog(
                            this,
                            "Maximum available stock reached.",
                            "Stock Limit",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                item.quantity++;
                refreshCart();
                return;
            }
        }

        cart.add(
                new CartItem(
                        productId,
                        productName,
                        price,
                        1,
                        stock
                )
        );

        refreshCart();
    }

    // ================= INCREASE QUANTITY =================

    private void increaseQuantity() {

        CartItem item = getSelectedItem();

        if (item == null) {
            return;
        }

        if (item.quantity >= item.stock) {

            JOptionPane.showMessageDialog(
                    this,
                    "Available stock limit reached.",
                    "Stock Limit",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        item.quantity++;
        refreshCart();
    }

    // ================= DECREASE QUANTITY =================

    private void decreaseQuantity() {

        CartItem item = getSelectedItem();

        if (item == null) {
            return;
        }

        if (item.quantity > 1) {
            item.quantity--;
        } else {
            cart.remove(item);
        }

        refreshCart();
    }

    // ================= GET SELECTED ITEM =================

    private CartItem getSelectedItem() {

        int viewRow = table.getSelectedRow();

        if (viewRow == -1) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please select a product first.",
                    "Selection Required",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        int row = table.convertRowIndexToModel(viewRow);

        if (row < 0 || row >= cart.size()) {
            return null;
        }

        return cart.get(row);
    }

    // ================= REMOVE PRODUCT =================

    private void removeSelected() {

        CartItem item = getSelectedItem();

        if (item == null) {
            return;
        }

        cart.remove(item);
        refreshCart();
    }

    // ================= CLEAR CART =================

    private void clearCart() {

        if (cart.isEmpty()) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Remove all products from your cart?",
                "Clear Cart",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            cart.clear();
            refreshCart();
        }
    }

    // ================= REFRESH CART =================

    void refreshCart() {

        model.setRowCount(0);

        double grandTotal = 0;

        for (CartItem item : cart) {

            double itemTotal = item.getTotal();
            grandTotal += itemTotal;

            model.addRow(
                    new Object[]{
                            item.productId,
                            item.productName,
                            String.format("₹%.2f", item.price),
                            item.quantity,
                            String.format("₹%.2f", itemTotal),
                            item.stock
                    }
            );
        }

        totalLabel.setText(
                String.format("Total: ₹%.2f", grandTotal)
        );

        table.clearSelection();
    }

    // ================= GET GRAND TOTAL =================

    public double getGrandTotal() {

        double total = 0;

        for (CartItem item : cart) {
            total += item.getTotal();
        }

        return total;
    }

    // ================= CHECKOUT =================

    private void openCheckout() {

        if (cart.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Your cart is empty. Add products before checkout.",
                    "Empty Cart",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        double total = getGrandTotal();

        if (total <= 0
                || Double.isNaN(total)
                || Double.isInfinite(total)) {

            JOptionPane.showMessageDialog(
                    this,
                    "Your cart total is invalid.",
                    "Checkout Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        try {

            CheckoutFrame checkoutFrame =
                    new CheckoutFrame(this, userId);

            checkoutFrame.setVisible(true);

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to open checkout.\n" + e.getMessage(),
                    "Checkout Error",
                    JOptionPane.ERROR_MESSAGE
            );

            e.printStackTrace();
        }
    }

    // ================= GET CART ITEMS =================

    public List<CartItem> getCartItems() {

        return new ArrayList<>(cart);
    }

    // ================= GET USER ID =================

    public int getUserId() {

        return userId;
    }

    // ================= TEST MAIN =================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            CartFrame frame = new CartFrame(1);
            frame.setVisible(true);

            frame.addProduct(
                    101,
                    "Laptop",
                    55000,
                    10
            );

            frame.addProduct(
                    102,
                    "Keyboard",
                    1500,
                    20
            );
        });
    }
}

