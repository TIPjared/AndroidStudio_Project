package com.example.finalprojectmobilecomputing;

public class AppData {

    private static AppData instance;

    private String qrCode;
    private String userId;
    private String rideTime;  // NEW field

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

    public void clear() {
        qrCode = null;
        userId = null;
        rideTime = null;
    }
}
