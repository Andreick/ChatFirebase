package com.example.chatfirebase;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChatFirebaseApplication extends Application implements ServiceConnection {

    private static final String TAG = "ChatFirebaseApplication";

    private User currentUser;
    private SinchService sinchService;

    public void setup() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser != null) {
            String uid = firebaseUser.getUid();
            String displayName = firebaseUser.getDisplayName();
            Uri photoUrl = firebaseUser.getPhotoUrl();

            if (displayName != null && photoUrl != null) {
                currentUser = new User(uid, displayName, photoUrl.toString());

                Intent serviceIntent = new Intent(this, SinchService.class);
                bindService(serviceIntent, this, BIND_AUTO_CREATE);
            }
            else {
                Log.e(TAG, "User without name or photo");
            }
        }
        else {
            Log.e(TAG, "Null Firebase user");
        }
    }

    public void close() {
        unbindService(this);
        FirebaseAuth.getInstance().signOut();
        currentUser = null;
        sinchService = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "Sinch Service connected");
        SinchService.SinchServiceBinder binder = (SinchService.SinchServiceBinder) service;
        sinchService = binder.getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e(TAG, "Sinch Service disconnected");
        sinchService = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public SinchService getSinchService() {
        return sinchService;
    }
}
