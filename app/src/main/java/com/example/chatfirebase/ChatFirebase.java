package com.example.chatfirebase;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallClientListener;

import java.util.Objects;

public class ChatFirebase extends Application {

    private User currentUser;
    private SinchClient sinchClient;
    private Call call;

    public void setup() {
        FirebaseUser firebaseUser = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());
        currentUser = new User(firebaseUser.getUid(), firebaseUser.getDisplayName(), Objects.requireNonNull(firebaseUser.getPhotoUrl()));

        sinchClient = Sinch.getSinchClientBuilder()
                .context(this)
                .userId(currentUser.getId())
                .applicationKey(getString(R.string.sinch_key))
                .applicationSecret(getString(R.string.sinch_secret))
                .environmentHost(getString(R.string.sinch_hostname))
                .build();
        sinchClient.setSupportCalling(true);
        sinchClient.startListeningOnActiveConnection();
        sinchClient.start();
        sinchClient.getCallClient().addCallClientListener(new SinchCallClientListener());
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Call getCall() {
        return call;
    }

    public void callContact(User contact) {
        if (call == null) {
            call = sinchClient.getCallClient().callUser(contact.getId());

            Intent emitterIntent = new Intent(ChatFirebase.this, CallEmitterActivity.class);
            emitterIntent.putExtra(getString(R.string.user_name), contact.getName());
            emitterIntent.putExtra(getString(R.string.user_profile_url), contact.getProfileUrl());

            emitterIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(emitterIntent);
        }
    }

    public void callEnded() {
        call = null;
    }

    public void terminate() {
        currentUser = null;
        if (sinchClient != null) {
            sinchClient.stopListeningOnActiveConnection();
            sinchClient.terminate();
            sinchClient = null;
            call = null;
        }
    }

    private class SinchCallClientListener implements CallClientListener {
        @Override
        public void onIncomingCall(CallClient callClient, Call incomingCall) {
            call = incomingCall;
            FirebaseFirestore.getInstance().collection(getString(R.string.collection_users))
                    .document(call.getRemoteUserId())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        User caller = Objects.requireNonNull(snapshot.toObject(User.class));

                        Intent receiverIntent = new Intent(ChatFirebase.this, CallReceiverActivity.class);
                        receiverIntent.putExtra(getString(R.string.user_name), caller.getName());
                        receiverIntent.putExtra(getString(R.string.user_profile_url), caller.getProfileUrl());

                        receiverIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(receiverIntent);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e);
                        call.hangup();
                        call = null;
                    });
        }
    }
}
