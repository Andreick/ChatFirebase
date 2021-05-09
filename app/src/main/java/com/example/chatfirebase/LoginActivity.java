package com.example.chatfirebase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText vEditEmail;
    private EditText vEditPassword;
    private Button vButtonLogin;
    private TextView vButtonRegister;
    private TextView vButtonLostPass;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        vEditEmail = findViewById(R.id.edtRemail);
        vEditPassword = findViewById(R.id.edtPassword);
        vButtonLogin = findViewById(R.id.btLogin);
        vButtonLostPass = findViewById(R.id.btLostPassword);
        vButtonRegister = findViewById(R.id.btRegister);
        progressBar = findViewById(R.id.progressBar);



        vButtonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),RegisterActivity.class));
            }
        });


        vButtonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = vEditEmail.getText().toString();
                String password = vEditPassword.getText().toString();

                if (email == null || email.isEmpty()){
                    vEditEmail.setError("Insira seu Email");
                    return;
                }
                if (password == null || password.isEmpty()){
                    vEditPassword.setError("Insira sua Senha");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);

                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Logado com sucesso", Toast.LENGTH_SHORT).show();
                            vEditEmail.getText().clear();
                            vEditPassword.getText().clear();
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);

                        } else {
                            Toast.makeText(LoginActivity.this, "Erro ! "+task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });

            }
        });


    }
}