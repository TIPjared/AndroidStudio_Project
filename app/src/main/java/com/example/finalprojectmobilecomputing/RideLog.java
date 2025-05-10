package com.example.finalprojectmobilecomputing;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RideLog {
    public double distance_traveled;
    public String location;
    public long timestamp;
    public String user_id;
    private String uid;
    private String status;
    private Double rating;
    private int duration; // Duration in minutes
    
    public RideLog() {
        // Required empty constructor for Firestore
    }
    
    public RideLog(String user_id, String location, double distance_traveled, long timestamp) {
        this.user_id = user_id;
        this.location = location;
        this.distance_traveled = distance_traveled;
        this.timestamp = timestamp;
        this.status = "Completed";
    }

    public double getDistance_traveled() { return distance_traveled; }
    public String getLocation() { return location; }
    public long getTimestamp() { return timestamp; }
    public String getUser_id() { return user_id; }

    public void setDistance_traveled(double distance_traveled) { this.distance_traveled = distance_traveled; }
    public void setLocation(String location) { this.location = location; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setUser_id(String user_id) { this.user_id = user_id; }

    public String getUid() { return uid; }
    public String getStatus() { return status != null ? status : "Completed"; }
    public Double getRating() { return rating; }
    public int getDuration() { return duration; }
    public Double getDistance() { return distance_traveled; }

    public void setUid(String uid) { this.uid = uid; }
    public void setStatus(String status) { this.status = status; }
    public void setRating(Double rating) { this.rating = rating; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getFormattedDistance() {
        return String.format(Locale.getDefault(), "%.1f km", distance_traveled);
    }
    
    public String getFormattedDuration() {
        return duration + " min";
    }
    
    public String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        Date date = new Date(timestamp);
        
        // Check if the date is today
        SimpleDateFormat todaySdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String todayStr = todaySdf.format(new Date());
        String dateStr = todaySdf.format(date);
        
        if (todayStr.equals(dateStr)) {
            return "Today";
        }
        
        return sdf.format(date);
    }
}
