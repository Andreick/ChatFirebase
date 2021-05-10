package com.example.chatfirebase;

import android.app.Activity;
import android.media.AudioManager;
import android.widget.Toast;

import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;

import java.util.List;

public class SinchCallListener implements CallListener {

    private final Activity activity;

    public SinchCallListener(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onCallProgressing(Call progressingCall) {
        Toast.makeText(activity, activity.getString(R.string.on_call_progressing), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCallEstablished(Call establishedCall) {
        activity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        Toast.makeText(activity, activity.getString(R.string.on_call_established), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCallEnded(Call endedCall) {
        activity.setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

        CallEndCause endCause = endedCall.getDetails().getEndCause();
        switch (endCause) {
            case HUNG_UP:
                Toast.makeText(activity, activity.getString(R.string.call_hang_up), Toast.LENGTH_SHORT).show();
                break;
            case FAILURE:
                SinchError e = endedCall.getDetails().getError();
                Toast.makeText(activity, activity.getString(R.string.log_msg) + e.getMessage(), Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(activity, endCause.toString(), Toast.LENGTH_SHORT).show();
        }

        activity.finish();
    }

    @Override
    public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {

    }
}
