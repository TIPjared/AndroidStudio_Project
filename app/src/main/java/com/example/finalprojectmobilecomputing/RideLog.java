package com.example.finalprojectmobilecomputing;

import com.google.android.gms.maps.model.LatLng;

public class RideLog {
    public String userId;
    public LatLng startLocation;
    public LatLng endLocation;
    public long timestamp;
    public String status;

    public RideLog(String userId, LatLng startLocation, LatLng endLocation, long timestamp, String status) {
        this.userId = userId;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.timestamp = timestamp;
        this.status = status;
    }
}
