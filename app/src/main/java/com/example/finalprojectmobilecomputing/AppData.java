package com.example.finalprojectmobilecomputing;

public class AppData {

    // Singleton instance
    private static AppData instance;

    // Data to share
    private String qrCode;
    private String userId;

    // Private constructor to prevent instantiation
    private AppData() { }

    // Get singleton instance
    public static synchronized AppData getInstance() {
        if (instance == null) {
            instance = new AppData();
        }
        return instance;
    }

    // Getters and setters
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

    // Optional: clear data when ride ends
    public void clear() {
        qrCode = null;
        userId = null;
    }
}
