package com.example.finalprojectmobilecomputing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class RideLogAdapter extends RecyclerView.Adapter<RideLogAdapter.RideLogViewHolder> {
    private List<RideLog> rideLogs;

    public RideLogAdapter(List<RideLog> rideLogs) {
        this.rideLogs = rideLogs;
    }

    @NonNull
    @Override
    public RideLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ride_log, parent, false);
        return new RideLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RideLogViewHolder holder, int position) {
        RideLog log = rideLogs.get(position);

        // Set date and status
        holder.tvDate.setText(log.getFormattedDate());
        holder.tvStatus.setText(log.getStatus());

        // Set location
        holder.tvLocation.setText(log.getLocation());

        // Set ride stats
        holder.tvDistance.setText(log.getFormattedDistance());
        holder.tvDuration.setText(String.format(Locale.getDefault(), "%d min", log.getDuration()));
        holder.tvTimestamp.setText(log.getFormattedTimestamp());

        // Button click listeners
        holder.btnViewDetails.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "View details for ride: " + log.getUid(), Toast.LENGTH_SHORT).show();
            // TODO: Implement view details functionality
        });

        holder.btnShare.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Share ride: " + log.getUid(), Toast.LENGTH_SHORT).show();
            // TODO: Implement share functionality
        });
    }

    @Override
    public int getItemCount() {
        return rideLogs.size();
    }

    static class RideLogViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvStatus, tvLocation, tvRouteLabel;
        TextView tvDistance, tvDuration, tvTimestamp;
        Button btnViewDetails, btnShare;

        RideLogViewHolder(View itemView) {
            super(itemView);
            // Header section
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            
            // Route section
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvRouteLabel = itemView.findViewById(R.id.tvRouteLabel);
            
            // Stats section
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            
            // Buttons
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnShare = itemView.findViewById(R.id.btnShare);
        }
    }
} 