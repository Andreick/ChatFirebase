package com.example.chatfirebase.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatfirebase.ChatFirebaseApplication;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authenticateUser();
    }

    private void authenticateUser() {
        String currentUid = FirebaseAuth.getInstance().getUid();

        if (currentUid == null) {
            goToLoginActivity();
        }
        else {
            goToHomeActivity();
        }
    }

    private void goToLoginActivity() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);
    }

    private void goToHomeActivity() {
        Intent homeIntent = new Intent(this, HomeActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);

        ChatFirebaseApplication application = (ChatFirebaseApplication) getApplication();
        application.setup();
    }
}