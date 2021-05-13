package com.example.chatfirebase;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sinch.android.rtc.calling.Call;
import com.squareup.picasso.Picasso;

public class CallReceiverActivity extends AppCompatActivity {

    private ChatFirebase chatFirebase;
    private Call call;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_receiver);

        ImageView vImgEmitter = findViewById(R.id.imgEmitter);
        TextView vTxtEmitterName = findViewById(R.id.txtEmitterName);
        ImageView vbtReject = findViewById(R.id.btReject);
        ImageView vbtAccept = findViewById(R.id.btAccept);

        chatFirebase = (ChatFirebase) getApplicationContext();
        call = chatFirebase.getCall();
        call.addCallListener(new SinchCallListener(this));

        String profileUrl = getIntent().getStringExtra(getString(R.string.user_profile_url));
        Picasso.get().load(profileUrl).into(vImgEmitter);

        String username = getIntent().getStringExtra(getString(R.string.user_name));
        vTxtEmitterName.setText(username);

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