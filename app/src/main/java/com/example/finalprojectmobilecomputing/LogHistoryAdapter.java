package com.example.finalprojectmobilecomputing;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class LogHistoryAdapter extends RecyclerView.Adapter<LogHistoryAdapter.ViewHolder> {

    private final List<LogHistory> ridesList;
    private final Context context;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public LogHistoryAdapter(Context context, List<LogHistory> ridesList) {
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
        LogHistory ride = ridesList.get(position);

        // Set location (currently always Antel Grand Village)
        holder.locationText.setText("Antel Grand Village");
        ride.setLocation("Antel Grand Village");

        // Distance, duration, and cost
        holder.distanceText.setText(String.format(Locale.getDefault(), "%.2f km", ride.getDistance()));
        holder.durationText.setText(String.format(Locale.getDefault(), "%d min", ride.getDuration()));
        holder.costText.setText(String.format(Locale.getDefault(), "₱%.0f", ride.getCost()));

        // Date label
        if (ride.getTimestamp() != null) {
            holder.dateLabel.setText(isToday(ride.getTimestamp()) ? "Today" : DATE_FORMAT.format(ride.getTimestamp()));
        } else {
            holder.dateLabel.setText("Unknown date");
        }

        // Status
        if (ride.getStatus() != null && ride.getStatus().equalsIgnoreCase("completed")) {
            holder.statusText.setText("Completed");
            holder.statusText.setBackgroundResource(R.drawable.status_background);
        }

        // Make the entire item clickable
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, HistoryDetailsActivity.class);
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
        java.util.Calendar calDate = java.util.Calendar.getInstance();
        calDate.setTime(date);
        return today.get(java.util.Calendar.YEAR) == calDate.get(java.util.Calendar.YEAR) &&
                today.get(java.util.Calendar.MONTH) == calDate.get(java.util.Calendar.MONTH) &&
                today.get(java.util.Calendar.DAY_OF_MONTH) == calDate.get(java.util.Calendar.DAY_OF_MONTH);
    }

    public void updateRides(List<LogHistory> newRides) {
        ridesList.clear();
        ridesList.addAll(newRides);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateLabel, locationText, distanceText, durationText, costText, statusText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateLabel = itemView.findViewById(R.id.dateLabel);
            locationText = itemView.findViewById(R.id.locationText);
            distanceText = itemView.findViewById(R.id.distanceText);
            durationText = itemView.findViewById(R.id.durationText);
            costText = itemView.findViewById(R.id.costText);
            statusText = itemView.findViewById(R.id.statusText);
        }
    }
}
