package com.example.chatfirebase;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.sinch.android.rtc.calling.Call;

public class CallReceiverActivity extends AppCompatActivity {

    private ChatFirebase chatFirebase;
    private Call call;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_receiver);

        ImageView vbtAccept = findViewById(R.id.btAccept);
        ImageView vbtReject = findViewById(R.id.btReject);

        chatFirebase = (ChatFirebase) getApplicationContext();
        call = chatFirebase.getCall();
        call.addCallListener(new SinchCallListener(this));

        vbtReject.setOnClickListener(view -> call.hangup());
        vbtAccept.setOnClickListener(view -> {
            call.answer();
            vbtAccept.setVisibility(View.INVISIBLE);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        chatFirebase.callEnded();
    }
}