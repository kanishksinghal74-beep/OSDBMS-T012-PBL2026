package com.smartwarehouse;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * RobotDAO — Data Access Object for Autonomous Mobile Robots (AGVs).
 *
 * Supports:
 * - Thread-safe robot assignment with MySQL row-level locking
 * - Live battery telemetry and warehouse grid location persistence
 * - Multi-robot fleet status querying and health tracking
 * - Emergency stop and fleet reset controls
 * - Full backward compatibility with existing project classes
 */
public class RobotDAO {

    // =========================================================
    // ROBOT DATA MODEL (POJO)
    // =========================================================
    public static class RobotInfo {
        private final int robotId;
        private final String robotName;
        private final String status;
        private final Integer orderId;
        private final int batteryLevel;
        private final String currentLocation;

        public RobotInfo(int robotId, String robotName, String status, Integer orderId, int batteryLevel, String currentLocation) {
            this.robotId = robotId;
            this.robotName = robotName;
            this.status = status;
            this.orderId = orderId;
            this.batteryLevel = batteryLevel;
            this.currentLocation = (currentLocation != null && !currentLocation.isBlank()) ? currentLocation : "Dock (2, 8)";
        }

        public int getRobotId() { return robotId; }
        public String getRobotName() { return robotName; }
        public String getStatus() { return status; }
        public Integer getOrderId() { return orderId; }
        public int getBatteryLevel() { return batteryLevel; }
        public String getCurrentLocation() { return currentLocation; }

        @Override
        public String toString() {
            return String.format("Robot %d | %s | Status: %s | Bat: %d%% | Loc: %s | Order: %s",
                    robotId, robotName, status, batteryLevel, currentLocation,
                    (orderId != null && orderId > 0) ? String.valueOf(orderId) : "NONE");
        }
    }

    // =========================================================
    // GET ALL ROBOTS (STRING LIST - BACKWARD COMPATIBLE)
    // =========================================================
    public List<String> getRobots() throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT robot_id, robot_name, status, order_id, battery_level, current_location " +
                     "FROM robots ORDER BY robot_id";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet r = ps.executeQuery()) {

            while (r.next()) {
                int robotId = r.getInt("robot_id");
                String robotName = r.getString("robot_name");
                String status = r.getString("status");

                int orderVal = r.getInt("order_id");
                Integer orderId = r.wasNull() ? null : orderVal;

                int battery = r.getInt("battery_level");
                if (r.wasNull()) battery = 100;

                String loc = r.getString("current_location");
                if (loc == null || loc.isBlank()) loc = "Dock Station";

                String text = String.format("Robot %d | %s | %s | Bat: %d%% | Loc: %s | Order: %s",
                        robotId, robotName, status, battery, loc,
                        (orderId != null) ? orderId : "NONE");

                list.add(text);
            }
        }
        return list;
    }

    // =========================================================
    // GET STRUCTURED ROBOT DETAILS LIST
    // =========================================================
    public List<RobotInfo> getRobotDetailsList() throws SQLException {
        List<RobotInfo> list = new ArrayList<>();
        String sql = "SELECT robot_id, robot_name, status, order_id, battery_level, current_location " +
                     "FROM robots ORDER BY robot_id";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet r = ps.executeQuery()) {

            while (r.next()) {
                int robotId = r.getInt("robot_id");
                String robotName = r.getString("robot_name");
                String status = r.getString("status");

                int orderVal = r.getInt("order_id");
                Integer orderId = r.wasNull() ? null : orderVal;

                int battery = r.getInt("battery_level");
                if (r.wasNull()) battery = 100;

                String loc = r.getString("current_location");

                list.add(new RobotInfo(robotId, robotName, status, orderId, battery, loc));
            }
        }
        return list;
    }

    // =========================================================
    // GET SINGLE ROBOT DETAILS
    // =========================================================
    public RobotInfo getRobotDetails(int robotId) throws SQLException {
        String sql = "SELECT robot_id, robot_name, status, order_id, battery_level, current_location " +
                     "FROM robots WHERE robot_id=?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, robotId);

            try (ResultSet r = ps.executeQuery()) {
                if (r.next()) {
                    String robotName = r.getString("robot_name");
                    String status = r.getString("status");

                    int orderVal = r.getInt("order_id");
                    Integer orderId = r.wasNull() ? null : orderVal;

                    int battery = r.getInt("battery_level");
                    if (r.wasNull()) battery = 100;

                    String loc = r.getString("current_location");

                    return new RobotInfo(robotId, robotName, status, orderId, battery, loc);
                }
            }
        }
        return null;
    }

    // =========================================================
    // GET AVAILABLE ROBOT ID
    // =========================================================
    public int getAvailableRobotId() throws SQLException {
        String sql = "SELECT robot_id FROM robots WHERE UPPER(status)='AVAILABLE' ORDER BY robot_id LIMIT 1";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet r = ps.executeQuery()) {

            if (r.next()) {
                return r.getInt("robot_id");
            }
        }
        return -1;
    }

    // =========================================================
    // ASSIGN SPECIFIC ROBOT TO ORDER
    // =========================================================
    public boolean assignRobot(int robotId, int orderId) throws SQLException {
        String sql = "UPDATE robots " +
                     "SET status='BUSY', order_id=?, assigned_order_id=? " +
                     "WHERE robot_id=? AND UPPER(status)='AVAILABLE'";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            ps.setInt(2, orderId);
            ps.setInt(3, robotId);

            return ps.executeUpdate() > 0;
        }
    }

    // =========================================================
    // AUTOMATICALLY ASSIGN FIRST AVAILABLE ROBOT (TRANSACTION)
    // =========================================================
    public int assignAvailableRobot(int orderId) throws SQLException {
        Connection c = null;

        try {
            c = DBConnection.getConnection();
            c.setAutoCommit(false);

            // Row-level lock to prevent race conditions between concurrent orders
            String findSQL = "SELECT robot_id FROM robots WHERE UPPER(status)='AVAILABLE' ORDER BY robot_id LIMIT 1 FOR UPDATE";
            int robotId = -1;

            try (PreparedStatement ps = c.prepareStatement(findSQL);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    robotId = rs.getInt("robot_id");
                }
            }

            if (robotId == -1) {
                c.rollback();
                return -1;
            }

            // Update assigned robot
            String updateSQL = "UPDATE robots " +
                               "SET status='BUSY', order_id=?, assigned_order_id=? " +
                               "WHERE robot_id=?";

            try (PreparedStatement ps = c.prepareStatement(updateSQL)) {
                ps.setInt(1, orderId);
                ps.setInt(2, orderId);
                ps.setInt(3, robotId);
                ps.executeUpdate();
            }

            c.commit();
            return robotId;

        } catch (SQLException e) {
            if (c != null) {
                try { c.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (c != null) {
                try {
                    c.setAutoCommit(true);
                    c.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    // =========================================================
    // UPDATE ROBOT TELEMETRY (LOCATION & BATTERY)
    // =========================================================
    public boolean updateTelemetry(int robotId, String location, int batteryLevel) throws SQLException {
        String sql = "UPDATE robots SET current_location=?, battery_level=? WHERE robot_id=?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, location);
            ps.setInt(2, Math.max(0, Math.min(100, batteryLevel)));
            ps.setInt(3, robotId);

            return ps.executeUpdate() > 0;
        }
    }

    // =========================================================
    // GET ROBOT NAME
    // =========================================================
    public String getRobotName(int robotId) throws SQLException {
        String sql = "SELECT robot_name FROM robots WHERE robot_id=?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, robotId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("robot_name");
                }
            }
        }
        return null;
    }

    // =========================================================
    // GET ROBOT ASSIGNED TO ORDER
    // =========================================================
    public int getRobotIdByOrder(int orderId) throws SQLException {
        String sql = "SELECT robot_id FROM robots WHERE order_id=? OR assigned_order_id=? LIMIT 1";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            ps.setInt(2, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("robot_id");
                }
            }
        }
        return -1;
    }

    // =========================================================
    // RELEASE ROBOT BY ID
    // =========================================================
    public boolean releaseRobot(int robotId) throws SQLException {
        String sql = "UPDATE robots " +
                     "SET status='AVAILABLE', order_id=NULL, assigned_order_id=NULL " +
                     "WHERE robot_id=?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, robotId);
            return ps.executeUpdate() > 0;
        }
    }

    // =========================================================
    // RELEASE ROBOT BY ORDER
    // =========================================================
    public boolean releaseRobotByOrder(int orderId) throws SQLException {
        String sql = "UPDATE robots " +
                     "SET status='AVAILABLE', order_id=NULL, assigned_order_id=NULL " +
                     "WHERE order_id=? OR assigned_order_id=?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            ps.setInt(2, orderId);

            return ps.executeUpdate() > 0;
        }
    }

    // =========================================================
    // UPDATE ROBOT STATUS
    // =========================================================
    public boolean updateRobotStatus(int robotId, String status) throws SQLException {
        String sql = "UPDATE robots SET status=? WHERE robot_id=?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, status != null ? status.trim().toUpperCase() : "AVAILABLE");
            ps.setInt(2, robotId);

            return ps.executeUpdate() > 0;
        }
    }

    // =========================================================
    // EMERGENCY STOP ENTIRE FLEET
    // =========================================================
    public boolean emergencyStopAll() throws SQLException {
        String sql = "UPDATE robots SET status='OFFLINE' WHERE UPPER(status) IN ('AVAILABLE', 'BUSY')";

        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement()) {

            return s.executeUpdate(sql) > 0;
        }
    }

    // =========================================================
    // RESET FLEET (RE-INITIALIZE TO AVAILABLE & 100% BATTERY)
    // =========================================================
    public boolean resetFleet() throws SQLException {
        String sql = "UPDATE robots " +
                     "SET status='AVAILABLE', order_id=NULL, assigned_order_id=NULL, " +
                     "battery_level=100, current_location='Dock Station (2, 8)'";

        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement()) {

            return s.executeUpdate(sql) > 0;
        }
    }

    // =========================================================
    // COUNT ACTIVE / BUSY ROBOTS
    // =========================================================
    public int getActiveRobotCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM robots WHERE UPPER(status)='BUSY'";

        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}