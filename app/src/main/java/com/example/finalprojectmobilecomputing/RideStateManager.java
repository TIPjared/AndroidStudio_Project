package com.example.finalprojectmobilecomputing;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Manages ride state persistence using SharedPreferences
 * Ensures rides survive app restarts and configuration changes
 */
public class RideStateManager {
    
    private static final String PREFS_NAME = "RideStatePrefs";
    private static final String KEY_RIDE_ACTIVE = "ride_active";
    private static final String KEY_RIDE_START_TIME = "ride_start_time";
    private static final String KEY_BIKE_ID = "bike_id";
    private static final String KEY_QR_CODE = "qr_code";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_RIDE_TIME = "ride_time";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_TOTAL_DISTANCE = "total_distance";
    private static final String KEY_DURATION_MILLIS = "duration_millis";
    
    private static RideStateManager instance;
    private SharedPreferences prefs;
    
    private RideStateManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized RideStateManager getInstance(Context context) {
        if (instance == null) {
            instance = new RideStateManager(context);
        }
        return instance;
    }
    
    /**
     * Save active ride state
     */
    public void saveRideState(String bikeId, String qrCode, String userId, 
                              String rideTime, String amount, long startTime) {
        Log.d("RideStateManager", "Saving ride state: bikeId=" + bikeId + ", startTime=" + startTime);
        prefs.edit()
            .putBoolean(KEY_RIDE_ACTIVE, true)
            .putLong(KEY_RIDE_START_TIME, startTime)
            .putString(KEY_BIKE_ID, bikeId)
            .putString(KEY_QR_CODE, qrCode)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_RIDE_TIME, rideTime)
            .putString(KEY_AMOUNT, amount)
            .putFloat(KEY_TOTAL_DISTANCE, 0f)
            .apply();
    }
    
    /**
     * Update ride progress (distance traveled)
     */
    public void updateRideProgress(float totalDistance, long durationMillis) {
        if (isRideActive()) {
            prefs.edit()
                .putFloat(KEY_TOTAL_DISTANCE, totalDistance)
                .putLong(KEY_DURATION_MILLIS, durationMillis)
                .apply();
        }
    }
    
    /**
     * Check if there's an active ride
     */
    public boolean isRideActive() {
        return prefs.getBoolean(KEY_RIDE_ACTIVE, false);
    }
    
    /**
     * Get ride start time
     */
    public long getRideStartTime() {
        return prefs.getLong(KEY_RIDE_START_TIME, 0);
    }
    
    /**
     * Get bike ID
     */
    public String getBikeId() {
        return prefs.getString(KEY_BIKE_ID, null);
    }
    
    /**
     * Get QR code
     */
    public String getQrCode() {
        return prefs.getString(KEY_QR_CODE, null);
    }
    
    /**
     * Get user ID
     */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }
    
    /**
     * Get ride time duration (e.g. "30 minutes")
     */
    public String getRideTime() {
        return prefs.getString(KEY_RIDE_TIME, null);
    }
    
    /**
     * Get amount
     */
    public String getAmount() {
        return prefs.getString(KEY_AMOUNT, null);
    }
    
    /**
     * Get total distance traveled
     */
    public float getTotalDistance() {
        return prefs.getFloat(KEY_TOTAL_DISTANCE, 0f);
    }
    
    /**
     * Get ride duration in milliseconds
     */
    public long getDurationMillis() {
        return prefs.getLong(KEY_DURATION_MILLIS, 0);
    }
    
    /**
     * Clear ride state (called when ride ends)
     */
    public void clearRideState() {
        Log.d("RideStateManager", "Clearing ride state");
        prefs.edit()
            .putBoolean(KEY_RIDE_ACTIVE, false)
            .remove(KEY_RIDE_START_TIME)
            .remove(KEY_BIKE_ID)
            .remove(KEY_QR_CODE)
            .remove(KEY_USER_ID)
            .remove(KEY_RIDE_TIME)
            .remove(KEY_AMOUNT)
            .remove(KEY_TOTAL_DISTANCE)
            .remove(KEY_DURATION_MILLIS)
            .apply();
    }
    
    /**
     * Get all ride state data as a bundle
     */
    public RideState getRideState() {
        if (!isRideActive()) {
            return null;
        }
        
        RideState state = new RideState();
        state.rideActive = true;
        state.rideStartTime = getRideStartTime();
        state.bikeId = getBikeId();
        state.qrCode = getQrCode();
        state.userId = getUserId();
        state.rideTime = getRideTime();
        state.amount = getAmount();
        state.totalDistance = getTotalDistance();
        state.durationMillis = getDurationMillis();
        
        return state;
    }
    
    /**
     * Data class to hold ride state
     */
    public static class RideState {
        public boolean rideActive;
        public long rideStartTime;
        public String bikeId;
        public String qrCode;
        public String userId;
        public String rideTime;
        public String amount;
        public float totalDistance;
        public long durationMillis;
    }
}

