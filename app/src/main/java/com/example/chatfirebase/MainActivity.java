package com.example.chatfirebase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private EditText vEditEmail;
    private EditText vEditPassword;
    private Button vButtonLogin;
    private TextView vButtonRegister;
    private TextView vButtonLostPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vEditEmail = findViewById(R.id.edtRemail);
        vEditPassword = findViewById(R.id.edtPassword);
        vButtonLogin = findViewById(R.id.btLogin);
        vButtonLostPass = findViewById(R.id.btLostPassword);
        vButtonRegister = findViewById(R.id.btRegister);
        
        vButtonRegister.setOnClickListener(view -> goToRegisterActivity());
        vButtonLogin.setOnClickListener(view -> login());
    }

    private void goToRegisterActivity() {
        Intent registerIntent = new Intent(this, RegisterActivity.class);
        startActivity(registerIntent);
    }

    // Faz o login do usuário no Firebase e vai para a MessagesActivity
    private void login() {
        String email = vEditEmail.getText().toString();
        String password = vEditPassword.getText().toString();

        if (email.isEmpty()) {
            vEditEmail.setError(getString(R.string.error_email));
            return;
        }
        if (password.isEmpty()) {
            vEditPassword.setError(getString(R.string.error_password));
            return;
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> goToMessagesActivity())
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.log_msg) + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void goToMessagesActivity() {
        Toast.makeText(this, getString(R.string.success_login), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MessagesActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}