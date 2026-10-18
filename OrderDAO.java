package com.smartwarehouse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class OrderDAO {

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");

    // =====================================================
    // CREATE ORDER - DEFAULT NORMAL DELIVERY
    // =====================================================

    public int createOrder(
            int userId,
            String deliveryAddress,
            double totalAmount,
            String paymentMethod,
            List<CartFrame.CartItem> cartItems
    ) throws SQLException {

        return createOrder(
                userId,
                deliveryAddress,
                totalAmount,
                paymentMethod,
                "NORMAL",
                cartItems
        );
    }

   

    public int createOrder(
            int userId,
            String deliveryAddress,
            double totalAmount,
            String paymentMethod,
            String deliveryType,
            List<CartFrame.CartItem> cartItems
    ) throws SQLException {

        if (userId <= 0) {
            throw new SQLException("Invalid user ID.");
        }

        if (cartItems == null || cartItems.isEmpty()) {
            throw new SQLException("Cart is empty.");
        }

        if (!Double.isFinite(totalAmount) || totalAmount < 0) {
            throw new SQLException("Invalid order total.");
        }

        if (deliveryType == null || deliveryType.trim().isEmpty()) {
            deliveryType = "NORMAL";
        }

        String orderSQL =
                "INSERT INTO orders " +
                "(user_id, delivery_address, total_amount, " +
                "payment_method, delivery_type, status, order_date) " +
                "VALUES (?, ?, ?, ?, ?, 'PLACED', ?)";

        String itemSQL =
                "INSERT INTO order_items " +
                "(order_id, product_id, quantity, unit_price) " +
                "VALUES (?, ?, ?, ?)";

        String stockSQL =
                "UPDATE inventory " +
                "SET quantity = quantity - ? " +
                "WHERE product_id = ? AND quantity >= ?";

        try (Connection c = DBConnection.getConnection()) {

            c.setAutoCommit(false);

            try {
                int orderId;

                // STEP 1: CREATE ORDER WITH INDIA LOCAL TIME
                LocalDateTime indiaNow =
                        LocalDateTime.now(INDIA_ZONE);

                Timestamp orderTimestamp =
                        Timestamp.valueOf(indiaNow);

                try (PreparedStatement ps = c.prepareStatement(
                        orderSQL,
                        Statement.RETURN_GENERATED_KEYS)) {

                    ps.setInt(1, userId);
                    ps.setString(2, deliveryAddress);
                    ps.setDouble(3, totalAmount);
                    ps.setString(4, paymentMethod);
                    ps.setString(5, deliveryType);
                    ps.setTimestamp(6, orderTimestamp);

                    int affected = ps.executeUpdate();

                    if (affected != 1) {
                        throw new SQLException(
                                "Order could not be created."
                        );
                    }

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Order ID could not be generated."
                            );
                        }

                        orderId = rs.getInt(1);
                    }
                }

                // STEP 2: INSERT ORDER ITEMS
                try (PreparedStatement ps = c.prepareStatement(itemSQL)) {

                    for (CartFrame.CartItem item : cartItems) {

                        if (item == null
                                || item.productId <= 0
                                || item.quantity <= 0
                                || !Double.isFinite(item.price)
                                || item.price < 0) {

                            throw new SQLException(
                                    "Invalid product, quantity or price in cart."
                            );
                        }

                        ps.setInt(1, orderId);
                        ps.setInt(2, item.productId);
                        ps.setInt(3, item.quantity);
                        ps.setDouble(4, item.price);

                        ps.addBatch();
                    }

                    ps.executeBatch();
                }

                // STEP 3: DEDUCT INVENTORY
                try (PreparedStatement ps = c.prepareStatement(stockSQL)) {

                    for (CartFrame.CartItem item : cartItems) {

                        ps.setInt(1, item.quantity);
                        ps.setInt(2, item.productId);
                        ps.setInt(3, item.quantity);

                        int updated = ps.executeUpdate();

                        if (updated != 1) {
                            throw new SQLException(
                                    "Insufficient stock or product not found: "
                                            + item.productName
                                            + ". Requested quantity: "
                                            + item.quantity
                            );
                        }
                    }
                }

                // STEP 4: COMMIT TRANSACTION
                c.commit();

                return orderId;

            } catch (SQLException | RuntimeException ex) {

                try {
                    c.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }

                throw ex;

            } finally {

                try {
                    c.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // Connection is closed after leaving try-with-resources.
                }
            }
        }
    }

    // =====================================================
    // GET ORDERS FOR ONE USER
    // =====================================================

    public List<Order> getOrdersByUser(int userId)
            throws SQLException {

        if (userId <= 0) {
            throw new SQLException("Invalid user ID.");
        }

        List<Order> list = new ArrayList<>();

        String sql =
                "SELECT order_id, user_id, delivery_address, " +
                "total_amount, payment_method, status, order_date " +
                "FROM orders " +
                "WHERE user_id = ? " +
                "ORDER BY order_date DESC";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        }

        return list;
    }

    // =====================================================
    // GET ONE ORDER
    // =====================================================

    public Order getOrder(int orderId) throws SQLException {

        if (orderId <= 0) {
            throw new SQLException("Invalid order ID.");
        }

        String sql =
                "SELECT order_id, user_id, delivery_address, " +
                "total_amount, payment_method, status, order_date " +
                "FROM orders WHERE order_id = ?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapOrder(rs);
                }
            }
        }

        return null;
    }

    // =====================================================
    // GET ORDER FOR A SPECIFIC USER
    // =====================================================

    public Order getOrderForUser(int orderId, int userId)
            throws SQLException {

        if (orderId <= 0 || userId <= 0) {
            throw new SQLException("Invalid order ID or user ID.");
        }

        String sql =
                "SELECT order_id, user_id, delivery_address, " +
                "total_amount, payment_method, status, order_date " +
                "FROM orders " +
                "WHERE order_id = ? AND user_id = ?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapOrder(rs);
                }
            }
        }

        return null;
    }

    // =====================================================
    // MAP DATABASE ROW TO ORDER
    // =====================================================

    private Order mapOrder(ResultSet rs) throws SQLException {

        Order order = new Order();

        order.setOrderId(rs.getInt("order_id"));
        order.setUserId(rs.getInt("user_id"));
        order.setDeliveryAddress(
                rs.getString("delivery_address")
        );
        order.setTotalAmount(rs.getDouble("total_amount"));
        order.setPaymentMethod(
                rs.getString("payment_method")
        );
        order.setStatus(rs.getString("status"));

        Timestamp orderDate = rs.getTimestamp("order_date");
        order.setOrderDate(orderDate);

        return order;
    }

    // =====================================================
    // UPDATE ORDER STATUS
    // =====================================================

    public boolean updateStatus(int orderId, String status)
            throws SQLException {

        if (orderId <= 0) {
            throw new SQLException("Invalid order ID.");
        }

        if (status == null || status.trim().isEmpty()) {
            throw new SQLException("Order status is required.");
        }

        String sql =
                "UPDATE orders SET status = ? WHERE order_id = ?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, status.trim());
            ps.setInt(2, orderId);

            return ps.executeUpdate() > 0;
        }
    }

    // =====================================================
    // GET ORDER ITEMS
    // =====================================================

    public List<String> getOrderItems(int orderId)
            throws SQLException {

        if (orderId <= 0) {
            throw new SQLException("Invalid order ID.");
        }

        List<String> list = new ArrayList<>();

        String sql =
                "SELECT p.product_name, oi.quantity, oi.unit_price " +
                "FROM order_items oi " +
                "JOIN products p ON oi.product_id = p.product_id " +
                "WHERE oi.order_id = ? " +
                "ORDER BY p.product_name";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    String item = String.format(
                            Locale.US,
                            "%s | Qty: %d | Price: ₹%.2f",
                            rs.getString("product_name"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price")
                    );

                    list.add(item);
                }
            }
        }

        return list;
    }

    // =====================================================
    // GET ORDER ITEMS ONLY FOR THE OWNER
    // =====================================================

    public List<String> getOrderItemsForUser(
            int orderId,
            int userId
    ) throws SQLException {

        if (orderId <= 0 || userId <= 0) {
            throw new SQLException("Invalid order ID or user ID.");
        }

        List<String> list = new ArrayList<>();

        String sql =
                "SELECT p.product_name, oi.quantity, oi.unit_price " +
                "FROM orders o " +
                "JOIN order_items oi ON o.order_id = oi.order_id " +
                "JOIN products p ON oi.product_id = p.product_id " +
                "WHERE o.order_id = ? AND o.user_id = ? " +
                "ORDER BY p.product_name";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    String item = String.format(
                            Locale.US,
                            "%s | Qty: %d | Price: ₹%.2f",
                            rs.getString("product_name"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price")
                    );

                    list.add(item);
                }
            }
        }

        return list;
    }
}