package com.example.chatfirebase;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
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

    ImageView vImgEmitter;
    TextView vTxtEmitterName;
    ImageView vbtReject;
    ImageView vbtAccept;
    ImageView vbtSpeaker;

    private SinchService sinchService;
    private Call call;
    private boolean speakerEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_receiver);

        vImgEmitter = findViewById(R.id.imgEmitter);
        vTxtEmitterName = findViewById(R.id.txtEmitterName);
        vbtReject = findViewById(R.id.btReject);
        vbtAccept = findViewById(R.id.btAccept);
        vbtSpeaker = findViewById(R.id.imgEmitterSpeaker);

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
            }
            catch (MissingPermissionException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        vbtSpeaker.setOnClickListener(view -> {
            if (speakerEnabled) {
                sinchService.getAudioController().disableSpeaker();
                vbtSpeaker.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.btn_viva_voz_desativado));
            }
            else {
                sinchService.getAudioController().enableSpeaker();
                vbtSpeaker.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.btn_viva_voz_ativado));
            }
            speakerEnabled = !speakerEnabled;
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e(TAG, "Sinch Service disconnected");
        finish();
    }

    @Override
    public void onBackPressed() {
        // Do not do anything
    }

    private class SinchCallListener implements CallListener {

        @Override
        public void onCallProgressing(Call progressingCall) {
            Toast.makeText(CallReceiverActivity.this, getString(R.string.on_call_progressing), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCallEstablished(Call establishedCall) {
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            vbtAccept.setEnabled(false);
            vbtAccept.setAlpha(0.5f);
            vbtSpeaker.setVisibility(View.VISIBLE);
            Toast.makeText(CallReceiverActivity.this, getString(R.string.on_call_established), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCallEnded(Call endedCall) {
            sinchService.callEnded();
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

            CallEndCause endCause = endedCall.getDetails().getEndCause();
            switch (endCause) {
                case HUNG_UP:
                    Toast.makeText(CallReceiverActivity.this, getString(R.string.call_hang_up), Toast.LENGTH_SHORT).show();
                    break;
                case FAILURE:
                    SinchError e = endedCall.getDetails().getError();
                    Toast.makeText(CallReceiverActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(CallReceiverActivity.this, endCause.toString(), Toast.LENGTH_SHORT).show();
            }

            finish();
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {

        }
    }
}