package com.example.finalprojectmobilecomputing;

import java.util.Date;

public class LogHistory {
    private String id;
    private String location;
    private double distance;
    private int duration;
    private Date timestamp;
    private String status;
    private String userId;
    private double cost;

    // 🔹 New fields from Firestore
    private String paymentType;
    private String paymentAccount;
    private String bikeId;

    // Empty constructor required by Firestore
    public LogHistory() {
    }

    public LogHistory(String id, String location, double distance, int duration, Date timestamp,
                      String status, String userId, double cost, String paymentType,
                      String paymentAccount, String bikeId) {
        this.id = id;
        this.location = location;
        this.distance = distance;
        this.duration = duration;
        this.timestamp = timestamp;
        this.status = status;
        this.userId = userId;
        this.cost = cost;
        this.paymentType = paymentType;
        this.paymentAccount = paymentAccount;
        this.bikeId = bikeId;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getPaymentAccount() { return paymentAccount; }
    public void setPaymentAccount(String paymentAccount) { this.paymentAccount = paymentAccount; }

    public String getBikeId() { return bikeId; }
    public void setBikeId(String bikeId) { this.bikeId = bikeId; }
}
