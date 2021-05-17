package com.example.chatfirebase;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sinch.android.rtc.calling.Call;
import com.squareup.picasso.Picasso;

public class CallEmitterActivity extends AppCompatActivity implements ServiceConnection {

    private static final String TAG = "CallEmitterActivity";

    private ImageView vbtReject;

    private Call call;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_emitter);

        ImageView vImgReceiver = findViewById(R.id.imgReceiver);
        TextView vTxtReceiverName = findViewById(R.id.txtReceiverName);
        vbtReject = findViewById(R.id.btReject2);

        Intent serviceIntent = new Intent(this, SinchService.class);
        bindService(serviceIntent, this, 0);

        String profileUrl = getIntent().getStringExtra(getString(R.string.user_profile_url));
        Picasso.get().load(profileUrl).into(vImgReceiver);

        String username = getIntent().getStringExtra(getString(R.string.user_name));
        vTxtReceiverName.setText(username);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        SinchService.SinchServiceBinder binder = (SinchService.SinchServiceBinder) service;
        SinchService sinchService = binder.getService();

        call = sinchService.getCall();
        call.addCallListener(new SinchCallListener(this, sinchService));
        vbtReject.setOnClickListener(view -> call.hangup());
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e(TAG, "Sinch Service disconnected");
        finish();
    }
}