package com.smartwarehouse;

import java.sql.Timestamp;

/**
 * Task — Entity Model for Warehouse Robotic and Processing Tasks.
 *
 * Synchronized with MySQL 'warehouse_tasks' table:
 * (task_id, order_id, task_type, arrival_time, burst_time, priority, status, robot_id, created_at)
 *
 * Fully integrated with:
 * - AdminDashboard.java (order approval & picking task dispatch)
 * - Scheduler.java (OS scheduling simulation algorithms)
 * - TaskDAO.java (database operations)
 */
public class Task {

    // Public fields preserved for 100% backward compatibility
    public int id;
    public Integer orderId;
    public String type;
    public int arrival;
    public int burst;
    public int priority;
    public String status;
    public Integer robotId;
    public Timestamp createdAt;

    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    /**
     * Original Constructor (Preserved for backward compatibility)
     */
    public Task(int id, String type, int arrival, int burst,
                int priority, String status, Integer robotId) {
        this(id, null, type, arrival, burst, priority, status, robotId, new Timestamp(System.currentTimeMillis()));
    }

    /**
     * Constructor with Order ID
     */
    public Task(int id, Integer orderId, String type, int arrival, int burst,
                int priority, String status, Integer robotId) {
        this(id, orderId, type, arrival, burst, priority, status, robotId, new Timestamp(System.currentTimeMillis()));
    }

    /**
     * Complete Constructor with Order ID and Created Timestamp
     */
    public Task(int id, Integer orderId, String type, int arrival, int burst,
                int priority, String status, Integer robotId, Timestamp createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.type = (type != null) ? type.toUpperCase() : "PICKING";
        this.arrival = arrival;
        this.burst = burst;
        this.priority = priority;
        this.status = (status != null) ? status.toUpperCase() : "WAITING";
        this.robotId = robotId;
        this.createdAt = (createdAt != null) ? createdAt : new Timestamp(System.currentTimeMillis());
    }

    // =========================================================
    // GETTERS & SETTERS
    // =========================================================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getArrival() {
        return arrival;
    }

    public void setArrival(int arrival) {
        this.arrival = arrival;
    }

    public int getBurst() {
        return burst;
    }

    public void setBurst(int burst) {
        this.burst = burst;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = (status != null) ? status.toUpperCase() : "WAITING";
    }

    public Integer getRobotId() {
        return robotId;
    }

    public void setRobotId(Integer robotId) {
        this.robotId = robotId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // =========================================================
    // HELPER METHODS FOR DASHBOARD & TELEMETRY
    // =========================================================

    public boolean isWaiting() {
        return "WAITING".equalsIgnoreCase(status);
    }

    public boolean isRunning() {
        return "RUNNING".equalsIgnoreCase(status);
    }

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status);
    }

    public String getPriorityLabel() {
        switch (priority) {
            case 1:  return "P1 (Critical / Express)";
            case 2:  return "P2 (High Priority)";
            case 3:  return "P3 (Standard)";
            default: return "P" + priority + " (Low)";
        }
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", orderId=" + (orderId != null ? orderId : "NONE") +
                ", type='" + type + '\'' +
                ", arrival=" + arrival +
                ", burst=" + burst +
                ", priority=" + priority +
                ", status='" + status + '\'' +
                ", robotId=" + (robotId != null ? robotId : "UNASSIGNED") +
                '}';
    }
}