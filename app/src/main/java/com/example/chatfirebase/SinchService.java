package com.example.chatfirebase;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.sinch.android.rtc.ClientRegistration;
import com.sinch.android.rtc.MissingPermissionException;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallClientListener;

public class SinchService extends Service {

    private static final String TAG = "SinchService";

    private final IBinder binder = new SinchServiceBinder();
    private SinchClient sinchClient;
    private Call call;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        startClient();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopClient();
    }

    private void startClient() {
        if (sinchClient == null) {
            String userId = FirebaseAuth.getInstance().getUid();

            if (!TextUtils.isEmpty(userId)) {
                sinchClient = Sinch.getSinchClientBuilder()
                        .context(this)
                        .userId(userId)
                        .applicationKey(getString(R.string.sinch_key))
                        .applicationSecret(getString(R.string.sinch_secret))
                        .environmentHost(getString(R.string.sinch_hostname))
                        .build();

                sinchClient.setSupportCalling(true);

                sinchClient.addSinchClientListener(new SinchClientListener());
                sinchClient.getCallClient().addCallClientListener(new SinchCallClientListener());

                Log.d(TAG, "Starting Sinch Client");
                sinchClient.start();
            }
        }
    }

    private void stopClient() {
        if (sinchIsStarted()) {
            sinchClient.stopListeningOnActiveConnection();
            sinchClient.terminateGracefully();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class SinchServiceBinder extends Binder {

        SinchService getService() {
            return SinchService.this;
        }
    }

    public boolean sinchIsStarted() {
        return (sinchClient != null && sinchClient.isStarted());
    }

    public void callUser(User contact) {
        if (sinchIsStarted()) {
            if (call == null) {
                try {
                    call = sinchClient.getCallClient().callUser(contact.getId());

                    Intent emitterIntent = new Intent(SinchService.this, CallEmitterActivity.class);
                    emitterIntent.putExtra(getString(R.string.user_name), contact.getName());
                    emitterIntent.putExtra(getString(R.string.user_profile_url), contact.getProfileUrl());

                    emitterIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(emitterIntent);
                }
                catch (MissingPermissionException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
        else {
            Toast.makeText(this, "O servidor de chamadas não foi iniciado", Toast.LENGTH_LONG).show();
        }
    }

    public Call getCall() {
        return call;
    }

    public void callEnded() {
        call = null;
    }

    public void retryStartAfterPermissionGranted() { SinchService.this.startClient(); }

    private class SinchCallClientListener implements CallClientListener {

        @Override
        public void onIncomingCall(CallClient callClient, Call incomingCall) {
            Log.d(TAG, "Incoming call");
            if (call == null) {
                call = incomingCall;

                Intent receiverIntent = new Intent(SinchService.this, CallReceiverActivity.class);

                receiverIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(receiverIntent);
            }
        }
    }

    private class SinchClientListener implements com.sinch.android.rtc.SinchClientListener {

        @Override
        public void onClientFailed(SinchClient client, SinchError error) {
            Log.e(TAG, "Sinch Client failed " + error.getMessage());
            sinchClient.terminate();
            sinchClient = null;
            call = null;
            Toast.makeText(SinchService.this, "Falha no servidor de chamadas", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onClientStarted(SinchClient client) {
            Log.d(TAG, "Sinch Client started");
            sinchClient.startListeningOnActiveConnection();
        }

        @Override
        public void onClientStopped(SinchClient client) {
            Log.d(TAG, "Sinch Client stopped");
        }

        @Override
        public void onLogMessage(int level, String area, String message) {
            switch (level) {
                case Log.DEBUG:
                    Log.d(area, message);
                    break;
                case Log.ERROR:
                    Log.e(area, message);
                    break;
                case Log.INFO:
                    Log.i(area, message);
                    break;
                case Log.VERBOSE:
                    Log.v(area, message);
                    break;
                case Log.WARN:
                    Log.w(area, message);
                    break;
            }
        }

        @Override
        public void onRegistrationCredentialsRequired(SinchClient client, ClientRegistration clientRegistration) {

        }
    }
}