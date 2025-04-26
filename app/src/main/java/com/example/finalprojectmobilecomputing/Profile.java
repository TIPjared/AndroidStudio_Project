package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class Profile extends AppCompatActivity {

    private TextView emailTextView;
    private EditText usernameEditText, ageEditText, genderEditText;
    private Button saveButton, returnButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        emailTextView = findViewById(R.id.emailEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        ageEditText = findViewById(R.id.ageEditText);
        genderEditText = findViewById(R.id.genderEditText);
        saveButton = findViewById(R.id.saveButton);
        returnButton = findViewById(R.id.returnButton);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userRef = db.collection("users").document(mAuth.getCurrentUser().getUid());

            loadUserData();
        }

        saveButton.setOnClickListener(v -> saveUserData());

        returnButton.setOnClickListener(v -> {
            Intent intent = new Intent(Profile.this, MainPage.class);
            startActivity(intent);
            finish();
        });

    }

    private void loadUserData() {
        // Set the email address from Firebase Authentication
        String email = mAuth.getCurrentUser().getEmail();
        emailTextView.setText(email);

        // Load other user data from Firestore
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String age = documentSnapshot.getString("age");
                String gender = documentSnapshot.getString("gender");

                usernameEditText.setText(username != null ? username : "Not yet set");
                ageEditText.setText(age != null ? age : "Not yet set");
                genderEditText.setText(gender != null ? gender : "Not yet set");
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(Profile.this, "Failed to load data", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveUserData() {
        String username = usernameEditText.getText().toString().trim();
        String age = ageEditText.getText().toString().trim();
        String gender = genderEditText.getText().toString().trim();

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("age", age);
        userData.put("gender", gender);

        userRef.set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> Toast.makeText(Profile.this, "Profile Updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(Profile.this, "Failed to update profile", Toast.LENGTH_SHORT).show());
    }
}
