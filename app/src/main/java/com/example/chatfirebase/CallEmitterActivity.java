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

import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;
import com.squareup.picasso.Picasso;

import java.util.List;

public class CallEmitterActivity extends AppCompatActivity implements ServiceConnection {

    private static final String TAG = "CallEmitterActivity";

    private ImageView vbtReject;
    private ImageView vbtSpeaker;

    private SinchService sinchService;
    private Call call;
    private boolean speakerEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_emitter);

        ImageView vImgReceiver = findViewById(R.id.imgReceiver);
        TextView vTxtReceiverName = findViewById(R.id.txtReceiverName);
        vbtReject = findViewById(R.id.btReject2);
        vbtSpeaker = findViewById(R.id.imgEmitterSpeaker);

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
        sinchService = binder.getService();

        call = sinchService.getCall();
        call.addCallListener(new SinchCallListener());

        vbtReject.setOnClickListener(view -> call.hangup());
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
            Toast.makeText(CallEmitterActivity.this, getString(R.string.on_call_progressing), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCallEstablished(Call establishedCall) {
            CallEmitterActivity.this.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            vbtSpeaker.setVisibility(View.VISIBLE);
            Toast.makeText(CallEmitterActivity.this, getString(R.string.on_call_established), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCallEnded(Call endedCall) {
            sinchService.callEnded();
            CallEmitterActivity.this.setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

            CallEndCause endCause = endedCall.getDetails().getEndCause();
            switch (endCause) {
                case HUNG_UP:
                    Toast.makeText(CallEmitterActivity.this, getString(R.string.call_hang_up), Toast.LENGTH_SHORT).show();
                    break;
                case FAILURE:
                    SinchError e = endedCall.getDetails().getError();
                    Toast.makeText(CallEmitterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(CallEmitterActivity.this, endCause.toString(), Toast.LENGTH_SHORT).show();
            }

            CallEmitterActivity.this.finish();
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {

        }
    }
}