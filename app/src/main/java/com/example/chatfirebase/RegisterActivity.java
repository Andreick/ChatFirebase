package com.example.chatfirebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.UUID;

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



        vButtonHaveAccout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),MainActivity.class));
            }
        });


        vButtonPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPhoto();
            }
        });


        vButtonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createUser();
            }
        });
    }

    public void createUser(){
        String nome = vEditName.getText().toString();
        String email = vEditEmail.getText().toString();
        String senha = vEditPassword.getText().toString();
        String confSenha = vEditConfirmPass.getText().toString();


        if (vImgPhoto.getDrawable() == null){
            Toast.makeText(this, "Insira uma foto", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nome == null || nome.isEmpty()){
            vEditName.setError("Insira seu nome");
            return;
        }
        if (email == null || email.isEmpty()){
            vEditEmail.setError("Insira seu Email");
            return;
        }
        if (senha == null || senha.isEmpty()){
            vEditPassword.setError("Insira sua Senha");
            return;
        }
        if (!senha.equals(confSenha)){
            vEditConfirmPass.setError("Senhas diferentes");
            return;
        }


        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){

                            saveUserFirebase();
                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0) {
            vSelectData = data.getData();

            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), vSelectData);
                vImgPhoto.setImageDrawable(new BitmapDrawable(bitmap));
                vButtonPhoto.setVisibility(View.INVISIBLE);
                vImgPhoto.setVisibility(View.VISIBLE);
            }catch (IOException e){

            }
        }
    }

    private void selectPhoto(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 0);
    }

    private void saveUserFirebase(){
        String filename = UUID.randomUUID().toString();
        final StorageReference reference = FirebaseStorage.getInstance().getReference("/imagens/" + filename);
        reference.putFile(vSelectData)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {

                                String uid = FirebaseAuth.getInstance().getUid();
                                String username = vEditName.getText().toString();
                                String profileUrl = uri.toString();

                                User user = new User(uid, username, profileUrl);

                                FirebaseFirestore.getInstance().collection("users")
                                        .document(uid)
                                        .set(user)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Intent intent = new Intent(RegisterActivity.this, MessagesActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {

                                            }
                                        });
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }
}