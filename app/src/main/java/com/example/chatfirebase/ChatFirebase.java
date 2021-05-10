package com.example.chatfirebase;

import android.app.Application;
import android.content.Intent;

import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallClientListener;

public class ChatFirebase extends Application {

    private SinchClient sinchClient;
    private Call call;

    public void setSinchClient(String currentUid) {
        sinchClient = Sinch.getSinchClientBuilder()
                .context(this)
                .userId(currentUid)
                .applicationKey(getString(R.string.sinch_key))
                .applicationSecret(getString(R.string.sinch_secret))
                .environmentHost(getString(R.string.sinch_hostname))
                .build();
        sinchClient.setSupportCalling(true);
        sinchClient.startListeningOnActiveConnection();
        sinchClient.start();
        sinchClient.getCallClient().addCallClientListener(new SinchCallClientListener());
    }

    public void callContact(String contactId) {
        if (call == null) {
            call = sinchClient.getCallClient().callUser(contactId);
            Intent emitterIntent = new Intent(ChatFirebase.this, CallEmitterActivity.class);
            emitterIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(emitterIntent);
        }
    }

    public Call getCall() {
        return call;
    }

    public void callEnded() {
        call = null;
    }

    public void terminateSinchClient() {
        if (sinchClient != null) {
            sinchClient.stopListeningOnActiveConnection();
            sinchClient.terminate();
            sinchClient = null;
        }
    }

    private class SinchCallClientListener implements CallClientListener {
        @Override
        public void onIncomingCall(CallClient callClient, Call incomingCall) {
            call = incomingCall;
            Intent receiverIntent = new Intent(ChatFirebase.this, CallReceiverActivity.class);
            receiverIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(receiverIntent);
        }
    }
}
