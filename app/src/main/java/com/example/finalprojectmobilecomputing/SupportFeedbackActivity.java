package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class SupportFeedbackActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private final String[] tabTitles = new String[]{"Support", "Feedback"};
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_feedback);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.supportToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Setup ViewPager and TabLayout
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        
        // Setup ViewPager adapter
        SupportFeedbackPagerAdapter pagerAdapter = new SupportFeedbackPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> 
            tab.setText(tabTitles[position])
        ).attach();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ViewPager adapter
    private static class SupportFeedbackPagerAdapter extends FragmentStateAdapter {
        
        public SupportFeedbackPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Return the appropriate fragment based on position
            return position == 0 ? new SupportFragment() : new FeedbackFragment();
        }

        @Override
        public int getItemCount() {
            return 2; // Support and Feedback tabs
        }
    }

    // Support Fragment class
    public static class SupportFragment extends Fragment {
        
        public SupportFragment() {
            super(R.layout.fragment_support);
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            
            // Initialize form submit
            view.findViewById(R.id.sendButton).setOnClickListener(v -> {
                EditText nameInput = view.findViewById(R.id.nameInput);
                EditText emailInput = view.findViewById(R.id.emailInput);
                EditText issueInput = view.findViewById(R.id.issueInput);
                
                String name = nameInput.getText().toString().trim();
                String email = emailInput.getText().toString().trim();
                String issue = issueInput.getText().toString().trim();
                
                // Validation
                if (TextUtils.isEmpty(name)) {
                    nameInput.setError("Please enter your name");
                    nameInput.requestFocus();
                    return;
                }
                
                if (TextUtils.isEmpty(email)) {
                    emailInput.setError("Please enter your email");
                    emailInput.requestFocus();
                    return;
                }
                
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailInput.setError("Please enter a valid email address");
                    emailInput.requestFocus();
                    return;
                }
                
                if (TextUtils.isEmpty(issue)) {
                    issueInput.setError("Please describe your issue");
                    issueInput.requestFocus();
                    return;
                }
                
                if (issue.length() < 20) {
                    issueInput.setError("Please provide more details (at least 20 characters)");
                    issueInput.requestFocus();
                    return;
                }
                
                // Clear any previous errors
                nameInput.setError(null);
                emailInput.setError(null);
                issueInput.setError(null);
                
                // Save to Firebase
                saveSupportRequest(name, email, issue);
                
                // Clear form
                clearForm(view);
                
                // Show success message
                Toast.makeText(getContext(), "Support request submitted. We'll contact you shortly.", Toast.LENGTH_LONG).show();
            });

            // Initialize card clicks
            view.findViewById(R.id.cardFaqs).setOnClickListener(v -> {
                // Open FAQ webpage or show FAQ dialog
                openFaqWebpage();
            });
            
            view.findViewById(R.id.cardContact).setOnClickListener(v -> {
                // Email the support team directly
                emailSupportTeam();
            });
            
            view.findViewById(R.id.cardHelpCenter).setOnClickListener(v -> {
                // Open help center webpage
                openHelpCenterWebpage();
            });
        }
        
        private void saveSupportRequest(String name, String email, String issue) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            
            Map<String, Object> supportRequest = new HashMap<>();
            supportRequest.put("name", name);
            supportRequest.put("email", email);
            supportRequest.put("issue", issue);
            supportRequest.put("timestamp", System.currentTimeMillis());
            supportRequest.put("status", "pending");
            supportRequest.put("priority", "medium");
            supportRequest.put("category", "general_support");
            supportRequest.put("userId", mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "guest");
            supportRequest.put("appVersion", "1.0.0");
            supportRequest.put("assignedTo", "");
            supportRequest.put("response", "");
            supportRequest.put("testId", "SUPPORT_TEST_" + System.currentTimeMillis()); // Unique test identifier
            supportRequest.put("submissionTime", new java.util.Date().toString()); // Human readable time
            
            Log.d("Support", "Attempting to save support request to Firebase");
            Log.d("Support", "Support request data: " + supportRequest.toString());
            
            db.collection("support_requests")
                .add(supportRequest)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Support", "Support request saved with ID: " + documentReference.getId());
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Support request submitted successfully!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Error handling
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error submitting request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Support", "Error saving support request", e);
                    }
                });
        }
        
        private void clearForm(View view) {
            ((EditText) view.findViewById(R.id.nameInput)).setText("");
            ((EditText) view.findViewById(R.id.emailInput)).setText("");
            ((EditText) view.findViewById(R.id.issueInput)).setText("");
        }
        
        private void openFaqWebpage() {
            Intent intent = new Intent(getContext(), FAQActivity.class);
            startActivity(intent);
        }
        
        private void emailSupportTeam() {
            Intent intent = new Intent(getContext(), ContactSupportActivity.class);
            startActivity(intent);
        }
        
        private void openHelpCenterWebpage() {
            Intent intent = new Intent(getContext(), HelpCenterActivity.class);
            startActivity(intent);
        }
    }

    // Feedback Fragment class
    public static class FeedbackFragment extends Fragment {
        
        private FirebaseFirestore db;
        private float userRating = 0;
        private boolean isAnonymous = false;
        
        public FeedbackFragment() {
            super(R.layout.fragment_feedback);
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            
            db = FirebaseFirestore.getInstance();
            
            // Test Firebase connectivity
            Log.d("Feedback", "=== FIREBASE CONNECTIVITY TEST ===");
            Log.d("Feedback", "Firestore instance: " + (db != null ? "OK" : "NULL"));
            
            // Test a simple read to verify connectivity
            db.collection("test").limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("Feedback", "Firebase connectivity test: SUCCESS");
                })
                .addOnFailureListener(e -> {
                    Log.e("Feedback", "Firebase connectivity test: FAILED - " + e.getMessage());
                });
            
            // Test a simple write to verify write permissions
            Map<String, Object> testData = new HashMap<>();
            testData.put("test", "connectivity");
            testData.put("timestamp", System.currentTimeMillis());
            
            db.collection("test").document("connectivity_test").set(testData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Feedback", "Firebase write test: SUCCESS");
                })
                .addOnFailureListener(e -> {
                    Log.e("Feedback", "Firebase write test: FAILED - " + e.getMessage());
                });
            
            // Setup rating bar
            RatingBar ratingBar = view.findViewById(R.id.ratingBar);
            ratingBar.setOnRatingBarChangeListener((rBar, rating, fromUser) -> {
                userRating = rating;
                updateRatingText(view, rating);
            });
            
            
            // Setup anonymous switch
            view.findViewById(R.id.anonymousSwitch).setOnClickListener(v -> 
                isAnonymous = ((com.google.android.material.switchmaterial.SwitchMaterial) v).isChecked()
            );
            
            // Add debug test button
            view.findViewById(R.id.submitFeedbackButton).setOnClickListener(v -> {
                // First, let's test Firebase connectivity
                testFirebaseConnection();
                
                // Then proceed with normal feedback submission
                EditText feedbackInput = view.findViewById(R.id.feedbackInput);
                String feedback = feedbackInput.getText().toString().trim();
                
                // Validation
                if (userRating == 0) {
                    Toast.makeText(getContext(), "Please rate your experience", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (TextUtils.isEmpty(feedback)) {
                    feedbackInput.setError("Please enter your feedback");
                    feedbackInput.requestFocus();
                    return;
                }
                
                if (feedback.length() < 10) {
                    feedbackInput.setError("Please provide more detailed feedback (at least 10 characters)");
                    feedbackInput.requestFocus();
                    return;
                }
                
                // Clear any previous errors
                feedbackInput.setError(null);
                
                // Check user authentication before saving
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                FirebaseUser currentUser = mAuth.getCurrentUser();
                
                if (currentUser == null) {
                    Log.e("Feedback", "❌ USER NOT LOGGED IN - Cannot save feedback");
                    Toast.makeText(getContext(), "Please log in first to submit feedback", Toast.LENGTH_LONG).show();
                    return;
                }
                
                Log.d("Feedback", "✅ USER LOGGED IN - UID: " + currentUser.getUid() + ", Email: " + currentUser.getEmail());
                
                // Save feedback to Firebase
                Log.d("Feedback", "About to save feedback: rating=" + userRating + ", feedback=" + feedback + ", anonymous=" + isAnonymous);
                saveFeedback(feedback);
                
                // Clear form
                feedbackInput.setText("");
                ratingBar.setRating(0);
                userRating = 0;
                isAnonymous = false;
                ((com.google.android.material.switchmaterial.SwitchMaterial) view.findViewById(R.id.anonymousSwitch)).setChecked(false);
                updateRatingText(view, 0);
                
                // Success message will be shown in saveFeedback method
            });
        }
        
        private void updateRatingText(View view, float rating) {
            TextView ratingText = view.findViewById(R.id.ratingText);
            if (rating == 0) {
                ratingText.setText(R.string.tap_to_rate);
            } else {
                ratingText.setText(String.format("%.1f/5", rating));
            }
        }
        
        private void saveFeedback(String feedback) {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();
            
            Log.d("Feedback", "=== FEEDBACK SAVE START ===");
            Log.d("Feedback", "Current user: " + (currentUser != null ? currentUser.getUid() : "null"));
            Log.d("Feedback", "User email: " + (currentUser != null ? currentUser.getEmail() : "null"));
            Log.d("Feedback", "Is anonymous: " + isAnonymous);
            Log.d("Feedback", "Rating: " + userRating);
            Log.d("Feedback", "Feedback text: " + feedback);
            
            // Always use the authenticated user's information, even if anonymous is selected
            // The anonymous flag is just for display purposes
            String userId = currentUser != null ? currentUser.getUid() : "anonymous";
            String userEmail = currentUser != null ? currentUser.getEmail() : "anonymous";
            
            // If user wants to submit anonymously, we'll mask the email in the feedback data
            if (isAnonymous && currentUser != null) {
                userEmail = "anonymous_user_" + userId.substring(0, Math.min(8, userId.length()));
            }
            
            Log.d("Feedback", "Final userId: " + userId + ", userEmail: " + userEmail);
            
            Map<String, Object> feedbackData = new HashMap<>();
            feedbackData.put("userId", userId);
            feedbackData.put("userEmail", userEmail);
            feedbackData.put("rating", userRating);
            feedbackData.put("feedback", feedback);
            feedbackData.put("type", "general"); // Simplified to general feedback
            feedbackData.put("isAnonymous", isAnonymous);
            feedbackData.put("timestamp", System.currentTimeMillis());
            feedbackData.put("status", "new");
            feedbackData.put("appVersion", "1.0.0");
            feedbackData.put("priority", "low");
            feedbackData.put("category", "user_feedback");
            feedbackData.put("testId", "NEW_TEST_" + System.currentTimeMillis()); // Unique test identifier
            feedbackData.put("submissionTime", new java.util.Date().toString()); // Human readable time
            
            Log.d("Feedback", "Attempting to save to Firebase collection 'feedback'");
            Log.d("Feedback", "Feedback data: " + feedbackData.toString());
            Log.d("Feedback", "Firestore instance: " + (db != null ? "OK" : "NULL"));
            
            if (db == null) {
                Log.e("Feedback", "FirebaseFirestore instance is null! Initializing...");
                db = FirebaseFirestore.getInstance();
                Log.d("Feedback", "Re-initialized Firestore: " + (db != null ? "OK" : "STILL NULL"));
                
                if (db == null) {
                    Log.e("Feedback", "Failed to initialize FirebaseFirestore!");
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Database connection error. Please try again.", Toast.LENGTH_LONG).show();
                    }
                    return;
                }
            }
            
            // Create unique document ID for each feedback submission
            String feedbackDocId = userId + "_" + System.currentTimeMillis();

            Log.d("Feedback", "Saving to document ID: " + feedbackDocId);
            
            db.collection("feedback")
                .add(feedbackData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Feedback", "Feedback saved successfully with ID: " + documentReference.getId());
                    Log.d("Feedback", "Document data: " + feedbackData.toString());
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Feedback submitted successfully!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Feedback", "Error saving feedback to Firebase", e);
                    Log.e("Feedback", "Failed data: " + feedbackData.toString());
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error submitting feedback: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        }
        
        private void testFirebaseConnection() {
            Log.d("Feedback", "=== FIREBASE CONNECTION TEST ===");
            
            try {
                // Test 1: Check Firebase Auth
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                FirebaseUser currentUser = mAuth.getCurrentUser();
                Log.d("Feedback", "Firebase Auth - Current User: " + (currentUser != null ? currentUser.getUid() : "NULL"));
                Log.d("Feedback", "Firebase Auth - User Email: " + (currentUser != null ? currentUser.getEmail() : "NULL"));
                
                // Test 2: Check Firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Log.d("Feedback", "Firestore Instance: " + (db != null ? "OK" : "NULL"));
                
                // Test 3: Simple write test with minimal data
                Map<String, Object> testData = new HashMap<>();
                testData.put("test", "simple_test");
                testData.put("timestamp", System.currentTimeMillis());
                testData.put("message", "Hello Firebase!");
                
                Log.d("Feedback", "Attempting to write test data...");
                
                db.collection("debug_test").add(testData)
                    .addOnSuccessListener(documentReference -> {
                        Log.d("Feedback", "✅ FIREBASE WRITE TEST: SUCCESS - Document ID: " + documentReference.getId());
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Firebase: SUCCESS! Check debug_test collection", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Feedback", "❌ FIREBASE WRITE TEST: FAILED - " + e.getMessage());
                        Log.e("Feedback", "Error details: " + e.toString());
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Firebase FAILED: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                    
            } catch (Exception e) {
                Log.e("Feedback", "❌ EXCEPTION in Firebase test: " + e.getMessage());
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Exception: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }
} 