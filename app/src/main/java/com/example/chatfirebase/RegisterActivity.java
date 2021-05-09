package com.example.chatfirebase;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private EditText vEditName;
    private EditText vEditPassword;
    private EditText vEditConfirmPass;
    private EditText vEditEmail;
    private Button vButtonRegister;
    private TextView vButtonHaveAccout;
    private Button vButtonPhoto;
    private Uri vSelectData;
    private ImageView vImgPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        vEditName = findViewById(R.id.edtRname);
        vEditEmail = findViewById(R.id.edtRemail);
        vEditPassword = findViewById(R.id.edtRpassword);
        vEditConfirmPass = findViewById(R.id.edtRconfirmPass);
        vButtonRegister = findViewById(R.id.btLogin);
        vButtonHaveAccout = findViewById(R.id.btHaveAccount);
        vButtonPhoto = findViewById(R.id.btPhoto);
        vImgPhoto = findViewById(R.id.imgPhoto);

        vButtonHaveAccout.setOnClickListener(view -> goToLoginActivity());
        vButtonPhoto.setOnClickListener(view -> selectPhoto());
        vButtonRegister.setOnClickListener(view -> createUser());
    }

    private void goToLoginActivity() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent);
    }

    // Cria o usuário no Firebase usando o email e a senha
    public void createUser() {
        String nome = vEditName.getText().toString();

        if (nome.isEmpty()) {
            vEditName.setError(getString(R.string.error_username));
            return;
        }

        String email = vEditEmail.getText().toString();

        if (email.isEmpty()) {
            vEditEmail.setError(getString(R.string.error_email));
            return;
        }

        String senha = vEditPassword.getText().toString();

        if (senha.isEmpty()) {
            vEditPassword.setError(getString(R.string.error_password));
            return;
        }

        String confSenha = vEditConfirmPass.getText().toString();

        if (!senha.equals(confSenha)) {
            vEditConfirmPass.setError(getString(R.string.error_confirm_password));
            return;
        }

        if (vImgPhoto.getDrawable() == null) {
            Toast.makeText(this, getString(R.string.error_photo), Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, senha)
                .addOnSuccessListener(authResult -> saveUserFirebase(nome))
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.log_msg) + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null) {
            if (requestCode == 0) { // Obtém o bitmap da foto escolhida e exibe a foto na ImageView
                vSelectData = data.getData();

                Bitmap bitmap;
                try {
                    if (Build.VERSION.SDK_INT < 28) {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), vSelectData);
                    }
                    else {
                        ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), vSelectData);
                        bitmap = ImageDecoder.decodeBitmap(source);
                    }
                    vImgPhoto.setImageDrawable(new BitmapDrawable(this.getResources(), bitmap));
                    vButtonPhoto.setAlpha(0);
                }
                catch (IOException e) {
                    Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e);
                }
            }
        }
    }

    // Solicita a escolha de uma imagem
    private void selectPhoto() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setType("image/*");
        startActivityForResult(pickIntent, 0);
    }

    // Salva a foto do usuário no Storage e os outros dados no Firestore
    private void saveUserFirebase(String username) {
        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getUid());
        final StorageReference reference = FirebaseStorage.getInstance().getReference(getString(R.string.storage_path_photos) + uid);

        reference.putFile(vSelectData)
                .addOnSuccessListener(taskSnapshot -> reference.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            Toast.makeText(this, "Registrando...", Toast.LENGTH_SHORT).show();
                            String profileUrl = uri.toString();

                            User user = new User(uid, username, profileUrl);

                            FirebaseFirestore.getInstance().collection(getString(R.string.collection_users))
                                    .document(uid)
                                    .set(user)
                                    .addOnSuccessListener(aVoid -> goToMessagesActivity())
                                    .addOnFailureListener(e -> Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e));
                        })
                        .addOnFailureListener(e -> Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e))
                .addOnFailureListener(e -> Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e)));
    }

    private void goToMessagesActivity() {
        Toast.makeText(this, getString(R.string.success_register), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MessagesActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}