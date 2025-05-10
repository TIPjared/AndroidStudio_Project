package com.example.finalprojectmobilecomputing;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RideHistoryAdapter extends RecyclerView.Adapter<RideHistoryAdapter.ViewHolder> {

    private final List<RideHistory> ridesList;
    private final Context context;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public RideHistoryAdapter(Context context, List<RideHistory> ridesList) {
        this.context = context;
        this.ridesList = ridesList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ride_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RideHistory ride = ridesList.get(position);
        
        // Set ride data
        if (ride.getLocation() != null && !ride.getLocation().isEmpty()) {
            // Check if the location is in format "lat,lng"
            if (ride.getLocation().matches("^[-+]?\\d+\\.\\d+,\\s*[-+]?\\d+\\.\\d+$")) {
                // Parse the coordinates and find the nearest predefined location
                String[] latLng = ride.getLocation().split(",");
                if (latLng.length == 2) {
                    try {
                        double lat = Double.parseDouble(latLng[0].trim());
                        double lng = Double.parseDouble(latLng[1].trim());
                        LatLng coordinates = new LatLng(lat, lng);
                        // Find the nearest location
                        String locationName = getNameFromLocation(coordinates);
                        holder.locationText.setText(locationName);
                        // Update the ride object's location for use in share function
                        ride.setLocation(locationName);
                    } catch (NumberFormatException e) {
                        holder.locationText.setText(getDefaultLocationName());
                    }
                } else {
                    holder.locationText.setText(getDefaultLocationName());
                }
            } else {
                // It's already a named location
                holder.locationText.setText(ride.getLocation());
            }
        } else {
            holder.locationText.setText(getDefaultLocationName());
        }
        
        holder.distanceText.setText(String.format(Locale.getDefault(), "%.1f km", ride.getDistance()));
        holder.durationText.setText(String.format(Locale.getDefault(), "%d min", ride.getDuration()));
        
        if (ride.getTimestamp() != null) {
            holder.timestampText.setText(TIME_FORMAT.format(ride.getTimestamp()));
        } else {
            holder.timestampText.setText("00:00");
        }
        
        // Set date label (Today or actual date)
        if (isToday(ride.getTimestamp())) {
            holder.dateLabel.setText("Today");
        } else if (ride.getTimestamp() != null) {
            holder.dateLabel.setText(DATE_FORMAT.format(ride.getTimestamp()));
        } else {
            holder.dateLabel.setText("Unknown date");
        }
        
        // Set status
        if (ride.getStatus() != null && ride.getStatus().equalsIgnoreCase("completed")) {
            holder.statusText.setText("Completed");
            holder.statusText.setBackgroundResource(R.drawable.status_background);
        }
        
        // Set click listeners
        holder.viewDetailsButton.setOnClickListener(v -> {
            // Open ride details activity
            Intent intent = new Intent(context, RideDetailsActivity.class);
            intent.putExtra("RIDE_ID", ride.getId());
            context.startActivity(intent);
        });
        
        holder.shareButton.setOnClickListener(v -> {
            // Share ride details
            String shareText = "I completed a ride of " + 
                    String.format(Locale.getDefault(), "%.1f km", ride.getDistance()) + 
                    " in " + String.format(Locale.getDefault(), "%d minutes", ride.getDuration()) + 
                    " with the PedalGo App!";
            
            // Always include the actual location name, which is now correctly set in the ride object
            shareText += "\nRoute: " + (ride.getLocation() != null ? ride.getLocation() : getDefaultLocationName());
            
            // Add date
            if (ride.getTimestamp() != null) {
                shareText += "\nDate: " + DATE_FORMAT.format(ride.getTimestamp());
            }
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            context.startActivity(Intent.createChooser(shareIntent, "Share your ride"));
        });
    }

    @Override
    public int getItemCount() {
        return ridesList.size();
    }
    
    private boolean isToday(java.util.Date date) {
        if (date == null) return false;
        java.util.Calendar today = java.util.Calendar.getInstance();
        java.util.Calendar dateToCheck = java.util.Calendar.getInstance();
        dateToCheck.setTime(date);
        
        return today.get(java.util.Calendar.YEAR) == dateToCheck.get(java.util.Calendar.YEAR) &&
               today.get(java.util.Calendar.MONTH) == dateToCheck.get(java.util.Calendar.MONTH) &&
               today.get(java.util.Calendar.DAY_OF_MONTH) == dateToCheck.get(java.util.Calendar.DAY_OF_MONTH);
    }
    
    public void updateRides(List<RideHistory> newRides) {
        ridesList.clear();
        ridesList.addAll(newRides);
        notifyDataSetChanged();
    }
    
    // Helper method to get the name of a location from coordinates
    private String getNameFromLocation(LatLng location) {
        // Define all predefined locations
        final LatLng RIZAL_PARK = new LatLng(14.5825, 120.9783);
        final LatLng FORT_SANTIAGO = new LatLng(14.5950, 120.9694);
        final LatLng CITY_HALL = new LatLng(14.589793, 120.981617);
        final LatLng CATHEDRAL = new LatLng(14.59147, 120.97356);
        final LatLng MUSEUM = new LatLng(14.5869, 120.9812);
        final LatLng QUIAPO_CHURCH = new LatLng(14.598782, 120.983783);
        final LatLng TIP_ARLEGUI = new LatLng(14.5964, 120.9904);
        
        // Calculate distance to each location
        double distToRizal = computeDistance(location, RIZAL_PARK);
        double distToFort = computeDistance(location, FORT_SANTIAGO);
        double distToCity = computeDistance(location, CITY_HALL);
        double distToCathedral = computeDistance(location, CATHEDRAL);
        double distToMuseum = computeDistance(location, MUSEUM);
        double distToQuiapo = computeDistance(location, QUIAPO_CHURCH);
        double distToTIP = computeDistance(location, TIP_ARLEGUI);
        
        // Find the closest location
        double minDist = Math.min(Math.min(Math.min(distToRizal, distToFort), 
                Math.min(distToCity, distToCathedral)), 
                Math.min(Math.min(distToMuseum, distToQuiapo), distToTIP));
        
        if (minDist == distToRizal) return "Rizal Park";
        if (minDist == distToFort) return "Fort Santiago";
        if (minDist == distToCity) return "City Hall";
        if (minDist == distToCathedral) return "Cathedral";
        if (minDist == distToMuseum) return "Museum";
        if (minDist == distToQuiapo) return "Quiapo Church";
        if (minDist == distToTIP) return "TIP Arlegui";
        
        // Default fallback
        return getDefaultLocationName();
    }
    
    // Compute Euclidean distance between two points
    private double computeDistance(LatLng point1, LatLng point2) {
        double dx = point1.latitude - point2.latitude;
        double dy = point1.longitude - point2.longitude;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    // Return the default location name
    private String getDefaultLocationName() {
        return "TIP Arlegui";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateLabel, locationText, distanceText, durationText, timestampText, statusText;
        TextView viewDetailsButton, shareButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            
            dateLabel = itemView.findViewById(R.id.dateLabel);
            locationText = itemView.findViewById(R.id.locationText);
            distanceText = itemView.findViewById(R.id.distanceText);
            durationText = itemView.findViewById(R.id.durationText);
            timestampText = itemView.findViewById(R.id.timestampText);
            statusText = itemView.findViewById(R.id.statusText);
            viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
            shareButton = itemView.findViewById(R.id.shareButton);
        }
    }
} 