package com.example.chatfirebase;

import androidx.annotation.NonNull;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.GroupieViewHolder;
import com.xwray.groupie.Item;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class HomeActivity extends AppCompatActivity {

    private Button vButtonLogout;
    private RadioGroup vRadioOptions;
    private RadioButton vInbox, vContacts, vCalls;
    private RecyclerView vViewContacts, vViewInbox;
    private GroupAdapter<GroupieViewHolder> contactsAdapter, inboxAdapter;
    private ImageView vimgProfile;

    private static String currentUid;
    private ChatFirebase chatFirebase;
    private Map<String, InboxItem> inboxItemMap;
    private LinkedList<InboxItem> linkedInboxItems;
    private Set<ContactItem> contactItemSet;
    private ListenerRegistration inboxRegistration;
    private ListenerRegistration contactsRegistration;

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
        vContacts = findViewById(R.id.btContacts);
        vCalls = findViewById(R.id.btCalls);
        vViewContacts = findViewById(R.id.lsContacts);
        vViewInbox = findViewById(R.id.lsInbox);
        vimgProfile = findViewById(R.id.imgProfile);

        inboxAdapter = new GroupAdapter<>();
        vViewInbox.setLayoutManager(new LinearLayoutManager(this));
        vViewInbox.setAdapter(inboxAdapter);

        contactsAdapter = new GroupAdapter<>();
        vViewContacts.setLayoutManager(new LinearLayoutManager(this));
        vViewContacts.setAdapter(contactsAdapter);

        inboxItemMap = new HashMap<>();
        linkedInboxItems = new LinkedList<>();
        contactItemSet = new TreeSet<>(new ContactItemComparator());

        chatFirebase = (ChatFirebase) getApplicationContext();
        chatFirebase.setup();

        vButtonLogout.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            goToLoginActivity();
        });

        contactsAdapter.setOnItemClickListener((item, view) -> {
            ContactItem contactItem = (ContactItem) item;
            goToChatActivity(contactItem.contactId);
        });

        inboxAdapter.setOnItemClickListener((item, view) -> {
            InboxItem inboxItem = (InboxItem) item;
            goToChatActivity(inboxItem.contactId);
        });

        vRadioOptions.setOnCheckedChangeListener((group, checkedId) -> onRadioButtonSelected(checkedId));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (vInbox.isChecked()) fetchInbox();
        if (vContacts.isChecked()) fetchContacts();
    }

    @Override
    protected void onStop() {
        super.onStop();
        linkedInboxItems.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatFirebase != null) chatFirebase.terminate();
    }

    private void goToLoginActivity() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);
    }

    // Atualiza a caixa de entrada em tempo real
    private void fetchInbox() {
        inboxRegistration = FirebaseFirestore.getInstance().collection(getString(R.string.collection_inboxes))
                .document(currentUid)
                .collection(getString(R.string.collection_inbox_message))
                .orderBy(getString(R.string.message_timestamp), Query.Direction.ASCENDING)
                .addSnapshotListener(this, (snapshots, e) -> {
                    if (e == null) {
                        if (snapshots != null) {
                            for (DocumentChange doc : snapshots.getDocumentChanges()) {
                                InboxMessage inboxMessage = doc.getDocument().toObject(InboxMessage.class);

                                switch (doc.getType()) {
                                    case ADDED:
                                        InboxItem newInboxItem = new InboxItem(inboxMessage);
                                        inboxItemMap.put(inboxMessage.getContactId(), newInboxItem);
                                        linkedInboxItems.addFirst(newInboxItem);
                                        break;
                                    case MODIFIED:
                                        InboxItem inboxItem = Objects.requireNonNull(inboxItemMap.get(inboxMessage.getContactId()));
                                        linkedInboxItems.remove(inboxItem);
                                        inboxItem.setMessage(inboxMessage);
                                        linkedInboxItems.addFirst(inboxItem);
                                        break;
                                }

                                inboxAdapter.update(linkedInboxItems, false);
                                contactsAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                    else {
                        Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e);
                    }
                });
    }

    // Atualiza os contatos em tempo real
    private void fetchContacts() {
        contactsRegistration = FirebaseFirestore.getInstance().collection(getString(R.string.collection_users))
                .whereNotEqualTo(getString(R.string.user_id), currentUid)
                .addSnapshotListener(this, (snapshots, e) -> {
                    if (e == null) {
                        if (snapshots != null) {
                            for (DocumentChange doc : snapshots.getDocumentChanges()) {
                                User contact = doc.getDocument().toObject(User.class);

                                if (doc.getType() == DocumentChange.Type.ADDED) {
                                    contactItemSet.add(new ContactItem(contact));
                                }

                                contactsAdapter.update(contactItemSet);
                                contactsAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                    else {
                        Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e);
                    }
                });
    }

    private void goToChatActivity(String contactId) {
        Intent chatIntent = new Intent(HomeActivity.this, ChatActivity.class);
        chatIntent.putExtra(getString(R.string.extra_contact), contactId);
        startActivity(chatIntent);
    }

    // Troca para o RecyclerView selecionado
    private void onRadioButtonSelected(int checkedId) {
        if (checkedId == R.id.btInbox) {
            inboxRegistration.remove();
            fetchInbox();
            if (contactsRegistration != null) contactsRegistration.remove();
            vViewInbox.setVisibility(View.VISIBLE);
            vViewContacts.setVisibility(View.INVISIBLE);

        }
        else if (checkedId == R.id.btContacts) {
            if (contactsRegistration != null) contactsRegistration.remove();
            fetchContacts();
            inboxRegistration.remove();
            linkedInboxItems.clear();
            vViewInbox.setVisibility(View.INVISIBLE);
            vViewContacts.setVisibility(View.VISIBLE);

        }
    }

    // Contato na tela de contatos
    private static class ContactItem extends Item<GroupieViewHolder> {

        private final String contactId;
        private final String contactProfileUrl;
        private final String contactName;

        public ContactItem(User contact) {
            contactId = contact.getId();
            contactProfileUrl = contact.getProfileUrl();
            contactName = contact.getName();
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {
            TextView txtUserNm = viewHolder.itemView.findViewById(R.id.txtUserName);
            ImageView imgPhoto = viewHolder.itemView.findViewById(R.id.imgUserPhoto);

            Picasso.get().load(contactProfileUrl).into(imgPhoto);
            txtUserNm.setText(contactName);
        }

        @Override
        public int getLayout() {
            return R.layout.card_user;
        }
    }

    // Compara os ContactItems de acordo com o nome do usuário
    private static class ContactItemComparator implements Comparator<ContactItem> {

        @Override
        public int compare(ContactItem ci1, ContactItem ci2) {
            return ci1.contactName.compareTo(ci2.contactName);
        }
    }

    // Contato na caixa de entrada
    private static class InboxItem extends Item<GroupieViewHolder>{

        private final String contactId;
        private final String contactProfileUrl;
        private final String contactName;
        private String senderId;
        private String lastMessage;

        public InboxItem(InboxMessage inboxMessage) {
            contactId = inboxMessage.getContactId();
            contactProfileUrl = inboxMessage.getContactProfileUrl();
            contactName = inboxMessage.getContactName();
            senderId = inboxMessage.getSenderId();
            lastMessage = inboxMessage.getText();
        }

        public void setMessage(Message message) {
            senderId = message.getSenderId();
            lastMessage = message.getText();
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {
            TextView username = viewHolder.itemView.findViewById(R.id.txtUserName2);
            TextView message = viewHolder.itemView.findViewById(R.id.txtLastMessage2);
            ImageView imgPhoto = viewHolder.itemView.findViewById(R.id.imgUserPhoto2);

            Picasso.get().load(contactProfileUrl).into(imgPhoto);

            String text = lastMessage;

            if (!currentUid.equals(senderId)) {
                text = contactName + ": " + text;
            }

            username.setText(contactName);
            message.setText(text);
        }

        @Override
        public int getLayout() {
            return R.layout.card_user_inbox;
        }
    }
}


