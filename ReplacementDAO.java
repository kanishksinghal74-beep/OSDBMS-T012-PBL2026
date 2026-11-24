package com.smartwarehouse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Stores user requests to replace an item from a completed order. */
public class ReplacementDAO {

    public ReplacementDAO() throws SQLException {
        ensureTable();
    }

    private void ensureTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS replacement_requests ("
                + "request_id INT PRIMARY KEY AUTO_INCREMENT, "
                + "order_id INT NOT NULL, "
                + "order_item_id INT NOT NULL, "
                + "user_id INT NOT NULL, "
                + "reason VARCHAR(500) NOT NULL, "
                + "status VARCHAR(30) NOT NULL DEFAULT 'PENDING', "
                + "requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "INDEX idx_replacement_user (user_id), "
                + "INDEX idx_replacement_status (status), "
                + "CONSTRAINT fk_replacement_order FOREIGN KEY (order_id) REFERENCES orders(order_id), "
                + "CONSTRAINT fk_replacement_item FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id), "
                + "CONSTRAINT fk_replacement_user FOREIGN KEY (user_id) REFERENCES users(user_id)"
                + ") ENGINE=InnoDB";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    public List<ReplacementItem> getDeliveredItems(int orderId, int userId)
            throws SQLException {
        String sql = "SELECT oi.order_item_id, p.product_name, oi.quantity "
                + "FROM orders o "
                + "JOIN order_items oi ON oi.order_id = o.order_id "
                + "JOIN products p ON p.product_id = oi.product_id "
                + "WHERE o.order_id = ? AND o.user_id = ? "
                + "AND UPPER(o.status) IN ('DELIVERED', 'COMPLETED') "
                + "ORDER BY p.product_name";

        List<ReplacementItem> items = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            statement.setInt(2, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    items.add(new ReplacementItem(
                            result.getInt("order_item_id"),
                            result.getString("product_name"),
                            result.getInt("quantity")
                    ));
                }
            }
        }
        return items;
    }

    public void submitRequest(int orderId, int orderItemId, int userId,
                              String reason) throws SQLException {
        String cleanedReason = reason == null ? "" : reason.trim();
        if (cleanedReason.isEmpty()) {
            throw new SQLException("Please enter a reason for replacement.");
        }

        String sql = "INSERT INTO replacement_requests "
                + "(order_id, order_item_id, user_id, reason) "
                + "SELECT o.order_id, oi.order_item_id, o.user_id, ? "
                + "FROM orders o JOIN order_items oi ON oi.order_id = o.order_id "
                + "WHERE o.order_id = ? AND oi.order_item_id = ? AND o.user_id = ? "
                + "AND UPPER(o.status) IN ('DELIVERED', 'COMPLETED') "
                + "AND NOT EXISTS (SELECT 1 FROM replacement_requests r "
                + "WHERE r.order_item_id = oi.order_item_id AND r.status = 'PENDING')";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cleanedReason);
            statement.setInt(2, orderId);
            statement.setInt(3, orderItemId);
            statement.setInt(4, userId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("This item is not eligible, or it already has a pending replacement request.");
            }
        }
    }

    public List<Object[]> getAllRequests() throws SQLException {
        String sql = "SELECT r.request_id, r.order_id, r.user_id, u.username, "
                + "p.product_name, oi.quantity, r.reason, r.status, r.requested_at "
                + "FROM replacement_requests r "
                + "JOIN users u ON u.user_id = r.user_id "
                + "JOIN order_items oi ON oi.order_item_id = r.order_item_id "
                + "JOIN products p ON p.product_id = oi.product_id "
                + "ORDER BY r.requested_at DESC";

        List<Object[]> rows = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                rows.add(new Object[]{
                        result.getInt("request_id"),
                        result.getInt("order_id"),
                        result.getInt("user_id"),
                        result.getString("username"),
                        result.getString("product_name"),
                        result.getInt("quantity"),
                        result.getString("reason"),
                        result.getString("status"),
                        result.getTimestamp("requested_at")
                });
            }
        }
        return rows;
    }

    public void updateRequestStatus(int requestId, String newStatus)
            throws SQLException {
        if (!"APPROVED".equals(newStatus) && !"REJECTED".equals(newStatus)) {
            throw new SQLException("Invalid replacement request status.");
        }

        String sql = "UPDATE replacement_requests SET status = ? "
                + "WHERE request_id = ? AND status = 'PENDING'";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newStatus);
            statement.setInt(2, requestId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("This request is no longer pending.");
            }
        }
    }

    public static final class ReplacementItem {
        private final int orderItemId;
        private final String productName;
        private final int quantity;

        public ReplacementItem(int orderItemId, String productName, int quantity) {
            this.orderItemId = orderItemId;
            this.productName = productName;
            this.quantity = quantity;
        }

        public int getOrderItemId() {
            return orderItemId;
        }

        @Override
        public String toString() {
            return productName + " (Qty: " + quantity + ")";
        }
    }
}
