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

import com.example.chatfirebase.R;
import com.example.chatfirebase.data.CallInfo;
import com.example.chatfirebase.services.SinchService;
import com.google.firebase.firestore.CollectionReference;
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
    private String contactName, contactProfileUrl;
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

        contactName = getIntent().getStringExtra(getString(R.string.contact_name));
        contactProfileUrl = getIntent().getStringExtra(getString(R.string.contact_profile_url));

        Picasso.get().load(contactProfileUrl).fit().centerCrop()
                .placeholder(R.drawable.profile_placeholder).into(vImgReceiver);
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
                displayMessage(getString(R.string.call_speaker_disabled));
                speakerIcon = R.drawable.disabled_speaker;
            }
            else {
                sinchService.getAudioController().enableSpeaker();
                displayMessage(getString(R.string.call_speaker_enabled));
                speakerIcon = R.drawable.enabled_speaker;
            }
            vbtSpeaker.setImageDrawable(ContextCompat.getDrawable(this, speakerIcon));
            speakerEnabled = !speakerEnabled;
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e(TAG, "Sinch Service disconnected");
        displayMessage(getString(R.string.failure_call_service));
        finish();
    }

    @Override
    public void onBackPressed() {
        // Do not do anything
    }

    private void registerCall(CallEndCause endCause) {
        String contactId = getIntent().getStringExtra(getString(R.string.contact_id));
        String currentUid = getIntent().getStringExtra(getString(R.string.user_id));
        String currentUserName = getIntent().getStringExtra(getString(R.string.user_name));
        String currentProfileUrl = getIntent().getStringExtra(getString(R.string.user_profile_url));

        CollectionReference talksReference = FirebaseFirestore.getInstance().collection(getString(R.string.collection_talks));
        WriteBatch batch = FirebaseFirestore.getInstance().batch();

        CallInfo callInfo = new CallInfo(currentUid, endCause.getValue(), true);

        callInfo.setContact(contactId, contactName, contactProfileUrl);
        batch.set(talksReference.document(currentUid).collection(getString(R.string.collection_talks_calls)).document(), callInfo);

        callInfo.setContact(currentUid, currentUserName, currentProfileUrl);
        callInfo.setViewed(endCause == CallEndCause.DENIED || endCause == CallEndCause.HUNG_UP);
        batch.set(talksReference.document(contactId).collection(getString(R.string.collection_talks_calls)).document(), callInfo);

        batch.commit().addOnFailureListener(e -> {
            Log.e(TAG, "Register call batch failed", e);
            displayMessage(getString(R.string.failure_call));
        });
    }

    private void displayMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private class SinchCallListener implements CallListener {

        @Override
        public void onCallProgressing(Call progressingCall) {
            displayMessage(getString(R.string.on_call_progressing));
        }

        @Override
        public void onCallEstablished(Call establishedCall) {
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.setVisibility(View.VISIBLE);
            chronometer.start();
            vbtSpeaker.setVisibility(View.VISIBLE);
            displayMessage(getString(R.string.on_call_established));
        }

        @Override
        public void onCallEnded(Call endedCall) {
            sinchService.callEnded();
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

            CallEndCause endCause = endedCall.getDetails().getEndCause();
            switch (endCause) {
                case DENIED:
                    displayMessage(getString(R.string.call_denied));
                    break;
                case CANCELED:
                    displayMessage(getString(R.string.call_canceled));
                    break;
                case HUNG_UP:
                    displayMessage(getString(R.string.call_hang_up));
                    break;
                case TIMEOUT:
                    displayMessage(getString(R.string.call_timeout));
                    break;
                case NO_ANSWER:
                    displayMessage(getString(R.string.call_no_answer));
                    break;
                case FAILURE:
                    SinchError e = endedCall.getDetails().getError();
                    displayMessage(e.getMessage());
                    break;
                default:
                    displayMessage(endCause.toString());
            }

            registerCall(endCause);
            finish();
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {

        }
    }
}