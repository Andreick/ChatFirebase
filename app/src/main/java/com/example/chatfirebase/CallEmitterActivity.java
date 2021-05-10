package com.example.chatfirebase;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.sinch.android.rtc.calling.Call;

public class CallEmitterActivity extends AppCompatActivity {

    private ChatFirebase chatFirebase;
    private Call call;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_emitter);

        ImageView vbtReject = findViewById(R.id.btReject2);

        chatFirebase = (ChatFirebase) getApplicationContext();
        call = chatFirebase.getCall();
        call.addCallListener(new SinchCallListener(this));

        vbtReject.setOnClickListener(view -> call.hangup());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        chatFirebase.callEnded();
    }
}