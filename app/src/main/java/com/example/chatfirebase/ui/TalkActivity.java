package com.example.chatfirebase.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatfirebase.R;
import com.example.chatfirebase.data.Message;
import com.example.chatfirebase.data.UserConnectionStatus;
import com.example.chatfirebase.services.SinchService;
import com.example.chatfirebase.util.BaseCallActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.GroupieViewHolder;
import com.xwray.groupie.Item;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TalkActivity extends BaseCallActivity {

    private static final String TAG = "TalkActivity";

    private final Map<String, SenderMessageItem> senderMessageItemMap = new HashMap<>();
    private final List<MessageItem> messageItems = new ArrayList<>();

    private String contactId, contactName, contactProfileUrl, currentUid;
    private CollectionReference talkContactReference, talkCurrentUserReference;
    private DocumentReference chatContactReference, chatCurrentUserReference;
    private com.google.firebase.firestore.Query messagesQuery;
    private com.google.firebase.database.Query contactStatusQuery;
    private EventListener<QuerySnapshot> messagesEventListener;
    private ValueEventListener contactStatusEventListener;

    private ImageView imgContact;
    private TextView txtNameContact, txtConnStatus;
    private RecyclerView rvMessages;
    private EditText editMessage;
    private GroupAdapter<GroupieViewHolder> messagesAdapter;
    private ImageView vbtSend, vbtCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_talk);

        contactId = getIntent().getStringExtra(getString(R.string.contact_id));
        contactName = getIntent().getStringExtra(getString(R.string.contact_name));
        contactProfileUrl = getIntent().getStringExtra(getString(R.string.contact_profile_url));
        currentUid = getIntent().getStringExtra(getString(R.string.user_id));

        CollectionReference talksReference = FirebaseFirestore.getInstance().collection(getString(R.string.collection_talks));
        DocumentReference contactReference = talksReference.document(contactId);
        DocumentReference currentUserReference = talksReference.document(currentUid);
        talkContactReference = contactReference.collection(currentUid);
        talkCurrentUserReference = currentUserReference.collection(contactId);
        chatContactReference = contactReference.collection(getString(R.string.collection_talks_chats)).document(currentUid);
        chatCurrentUserReference = currentUserReference.collection(getString(R.string.collection_talks_chats)).document(contactId);

        messagesQuery = talkCurrentUserReference.orderBy(getString(R.string.timestamp), Query.Direction.ASCENDING);

        contactStatusQuery = FirebaseDatabase.getInstance().getReference(getString(R.string.database_users))
                .child(contactId)
                .child(getString(R.string.connection_status));

        setMessagesEventListener();
        setContactStatusEventListener();

        imgContact = findViewById(R.id.civ_chat_photo);
        txtNameContact = findViewById(R.id.tv_chat_name);
        txtConnStatus = findViewById(R.id.tv_chat_conn_status);
        rvMessages = findViewById(R.id.rv_chat);
        editMessage = findViewById(R.id.et_message);
        vbtSend = findViewById(R.id.civ_send);
        vbtCall = findViewById(R.id.iv_talk_call);

        messagesAdapter = new GroupAdapter<>();
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(messagesAdapter);

        Picasso.get().load(contactProfileUrl).fit().centerCrop()
                .placeholder(R.drawable.profile_placeholder).into(imgContact);
        txtNameContact.setText(contactName);

        vbtSend.setOnClickListener(view -> sendMessage());
        vbtCall.setOnClickListener(view -> {
            Intent sinchServiceIntent = new Intent(this, SinchService.class);
            sinchServiceIntent.putExtra(getString(R.string.contact_id), contactId);
            sinchServiceIntent.putExtra(getString(R.string.contact_name), contactName);
            sinchServiceIntent.putExtra(getString(R.string.contact_profile_url), contactProfileUrl);
            attemptToStartSinchService(sinchServiceIntent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        messagesQuery.addSnapshotListener(this, messagesEventListener);
        contactStatusQuery.addValueEventListener(contactStatusEventListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        contactStatusQuery.removeEventListener(contactStatusEventListener);
        senderMessageItemMap.clear();
        messageItems.clear();
        messagesAdapter.clear();
    }

    // Exibe todas as mensagens e atualiza a conversa em tempo real
    private void setMessagesEventListener() {
        messagesEventListener = (snapshots, exception) -> {
            if (exception == null) {
                if (snapshots != null) {
                    boolean messagesRead = false;
                    WriteBatch batch = null;

                    for (DocumentChange doc: snapshots.getDocumentChanges()) {
                        String messageId = doc.getDocument().getId();

                        switch (doc.getType()) {
                            case ADDED:
                                Log.d(TAG, "Message " + messageId + " ADDED");
                                Message message = doc.getDocument().toObject(Message.class);
                                MessageItem messageItem;
                                if (currentUid.equals(message.getSenderId())) {
                                    Log.d(TAG, "currentUid equals senderId");
                                    messageItem = new SenderMessageItem(message);
                                    senderMessageItemMap.put(messageId, (SenderMessageItem) messageItem);
                                }
                                else {
                                    if (!message.isRead()) {
                                        Log.d(TAG, "message not read");
                                        if (!messagesRead) {
                                            Log.d(TAG, "!messagesRead");
                                            batch = FirebaseFirestore.getInstance().batch();
                                            messagesRead = true;
                                        }
                                        batch.update(talkContactReference.document(messageId),
                                                getString(R.string.read), true);
                                        batch.update(talkCurrentUserReference.document(messageId),
                                                getString(R.string.read), true);
                                    }
                                    messageItem = new ReceiverMessageItem(message);
                                }
                                messageItems.add(messageItem);
                                break;
                            case MODIFIED:
                                Log.d(TAG, "Message " + messageId + " MODIFIED");
                                SenderMessageItem senderMessageItem = senderMessageItemMap.get(messageId);
                                if (senderMessageItem != null) senderMessageItem.setRead();
                                else Log.e(TAG, "Null sender message item");
                                break;
                        }
                    }

                    if (messagesRead) {
                        batch.update(chatContactReference, getString(R.string.last_message_read), true);
                        batch.update(chatCurrentUserReference, getString(R.string.unreadMessages), 0);
                        batch.commit()
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update read messages batch", e));
                    }
                    rvMessages.post(() -> rvMessages.smoothScrollToPosition(messageItems.size()));
                    messagesAdapter.replaceAll(messageItems);
                }
                else {
                    Log.e(TAG, "Null messages snapshot");
                    Toast.makeText(this, getString(R.string.failure_messages), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            else {
                Log.e(TAG, "Messages snapshot listener failed", exception);
                Toast.makeText(this, getString(R.string.failure_messages), Toast.LENGTH_SHORT).show();
                finish();
            }
        };
    }

    public void setContactStatusEventListener() {
        contactStatusEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange: " + snapshot.getKey());
                Object snapshotValue = snapshot.getValue();

                if (snapshotValue != null) {
                    int contactConnStatus = Integer.parseInt(snapshotValue.toString());
                    if (contactConnStatus == UserConnectionStatus.OFFLINE.ordinal()) {
                        txtConnStatus.setText(getText(R.string.user_offline));
                    }
                    else if (contactConnStatus == UserConnectionStatus.AWAY.ordinal()) {
                        txtConnStatus.setText(getText(R.string.user_away));
                    }
                    else if (contactConnStatus == UserConnectionStatus.ONLINE.ordinal()) {
                        txtConnStatus.setText(getText(R.string.user_online));
                    }
                }
                else {
                    Log.e(TAG, "Null connection status");
                    Toast.makeText(TalkActivity.this, getString(R.string.failure_contact), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Contact status value event listener failed", error.toException());
                Toast.makeText(TalkActivity.this, getString(R.string.failure_contact), Toast.LENGTH_SHORT).show();
                finish();
            }
        };
    }

    // Salva a mensagem para o usuário atual e para o contato no Firestore
    private void sendMessage() {
        String text = editMessage.getText().toString();

        if (!text.isEmpty()) {
            Message message = new Message(currentUid, text);

            WriteBatch batch = FirebaseFirestore.getInstance().batch();

            DocumentReference messageUserReference = talkCurrentUserReference.document();
            batch.set(messageUserReference, message);

            DocumentReference messageContactReference = talkContactReference.document(messageUserReference.getId());
            batch.set(messageContactReference, message);

            if (messageItems.isEmpty()) {
                Map<String, Object> chatInfo = new HashMap<>();
                String name = getString(R.string.name);
                String profileUrl = getString(R.string.profile_url);

                chatInfo.put(name, getIntent().getStringExtra(getString(R.string.user_name)));
                chatInfo.put(profileUrl, getIntent().getStringExtra(getString(R.string.user_profile_url)));
                batch.set(chatContactReference, chatInfo);

                chatInfo.put(getString(R.string.unreadMessages), 0);
                chatInfo.put(name, contactName);
                chatInfo.put(profileUrl, contactProfileUrl);
                batch.set(chatCurrentUserReference, chatInfo);
            }

            batch.update(chatContactReference, getString(R.string.last_message), message);
            batch.update(chatContactReference, getString(R.string.unreadMessages), FieldValue.increment(1));

            batch.update(chatCurrentUserReference, getString(R.string.last_message), message);

            batch.commit().addOnFailureListener(e -> {
                Log.e(TAG, "Send message batch failed", e);
                Toast.makeText(this, getString(R.string.failure_message), Toast.LENGTH_SHORT).show();
            });

            editMessage.setText(null);
        }
    }

    private abstract static class MessageItem extends Item<GroupieViewHolder> {

        private final String text;
        private final String date;

        private MessageItem(Message message) {
            text = message.getText();
            date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(message.getTimestamp());
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {
            TextView tvText = viewHolder.itemView.findViewById(R.id.tv_message_text);
            TextView tvDate = viewHolder.itemView.findViewById(R.id.tv_message_date);

            tvText.setText(text);
            tvDate.setText(date);
        }
    }

    private static class ReceiverMessageItem extends MessageItem {

        private ReceiverMessageItem(Message message) {
            super(message);
        }

        @Override
        public int getLayout() {
            return R.layout.message_receiver;
        }
    }

    private static class SenderMessageItem extends MessageItem {

        private boolean read;

        private SenderMessageItem(Message message) {
            super(message);
            read = message.isRead();
        }

        public void setRead() {
            read = true;
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {
            super.bind(viewHolder, position);
            ImageView ivRead = viewHolder.itemView.findViewById(R.id.iv_sender_read_icon);

            int readIcon = read ? R.drawable.ic_message_read_icon : R.drawable.ic_message_unread_icon;
            ivRead.setImageDrawable(ContextCompat.getDrawable(viewHolder.itemView.getContext(), readIcon));
        }

        @Override
        public int getLayout() {
            return R.layout.message_sender;
        }
    }
}