package com.example.finalprojectmobilecomputing;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * PrivacyManager - Utility class to manage and enforce privacy settings throughout the app
 * This ensures privacy settings are consistently applied across all features
 */
public class PrivacyManager {
    
    private static final String PREFS_NAME = "SikadPrivacyPrefs";
    
    // Privacy setting keys
    public static final String KEY_SHARE_RIDE_HISTORY = "share_ride_history";
    public static final String KEY_SHOW_ON_LEADERBOARDS = "show_on_leaderboards";
    public static final String KEY_SHARE_ACHIEVEMENTS = "share_achievements";
    public static final String KEY_BACKGROUND_LOCATION = "background_location";
    public static final String KEY_PUSH_NOTIFICATIONS = "push_notifications";
    public static final String KEY_EMAIL_NOTIFICATIONS = "email_notifications";
    public static final String KEY_MARKETING_COMMUNICATIONS = "marketing_communications";
    public static final String KEY_USAGE_ANALYTICS = "usage_analytics";
    public static final String KEY_SHARE_BIKE_LOCATION = "share_bike_location";
    public static final String KEY_ANONYMOUS_USAGE = "anonymous_usage";
    public static final String KEY_BIKE_HEALTH_DATA = "bike_health_data";
    public static final String KEY_EMERGENCY_CONTACTS = "emergency_contacts";
    public static final String KEY_GEOTAGGING = "geotagging";
    
    private Context context;
    private SharedPreferences prefs;
    
    public PrivacyManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Check if a specific privacy setting is enabled
     */
    public boolean isPrivacySettingEnabled(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }
    
    /**
     * Get user's location sharing preference
     */
    public boolean canShareLocation() {
        return isPrivacySettingEnabled(KEY_SHARE_BIKE_LOCATION, false);
    }
    
    /**
     * Check if user is in anonymous mode
     */
    public boolean isAnonymousMode() {
        return isPrivacySettingEnabled(KEY_ANONYMOUS_USAGE, true);
    }
    
    /**
     * Check if emergency contacts can be notified
     */
    public boolean canNotifyEmergencyContacts() {
        return isPrivacySettingEnabled(KEY_EMERGENCY_CONTACTS, true);
    }
    
    /**
     * Check if bike health data can be shared
     */
    public boolean canShareBikeHealthData() {
        return isPrivacySettingEnabled(KEY_BIKE_HEALTH_DATA, false);
    }
    
    /**
     * Check if geotagging is enabled for photos
     */
    public boolean isGeotaggingEnabled() {
        return isPrivacySettingEnabled(KEY_GEOTAGGING, false);
    }
    
    /**
     * Check if push notifications are enabled
     */
    public boolean arePushNotificationsEnabled() {
        return isPrivacySettingEnabled(KEY_PUSH_NOTIFICATIONS, true);
    }
    
    /**
     * Check if email notifications are enabled
     */
    public boolean areEmailNotificationsEnabled() {
        return isPrivacySettingEnabled(KEY_EMAIL_NOTIFICATIONS, true);
    }
    
    /**
     * Check if usage analytics are enabled
     */
    public boolean isUsageAnalyticsEnabled() {
        return isPrivacySettingEnabled(KEY_USAGE_ANALYTICS, true);
    }
    
    /**
     * Check if ride history can be shared
     */
    public boolean canShareRideHistory() {
        return isPrivacySettingEnabled(KEY_SHARE_RIDE_HISTORY, false);
    }
    
    /**
     * Check if user can appear on leaderboards
     */
    public boolean canShowOnLeaderboards() {
        return isPrivacySettingEnabled(KEY_SHOW_ON_LEADERBOARDS, true);
    }
    
    /**
     * Apply privacy settings to location data
     */
    public String anonymizeLocationData(String locationData) {
        if (isAnonymousMode()) {
            // Remove or mask identifying location information
            return "Anonymous Location";
        }
        return locationData;
    }
    
    /**
     * Apply privacy settings to user data
     */
    public String anonymizeUserData(String userData) {
        if (isAnonymousMode()) {
            // Remove or mask identifying user information
            return "Anonymous User";
        }
        return userData;
    }
    
    /**
     * Check if data can be sent to analytics
     */
    public boolean canSendToAnalytics() {
        return isUsageAnalyticsEnabled() && !isAnonymousMode();
    }
    
    /**
     * Log privacy-aware data usage
     */
    public void logPrivacyEvent(String event, String data) {
        if (canSendToAnalytics()) {
            Log.d("PRIVACY_EVENT", event + ": " + anonymizeUserData(data));
        }
    }
    
    /**
     * Get privacy-compliant user ID for analytics
     */
    public String getPrivacyCompliantUserId(String userId) {
        if (isAnonymousMode()) {
            return "anonymous_" + userId.hashCode();
        }
        return userId;
    }
    
    /**
     * Check if emergency contact notification should be sent
     */
    public boolean shouldNotifyEmergencyContacts(String eventType) {
        if (!canNotifyEmergencyContacts()) {
            Log.d("PRIVACY", "Emergency contact notification blocked by privacy settings");
            return false;
        }
        
        // Only notify for safety-critical events
        return "crash".equals(eventType) || "theft".equals(eventType) || "emergency".equals(eventType);
    }
    
    /**
     * Apply privacy settings to bike tracking data
     */
    public boolean shouldTrackBikeLocation(String bikeId) {
        if (!canShareLocation()) {
            Log.d("PRIVACY", "Bike location tracking disabled by privacy settings");
            return false;
        }
        return true;
    }
    
    /**
     * Get privacy summary for debugging
     */
    public String getPrivacySummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Privacy Settings Summary:\n");
        summary.append("- Location Sharing: ").append(canShareLocation()).append("\n");
        summary.append("- Anonymous Mode: ").append(isAnonymousMode()).append("\n");
        summary.append("- Emergency Contacts: ").append(canNotifyEmergencyContacts()).append("\n");
        summary.append("- Analytics: ").append(canSendToAnalytics()).append("\n");
        summary.append("- Push Notifications: ").append(arePushNotificationsEnabled()).append("\n");
        return summary.toString();
    }
}
