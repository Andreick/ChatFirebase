package com.example.chatfirebase;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.sinch.android.rtc.MissingPermissionException;
import com.sinch.android.rtc.calling.Call;
import com.squareup.picasso.Picasso;

public class CallReceiverActivity extends AppCompatActivity implements ServiceConnection {

    private static final String TAG = "CallReceiverActivity";

    ImageView vImgEmitter;
    TextView vTxtEmitterName;
    ImageView vbtReject;
    ImageView vbtAccept;

    private Call call;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_receiver);

        vImgEmitter = findViewById(R.id.imgEmitter);
        vTxtEmitterName = findViewById(R.id.txtEmitterName);
        vbtReject = findViewById(R.id.btReject);
        vbtAccept = findViewById(R.id.btAccept);

        Intent serviceIntent = new Intent(this, SinchService.class);
        bindService(serviceIntent, this, 0);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        SinchService.SinchServiceBinder binder = (SinchService.SinchServiceBinder) service;
        SinchService sinchService = binder.getService();

        call = sinchService.getCall();
        call.addCallListener(new SinchCallListener(this, sinchService));

        FirebaseFirestore.getInstance().collection(getString(R.string.collection_users))
                .document(call.getRemoteUserId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String profileUrl = (String) snapshot.get(getString(R.string.user_profile_url));
                    Picasso.get().load(profileUrl).into(vImgEmitter);

                    String username = (String) snapshot.get(getString(R.string.user_name));
                    vTxtEmitterName.setText(username);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Get remote user failure");
                    call.hangup();
                });

        vbtReject.setOnClickListener(view -> call.hangup());
        vbtAccept.setOnClickListener(view -> {
            try {
                call.answer();
                vbtAccept.setVisibility(View.INVISIBLE);
            }
            catch (MissingPermissionException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e(TAG, "Sinch Service disconnected");
        finish();
    }
}