package com.example.chatfirebase.ui;

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

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import com.example.chatfirebase.ChatFirebaseApplication;
import com.example.chatfirebase.R;
import com.example.chatfirebase.data.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

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
    private String displayName;
    private Uri photoUrl;
    private StorageReference photoReference;
    private boolean isRegistered;

    ActivityResultLauncher<String> getContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri == null) return;
                    vSelectData = uri;
                    try {
                        Bitmap bitmap;
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), vSelectData);
                        }
                        else {
                            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), vSelectData);
                            bitmap = ImageDecoder.decodeBitmap(source);
                        }
                        vImgPhoto.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                        vButtonPhoto.setAlpha(0);
                    }
                    catch (IOException e) {
                        Log.e(TAG, "Bitmap exception", e);
                        displayMessage(getString(R.string.failure_select_photo));
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        vEditName = findViewById(R.id.et_register_name);
        vEditEmail = findViewById(R.id.et_register_email);
        vEditPassword = findViewById(R.id.et_register_password);
        vEditConfirmPass = findViewById(R.id.et_register_confirm_password);
        vButtonRegister = findViewById(R.id.btn_register);
        vButtonHaveAccount = findViewById(R.id.tv_register_have_account);
        vButtonPhoto = findViewById(R.id.btn_register_photo);
        vImgPhoto = findViewById(R.id.civ_register_photo);
        loadingBar = findViewById(R.id.pb_register);

        vButtonPhoto.setOnClickListener(view -> getContent.launch("image/*"));
        vButtonRegister.setOnClickListener(view -> createUser());
        vButtonHaveAccount.setOnClickListener(view -> goToLoginActivity());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRegistered) goToHomeActivity();
    }

    // Cria o usuário no Firebase usando o email e a senha
    private void createUser() {
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
            displayMessage(getString(R.string.error_photo));
            hasInvalidField = true;
        }

        if (hasInvalidField) return;

        loadingBar.setVisibility(View.VISIBLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    currentUser = authResult.getUser();
                    displayName = name;
                    savePhotoInStorage();
                })
                .addOnFailureListener(e -> {
                    loadingBar.setVisibility(View.GONE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    displayMessage(e.getMessage());
                });
    }

    // Salva a foto do usuário no Storage
    private void savePhotoInStorage() {
        photoReference = FirebaseStorage.getInstance().getReference(getString(R.string.storage_path_photos) + currentUser.getUid());
        photoReference.putFile(vSelectData)
                .addOnSuccessListener(taskSnapshot -> photoReference.getDownloadUrl()
                        .addOnSuccessListener(this, photoUri -> {
                            photoUrl = photoUri;
                            updateUser();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get photo URL", e);
                            deletePhotoInStorage();
                            deleteUser();
                        })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save photo", e);
                    deleteUser();
                }));
    }

    // Atualiza os dados do usuário no Firebase
    private void updateUser() {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .setPhotoUri(photoUrl)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnSuccessListener(this, unused -> saveUserInDatabase())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "User update failed", e);
                    deletePhotoInStorage();
                    deleteUser();
                });
    }

    // Salva o usuário no Firebase Database
    private void saveUserInDatabase() {
        User user = new User(displayName, photoUrl.toString());

        FirebaseDatabase.getInstance().getReference(getString(R.string.database_users))
                .child(currentUser.getUid())
                .setValue(user)
                .addOnSuccessListener(unused -> {
                    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                        goToHomeActivity();
                    }
                    else {
                        isRegistered = true;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save user in Firestore", e);
                    deletePhotoInStorage();
                    deleteUser();
                });
    }

    // Deleta o usuário do Firebase
    private void deleteUser() {
        currentUser.delete()
                .addOnCompleteListener(task -> {
                   if (task.isSuccessful()) {
                       Log.d(TAG, "User deleted");
                   }
                   else {
                       Log.e(TAG, "User deletion failed", task.getException());
                   }
                   loadingBar.setVisibility(View.GONE);
                   getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                   displayMessage(getString(R.string.failure_create_account));
                });
    }

    // Deleta a foto do Storage
    private void deletePhotoInStorage() {
        photoReference.delete()
                .addOnSuccessListener(unused -> Log.d(TAG, "Photo deleted"))
                .addOnFailureListener(e -> Log.e(TAG, "Photo deletion failed", e));
    }

    private void goToHomeActivity() {
        loadingBar.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        displayMessage(getString(R.string.success_register));

        Intent homeIntent = new Intent(this, HomeActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);

        ChatFirebaseApplication application = (ChatFirebaseApplication) getApplication();
        application.setup(currentUser.getUid());
    }

    private void goToLoginActivity() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent);
    }

    private void displayMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}