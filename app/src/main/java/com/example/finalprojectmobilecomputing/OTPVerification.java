package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class OTPVerification extends AppCompatActivity {

    private EditText otpEditText;
    private Button verifyOtpButton;

    private String verificationId;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otpverification);

        otpEditText = findViewById(R.id.otpEditText);
        verifyOtpButton = findViewById(R.id.verifyOtpButton);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();

        // 🔍 Step 1: Retrieve phone number from Firestore
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String phoneNumber = documentSnapshot.getString("phone");

                        if (phoneNumber != null && !phoneNumber.isEmpty()) {
                            String phoneNumberWithCode = "+63" + phoneNumber;
                            sendOTP(phoneNumberWithCode); // ✅ Fixed: Call sendOTP properly
                        } else {
                            Toast.makeText(this, "Phone number is missing in Firestore", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch phone: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // ✅ Step 2: Verify OTP manually (if not auto)
        verifyOtpButton.setOnClickListener(v -> {
            String code = otpEditText.getText().toString().trim();

            if (code.isEmpty() || verificationId == null) {
                Toast.makeText(this, "Enter OTP first", Toast.LENGTH_SHORT).show();
                return;
            }

            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            signInWithPhoneAuthCredential(credential);
        });
    }

    // 📤 Send OTP using Firebase PhoneAuth
    private void sendOTP(String phoneNumberWithCode) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumberWithCode) // Example: +639XXXXXXXXX
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        // Optional: Auto verification (can autofill)
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Toast.makeText(OTPVerification.this, "Verification Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(String verificationIdFromFirebase, PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = verificationIdFromFirebase;
                        Toast.makeText(OTPVerification.this, "OTP sent to your phone", Toast.LENGTH_SHORT).show();
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    // ✅ Sign in with OTP credential
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(OTPVerification.this, MainPage.class));
                        finish();
                    } else {
                        Toast.makeText(this, "OTP Verification Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
