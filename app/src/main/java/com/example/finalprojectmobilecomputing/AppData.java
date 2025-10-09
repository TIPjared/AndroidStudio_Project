package com.example.finalprojectmobilecomputing;

public class AppData {

    private static AppData instance;

    private String qrCode;
    private String userId;
    private String userName;
    private String rideTime;  // NEW field
    private String bikeId;  // Bike ID from QR code
    private String amount;

    private AppData() { }

    public static synchronized AppData getInstance() {
        if (instance == null) {
            instance = new AppData();
        }
        return instance;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRideTime() {
        return rideTime;
    }

    public void setRideTime(String rideTime) {
        this.rideTime = rideTime;
    }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getBikeId() {
        return bikeId;
    }

    public void setBikeId(String bikeId) {
        this.bikeId = bikeId;
    }

    public void clear() {
        qrCode = null;
        userId = null;
        userName = null;
        rideTime = null;
        bikeId = null;
        amount = null;
    }
}
