package com.example.chatfirebase;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
/*import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;*/

public class ChatFirebaseApplication extends Application implements LifecycleObserver, ServiceConnection {

    private static final String TAG = "ChatFirebaseApplication";

    private User currentUser;
    //private DatabaseReference userReference;
    private SinchService sinchService;

    /*@OnLifecycleEvent(Lifecycle.Event.ON_START)
    private void userOnline() {
        updateUserStatus(UserConnectionStatus.ONLINE);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void userAbsent() {
        updateUserStatus(UserConnectionStatus.ABSENT);
    }

    private void updateUserStatus(UserConnectionStatus status) {
        userReference.setValue(status.ordinal())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update " + status + " status", e));
    }*/

    public void setup() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser != null) {
            String uid = firebaseUser.getUid();
            String displayName = firebaseUser.getDisplayName();
            Uri photoUrl = firebaseUser.getPhotoUrl();

            if (displayName != null && photoUrl != null) {
                currentUser = new User(uid, displayName, photoUrl.toString());

                /*userReference = FirebaseDatabase.getInstance().getReference(getString(R.string.database_users))
                        .child(uid)
                        .child(getString(R.string.user_connection_status));
                userReference.onDisconnect().setValue(UserConnectionStatus.OFFLINE.ordinal())
                        .addOnFailureListener(e -> {
                            Log.d(TAG, "Could not establish onDisconnect event");
                        });*/

                ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

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
        ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
        unbindService(this);
        FirebaseAuth.getInstance().signOut();
        currentUser = null;
        //userReference = null;
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
