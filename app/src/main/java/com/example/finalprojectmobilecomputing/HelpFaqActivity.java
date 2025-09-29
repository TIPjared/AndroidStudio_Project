package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class HelpFaqActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_faq);

        // Back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Setup FAQ items click listeners
        setupFaqItemClickListeners();

        // Contact Support Button
        findViewById(R.id.contactSupportButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent supportIntent = new Intent(HelpFaqActivity.this, SupportFeedbackActivity.class);
                startActivity(supportIntent);
            }
        });
    }

    private void setupFaqItemClickListeners() {
        // FAQ items
        int[] faqCards = {
            R.id.faqCard1, R.id.faqCard2, R.id.faqCard3, 
            R.id.faqCard4, R.id.faqCard5, R.id.faqCard6,
            R.id.faqCard7, R.id.faqCard8
        };

        int[] answerLayouts = {
            R.id.faqAnswer1, R.id.faqAnswer2, R.id.faqAnswer3,
            R.id.faqAnswer4, R.id.faqAnswer5, R.id.faqAnswer6,
            R.id.faqAnswer7, R.id.faqAnswer8
        };

        int[] arrows = {
            R.id.faqArrow1, R.id.faqArrow2, R.id.faqArrow3,
            R.id.faqArrow4, R.id.faqArrow5, R.id.faqArrow6,
            R.id.faqArrow7, R.id.faqArrow8
        };

        for (int i = 0; i < faqCards.length; i++) {
            final int index = i;
            CardView card = findViewById(faqCards[i]);
            
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LinearLayout answerLayout = findViewById(answerLayouts[index]);
                    TextView arrow = findViewById(arrows[index]);
                    
                    if (answerLayout.getVisibility() == View.GONE) {
                        answerLayout.setVisibility(View.VISIBLE);
                        arrow.setText("▲");
                    } else {
                        answerLayout.setVisibility(View.GONE);
                        arrow.setText("▼");
                    }
                }
            });
        }
    }
}
