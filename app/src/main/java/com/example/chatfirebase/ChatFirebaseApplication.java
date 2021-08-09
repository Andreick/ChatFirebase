package com.example.chatfirebase;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.example.chatfirebase.data.UserConnectionStatus;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChatFirebaseApplication extends Application implements LifecycleObserver {

    private static final String TAG = "ChatFirebaseApplication";

    private int connectionStatus;
    private DatabaseReference currentUserReference;

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private void userOnline() {
        updateUserStatus(UserConnectionStatus.ONLINE);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void userAbsent() {
        updateUserStatus(UserConnectionStatus.AWAY);
    }

    private void updateUserStatus(UserConnectionStatus status) {
        connectionStatus = status.ordinal();
        currentUserReference.setValue(connectionStatus)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update " + status + " status", e));
    }

    public void setup(String currentUid) {
        if (currentUserReference != null) return;
        currentUserReference = FirebaseDatabase.getInstance().getReference(getString(R.string.database_users))
                .child(currentUid)
                .child(getString(R.string.connection_status));

        currentUserReference.onDisconnect().setValue(UserConnectionStatus.OFFLINE.ordinal())
                .addOnFailureListener(e -> Log.e(TAG, "Could not establish onDisconnect event"));

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    public int getConnectionStatus() {
        return connectionStatus;
    }

    public void close() {
        ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
        updateUserStatus(UserConnectionStatus.OFFLINE);
        FirebaseAuth.getInstance().signOut();
        currentUserReference = null;
    }
}
