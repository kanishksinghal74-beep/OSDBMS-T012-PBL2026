
package com.smartwarehouse;

import java.sql.Timestamp;

public class Order {

    private int orderId;
    private int userId;
    private String deliveryAddress;
    private double totalAmount;
    private String paymentMethod;
    private String status;
    private Timestamp orderDate;

    // NEW: Delivery scheduling fields
    private String deliveryType = "NORMAL";
    private int priority = 2;

    public Order() {
    }

    // Existing constructor retained for compatibility
    public Order(
            int orderId,
            int userId,
            String deliveryAddress,
            double totalAmount,
            String paymentMethod,
            String status,
            Timestamp orderDate
    ) {
        this.orderId = orderId;
        this.userId = userId;
        this.deliveryAddress = deliveryAddress;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.orderDate = orderDate;
    }

    // Existing constructor retained for compatibility
    public Order(
            int userId,
            String deliveryAddress,
            double totalAmount,
            String paymentMethod
    ) {
        this.userId = userId;
        this.deliveryAddress = deliveryAddress;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.status = "PLACED";
        setDeliveryType("NORMAL");
    }

    // New constructor with delivery selection
    public Order(
            int userId,
            String deliveryAddress,
            double totalAmount,
            String paymentMethod,
            String deliveryType
    ) {
        this.userId = userId;
        this.deliveryAddress = deliveryAddress;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.status = "PLACED";
        setDeliveryType(deliveryType);
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Timestamp orderDate) {
        this.orderDate = orderDate;
    }

    // Delivery type: URGENT or NORMAL
    public String getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        if (deliveryType != null
                && deliveryType.trim().equalsIgnoreCase("URGENT")) {
            this.deliveryType = "URGENT";
            this.priority = 1;
        } else {
            this.deliveryType = "NORMAL";
            this.priority = 2;
        }
    }

    // Lower number means higher priority
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        if (priority == 1) {
            this.priority = 1;
            this.deliveryType = "URGENT";
        } else {
            this.priority = 2;
            this.deliveryType = "NORMAL";
        }
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", deliveryAddress='" + deliveryAddress + '\'' +
                ", totalAmount=" + totalAmount +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", status='" + status + '\'' +
                ", deliveryType='" + deliveryType + '\'' +
                ", priority=" + priority +
                ", orderDate=" + orderDate +
                '}';
    }
}

