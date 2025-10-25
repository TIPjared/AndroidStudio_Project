package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.os.Handler;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;
    private EditText emailEditText, phoneEditText;
    private TextInputEditText passwordEditText, confirmPasswordEditText;
    private Button signUpButton;
    private TextView goToLogin;
    private View googleSignUpButton;
    private CheckBox termsCheckbox;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        // Initialize views with error handling
        try {
            emailEditText = findViewById(R.id.emailEditText);
            passwordEditText = findViewById(R.id.passwordEditText);
            confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
            phoneEditText = findViewById(R.id.phoneEditText);
            signUpButton = findViewById(R.id.signUpButton);
            goToLogin = findViewById(R.id.goToLogin);
            googleSignUpButton = findViewById(R.id.googleSignUpButton);
            termsCheckbox = findViewById(R.id.termsCheckbox);

            // Google Sign-In setup
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception e) {
            Log.e("SignUpActivity", "Error initializing views or Google Sign-In", e);
            Toast.makeText(this, "Initialization error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // Set click listeners with animations
        signUpButton.setOnClickListener(view -> {
            animateButton(signUpButton);
            new Handler().postDelayed(() -> signUp(), 100);
        });

        goToLogin.setOnClickListener(view -> {
            animateButton(goToLogin);
            new Handler().postDelayed(() -> {
                Intent loginIntent = new Intent(SignUpActivity.this, MainActivity.class);
                startActivity(loginIntent);
                overridePendingTransition(R.anim.activity_slide_in_left, R.anim.activity_slide_out_right);
                finish();
            }, 100);
        });

        // Google sign-up button
        googleSignUpButton.setOnClickListener(view -> {
            animateButton(googleSignUpButton);
            new Handler().postDelayed(() -> signUpWithGoogle(), 100);
        });

        // Start animations
        startSignUpAnimations();
    }

    private void startSignUpAnimations() {
        try {
            // Animate title texts
            TextView titleText = findViewById(R.id.textView8);
            TextView subtitleText = findViewById(R.id.textView9);

            if (titleText == null || subtitleText == null) {
                Log.w("SignUpActivity", "Animation views not found, skipping animations");
                return;
            }

            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            Animation slideInLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            Animation bounceIn = AnimationUtils.loadAnimation(this, R.anim.bounce_in);

            // Stagger animations
            titleText.startAnimation(fadeIn);

            new Handler().postDelayed(() -> {
                if (subtitleText != null) subtitleText.startAnimation(slideInLeft);
            }, 200);

            new Handler().postDelayed(() -> {
                View mainCard = findViewById(R.id.mainCardContainer);
                if (mainCard != null) mainCard.startAnimation(slideUp);
            }, 400);

            new Handler().postDelayed(() -> {
                if (signUpButton != null) signUpButton.startAnimation(bounceIn);
            }, 600);

            new Handler().postDelayed(() -> {
                View googleContainer = findViewById(R.id.googleSignInContainer);
                if (googleContainer != null) googleContainer.startAnimation(bounceIn);
            }, 700);
        } catch (Exception e) {
            Log.e("SignUpActivity", "Error in animations", e);
            // Don't crash, just skip animations
        }
    }

    private void animateButton(View button) {
        try {
            if (button != null) {
                Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
                button.startAnimation(buttonPress);
            }
        } catch (Exception e) {
            Log.e("SignUpActivity", "Error animating button", e);
            // Don't crash, just skip animation
        }
    }

    private void signUp() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String phoneInput = phoneEditText.getText().toString().trim();
        
        // Check if terms and conditions are agreed to
        if (!termsCheckbox.isChecked()) {
            Toast.makeText(SignUpActivity.this, "Please agree to the Terms and Conditions to continue", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Validate phone number format
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phoneInput.isEmpty()) {
            Toast.makeText(SignUpActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate Philippine phone format (must start with 09 and be 11 digits)
        if (!phoneInput.startsWith("09") || phoneInput.length() != 11) {
            Toast.makeText(SignUpActivity.this, "Phone number must start with 09 and be 11 digits (e.g., 09171234567)", Toast.LENGTH_LONG).show();
            phoneEditText.setError("Invalid format");
            phoneEditText.requestFocus();
            return;
        }
        
        // Convert to international format for Firebase
        final String phone = "+63" + phoneInput.substring(1);

        if (!password.equals(confirmPassword)) {
            Toast.makeText(SignUpActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
        } else if (!isStrongPassword(password)) {
            Toast.makeText(SignUpActivity.this, "Password must be at least 8 characters and include upper, lower, digit, and special character.", Toast.LENGTH_LONG).show();
        } else {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .whereEqualTo("phone", phone)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Toast.makeText(SignUpActivity.this, "Phone number already registered", Toast.LENGTH_SHORT).show();
                        } else {
                            // Proceed with registration
                            mAuth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(this, task -> {
                                        if (task.isSuccessful()) {
                                            String userId = mAuth.getCurrentUser().getUid();

                                            String deviceId = Settings.Secure.getString(
                                                    getContentResolver(), Settings.Secure.ANDROID_ID);

                                            Map<String, Object> newUser = new HashMap<>();
                                            newUser.put("email", email);
                                            newUser.put("phone", phone);
                                            newUser.put("deviceId", null);
                                            newUser.put("phoneVerified", false);
                                            newUser.put("name", "");
                                            newUser.put("birthdate", "");
                                            newUser.put("gender", "");
                                            newUser.put("profileImageUrl", "");
                                            newUser.put("status", "ACTIVE");
                                            newUser.put("createdAt", System.currentTimeMillis());

                                            db.collection("users").document(userId)
                                                    .set(newUser)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(SignUpActivity.this, "Account created! Please verify your phone number.", Toast.LENGTH_SHORT).show();
                                                        
                                                        // Navigate to OTP Verification for new sign-up
                                                        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                                                        intent.putExtra("phone", phone); // optional, if you want to prefill phone
                                                        startActivity(intent);
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(SignUpActivity.this, "Failed to save user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });

                                        } else {
                                            Toast.makeText(SignUpActivity.this, "Sign up failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SignUpActivity.this, "Error checking phone: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        }
    }

    private boolean isStrongPassword(String password) {
        // At least 8 characters, 1 digit, 1 lower, 1 upper, 1 special char
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
        return password.matches(passwordPattern);
    }
    private void signUpWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                String errorMessage = "Google Sign-Up failed: " + e.getStatusCode() + " - " + e.getMessage();
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                Log.e("GoogleSignIn", "Sign-in failed with code: " + e.getStatusCode(), e);
        }
    }

    public void showTermsAndConditions(View view) {
        // Create a custom dialog for Terms and Conditions
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Terms and Conditions");
        
        // Create a scrollable text view for the terms content
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText(getTermsOfServiceContent());
        textView.setPadding(50, 50, 50, 50);
        textView.setTextSize(14);
        textView.setTextColor(android.graphics.Color.BLACK);
        scrollView.addView(textView);
        
        builder.setView(scrollView);
        builder.setPositiveButton("I Agree", (dialog, which) -> {
            termsCheckbox.setChecked(true);
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            termsCheckbox.setChecked(false);
            dialog.dismiss();
        });
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String getTermsOfServiceContent() {
        return "TERMS OF SERVICE\n\n" +
                "Last updated: " + new java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(new java.util.Date()) + "\n\n" +
                
                "1. AGREEMENT TO TERMS\n\n" +
                "By using the Sikad bike rental system, you agree to be bound by these Terms of Service. If you do not agree to these terms, please do not use our service.\n\n" +
                
                "2. DESCRIPTION OF SERVICE\n\n" +
                "Sikad is a smart bike rental system that provides:\n" +
                "• IoT-enabled bicycles for rent in residential communities\n" +
                "• Real-time GPS tracking and geofencing\n" +
                "• Mobile application for bike discovery and rental\n" +
                "• AI-powered theft detection and safety features\n" +
                "• Community-based bike sharing infrastructure\n\n" +
                
                "3. ELIGIBILITY\n\n" +
                "3.1 Age Requirements:\n" +
                "• Users must be at least 13 years old\n" +
                "• Users under 18 must have parental consent\n" +
                "• Valid identification may be required\n\n" +
                
                "3.2 Account Requirements:\n" +
                "• Valid email address and phone number\n" +
                "• Payment method (GCash, credit/debit card)\n" +
                "• Emergency contact information\n" +
                "• Agreement to community rules and regulations\n\n" +
                
                "4. BIKE RENTAL TERMS\n\n" +
                "4.1 Rental Process:\n" +
                "• Scan QR code to initiate rental\n" +
                "• Complete payment through PayMongo\n" +
                "• Two-step verification for bike unlocking\n" +
                "• Rental starts upon successful payment\n\n" +
                
                "4.2 Usage Rules:\n" +
                "• Stay within designated community boundaries\n" +
                "• Follow traffic laws and safety regulations\n" +
                "• Do not tamper with IoT devices or bike components\n" +
                "• Return bike to designated areas after use\n" +
                "• Report any issues or damages immediately\n\n" +
                
                "4.3 Prohibited Activities:\n" +
                "• Riding outside community boundaries\n" +
                "• Attempting to disable or damage IoT sensors\n" +
                "• Using bikes for illegal activities\n" +
                "• Sharing account credentials with others\n" +
                "• Unauthorized bike modifications\n\n" +
                
                "5. PAYMENT AND FEES\n\n" +
                "5.1 Rental Fees:\n" +
                "• Pay-per-ride pricing model\n" +
                "• Fees charged through PayMongo payment gateway\n" +
                "• All fees are non-refundable unless service failure\n\n" +
                
                "5.2 Additional Charges:\n" +
                "• Late return fees if bike not returned on time\n" +
                "• Damage fees for bike or IoT equipment damage\n" +
                "• Boundary violation penalties\n" +
                "• Emergency response fees for false alarms\n\n" +
                
                "6. SAFETY AND SECURITY\n\n" +
                "6.1 User Safety:\n" +
                "• Helmets recommended for all riders\n" +
                "• Follow community traffic rules\n" +
                "• Be aware of weather conditions\n" +
                "• Emergency contacts will be notified in case of incidents\n\n" +
                
                "6.2 Theft Prevention:\n" +
                "• AI-powered motion detection monitors for theft\n" +
                "• Geofencing alerts when bikes leave boundaries\n" +
                "• Automatic braking system for security violations\n" +
                "• SMS alerts to administrators for suspicious activity\n\n" +
                
                "7. LIABILITY AND INSURANCE\n\n" +
                "7.1 User Responsibility:\n" +
                "• Users ride at their own risk\n" +
                "• Personal insurance recommended\n" +
                "• Responsible for bike damage due to misuse\n" +
                "• Must report accidents or incidents immediately\n\n" +
                
                "7.2 System Limitations:\n" +
                "• GPS accuracy may vary in certain areas\n" +
                "• IoT sensors may malfunction due to weather\n" +
                "• Network connectivity required for full functionality\n" +
                "• Emergency response times may vary\n\n" +
                
                "8. DATA COLLECTION AND PRIVACY\n\n" +
                "8.1 Data Collection:\n" +
                "• Location data for GPS tracking and geofencing\n" +
                "• Motion sensor data for theft detection\n" +
                "• Usage patterns for system optimization\n" +
                "• Payment information through secure gateways\n\n" +
                
                "8.2 Privacy Rights:\n" +
                "• Control over data sharing preferences\n" +
                "• Right to access and delete personal data\n" +
                "• Anonymous usage mode available\n" +
                "• Detailed privacy policy available in app\n\n" +
                
                "9. SYSTEM AVAILABILITY\n\n" +
                "9.1 Service Availability:\n" +
                "• System available 24/7 with maintenance windows\n" +
                "• Weather-related service interruptions possible\n" +
                "• Network outages may affect functionality\n" +
                "• Emergency maintenance may be required\n\n" +
                
                "9.2 Technical Support:\n" +
                "• Support available through app and email\n" +
                "• Response times: 24-48 hours for non-emergencies\n" +
                "• Emergency support for safety-related issues\n" +
                "• Community administrator assistance available\n\n" +
                
                "10. COMMUNITY RULES\n\n" +
                "10.1 Respectful Use:\n" +
                "• Respect other community members\n" +
                "• Follow community-specific guidelines\n" +
                "• Report misuse or violations\n" +
                "• Maintain bike cleanliness and condition\n\n" +
                
                "10.2 Environmental Responsibility:\n" +
                "• Support eco-friendly transportation\n" +
                "• Reduce carbon footprint in community\n" +
                "• Promote sustainable mobility practices\n" +
                "• Participate in community improvement initiatives\n\n" +
                
                "11. ACCOUNT SUSPENSION AND TERMINATION\n\n" +
                "11.1 Grounds for Suspension:\n" +
                "• Violation of community boundaries\n" +
                "• Repeated damage to bikes or equipment\n" +
                "• Fraudulent payment activities\n" +
                "• Harassment of other users\n" +
                "• Tampering with IoT devices\n\n" +
                
                "11.2 Account Termination:\n" +
                "• Severe or repeated violations\n" +
                "• Illegal activities using the service\n" +
                "• Failure to pay fees or damages\n" +
                "• Violation of community safety rules\n\n" +
                
                "12. INTELLECTUAL PROPERTY\n\n" +
                "12.1 Sikad Technology:\n" +
                "• All IoT hardware and software is proprietary\n" +
                "• AI algorithms and theft detection systems\n" +
                "• Mobile application and backend systems\n" +
                "• Community management and analytics tools\n\n" +
                
                "12.2 User Content:\n" +
                "• Users retain rights to personal data\n" +
                "• Feedback and suggestions may be used for improvement\n" +
                "• No compensation for user-contributed ideas\n" +
                "• Respect for intellectual property of others\n\n" +
                
                "13. DISPUTE RESOLUTION\n\n" +
                "13.1 Dispute Process:\n" +
                "• Contact customer support first\n" +
                "• Community administrator mediation\n" +
                "• Formal complaint process available\n" +
                "• Legal action as last resort\n\n" +
                
                "13.2 Governing Law:\n" +
                "• Philippine law governs these terms\n" +
                "• Manila courts have jurisdiction\n" +
                "• Community-specific regulations apply\n" +
                "• International users subject to local laws\n\n" +
                
                "14. CHANGES TO TERMS\n\n" +
                "We may update these Terms of Service periodically. Users will be notified of significant changes through the app. Continued use after changes constitutes acceptance of updated terms.\n\n" +
                
                "15. CONTACT INFORMATION\n\n" +
                "For questions about these Terms of Service:\n" +
                "• Email: support@sikad.com\n" +
                "• Phone: +63-XXX-XXX-XXXX\n" +
                "• Address: Technological Institute of the Philippines, Manila\n" +
                "• Community Administrator: Available through app\n\n" +
                
                "These Terms of Service are designed specifically for the Sikad community bike rental system and reflect our commitment to providing safe, efficient, and sustainable transportation services.";
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Save basic user info to Firestore
                        String userId = mAuth.getCurrentUser().getUid();
                        String email = mAuth.getCurrentUser().getEmail();

                        // Create a user entry in Firestore (without phone initially)
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        Map<String, Object> newUser = new HashMap<>();
                        newUser.put("email", email);
                        newUser.put("phone", "");
                        newUser.put("deviceId", null);
                        newUser.put("phoneVerified", false);
                        newUser.put("name", "");
                        newUser.put("birthdate", "");
                        newUser.put("gender", "");
                        newUser.put("profileImageUrl", "");
                        newUser.put("status", "ACTIVE");
                        newUser.put("createdAt", System.currentTimeMillis());
                        
                        db.collection("users").document(userId).set(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    // Navigate to OTP verification to collect phone number
                                    Intent intent = new Intent(SignUpActivity.this, OTPVerification.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SignUpActivity.this, "Failed to create user record: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(SignUpActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}