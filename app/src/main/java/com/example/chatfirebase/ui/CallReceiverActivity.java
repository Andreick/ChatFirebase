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
import com.example.chatfirebase.data.User;
import com.example.chatfirebase.services.SinchService;
import com.google.firebase.database.FirebaseDatabase;
import com.sinch.android.rtc.MissingPermissionException;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;
import com.squareup.picasso.Picasso;

import java.util.List;

public class CallReceiverActivity extends AppCompatActivity implements ServiceConnection {

    private static final String TAG = "CallReceiverActivity";

    private SinchService sinchService;
    private Call call;
    private boolean speakerEnabled;

    private ImageView vImgEmitter;
    private TextView vTxtEmitterName;
    private ImageView vbtAccept, vbtReject, vbtSpeaker;
    private Chronometer chronometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_receiver);

        vImgEmitter = findViewById(R.id.civ_emitter_photo);
        vTxtEmitterName = findViewById(R.id.tv_emitter_name);
        vbtAccept = findViewById(R.id.iv_receiver_answer);
        vbtReject = findViewById(R.id.iv_receiver_hang_up);
        vbtSpeaker = findViewById(R.id.iv_emitter_speaker);
        chronometer = findViewById(R.id.receiver_chronometer);

        Intent serviceIntent = new Intent(this, SinchService.class);
        bindService(serviceIntent, this, 0);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        SinchService.SinchServiceBinder binder = (SinchService.SinchServiceBinder) service;
        sinchService = binder.getService();

        call = sinchService.getCall();

        if (call == null) {
            finish();
            return;
        }

        call.addCallListener(new SinchCallListener());

        FirebaseDatabase.getInstance().getReference(getString(R.string.database_users))
                .child(call.getRemoteUserId()).get()
                .addOnSuccessListener(snapshot -> {
                    User contact = snapshot.getValue(User.class);

                    if (contact != null) {
                        Picasso.get().load(contact.getProfileUrl())
                                .fit().centerCrop()
                                .placeholder(R.drawable.profile_placeholder)
                                .into(vImgEmitter);
                        vTxtEmitterName.setText(contact.getName());
                    }
                    else {
                        Log.e(TAG, "Null contact");
                        displayMessage(getString(R.string.failure_contact));
                        call.hangup();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Get remote user failure");
                    displayMessage(getString(R.string.failure_contact));
                    call.hangup();
                });

        vbtReject.setOnClickListener(view -> call.hangup());
        vbtAccept.setOnClickListener(view -> {
            try {
                call.answer();
            }
            catch (MissingPermissionException e) {
                displayMessage(getString(R.string.permission_microphone_phone));
                Log.e(TAG, e.getMessage());
            }
        });
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
        displayMessage(getString(R.string.failure_sinch_service));
        finish();
    }

    @Override
    public void onBackPressed() {
        // Do not do anything
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
            vbtAccept.setEnabled(false);
            vbtAccept.setAlpha(0.5f);
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
                case FAILURE:
                    SinchError e = endedCall.getDetails().getError();
                    displayMessage(e.getMessage());
                    break;
                default:
                    displayMessage(endCause.toString());
            }

            finish();
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {

        }
    }
}