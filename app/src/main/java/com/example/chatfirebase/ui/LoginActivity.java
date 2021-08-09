package com.example.chatfirebase.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import com.example.chatfirebase.ChatFirebaseApplication;
import com.example.chatfirebase.R;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText vEditEmail;
    private EditText vEditPassword;
    private Button vButtonLogin;
    private TextView vButtonRegister;
    private TextView vButtonLostPass;
    private ProgressBar loadingBar;

    private boolean isLoggedIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        vEditEmail = findViewById(R.id.et_login_email);
        vEditPassword = findViewById(R.id.et_login_password);
        vButtonLogin = findViewById(R.id.btn_login);
        vButtonLostPass = findViewById(R.id.tv_login_lost_password);
        vButtonRegister = findViewById(R.id.tv_login_register);
        loadingBar = findViewById(R.id.pb_login);

        vButtonRegister.setOnClickListener(view -> goToRegisterActivity());
        vButtonLogin.setOnClickListener(view -> login());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isLoggedIn) goToHomeActivity();
    }

    private void goToRegisterActivity() {
        Intent registerIntent = new Intent(this, RegisterActivity.class);
        startActivity(registerIntent);
    }

    // Faz o login do usuário no Firebase e vai para a HomeActivity
    private void login() {
        boolean hasInvalidField = false;
        
        String email = vEditEmail.getText().toString();
        String password = vEditPassword.getText().toString();

        if (email.isEmpty()) {
            vEditEmail.setError(getString(R.string.error_email));
            hasInvalidField = true;
        }
        if (password.isEmpty()) {
            vEditPassword.setError(getString(R.string.error_password));
            hasInvalidField = true;
        }

        if (hasInvalidField) return;

        loadingBar.setVisibility(View.VISIBLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                        goToHomeActivity();
                    }
                    else {
                        isLoggedIn = true;
                    }
                })
                .addOnFailureListener(e -> {
                    loadingBar.setVisibility(View.GONE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void goToHomeActivity() {
        loadingBar.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        Toast.makeText(this, getString(R.string.success_login), Toast.LENGTH_SHORT).show();

        Intent homeIntent = new Intent(this, HomeActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);

        ChatFirebaseApplication application = (ChatFirebaseApplication) getApplication();
        application.setup(FirebaseAuth.getInstance().getUid());
    }
}