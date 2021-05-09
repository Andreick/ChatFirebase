package com.example.chatfirebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.GroupieViewHolder;
import com.xwray.groupie.Item;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class HomeActivity extends AppCompatActivity {

    private Button vButtonLogout;
    private RadioGroup vRadioOptions;
    private RadioButton vInbox, vGroups, vCalls;
    private RecyclerView vViewContacts, vViewInbox;
    private GroupAdapter<GroupieViewHolder> contactsAdapter, InboxAdapter;
    private ImageView vimgProfile;

    private String currentUid;
    private Set<ContactItem> contactItemSet;
    private Map<String, User> contactMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        currentUid = FirebaseAuth.getInstance().getUid();

        if (currentUid == null) {
            goToLoginActivity();
            return;
        }

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

        contactItemSet = new TreeSet<>(new ContactItemComparator());
        contactMap = new HashMap<>();

        fetchContacts();
        fetchInbox();

        vRadioOptions.setOnCheckedChangeListener((group, checkedId) -> onRadioButtonSelected(checkedId));

        vButtonLogout.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            goToLoginActivity();
        });

        contactsAdapter.setOnItemClickListener((item, view) -> goToChatActivity((ContactItem) item));
    }

    private void goToLoginActivity() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);
    }

    private void fetchContacts() {
        FirebaseFirestore.getInstance().collection(getString(R.string.collection_users))
                .orderBy(getString(R.string.user_name))
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e == null) {
                        List<DocumentChange> documentChanges = Objects.requireNonNull(querySnapshot).getDocumentChanges();

                        for (DocumentChange documentChange : documentChanges) {
                            User user = Objects.requireNonNull(documentChange.getDocument().toObject(User.class));

                            if (documentChange.getType() == DocumentChange.Type.ADDED) {

                                if (!currentUid.equals(user.getId())) {
                                    contactItemSet.add(new ContactItem(user));
                                    contactMap.put(user.getId(), user);
                                }
                            }

                            contactsAdapter.update(contactItemSet);
                        }
                    }
                    else {
                        Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e);
                    }
                });
    }

    private void fetchInbox() {
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

    private class ContactItemComparator implements Comparator<ContactItem> {

        @Override
        public int compare(ContactItem ci1, ContactItem ci2) {
            return ci1.contact.getName().compareTo(ci2.contact.getName());
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


