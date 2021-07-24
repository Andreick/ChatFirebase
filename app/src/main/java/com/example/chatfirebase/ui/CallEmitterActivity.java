package com.example.chatfirebase.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.chatfirebase.ChatFirebaseApplication;
import com.example.chatfirebase.R;
import com.example.chatfirebase.data.CallInfo;
import com.example.chatfirebase.data.User;
import com.example.chatfirebase.services.SinchService;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;
import com.squareup.picasso.Picasso;

import java.util.List;

public class CallEmitterActivity extends AppCompatActivity implements ServiceConnection {

    private static final String TAG = "CallEmitterActivity";

    private SinchService sinchService;
    private Call call;
    private String contactName;
    private String contactProfileUrl;
    private boolean speakerEnabled;

    private ImageView vbtReject, vbtSpeaker;
    private Chronometer chronometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_emitter);

        ImageView vImgReceiver = findViewById(R.id.civ_receiver_photo);
        TextView vTxtReceiverName = findViewById(R.id.tv_receiver_name);
        vbtReject = findViewById(R.id.iv_emitter_hang_up);
        vbtSpeaker = findViewById(R.id.iv_emitter_speaker);
        chronometer = findViewById(R.id.emitter_chronometer);

        Intent serviceIntent = new Intent(this, SinchService.class);
        bindService(serviceIntent, this, 0);

        contactName = getIntent().getStringExtra(getString(R.string.extra_contact_name));
        contactProfileUrl = getIntent().getStringExtra(getString(R.string.extra_contact_profile_url));

        Picasso.get().load(contactProfileUrl).placeholder(R.drawable.profile_placeholder).into(vImgReceiver);
        vTxtReceiverName.setText(contactName);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        SinchService.SinchServiceBinder binder = (SinchService.SinchServiceBinder) service;
        sinchService = binder.getService();

        call = sinchService.getCall();
        call.addCallListener(new SinchCallListener());

        vbtReject.setOnClickListener(view -> call.hangup());
        vbtSpeaker.setOnClickListener(view -> {
            int speakerIcon;
            if (speakerEnabled) {
                sinchService.getAudioController().disableSpeaker();
                Toast.makeText(this, getString(R.string.call_speaker_disabled), Toast.LENGTH_SHORT).show();
                speakerIcon = R.drawable.disabled_speaker;
            }
            else {
                sinchService.getAudioController().enableSpeaker();
                Toast.makeText(this, getString(R.string.call_speaker_enabled), Toast.LENGTH_SHORT).show();
                speakerIcon = R.drawable.enabled_speaker;
            }
            vbtSpeaker.setImageDrawable(ContextCompat.getDrawable(this, speakerIcon));
            speakerEnabled = !speakerEnabled;
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e(TAG, "Sinch Service disconnected");
        Toast.makeText(this, getString(R.string.failure_sinch_service), Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        // Do not do anything
    }

    private void registerCall(boolean answered) {
        long timestamp = System.currentTimeMillis();
        String contactId = getIntent().getStringExtra(getString(R.string.extra_contact_id));
        String currentUid = getIntent().getStringExtra(getString(R.string.extra_user_id));
        User currentUser = ((ChatFirebaseApplication) getApplication()).getCurrentUser();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        CollectionReference talksReference = firestore.collection(getString(R.string.collection_talks));
        WriteBatch batch = firestore.batch();

        DocumentReference callCurrentUserReference = talksReference.document(currentUid).collection(getString(R.string.collection_talks_calls)).document();
        batch.set(callCurrentUserReference, new CallInfo(contactId, contactName, contactProfileUrl, timestamp, answered));

        DocumentReference callContactReference = talksReference.document(contactId).collection(getString(R.string.collection_talks_calls)).document();
        batch.set(callContactReference, new CallInfo(currentUid, currentUser.getName(), currentUser.getProfileUrl(), timestamp, answered));

        batch.commit().addOnFailureListener(e -> {
            Log.e(TAG, "Register call batch failed", e);
            Toast.makeText(this, getString(R.string.failure_call), Toast.LENGTH_SHORT).show();
        });
    }

    private class SinchCallListener implements CallListener {

        @Override
        public void onCallProgressing(Call progressingCall) {
            Toast.makeText(CallEmitterActivity.this, getString(R.string.on_call_progressing), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCallEstablished(Call establishedCall) {
            CallEmitterActivity.this.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.setVisibility(View.VISIBLE);
            chronometer.start();
            vbtSpeaker.setVisibility(View.VISIBLE);
            Toast.makeText(CallEmitterActivity.this, getString(R.string.on_call_established), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCallEnded(Call endedCall) {
            sinchService.callEnded();
            CallEmitterActivity.this.setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

            CallEndCause endCause = endedCall.getDetails().getEndCause();
            boolean answered = false;
            switch (endCause) {
                case HUNG_UP:
                    Toast.makeText(CallEmitterActivity.this, getString(R.string.call_hang_up), Toast.LENGTH_SHORT).show();
                    answered = true;
                    break;
                case FAILURE:
                    SinchError e = endedCall.getDetails().getError();
                    Toast.makeText(CallEmitterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(CallEmitterActivity.this, endCause.toString(), Toast.LENGTH_SHORT).show();
            }

            CallEmitterActivity.this.registerCall(answered);
            CallEmitterActivity.this.finish();
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {

        }
    }
}