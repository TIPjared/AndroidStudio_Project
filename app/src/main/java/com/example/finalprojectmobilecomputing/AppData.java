package com.example.finalprojectmobilecomputing;

public class AppData {

    private static AppData instance;

    private String qrCode;
    private String userId;
    private String rideDuration;  // NEW field

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

    public String getRideDuration() {
        return rideDuration;
    }

    public void setRideDuration(String rideDuration) {
        this.rideDuration = rideDuration;
    }

    public void clear() {
        qrCode = null;
        userId = null;
        rideDuration = null;
    }
}
