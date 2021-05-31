package com.example.chatfirebase;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.GroupieViewHolder;
import com.xwray.groupie.Item;

import java.text.DateFormat;
import java.util.LinkedList;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private final DateFormat dateFormatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    private final LinkedList<MessageItem> linkedMessages = new LinkedList<>();

    private String currentUid;
    private String contactId;
    private User currentUser;
    private User contact;
    private SinchService sinchService;
    private CollectionReference conversationsReference;
    private CollectionReference chatsReference;
    private com.google.firebase.database.Query contactQuery;
    private ValueEventListener contactEventListener;
    private com.google.firebase.firestore.Query messagesQuery;
    private EventListener<QuerySnapshot> messagesEventListener;

    private ImageView imgContact;
    private TextView txtNameContact, txtConnStatus;
    private RecyclerView rvChat;
    private EditText editChat;
    private GroupAdapter<GroupieViewHolder> chatAdapter;
    private ImageView vbtSend, vbtCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        currentUid = getIntent().getStringExtra(getString(R.string.extra_user_id));
        contactId = getIntent().getStringExtra(getString(R.string.extra_contact_id));
        String contactName = getIntent().getStringExtra(getString(R.string.extra_contact_name));
        String contactProfileUrl = getIntent().getStringExtra(getString(R.string.extra_contact_profile_url));

        ChatFirebaseApplication application = (ChatFirebaseApplication) getApplication();
        currentUser = application.getCurrentUser();
        contact = new User(contactName, contactProfileUrl);
        sinchService = application.getSinchService();

        conversationsReference = FirebaseFirestore.getInstance().collection(getString(R.string.collection_conversations));
        chatsReference = FirebaseFirestore.getInstance().collection(getString(R.string.collection_chats));

        contactQuery = FirebaseDatabase.getInstance().getReference(getString(R.string.database_users))
                .child(contactId);

        setContactEventListener();

        messagesQuery = conversationsReference.document(currentUid)
                .collection(contactId)
                .orderBy(getString(R.string.message_timestamp), Query.Direction.ASCENDING);

        setMessagesEventListener();

        imgContact = findViewById(R.id.imgContact);
        txtNameContact = findViewById(R.id.txtNameContact);
        txtConnStatus = findViewById(R.id.txt_chat_conn_status);
        rvChat = findViewById(R.id.lsViewChat);
        editChat = findViewById(R.id.edtChat);
        vbtSend = findViewById(R.id.btSend);
        vbtCall = findViewById(R.id.imgCall);

        chatAdapter = new GroupAdapter<>();
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);

        Picasso.get().load(contactProfileUrl).into(imgContact);
        txtNameContact.setText(contactName);

        vbtSend.setOnClickListener(view -> sendMessage());
        vbtCall.setOnClickListener(view -> sinchService.callUser(contactId, contact));
    }

    @Override
    protected void onStart() {
        super.onStart();
        contactQuery.addValueEventListener(contactEventListener);
        messagesQuery.addSnapshotListener(this, messagesEventListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        contactQuery.removeEventListener(contactEventListener);
        linkedMessages.clear();
        chatAdapter.clear();
    }

    public void setContactEventListener() {
        contactEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange: " + snapshot.getKey());
                contact = snapshot.getValue(User.class);

                if (contact != null) {
                    int contactConnStatus = contact.getConnectionStatus();
                    if (contactConnStatus == UserConnectionStatus.OFFLINE.ordinal()) {
                        txtConnStatus.setText(getText(R.string.user_offline));
                    }
                    else if (contactConnStatus == UserConnectionStatus.ABSENT.ordinal()) {
                        txtConnStatus.setText(getText(R.string.user_absent));
                    }
                    else if (contactConnStatus == UserConnectionStatus.ONLINE.ordinal()) {
                        txtConnStatus.setText(getText(R.string.user_online));
                    }
                }
                else {
                    Log.e(TAG, "Null contact");
                    Toast.makeText(ChatActivity.this, getString(R.string.failure_contact), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Contact child event listener failed", error.toException());
                Toast.makeText(ChatActivity.this, getString(R.string.failure_contact), Toast.LENGTH_SHORT).show();
                finish();
            }
        };
    }

    // Exibe todas as mensagens e atualiza a conversa em tempo real
    private void setMessagesEventListener() {
        messagesEventListener = (snapshots, e) -> {
            if (e == null) {
                if (snapshots != null) {
                    for (DocumentChange doc: snapshots.getDocumentChanges()) {
                        Message message = doc.getDocument().toObject(Message.class);

                        if (doc.getType() == DocumentChange.Type.ADDED) {
                            linkedMessages.addLast(new MessageItem(message));
                            rvChat.post(() -> rvChat.smoothScrollToPosition(linkedMessages.size() - 1));
                            chatAdapter.replaceAll(linkedMessages);
                        }
                    }
                }
                else {
                    Log.e(TAG, "Null messages snapshot");
                    Toast.makeText(this, getString(R.string.failure_messages), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            else {
                Log.e(TAG, "Messages snapshot listener failed", e);
                Toast.makeText(this, getString(R.string.failure_messages), Toast.LENGTH_SHORT).show();
                finish();
            }
        };
    }

    // Salva a mensagem para o usuário atual e para o contato no Firestore
    private void sendMessage() {
        String text = editChat.getText().toString();

        if (!text.isEmpty()) {
            Message message = new Message(currentUid, text);

            saveMessageInFirestore(currentUid, contactId, contact, message);
            saveMessageInFirestore(contactId, currentUid, currentUser, message);

            editChat.setText(null);
        }
    }

    // Salva a mensagem na coleção de mensagens e na coleção de caixas de entrada
    private void saveMessageInFirestore(String uid, String contactId, User contact, Message message) {
        conversationsReference.document(uid)
                    .collection(contactId)
                    .add(message)
                    .addOnSuccessListener(documentReference -> {
                        Chat chat = new Chat(contactId, contact, message);

                        chatsReference.document(uid)
                                .collection(getString(R.string.collection_chat))
                                .document(contactId)
                                .set(chat)
                                .addOnFailureListener(e -> Log.e(TAG, "Set " + uid + " chat failed", e));
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Add " + uid + " conversation failed", e);
                        Toast.makeText(this, getString(R.string.failure_message), Toast.LENGTH_SHORT).show();
                    });
    }

    private class MessageItem extends Item<GroupieViewHolder> {

        private final String senderId;
        private final String text;
        private final String date;

        private MessageItem(Message message) {
            senderId = message.getSenderId();
            text = message.getText();
            date = dateFormatter.format(message.getTimestamp());
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {
            TextView txtMessage = viewHolder.itemView.findViewById(R.id.txt_message);

            SpannableString message = new SpannableString(text + "\n" + date + " " + " ");
            int readIcon = R.drawable.message_unread_12;
            int dateStart = text.length() + 1;
            int dateEnd = dateStart + date.length();

            message.setSpan(new AbsoluteSizeSpan(12, true),
                    dateStart, dateEnd, 0);
            message.setSpan(new ForegroundColorSpan(ContextCompat.getColor(ChatActivity.this, R.color.eerie_black)),
                    dateStart, dateEnd, 0);
            message.setSpan(new ImageSpan(ChatActivity.this, readIcon, DynamicDrawableSpan.ALIGN_BASELINE),
                    dateEnd + 1, message.length(), 0);
            message.setSpan(new ForegroundColorSpan(Color.RED),
                    dateEnd + 1, message.length(), 0);

            txtMessage.setText(message);
        }

        @Override
        public int getLayout() {
            return senderId.equals(currentUid)
                    ? R.layout.message_sender
                    : R.layout.message_receiver;
        }
    }
}