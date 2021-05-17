package com.example.chatfirebase;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class CallNotificationService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent fullScreenIntent = new Intent(getApplicationContext(), CallReceiverActivity.class);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), getString(R.string.channel_id))
                .setSmallIcon(R.drawable.ic_baseline_call_24)
                .setContentTitle(getString(R.string.call_notification_title))
                .setContentText(getString(R.string.call_notification_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true);

        startForeground(1, builder.build());

        return START_STICKY;
    }
}
