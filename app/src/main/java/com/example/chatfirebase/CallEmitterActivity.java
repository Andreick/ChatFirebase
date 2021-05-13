package com.example.chatfirebase;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sinch.android.rtc.calling.Call;
import com.squareup.picasso.Picasso;

public class CallEmitterActivity extends AppCompatActivity {

    private ChatFirebase chatFirebase;
    private Call call;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_emitter);

        ImageView vImgReceiver = findViewById(R.id.imgReceiver);
        TextView vTxtReceiverName = findViewById(R.id.txtReceiverName);
        ImageView vbtReject = findViewById(R.id.btReject2);

        chatFirebase = (ChatFirebase) getApplicationContext();
        call = chatFirebase.getCall();
        call.addCallListener(new SinchCallListener(this));

        String profileUrl = getIntent().getStringExtra(getString(R.string.user_profile_url));
        Picasso.get().load(profileUrl).into(vImgReceiver);

        String username = getIntent().getStringExtra(getString(R.string.user_name));
        vTxtReceiverName.setText(username);

        vbtReject.setOnClickListener(view -> call.hangup());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        chatFirebase.callEnded();
    }
}