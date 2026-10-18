package com.smartwarehouse;

import java.sql.*;
import java.util.*;

public class ProductDAO {

    // =========================================================
    // GET ALL ACTIVE PRODUCTS
    // =========================================================

    public List<Product> getProducts() throws SQLException {

        List<Product> products = new ArrayList<>();

        String sql =
                "SELECT p.product_id, p.product_name, p.category, " +
                "p.price, i.quantity, i.reorder_level " +
                "FROM products p " +
                "JOIN inventory i ON p.product_id = i.product_id " +
                "WHERE p.active = TRUE " +
                "ORDER BY p.product_id";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement s = c.prepareStatement(sql);
                ResultSet r = s.executeQuery()
        ) {

            while (r.next()) {

                products.add(
                        new Product(
                                r.getInt("product_id"),
                                r.getString("product_name"),
                                r.getString("category"),
                                r.getDouble("price"),
                                r.getInt("quantity"),
                                r.getInt("reorder_level")
                        )
                );
            }
        }

        return products;
    }


    // =========================================================
    // ADD PRODUCT
    // =========================================================

    public void addProduct(
            String name,
            String category,
            double price,
            int stock
    ) throws SQLException {

        if (name == null || name.trim().isEmpty()) {
            throw new SQLException("Product name is required.");
        }

        if (category == null || category.trim().isEmpty()) {
            throw new SQLException("Category is required.");
        }

        if (price < 0) {
            throw new SQLException("Price cannot be negative.");
        }

        if (stock < 0) {
            throw new SQLException("Stock cannot be negative.");
        }

        String productSQL =
                "INSERT INTO products(product_name,category,price) " +
                "VALUES(?,?,?)";

        String inventorySQL =
                "INSERT INTO inventory(product_id,quantity,reorder_level) " +
                "VALUES(?,?,?)";

        try (Connection c = DBConnection.getConnection()) {

            c.setAutoCommit(false);

            try {

                int productId;

                // INSERT PRODUCT
                try (
                        PreparedStatement p =
                                c.prepareStatement(
                                        productSQL,
                                        Statement.RETURN_GENERATED_KEYS
                                )
                ) {

                    p.setString(1, name.trim());
                    p.setString(2, category.trim());
                    p.setDouble(3, price);

                    p.executeUpdate();

                    try (ResultSet keys = p.getGeneratedKeys()) {

                        if (!keys.next()) {
                            throw new SQLException(
                                    "Unable to generate Product ID."
                            );
                        }

                        productId = keys.getInt(1);
                    }
                }


                // INSERT INVENTORY
                try (
                        PreparedStatement i =
                                c.prepareStatement(inventorySQL)
                ) {

                    i.setInt(1, productId);
                    i.setInt(2, stock);
                    i.setInt(3, 5);

                    i.executeUpdate();
                }

                c.commit();

            } catch (SQLException e) {

                c.rollback();

                throw e;
            }
        }
    }


    // =========================================================
    // UPDATE PRODUCT
    // =========================================================

    public void updateProduct(
            int productId,
            String name,
            String category,
            double price
    ) throws SQLException {

        String sql =
                "UPDATE products " +
                "SET product_name=?, category=?, price=? " +
                "WHERE product_id=?";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(sql)
        ) {

            p.setString(1, name.trim());
            p.setString(2, category.trim());
            p.setDouble(3, price);
            p.setInt(4, productId);

            p.executeUpdate();
        }
    }


    // =========================================================
    // UPDATE STOCK
    // =========================================================

    public void updateStock(
            int productId,
            int quantity
    ) throws SQLException {

        if (quantity < 0) {
            throw new SQLException(
                    "Stock cannot be negative."
            );
        }

        String sql =
                "UPDATE inventory " +
                "SET quantity=? " +
                "WHERE product_id=?";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(sql)
        ) {

            p.setInt(1, quantity);
            p.setInt(2, productId);

            int rows = p.executeUpdate();

            if (rows == 0) {
                throw new SQLException(
                        "Inventory record not found."
                );
            }
        }
    }


    // =========================================================
    // INCREASE STOCK
    // =========================================================

    public void increaseStock(
            int productId,
            int amount
    ) throws SQLException {

        if (amount <= 0) {
            throw new SQLException(
                    "Stock increase must be greater than 0."
            );
        }

        String sql =
                "UPDATE inventory " +
                "SET quantity = quantity + ? " +
                "WHERE product_id=?";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(sql)
        ) {

            p.setInt(1, amount);
            p.setInt(2, productId);

            p.executeUpdate();
        }
    }


    // =========================================================
    // DECREASE STOCK
    // =========================================================

    public void decreaseStock(
            int productId,
            int amount
    ) throws SQLException {

        if (amount <= 0) {
            throw new SQLException(
                    "Stock decrease must be greater than 0."
            );
        }

        String sql =
                "UPDATE inventory " +
                "SET quantity = quantity - ? " +
                "WHERE product_id=? AND quantity >= ?";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(sql)
        ) {

            p.setInt(1, amount);
            p.setInt(2, productId);
            p.setInt(3, amount);

            int rows = p.executeUpdate();

            if (rows == 0) {
                throw new SQLException(
                        "Insufficient stock or product not found."
                );
            }
        }
    }


    // =========================================================
    // UPDATE REORDER LEVEL
    // =========================================================

    public void updateReorderLevel(
            int productId,
            int reorderLevel
    ) throws SQLException {

        if (reorderLevel < 0) {
            throw new SQLException(
                    "Reorder level cannot be negative."
            );
        }

        String sql =
                "UPDATE inventory " +
                "SET reorder_level=? " +
                "WHERE product_id=?";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(sql)
        ) {

            p.setInt(1, reorderLevel);
            p.setInt(2, productId);

            p.executeUpdate();
        }
    }


    // =========================================================
    // DELETE / DEACTIVATE PRODUCT
    // =========================================================

    public void deleteProduct(
            int productId
    ) throws SQLException {

        // Soft delete so old order/inventory records
        // are not unnecessarily destroyed.

        String sql =
                "UPDATE products " +
                "SET active=FALSE " +
                "WHERE product_id=?";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(sql)
        ) {

            p.setInt(1, productId);

            int rows = p.executeUpdate();

            if (rows == 0) {
                throw new SQLException(
                        "Product not found."
                );
            }
        }
    }


    // =========================================================
    // SEARCH PRODUCT
    // =========================================================

    public List<Product> searchProducts(
            String keyword
    ) throws SQLException {

        List<Product> products = new ArrayList<>();

        String sql =
                "SELECT p.product_id, p.product_name, p.category, " +
                "p.price, i.quantity, i.reorder_level " +
                "FROM products p " +
                "JOIN inventory i ON p.product_id=i.product_id " +
                "WHERE p.active=TRUE " +
                "AND (p.product_name LIKE ? OR p.category LIKE ?) " +
                "ORDER BY p.product_id";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(sql)
        ) {

            String key = "%" + keyword.trim() + "%";

            p.setString(1, key);
            p.setString(2, key);

            try (ResultSet r = p.executeQuery()) {

                while (r.next()) {

                    products.add(
                            new Product(
                                    r.getInt("product_id"),
                                    r.getString("product_name"),
                                    r.getString("category"),
                                    r.getDouble("price"),
                                    r.getInt("quantity"),
                                    r.getInt("reorder_level")
                            )
                    );
                }
            }
        }

        return products;
    }


    // =========================================================
    // GET SINGLE PRODUCT
    // =========================================================

    public Product getProduct(
            int productId
    ) throws SQLException {

        String sql =
                "SELECT p.product_id, p.product_name, " +
                "p.category, p.price, i.quantity, i.reorder_level " +
                "FROM products p " +
                "JOIN inventory i ON p.product_id=i.product_id " +
                "WHERE p.product_id=? AND p.active=TRUE";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(sql)
        ) {

            p.setInt(1, productId);

            try (ResultSet r = p.executeQuery()) {

                if (r.next()) {

                    return new Product(
                            r.getInt("product_id"),
                            r.getString("product_name"),
                            r.getString("category"),
                            r.getDouble("price"),
                            r.getInt("quantity"),
                            r.getInt("reorder_level")
                    );
                }
            }
        }

        return null;
    }


    // =========================================================
    // LOW STOCK PRODUCTS
    // =========================================================

    public List<Product> getLowStockProducts()
            throws SQLException {

        List<Product> products = new ArrayList<>();

        String sql =
                "SELECT p.product_id, p.product_name, " +
                "p.category, p.price, i.quantity, i.reorder_level " +
                "FROM products p " +
                "JOIN inventory i ON p.product_id=i.product_id " +
                "WHERE p.active=TRUE " +
                "AND i.quantity <= i.reorder_level " +
                "ORDER BY i.quantity ASC";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(sql);
                ResultSet r = p.executeQuery()
        ) {

            while (r.next()) {

                products.add(
                        new Product(
                                r.getInt("product_id"),
                                r.getString("product_name"),
                                r.getString("category"),
                                r.getDouble("price"),
                                r.getInt("quantity"),
                                r.getInt("reorder_level")
                        )
                );
            }
        }

        return products;
    }
}
