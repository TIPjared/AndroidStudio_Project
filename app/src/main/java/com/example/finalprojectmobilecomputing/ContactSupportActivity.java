package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ContactSupportActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_support);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Email contact button
        TextView emailContact = findViewById(R.id.emailContact);
        emailContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@sikad.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Sikad App Support Request");
            intent.putExtra(Intent.EXTRA_TEXT, "Hello Sikad Support Team,\n\nI need assistance with:\n\n");
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "Send Email"));
            } else {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        });

        // Phone contact button
        TextView phoneContact = findViewById(R.id.phoneContact);
        phoneContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:+63-2-1234-5678"));
            startActivity(intent);
        });

        // Business hours info
        TextView businessHours = findViewById(R.id.businessHours);
        businessHours.setText("Monday - Friday: 8:00 AM - 6:00 PM\nSaturday: 9:00 AM - 5:00 PM\nSunday: Closed");
    }
}
