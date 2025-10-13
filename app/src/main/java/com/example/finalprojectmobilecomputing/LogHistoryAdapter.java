package com.example.finalprojectmobilecomputing;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.json.JSONObject;

public class LogHistoryAdapter extends RecyclerView.Adapter<LogHistoryAdapter.ViewHolder> {

    private final List<LogHistory> ridesList;
    private final Context context;
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private static final String DELETE_URL = "https://sikad-server.onrender.com/deleteRide";

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

        // Set location, distance, duration, cost
        holder.locationText.setText("Antel Grand Village");
        ride.setLocation("Antel Grand Village");

        holder.distanceText.setText(String.format(Locale.getDefault(), "%.2f km", ride.getDistance()));
        holder.durationText.setText(String.format(Locale.getDefault(), "%d min", ride.getDuration()));
        holder.costText.setText(String.format(Locale.getDefault(), "₱%.0f", ride.getCost()));

        // Date label
        if (ride.getTimestamp() != null) {
            holder.dateLabel.setText(isToday(ride.getTimestamp()) ? "Today" : DATE_FORMAT.format(ride.getTimestamp()));
        } else holder.dateLabel.setText("Unknown date");

        // Status
        if (ride.getStatus() != null && ride.getStatus().equalsIgnoreCase("completed")) {
            holder.statusText.setText("Completed");
            holder.statusText.setBackgroundResource(R.drawable.status_background);
        }

        // View Details button
        holder.viewDetailsButton.setOnClickListener(v -> {
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
        Calendar today = Calendar.getInstance();
        Calendar calDate = Calendar.getInstance();
        calDate.setTime(date);
        return today.get(Calendar.YEAR) == calDate.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == calDate.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) == calDate.get(Calendar.DAY_OF_MONTH);
    }

    // AsyncTask for server delete
    private class DeleteRideTask extends AsyncTask<Void, Void, Boolean> {
        private final String rideId;
        private final String userId;
        private final int position;

        DeleteRideTask(String rideId, String userId, int position) {
            this.rideId = rideId;
            this.userId = userId;
            this.position = position;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                URL url = new URL(DELETE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("rideId", rideId);
                json.put("userId", userId);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.toString().getBytes());
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                return responseCode == 200;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(context, "Ride deleted successfully", Toast.LENGTH_SHORT).show();
                ridesList.remove(position);
                notifyItemRemoved(position);
            } else {
                Toast.makeText(context, "Failed to delete ride", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ViewHolder
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateLabel, locationText, distanceText, durationText, costText, statusText, viewDetailsButton, deleteButton;

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
