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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.GroupieViewHolder;
import com.xwray.groupie.Item;

import java.util.List;
import java.util.Objects;

public class ChatActivity extends AppCompatActivity {

    private ImageView vImgContact;
    private TextView vtxtNameContact;
    private RecyclerView vlsViewChat;
    private EditText vEditChat;
    private GroupAdapter<GroupieViewHolder> adapter;
    private ImageView vbtSend;

    private User contact;
    private String currentUid;
    private CollectionReference conversations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        vImgContact = findViewById(R.id.imgContact);
        vtxtNameContact = findViewById(R.id.txtNameContact);
        vlsViewChat = findViewById(R.id.lsViewChat);
        vbtSend = findViewById(R.id.btSend);
        vEditChat = findViewById(R.id.edtChat);

        contact = getIntent().getExtras().getParcelable(getString(R.string.extra_contact));
        Picasso.get().load(contact.getProfileUrl()).into(vImgContact);
        vtxtNameContact.setText(contact.getName());

        currentUid = FirebaseAuth.getInstance().getUid();
        conversations = FirebaseFirestore.getInstance().collection(getString(R.string.collection_conversations));

        adapter = new GroupAdapter<>();
        vlsViewChat.setLayoutManager(new LinearLayoutManager(this));
        vlsViewChat.setAdapter(adapter);

        fetchMessages();

        vbtSend.setOnClickListener(view -> sendMessage());
    }

    // Exibe todas as mensagens da conversa
    private void fetchMessages() {
        conversations.document(currentUid)
                .collection(contact.getId())
                .orderBy(getString(R.string.message_timestamp), Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e == null) {
                        List<DocumentChange> documentChanges = Objects.requireNonNull(querySnapshot).getDocumentChanges();

                        for (DocumentChange doc: documentChanges){
                            if (doc.getType() == DocumentChange.Type.ADDED){
                                Message message = Objects.requireNonNull(doc.getDocument().toObject(Message.class));
                                adapter.add(new MessageItem(message));
                            }
                        }
                    }
                    else {
                        Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e);
                    }
                });
    }

    // Salva a mensagem para o usuário atual e para o contato no Firestore
    private void sendMessage() {
        String text = vEditChat.getText().toString();

        if (!text.isEmpty()) {
            Message message = new Message(currentUid, text);

            saveMessageInFirestore(currentUid, contact.getId(), message);
            saveMessageInFirestore(contact.getId(), currentUid, message);

            vEditChat.setText(null);
        }
    }

    // Salva a mensagem na coleção de mensagens e na coleção de caixas de entrada
    private void saveMessageInFirestore(String uid, String contactId, Message message) {
        conversations.document(uid)
                    .collection(contactId)
                    .add(message)
                    .addOnSuccessListener(documentReference -> {
                        InboxMessage inboxMessage = new InboxMessage(contactId, message);

                        FirebaseFirestore.getInstance().collection(getString(R.string.collection_inboxes))
                                .document(uid)
                                .collection(getString(R.string.collection_inbox_message))
                                .document(contactId)
                                .set(inboxMessage)
                                .addOnFailureListener(e -> Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e));
                    })
                    .addOnFailureListener(e -> Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e));
    }

    // Balão de mensagem
    private class MessageItem extends Item<GroupieViewHolder> {

        private final Message message;

        private MessageItem(Message message) {
            this.message = message;
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {
            TextView txtMessage = viewHolder.itemView.findViewById(R.id.txtMessage);

            txtMessage.setText(message.getText());
        }

        @Override
        public int getLayout() {
            return message.getSenderId().equals(currentUid)
                    ? R.layout.message_users
                    : R.layout.message_contacts;
        }
    }
}