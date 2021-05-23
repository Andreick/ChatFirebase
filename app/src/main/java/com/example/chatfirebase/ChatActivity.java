package com.example.chatfirebase;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.util.LinkedList;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private final LinkedList<MessageItem> linkedMessages = new LinkedList<>();

    private User currentUser;
    private Query contactQuery;
    private EventListener<QuerySnapshot> contactEventListener;
    private CollectionReference conversationsReference;
    private Query messagesQuery;
    private EventListener<QuerySnapshot> messagesEventListener;
    private User contact;

    private ImageView imgContact;
    private TextView txtNameContact;
    private RecyclerView rvChat;
    private EditText editChat;
    private GroupAdapter<GroupieViewHolder> chatAdapter;
    private ImageView vbtSend, vbtCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ChatFirebaseApplication application = (ChatFirebaseApplication) getApplication();
        currentUser = application.getCurrentUser();

        String contactId = getIntent().getStringExtra(getString(R.string.user_id));

        contactQuery = FirebaseFirestore.getInstance().collection(getString(R.string.collection_users))
                .whereEqualTo(getString(R.string.user_id), contactId);

        setContactEventListener();

        conversationsReference = FirebaseFirestore.getInstance().collection(getString(R.string.collection_conversations));

        messagesQuery = conversationsReference.document(currentUser.getId())
                .collection(contactId)
                .orderBy(getString(R.string.message_timestamp), Query.Direction.ASCENDING);

        setMessagesEventListener();

        imgContact = findViewById(R.id.imgContact);
        txtNameContact = findViewById(R.id.txtNameContact);
        rvChat = findViewById(R.id.lsViewChat);
        editChat = findViewById(R.id.edtChat);
        vbtSend = findViewById(R.id.btSend);
        vbtCall = findViewById(R.id.imgCall);

        chatAdapter = new GroupAdapter<>();
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);

        vbtSend.setEnabled(false);
        vbtCall.setEnabled(false);

        SinchService sinchService = application.getSinchService();

        vbtSend.setOnClickListener(view -> sendMessage());
        vbtCall.setOnClickListener(view -> sinchService.callUser(contact));
    }

    @Override
    protected void onStart() {
        super.onStart();
        contactQuery.addSnapshotListener(this, contactEventListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        linkedMessages.clear();
        vbtSend.setEnabled(false);
        vbtCall.setEnabled(false);
    }

    // Atualiza os dados do contato em tempo real
    private void setContactEventListener() {
        contactEventListener = (snapshots, e) -> {
            if (e == null) {
                if (snapshots != null) {
                    for (DocumentChange doc : snapshots.getDocumentChanges()) {
                        switch (doc.getType()) {
                            case ADDED:
                                contact = doc.getDocument().toObject(User.class);
                                messagesQuery.addSnapshotListener(this, messagesEventListener);

                                Picasso.get().load(contact.getProfileUrl()).into(imgContact);
                                txtNameContact.setText(contact.getName());

                                vbtSend.setEnabled(true);
                                vbtCall.setEnabled(true);
                                break;
                            case MODIFIED:
                                break;
                        }
                    }
                }
            }
            else {
                Log.e(TAG, "Contact snapshot listener failed", e);
                Toast.makeText(this, "Failed to load contact", Toast.LENGTH_SHORT).show();
                finish();
            }
        };
    }

    // Exibe todas as mensagens e atualiza a conversa em tempo real
    private void setMessagesEventListener() {
        messagesEventListener = (snapshots, e) -> {
            if (e == null) {
                if (snapshots != null) {
                    for (DocumentChange doc: snapshots.getDocumentChanges()){
                        if (doc.getType() == DocumentChange.Type.ADDED){
                            Message message = doc.getDocument().toObject(Message.class);
                            linkedMessages.addLast(new MessageItem(message));
                        }
                    }

                    rvChat.smoothScrollToPosition(linkedMessages.size() - 1);
                    chatAdapter.replaceAll(linkedMessages);
                }
            }
            else {
                Log.e(TAG, "Messages snapshot listener failed", e);
                Toast.makeText(this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                finish();
            }
        };
    }

    // Salva a mensagem para o usuário atual e para o contato no Firestore
    private void sendMessage() {
        String text = editChat.getText().toString();

        if (!text.isEmpty()) {
            Message message = new Message(currentUser.getId(), text);

            saveMessageInFirestore(currentUser.getId(), contact, message);
            saveMessageInFirestore(contact.getId(), currentUser, message);

            editChat.setText(null);
        }
    }

    // Salva a mensagem na coleção de mensagens e na coleção de caixas de entrada
    private void saveMessageInFirestore(String uid, User contact, Message message) {
        conversationsReference.document(uid)
                    .collection(contact.getId())
                    .add(message)
                    .addOnSuccessListener(documentReference -> {
                        InboxMessage inboxMessage = new InboxMessage(contact, message);

                        FirebaseFirestore.getInstance().collection(getString(R.string.collection_inboxes))
                                .document(uid)
                                .collection(getString(R.string.collection_inbox_message))
                                .document(contact.getId())
                                .set(inboxMessage)
                                .addOnFailureListener(e -> Log.e(TAG, "Set " + uid + " inbox failed", e));
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Add " + uid + " conversation failed", e));
    }

    private class MessageItem extends Item<GroupieViewHolder> {

        private final String senderId;
        private final String text;

        private MessageItem(Message message) {
            senderId = message.getSenderId();
            text = message.getText();
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {
            TextView txtMessage = viewHolder.itemView.findViewById(R.id.txtMessage);

            txtMessage.setText(text);
        }

        @Override
        public int getLayout() {
            return senderId.equals(currentUser.getId())
                    ? R.layout.message_users
                    : R.layout.message_contacts;
        }
    }
}