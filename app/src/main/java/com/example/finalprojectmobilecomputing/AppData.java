package com.example.finalprojectmobilecomputing;

public class AppData {
    private static AppData instance;
    private String qrCode;
    private String userId;

    private AppData() {
        // Private constructor to prevent instantiation
    }

    public static AppData getInstance() {
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
}
