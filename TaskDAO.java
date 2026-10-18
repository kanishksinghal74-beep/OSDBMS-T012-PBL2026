package com.smartwarehouse;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    // =========================================================
    // GET WAITING TASKS
    // =========================================================

    public List<Task> getWaitingTasks()
            throws SQLException {

        List<Task> tasks =
                new ArrayList<>();

        String sql =
                "SELECT task_id, task_type, arrival_time, " +
                "burst_time, priority, status, robot_id " +
                "FROM warehouse_tasks " +
                "WHERE status='WAITING' " +
                "ORDER BY task_id";

        try (
                Connection c =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        c.prepareStatement(sql);

                ResultSet rs =
                        ps.executeQuery()
        ) {

            while (rs.next()) {

                int taskId =
                        rs.getInt("task_id");

                String type =
                        rs.getString("task_type");

                int arrival =
                        rs.getInt("arrival_time");

                int burst =
                        rs.getInt("burst_time");

                int priority =
                        rs.getInt("priority");

                String status =
                        rs.getString("status");


                // ---------------------------------------------
                // ROBOT ID
                // ---------------------------------------------

                int robotValue =
                        rs.getInt("robot_id");

                Integer robotId;

                if (rs.wasNull()) {
                    robotId = null;
                } else {
                    robotId = robotValue;
                }


                tasks.add(
                        new Task(
                                taskId,
                                type,
                                arrival,
                                burst,
                                priority,
                                status,
                                robotId
                        )
                );
            }
        }

        return tasks;
    }


    // =========================================================
    // CREATE DEMO TASKS
    // =========================================================

    public void createDemoTasks()
            throws SQLException {

        String sql =
                "INSERT INTO warehouse_tasks " +
                "(order_id, task_type, arrival_time, " +
                "burst_time, priority, status) " +
                "VALUES " +
                "(NULL, 'PICKING', 0, 8, 2, 'WAITING'), " +
                "(NULL, 'PACKING', 1, 5, 3, 'WAITING'), " +
                "(NULL, 'DISPATCH', 2, 4, 1, 'WAITING')";

        try (
                Connection c =
                        DBConnection.getConnection();

                Statement s =
                        c.createStatement()
        ) {

            s.executeUpdate(sql);
        }
    }


    // =========================================================
    // CREATE TASK FOR ORDER
    // =========================================================

    public int createTask(
            Integer orderId,
            String taskType,
            int arrivalTime,
            int burstTime,
            int priority
    ) throws SQLException {

        String sql =
                "INSERT INTO warehouse_tasks " +
                "(order_id, task_type, arrival_time, " +
                "burst_time, priority, status, robot_id) " +
                "VALUES (?, ?, ?, ?, ?, 'WAITING', NULL)";

        try (
                Connection c =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        c.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            if (orderId == null) {
                ps.setNull(
                        1,
                        Types.INTEGER
                );
            } else {
                ps.setInt(
                        1,
                        orderId
                );
            }

            ps.setString(
                    2,
                    taskType
            );

            ps.setInt(
                    3,
                    arrivalTime
            );

            ps.setInt(
                    4,
                    burstTime
            );

            ps.setInt(
                    5,
                    priority
            );

            ps.executeUpdate();


            try (
                    ResultSet rs =
                            ps.getGeneratedKeys()
            ) {

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return -1;
    }


    // =========================================================
    // UPDATE TASK STATUS
    // =========================================================

    public boolean updateTaskStatus(
            int taskId,
            String status
    ) throws SQLException {

        String sql =
                "UPDATE warehouse_tasks " +
                "SET status=? " +
                "WHERE task_id=?";

        try (
                Connection c =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        c.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    status
            );

            ps.setInt(
                    2,
                    taskId
            );

            return ps.executeUpdate() > 0;
        }
    }


    // =========================================================
    // ASSIGN ROBOT TO TASK
    // =========================================================

    public boolean assignRobot(
            int taskId,
            int robotId
    ) throws SQLException {

        String sql =
                "UPDATE warehouse_tasks " +
                "SET robot_id=?, status='RUNNING' " +
                "WHERE task_id=?";

        try (
                Connection c =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        c.prepareStatement(sql)
        ) {

            ps.setInt(
                    1,
                    robotId
            );

            ps.setInt(
                    2,
                    taskId
            );

            return ps.executeUpdate() > 0;
        }
    }


    // =========================================================
    // GET TASKS BY ORDER
    // =========================================================

    public List<Task> getTasksByOrder(
            int orderId
    ) throws SQLException {

        List<Task> tasks =
                new ArrayList<>();

        String sql =
                "SELECT task_id, task_type, arrival_time, " +
                "burst_time, priority, status, robot_id " +
                "FROM warehouse_tasks " +
                "WHERE order_id=? " +
                "ORDER BY task_id";

        try (
                Connection c =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        c.prepareStatement(sql)
        ) {

            ps.setInt(
                    1,
                    orderId
            );

            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                while (rs.next()) {

                    int robotValue =
                            rs.getInt("robot_id");

                    Integer robotId =
                            rs.wasNull()
                                    ? null
                                    : robotValue;

                    tasks.add(
                            new Task(
                                    rs.getInt("task_id"),
                                    rs.getString("task_type"),
                                    rs.getInt("arrival_time"),
                                    rs.getInt("burst_time"),
                                    rs.getInt("priority"),
                                    rs.getString("status"),
                                    robotId
                            )
                    );
                }
            }
        }

        return tasks;
    }
}