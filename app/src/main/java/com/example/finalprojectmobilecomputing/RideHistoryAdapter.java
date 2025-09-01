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
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a", Locale.getDefault());
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
        
        // Set ride data - Always show Antel Grand Village
        holder.locationText.setText("Antel Grand Village");
        ride.setLocation("Antel Grand Village");
        
        holder.distanceText.setText(String.format(Locale.getDefault(), "%.1f km", ride.getDistance()));
        holder.durationText.setText(String.format(Locale.getDefault(), "%d min", ride.getDuration()));
        
        // Display cost
        holder.costText.setText(String.format(Locale.getDefault(), "₱%.0f", ride.getCost()));
        
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
        // Define Antel Grand Village location and geofence
        final LatLng ANTEL_GRAND_VILLAGE = new LatLng(14.5964, 120.9904); // Main entrance coordinates
        
        // Calculate distance to Antel Grand Village
        double distToAntel = computeDistance(location, ANTEL_GRAND_VILLAGE);
        
        // If within 500 meters of Antel Grand Village, consider it as Antel Grand Village
        if (distToAntel <= 0.005) { // Approximately 500 meters in degrees
            return "Antel Grand Village";
        }
        
        // Default fallback - all rides are considered within Antel Grand Village
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
        return "Antel Grand Village";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateLabel, locationText, distanceText, durationText, costText, statusText;
        TextView viewDetailsButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            
            dateLabel = itemView.findViewById(R.id.dateLabel);
            locationText = itemView.findViewById(R.id.locationText);
            distanceText = itemView.findViewById(R.id.distanceText);
            durationText = itemView.findViewById(R.id.durationText);
            costText = itemView.findViewById(R.id.costText);
            statusText = itemView.findViewById(R.id.statusText);
            viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
        }
    }
} 