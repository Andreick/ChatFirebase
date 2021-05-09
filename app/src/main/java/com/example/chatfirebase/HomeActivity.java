package com.example.chatfirebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.GroupieViewHolder;
import com.xwray.groupie.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private Button vButtonLogout;
    private RadioGroup vRadioOptions;
    private RadioButton vInbox, vGroups, vCalls;
    private RecyclerView vViewContacts, vViewInbox;
    private GroupAdapter<GroupieViewHolder> contactsAdapter, InboxAdapter;
    private ImageView vimgProfile;

    private final Map<String, User> contactMap = new HashMap<>();
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        currentUid = FirebaseAuth.getInstance().getUid();

        if (currentUid == null) goToLoginActivity();

        vButtonLogout = findViewById(R.id.btLogout);
        vRadioOptions = findViewById(R.id.rdOptions);
        vInbox = findViewById(R.id.btInbox);
        vGroups = findViewById(R.id.btContacts);
        vCalls = findViewById(R.id.btCalls);
        vViewContacts = findViewById(R.id.lsContacts);
        vViewInbox = findViewById(R.id.lsInbox);
        vimgProfile = findViewById(R.id.imgProfile);

        contactsAdapter = new GroupAdapter<>();
        vViewContacts.setLayoutManager(new LinearLayoutManager(this));
        vViewContacts.setAdapter(contactsAdapter);

        InboxAdapter = new GroupAdapter<>();
        vViewInbox.setLayoutManager(new LinearLayoutManager(this));
        vViewInbox.setAdapter(InboxAdapter);

        fetchUsers();
        fetchMessages();

        vButtonLogout.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            goToLoginActivity();
        });

        vRadioOptions.setOnCheckedChangeListener((group, checkedId) -> onRadioButtonSelected(checkedId));

        contactsAdapter.setOnItemClickListener((item, view) -> goToChatActivity((ContactItem) item));
    }

    private void goToLoginActivity() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);
    }

    private void fetchMessages() {
        final String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("/last-messages")
                .document(uid)
                .collection("/contacts")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        List<DocumentChange> documentChanges = value.getDocumentChanges();
                        if (documentChanges != null){
                            for (DocumentChange doc: documentChanges){
                                if (doc.getType() == DocumentChange.Type.ADDED){
                                    InboxMessage inboxMessage = doc.getDocument().toObject(InboxMessage.class);

                                    InboxAdapter.add(new InboxItem(inboxMessage));
                                }
                            }
                        }
                    }
                });
    }

    private void fetchUsers() {

        FirebaseFirestore.getInstance().collection("/users")
            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                    if (error != null){

                        return;
                    }
                    final List<DocumentSnapshot> docs = value.getDocuments();
                    for (DocumentSnapshot doc: docs){
                        User user = doc.toObject(User.class);
                        contactsAdapter.add(new ContactItem(user));
                        contactsAdapter.notifyDataSetChanged();
                    }
                }
            });
    }

    private void goToChatActivity(ContactItem contactItem) {
        Intent chatIntent = new Intent(HomeActivity.this, ChatActivity.class);
        chatIntent.putExtra(getString(R.string.extra_contact), contactItem.contact);
        startActivity(chatIntent);
    }

    private void onRadioButtonSelected(int checkedId) {
        if (checkedId == R.id.btInbox) {
            vViewInbox.setVisibility(View.VISIBLE);
            vViewContacts.setVisibility(View.INVISIBLE);

        }
        if (checkedId == R.id.btContacts){
            vViewInbox.setVisibility(View.INVISIBLE);
            vViewContacts.setVisibility(View.VISIBLE);

        }
    }

    private class ContactItem extends Item<GroupieViewHolder> {

        private final User contact;

        public ContactItem(User contact) {
            this.contact = contact;
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {
            TextView txtUserNm = viewHolder.itemView.findViewById(R.id.txtUserName);
            ImageView imgPhoto = viewHolder.itemView.findViewById(R.id.imgUserPhoto);

            Picasso.get().load(contact.getProfileUrl()).into(imgPhoto);
            txtUserNm.setText(contact.getName());
        }

        @Override
        public int getLayout() {
            return R.layout.card_user;
        }
    }

    private class InboxItem extends Item<GroupieViewHolder>{

        private final InboxMessage inboxMessage;
        private User contact;

        private InboxItem(InboxMessage inboxMessage) { this.inboxMessage = inboxMessage; }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {
            TextView username = viewHolder.itemView.findViewById(R.id.txtUserName2);
            TextView message = viewHolder.itemView.findViewById(R.id.txtLastMessage2);
            ImageView imgPhoto = viewHolder.itemView.findViewById(R.id.imgUserPhoto2);

            //username.setText(contactChat.getContactName());
            message.setText(inboxMessage.getText());
            //Picasso.get().load(contactChat.getProfileUrl()).into(imgPhoto);
        }

        @Override
        public int getLayout() {
            return R.layout.card_user_inbox;
        }
    }
}


