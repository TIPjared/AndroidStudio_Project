package com.example.finalprojectmobilecomputing;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;

public class AccountStatusDialog extends Dialog {
    private String accountStatus;
    private Context context;

    public AccountStatusDialog(@NonNull Context context, String accountStatus) {
        super(context);
        this.context = context;
        this.accountStatus = accountStatus;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_account_status);

        TextView statusTitle = findViewById(R.id.statusTitle);
        TextView statusMessage = findViewById(R.id.statusMessage);
        Button appealButton = findViewById(R.id.appealButton);
        Button logoutButton = findViewById(R.id.logoutButton);

        // Set status-specific content
        if ("SUSPENDED".equals(accountStatus)) {
            statusTitle.setText("Account Suspended");
            statusMessage.setText("Your account has been suspended. If you believe this is an error, you can appeal this decision by contacting our support team.");
            appealButton.setVisibility(View.VISIBLE);
        } else if ("BANNED".equals(accountStatus)) {
            statusTitle.setText("Account Banned");
            statusMessage.setText("Your account has been permanently banned. If you believe this is an error, you can appeal this decision by contacting our support team.");
            appealButton.setVisibility(View.VISIBLE);
        } else {
            statusTitle.setText("Account Status Issue");
            statusMessage.setText("There seems to be an issue with your account status. Please contact support for assistance.");
            appealButton.setVisibility(View.GONE);
        }

        appealButton.setOnClickListener(v -> {
            // Open support/contact activity
            Intent intent = new Intent(context, ContactSupportActivity.class);
            intent.putExtra("appeal", true);
            intent.putExtra("accountStatus", accountStatus);
            context.startActivity(intent);
            dismiss();
        });

        logoutButton.setOnClickListener(v -> {
            // Sign out user and return to login
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
            dismiss();
        });

        // Prevent dismissing dialog by clicking outside
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }
}
