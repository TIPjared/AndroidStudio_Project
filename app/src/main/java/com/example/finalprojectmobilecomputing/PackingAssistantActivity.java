package com.example.finalprojectmobilecomputing;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

public class PackingAssistantActivity extends AppCompatActivity {

    private EditText userdestination;
    private Button submitButton;

    private TextView aiResponse;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_packing_assistant);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.PackingAssistant), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

        });
        userdestination = findViewById(R.id.user_input);
        submitButton = findViewById(R.id.submit_button);
        aiResponse = findViewById(R.id.ai_response);

        submitButton.setOnClickListener(v -> {
            String userDescription = userdestination.getText().toString();
            Airecommender(userDescription);
        });
    }
    public void Airecommender(String Input){
    String Prompt = Input;

    String API_KEY = "AIzaSyB3haers8c6B7dIPbk3IiBTz6uHDJMUM5k";

    GenerativeModel gmodel = new GenerativeModel("gemini-2.0-flash-lite", API_KEY);
    GenerativeModelFutures modelFutures = GenerativeModelFutures.from(gmodel);

        Content content = new Content.Builder()
                .addText(Prompt)
                .build();


        ListenableFuture<GenerateContentResponse> response = modelFutures.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {

            @Override
            public void onSuccess(GenerateContentResponse result) {
                aiResponse.setText(result.getText().trim());
            }

            @Override
            public void onFailure(Throwable t) {
                Toast.makeText(PackingAssistantActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        }, MoreExecutors.directExecutor());
    }

}