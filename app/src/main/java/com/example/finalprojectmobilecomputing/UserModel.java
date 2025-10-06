package com.example.finalprojectmobilecomputing;

public class UserModel {
    private String email;
    private String phone;
    private String username;
    private String age;
    private String gender;
    private String profileImageUrl;
    private String deviceId;
    private boolean phoneVerified;
    private boolean otpVerified;
    private boolean emailVerified;
    private String accountStatus;
    private long verificationTimestamp;
    private long lastLoginTimestamp;
    private int totalRides;
    private double totalDistance;
    private long accountCreatedAt;

    public UserModel() {
        // Required empty constructor for Firebase
    }

    public UserModel(String email, String phone) {
        this.email = email;
        this.phone = phone;
    }

    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getUsername() { return username; }
    public String getAge() { return age; }
    public String getGender() { return gender; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getDeviceId() { return deviceId; }
    public boolean isPhoneVerified() { return phoneVerified; }
    public boolean isOtpVerified() { return otpVerified; }
    public boolean isEmailVerified() { return emailVerified; }
    public String getAccountStatus() { return accountStatus; }
    public long getVerificationTimestamp() { return verificationTimestamp; }
    public long getLastLoginTimestamp() { return lastLoginTimestamp; }
    public int getTotalRides() { return totalRides; }
    public double getTotalDistance() { return totalDistance; }
    public long getAccountCreatedAt() { return accountCreatedAt; }

    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setUsername(String username) { this.username = username; }
    public void setAge(String age) { this.age = age; }
    public void setGender(String gender) { this.gender = gender; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }
    public void setOtpVerified(boolean otpVerified) { this.otpVerified = otpVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
    public void setVerificationTimestamp(long verificationTimestamp) { this.verificationTimestamp = verificationTimestamp; }
    public void setLastLoginTimestamp(long lastLoginTimestamp) { this.lastLoginTimestamp = lastLoginTimestamp; }
    public void setTotalRides(int totalRides) { this.totalRides = totalRides; }
    public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }
    public void setAccountCreatedAt(long accountCreatedAt) { this.accountCreatedAt = accountCreatedAt; }

    // Helper methods
    public boolean isFullyVerified() {
        return phoneVerified && otpVerified && emailVerified;
    }

    public String getVerificationStatus() {
        if (isFullyVerified()) {
            return "Fully Verified";
        } else if (phoneVerified && otpVerified) {
            return "Phone & OTP Verified";
        } else if (phoneVerified) {
            return "Phone Verified Only";
        } else {
            return "Not Verified";
        }
    }
}
