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
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.io.IOException;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private EditText vEditName;
    private EditText vEditPassword;
    private EditText vEditConfirmPass;
    private EditText vEditEmail;
    private Button vButtonRegister;
    private TextView vButtonHaveAccount;
    private Button vButtonPhoto;
    private Uri vSelectData;
    private ImageView vImgPhoto;
    private ProgressBar loadingBar;

    private FirebaseUser currentUser;
    private boolean isRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        vEditName = findViewById(R.id.edtRname);
        vEditEmail = findViewById(R.id.edtRemail);
        vEditPassword = findViewById(R.id.edtRpassword);
        vEditConfirmPass = findViewById(R.id.edtRconfirmPass);
        vButtonRegister = findViewById(R.id.btLogin);
        vButtonHaveAccount = findViewById(R.id.btHaveAccount);
        vButtonPhoto = findViewById(R.id.btPhoto);
        vImgPhoto = findViewById(R.id.imgPhoto);
        loadingBar = findViewById(R.id.progressBar);

        vButtonHaveAccount.setOnClickListener(view -> goToLoginActivity());
        vButtonPhoto.setOnClickListener(view -> selectPhoto());
        vButtonRegister.setOnClickListener(view -> createUser());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRegistered) goToHomeActivity();
    }

    private void goToLoginActivity() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent);
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

    // Cria o usuário no Firebase usando o email e a senha
    public void createUser() {
        boolean hasInvalidField = false;

        String name = vEditName.getText().toString();
        String email = vEditEmail.getText().toString();
        String password = vEditPassword.getText().toString();
        String confPassword = vEditConfirmPass.getText().toString();

        if (name.isEmpty()) {
            vEditName.setError(getString(R.string.error_username));
            hasInvalidField = true;
        }
        if (email.isEmpty()) {
            vEditEmail.setError(getString(R.string.error_email));
            hasInvalidField = true;
        }
        if (password.isEmpty()) {
            vEditPassword.setError(getString(R.string.error_password));
            hasInvalidField = true;
        }
        if (confPassword.isEmpty()) {
            vEditConfirmPass.setError(getString(R.string.error_password));
            hasInvalidField = true;
        }
        else if (!password.equals(confPassword)) {
            vEditConfirmPass.setError(getString(R.string.error_confirm_password));
            hasInvalidField = true;
        }
        if (vImgPhoto.getDrawable() == null) {
            Toast.makeText(this, getString(R.string.error_photo), Toast.LENGTH_SHORT).show();
            hasInvalidField = true;
        }

        if (hasInvalidField) return;

        loadingBar.setVisibility(View.VISIBLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    currentUser = authResult.getUser();
                    updateUser(name);
                })
                .addOnFailureListener(e -> {
                    loadingBar.setVisibility(View.GONE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    Toast.makeText(this, getString(R.string.log_msg) + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Salva a foto do usuário no Storage e atualiza os dados no Firebase
    private void updateUser(String displayName) {
        FirebaseStorage.getInstance().getReference(getString(R.string.storage_path_photos) + currentUser.getUid())
                .putFile(vSelectData)
                .addOnSuccessListener(taskSnapshot -> taskSnapshot.getStorage().getDownloadUrl()
                        .addOnSuccessListener(this, photoUri -> {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName)
                                    .setPhotoUri(photoUri)
                                    .build();

                            currentUser.updateProfile(profileUpdates)
                                    .addOnSuccessListener(this, unused -> saveUserInFirestore())
                                    .addOnFailureListener(this::onRegisterFailure);
                        })
                        .addOnFailureListener(this::onRegisterFailure)
                .addOnFailureListener(this::onRegisterFailure));
    }

    // Salva o usuário no Firestore
    private void saveUserInFirestore() {
        User user = new User(currentUser.getUid(), currentUser.getDisplayName(), Objects.requireNonNull(currentUser.getPhotoUrl()));

        FirebaseFirestore.getInstance().collection(getString(R.string.collection_users))
                .document(currentUser.getUid())
                .set(user)
                .addOnSuccessListener(unused -> {
                    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                        goToHomeActivity();
                    }
                    else {
                        isRegistered = true;
                    }
                })
                .addOnFailureListener(this::onRegisterFailure);
    }

    // Deleta o usuário do Firebase em caso de erro
    private void onRegisterFailure(Exception e) {
        Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e);
        currentUser.delete()
                .addOnCompleteListener(task -> {
                   if (!task.isSuccessful()) {
                       Log.e(getString(R.string.log_tag), getString(R.string.log_msg), task.getException());
                   }
                   loadingBar.setVisibility(View.GONE);
                   getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                   Toast.makeText(this, getString(R.string.success_register), Toast.LENGTH_SHORT).show();
                });
    }

    private void goToHomeActivity() {
        loadingBar.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        Toast.makeText(this, getString(R.string.success_register), Toast.LENGTH_SHORT).show();
        Intent homeIntent = new Intent(this, HomeActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }
}