package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

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
                
                if (TextUtils.isEmpty(name)) {
                    nameInput.setError("Please enter your name");
                    return;
                }
                
                if (TextUtils.isEmpty(email)) {
                    emailInput.setError("Please enter your email");
                    return;
                }
                
                if (TextUtils.isEmpty(issue)) {
                    issueInput.setError("Please describe your issue");
                    return;
                }
                
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
            
            Map<String, Object> supportRequest = new HashMap<>();
            supportRequest.put("name", name);
            supportRequest.put("email", email);
            supportRequest.put("issue", issue);
            supportRequest.put("timestamp", System.currentTimeMillis());
            supportRequest.put("status", "pending");
            
            db.collection("support_requests")
                .add(supportRequest)
                .addOnSuccessListener(documentReference -> {
                    // Success, already handled in the click listener
                })
                .addOnFailureListener(e -> {
                    // Error handling
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        }
        
        private void clearForm(View view) {
            ((EditText) view.findViewById(R.id.nameInput)).setText("");
            ((EditText) view.findViewById(R.id.emailInput)).setText("");
            ((EditText) view.findViewById(R.id.issueInput)).setText("");
        }
        
        private void openFaqWebpage() {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://example.com/pedalgo/faq"));
            startActivity(intent);
        }
        
        private void emailSupportTeam() {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@pedalgo.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "PedalGo App Support Request");
            startActivity(Intent.createChooser(intent, "Send Email"));
        }
        
        private void openHelpCenterWebpage() {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://example.com/pedalgo/help"));
            startActivity(intent);
        }
    }

    // Feedback Fragment class
    public static class FeedbackFragment extends Fragment {
        
        private FirebaseFirestore db;
        private float userRating = 0;
        private String feedbackType = "suggestion";
        private boolean isAnonymous = false;
        
        public FeedbackFragment() {
            super(R.layout.fragment_feedback);
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            
            db = FirebaseFirestore.getInstance();
            
            // Setup rating bar
            RatingBar ratingBar = view.findViewById(R.id.ratingBar);
            ratingBar.setOnRatingBarChangeListener((rBar, rating, fromUser) -> {
                userRating = rating;
                updateRatingText(view, rating);
            });
            
            // Setup feedback type radio buttons
            view.findViewById(R.id.radioSuggestion).setOnClickListener(v -> feedbackType = "suggestion");
            view.findViewById(R.id.radioBug).setOnClickListener(v -> feedbackType = "bug");
            view.findViewById(R.id.radioCompliment).setOnClickListener(v -> feedbackType = "compliment");
            
            // Setup anonymous switch
            view.findViewById(R.id.anonymousSwitch).setOnClickListener(v -> 
                isAnonymous = ((com.google.android.material.switchmaterial.SwitchMaterial) v).isChecked()
            );
            
            // Initialize submit button
            view.findViewById(R.id.submitFeedbackButton).setOnClickListener(v -> {
                EditText feedbackInput = view.findViewById(R.id.feedbackInput);
                String feedback = feedbackInput.getText().toString().trim();
                
                if (userRating == 0) {
                    Toast.makeText(getContext(), "Please rate your experience", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (TextUtils.isEmpty(feedback)) {
                    feedbackInput.setError("Please enter your feedback");
                    return;
                }
                
                // Save feedback to Firebase
                saveFeedback(feedback);
                
                // Clear form
                feedbackInput.setText("");
                ratingBar.setRating(0);
                userRating = 0;
                updateRatingText(view, 0);
                
                // Show success message
                Toast.makeText(getContext(), "Thank you for your feedback!", Toast.LENGTH_LONG).show();
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
            String userId = isAnonymous ? "anonymous" : 
                (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous");
            
            Map<String, Object> feedbackData = new HashMap<>();
            feedbackData.put("userId", userId);
            feedbackData.put("rating", userRating);
            feedbackData.put("feedback", feedback);
            feedbackData.put("type", feedbackType);
            feedbackData.put("isAnonymous", isAnonymous);
            feedbackData.put("timestamp", System.currentTimeMillis());
            
            db.collection("feedback")
                .add(feedbackData)
                .addOnSuccessListener(documentReference -> {
                    // Success, already handled in the click listener
                })
                .addOnFailureListener(e -> {
                    // Error handling
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        }
    }
} 