package com.example.finalprojectmobilecomputing;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDataService {
    private static final String TAG = "UserDataService";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public UserDataService() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    // Interface for data callbacks
    public interface UserDataCallback {
        void onSuccess(UserModel user);
        void onFailure(String error);
    }

    public interface RideHistoryCallback {
        void onSuccess(List<RideLog> rides);
        void onFailure(String error);
    }

    public interface FeedbackCallback {
        void onSuccess(List<Map<String, Object>> feedbacks);
        void onFailure(String error);
    }

    public interface CompleteUserDataCallback {
        void onSuccess(Map<String, Object> completeData);
        void onFailure(String error);
    }

    // Get current user's complete profile data
    public void getUserProfile(UserDataCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("User not logged in");
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserModel user = documentSnapshot.toObject(UserModel.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure("User document not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user profile", e);
                    callback.onFailure("Failed to load user profile: " + e.getMessage());
                });
    }

    // Get user's ride history
    public void getUserRideHistory(RideHistoryCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("User not logged in");
            return;
        }

        db.collection("ride_logs")
                .whereEqualTo("user_id", currentUser.getUid())
                .orderBy("start_time", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<RideLog> rides = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        RideLog ride = document.toObject(RideLog.class);
                        ride.setUid(document.getId());
                        rides.add(ride);
                    }
                    callback.onSuccess(rides);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting ride history", e);
                    callback.onFailure("Failed to load ride history: " + e.getMessage());
                });
    }

    // Get user's feedback and ratings
    public void getUserFeedback(FeedbackCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("User not logged in");
            return;
        }

        db.collection("feedback")
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> feedbacks = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        feedbacks.add(document.getData());
                    }
                    callback.onSuccess(feedbacks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting feedback", e);
                    callback.onFailure("Failed to load feedback: " + e.getMessage());
                });
    }

    // Get complete user data (profile + ride history + feedback)
    public void getCompleteUserData(CompleteUserDataCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("User not logged in");
            return;
        }

        Map<String, Object> completeData = new HashMap<>();
        String userId = currentUser.getUid();

        // Get user profile
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        completeData.put("profile", userDoc.getData());
                        
                        // Get ride history
                        db.collection("ride_logs")
                                .whereEqualTo("user_id", userId)
                                .orderBy("start_time", Query.Direction.DESCENDING)
                                .get()
                                .addOnSuccessListener(rideSnapshot -> {
                                    List<Map<String, Object>> rides = new ArrayList<>();
                                    for (QueryDocumentSnapshot doc : rideSnapshot) {
                                        rides.add(doc.getData());
                                    }
                                    completeData.put("rideHistory", rides);

                                    // Get feedback
                                    db.collection("feedback")
                                            .whereEqualTo("userId", userId)
                                            .orderBy("timestamp", Query.Direction.DESCENDING)
                                            .get()
                                            .addOnSuccessListener(feedbackSnapshot -> {
                                                List<Map<String, Object>> feedbacks = new ArrayList<>();
                                                for (QueryDocumentSnapshot doc : feedbackSnapshot) {
                                                    feedbacks.add(doc.getData());
                                                }
                                                completeData.put("feedback", feedbacks);
                                                completeData.put("dataRetrievedAt", System.currentTimeMillis());
                                                callback.onSuccess(completeData);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error getting feedback", e);
                                                completeData.put("feedback", new ArrayList<>());
                                                callback.onSuccess(completeData);
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error getting ride history", e);
                                    completeData.put("rideHistory", new ArrayList<>());
                                    callback.onSuccess(completeData);
                                });
                    } else {
                        callback.onFailure("User document not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user profile", e);
                    callback.onFailure("Failed to load user data: " + e.getMessage());
                });
    }

    // Update user's last login timestamp
    public void updateLastLogin() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        Map<String, Object> update = new HashMap<>();
        update.put("lastLoginTimestamp", System.currentTimeMillis());

        db.collection("users").document(currentUser.getUid())
                .update(update)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Last login updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update last login", e));
    }

    // Check if user is fully verified
    public void checkUserVerificationStatus(UserDataCallback callback) {
        getUserProfile(new UserDataCallback() {
            @Override
            public void onSuccess(UserModel user) {
                if (user.isFullyVerified()) {
                    callback.onSuccess(user);
                } else {
                    callback.onFailure("User not fully verified. Status: " + user.getVerificationStatus());
                }
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    // Export user data as formatted string
    public void exportUserDataAsString(CompleteUserDataCallback callback) {
        getCompleteUserData(new CompleteUserDataCallback() {
            @Override
            public void onSuccess(Map<String, Object> completeData) {
                StringBuilder exportData = new StringBuilder();
                exportData.append("=== SIKAD USER DATA EXPORT ===\n");
                exportData.append("Export Date: ").append(new java.util.Date()).append("\n\n");

                // Profile data
                if (completeData.containsKey("profile")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> profile = (Map<String, Object>) completeData.get("profile");
                    exportData.append("=== PROFILE INFORMATION ===\n");
                    for (Map.Entry<String, Object> entry : profile.entrySet()) {
                        exportData.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                    }
                    exportData.append("\n");
                }

                // Ride history
                if (completeData.containsKey("rideHistory")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rides = (List<Map<String, Object>>) completeData.get("rideHistory");
                    exportData.append("=== RIDE HISTORY ===\n");
                    exportData.append("Total Rides: ").append(rides.size()).append("\n\n");
                    
                    for (int i = 0; i < rides.size(); i++) {
                        Map<String, Object> ride = rides.get(i);
                        exportData.append("Ride ").append(i + 1).append(":\n");
                        for (Map.Entry<String, Object> entry : ride.entrySet()) {
                            exportData.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                        }
                        exportData.append("\n");
                    }
                }

                // Feedback
                if (completeData.containsKey("feedback")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> feedbacks = (List<Map<String, Object>>) completeData.get("feedback");
                    exportData.append("=== FEEDBACK & RATINGS ===\n");
                    exportData.append("Total Feedback Entries: ").append(feedbacks.size()).append("\n\n");
                    
                    for (int i = 0; i < feedbacks.size(); i++) {
                        Map<String, Object> feedback = feedbacks.get(i);
                        exportData.append("Feedback ").append(i + 1).append(":\n");
                        for (Map.Entry<String, Object> entry : feedback.entrySet()) {
                            exportData.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                        }
                        exportData.append("\n");
                    }
                }

                callback.onSuccess(completeData);
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }
}
