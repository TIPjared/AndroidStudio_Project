package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class AboutSikadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_sikad);

        // Back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Contact Support Button
        Button contactSupportButton = findViewById(R.id.contactSupportButton);
        contactSupportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent supportIntent = new Intent(AboutSikadActivity.this, SupportFeedbackActivity.class);
                startActivity(supportIntent);
            }
        });

        // Visit Website Button
        Button visitWebsiteButton = findViewById(R.id.visitWebsiteButton);
        visitWebsiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sikad.app"));
                startActivity(browserIntent);
            }
        });

        // Hardware Info Card Click
        CardView hardwareInfoCard = findViewById(R.id.hardwareInfoCard);
        hardwareInfoCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AboutSikadActivity.this, "Hardware details are shown here in the card!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
