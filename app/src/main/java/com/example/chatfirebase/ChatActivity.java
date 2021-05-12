package com.example.chatfirebase;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.GroupieViewHolder;
import com.xwray.groupie.Item;

import java.util.LinkedList;

public class ChatActivity extends AppCompatActivity {

    private ImageView vImgContact;
    private TextView vTxtNameContact;
    private RecyclerView vlsViewChat;
    private EditText vEditChat;
    private GroupAdapter<GroupieViewHolder> adapter;
    private ImageView vbtSend, vbtCall;

    private static User currentUser;
    private User contact;
    private CollectionReference conversations;
    private LinkedList<MessageItem> linkedMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        vImgContact = findViewById(R.id.imgContact);
        vTxtNameContact = findViewById(R.id.txtNameContact);
        vlsViewChat = findViewById(R.id.lsViewChat);
        vEditChat = findViewById(R.id.edtChat);
        vbtSend = findViewById(R.id.btSend);
        vbtCall = findViewById(R.id.imgCall);

        adapter = new GroupAdapter<>();
        vlsViewChat.setLayoutManager(new LinearLayoutManager(this));
        vlsViewChat.setAdapter(adapter);

        ChatFirebase chatFirebase = (ChatFirebase) getApplicationContext();
        currentUser = chatFirebase.getCurrentUser();
        conversations = FirebaseFirestore.getInstance().collection(getString(R.string.collection_conversations));
        linkedMessages = new LinkedList<>();

        vbtSend.setOnClickListener(view -> sendMessage());
        vbtCall.setOnClickListener(view -> {
            if (contact != null) chatFirebase.callContact(contact);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        fetchContact();
    }

    @Override
    protected void onStop() {
        super.onStop();
        linkedMessages.clear();
    }

    // Atualiza os dados do contato em tempo real
    private void fetchContact() {
        String contactId = getIntent().getStringExtra(getString(R.string.extra_contact));
        FirebaseFirestore.getInstance().collection(getString(R.string.collection_users))
                .whereEqualTo(getString(R.string.user_id), contactId)
                .addSnapshotListener(this, (snapshots, e) -> {
                    if (e == null) {
                        if (snapshots != null) {
                            for (DocumentChange doc : snapshots.getDocumentChanges()) {
                                switch (doc.getType()) {
                                    case ADDED:
                                        contact = doc.getDocument().toObject(User.class);
                                        fetchMessages();
                                        Picasso.get().load(contact.getProfileUrl()).into(vImgContact);
                                        vTxtNameContact.setText(contact.getName());
                                        break;
                                    case MODIFIED:
                                        break;
                                }
                            }
                        }
                    }
                    else {
                        Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e);
                    }
                });
    }

    // Exibe todas as mensagens e atualiza a conversa em tempo real
    private void fetchMessages() {
        conversations.document(currentUser.getId())
                .collection(contact.getId())
                .orderBy(getString(R.string.message_timestamp), Query.Direction.ASCENDING)
                .addSnapshotListener(this, (snapshots, e) -> {
                    if (e == null) {
                        if (snapshots != null) {
                            for (DocumentChange doc: snapshots.getDocumentChanges()){
                                if (doc.getType() == DocumentChange.Type.ADDED){
                                    Message message = doc.getDocument().toObject(Message.class);
                                    linkedMessages.addLast(new MessageItem(message));
                                }
                            }

                            adapter.replaceAll(linkedMessages);
                        }
                    }
                    else {
                        Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e);
                    }
                });
    }

    // Salva a mensagem para o usuário atual e para o contato no Firestore
    private void sendMessage() {
        if (contact != null) {
            String text = vEditChat.getText().toString();

            if (!text.isEmpty()) {
                Message message = new Message(currentUser.getId(), text);

                saveMessageInFirestore(currentUser.getId(), contact, message);
                saveMessageInFirestore(contact.getId(), currentUser, message);

                vEditChat.setText(null);
            }
        }
    }

    // Salva a mensagem na coleção de mensagens e na coleção de caixas de entrada
    private void saveMessageInFirestore(String uid, User contact, Message message) {
        conversations.document(uid)
                    .collection(contact.getId())
                    .add(message)
                    .addOnSuccessListener(documentReference -> {
                        InboxMessage inboxMessage = new InboxMessage(contact, message);

                        FirebaseFirestore.getInstance().collection(getString(R.string.collection_inboxes))
                                .document(uid)
                                .collection(getString(R.string.collection_inbox_message))
                                .document(contact.getId())
                                .set(inboxMessage)
                                .addOnFailureListener(e -> Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e));
                    })
                    .addOnFailureListener(e -> Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e));
    }

    // Balão de mensagem
    private static class MessageItem extends Item<GroupieViewHolder> {

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