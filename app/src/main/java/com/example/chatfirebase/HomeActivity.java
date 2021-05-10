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
import com.google.firebase.firestore.Query;
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
    private GroupAdapter<GroupieViewHolder> contactsAdapter, inboxAdapter;
    private ImageView vimgProfile;

    private String currentUid;
    private Set<ContactItem> contactItemSet;
    private Map<String, InboxItem> inboxItemMap;

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

        inboxAdapter = new GroupAdapter<>();
        vViewInbox.setLayoutManager(new LinearLayoutManager(this));
        vViewInbox.setAdapter(inboxAdapter);

        contactItemSet = new TreeSet<>(new ContactItemComparator());
        inboxItemMap = new HashMap<>();

        fetchContacts();
        fetchInbox();

        vButtonLogout.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            goToLoginActivity();
        });

        contactsAdapter.setOnItemClickListener((item, view) -> {
            ContactItem contactItem = (ContactItem) item;
            goToChatActivity(contactItem.contact);
        });

        inboxAdapter.setOnItemClickListener((item, view) -> {
            InboxItem inboxItem = (InboxItem) item;
            goToChatActivity(inboxItem.contact);
        });

        vRadioOptions.setOnCheckedChangeListener((group, checkedId) -> onRadioButtonSelected(checkedId));
    }

    private void goToLoginActivity() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);
    }

    // Exibe todos os usuários cadastrados no Firebase
    private void fetchContacts() {
        FirebaseFirestore.getInstance().collection(getString(R.string.collection_users))
                .orderBy(getString(R.string.user_name), Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e == null) {
                        List<DocumentChange> documentChanges = Objects.requireNonNull(querySnapshot).getDocumentChanges();

                        for (DocumentChange documentChange : documentChanges) {
                            User user = Objects.requireNonNull(documentChange.getDocument().toObject(User.class));

                            if (documentChange.getType() == DocumentChange.Type.ADDED) {

                                if (!currentUid.equals(user.getId())) {
                                    contactItemSet.add(new ContactItem(user));
                                    inboxItemMap.put(user.getId(), new InboxItem(user));
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

    // Exibe a caixa de entrada com o contato e a última mensagem da conversa
    private void fetchInbox() {
        FirebaseFirestore.getInstance().collection(getString(R.string.collection_inboxes))
                .document(currentUid)
                .collection(getString(R.string.collection_inbox_message))
                .orderBy(getString(R.string.message_timestamp), Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e == null) {
                        List<DocumentChange> documentChanges = Objects.requireNonNull(querySnapshot).getDocumentChanges();

                        for (DocumentChange documentChange : documentChanges) {
                            InboxMessage inboxMessage = Objects.requireNonNull(documentChange.getDocument().toObject(InboxMessage.class));

                            InboxItem inboxItem = Objects.requireNonNull(inboxItemMap.get(inboxMessage.getContactId()));
                            inboxItem.setLastMessage(inboxMessage);

                            switch (documentChange.getType()) {
                                case MODIFIED:
                                    inboxAdapter.remove(inboxItem);
                                    inboxAdapter.add(0, inboxItem);
                                    break;
                                case ADDED:
                                    inboxAdapter.add(0, inboxItem);
                                    break;
                            }
                        }
                    }
                    else {
                        Log.e(getString(R.string.log_tag), getString(R.string.log_msg), e);
                    }
                });
    }

    private void goToChatActivity(User contact) {
        Intent chatIntent = new Intent(HomeActivity.this, ChatActivity.class);
        chatIntent.putExtra(getString(R.string.extra_contact), contact);
        startActivity(chatIntent);
    }

    // Troca para o RecyclerView selecionado
    private void onRadioButtonSelected(int checkedId) {
        if (checkedId == R.id.btInbox) {
            vViewInbox.setVisibility(View.VISIBLE);
            vViewContacts.setVisibility(View.INVISIBLE);

        }
        else if (checkedId == R.id.btContacts) {
            vViewInbox.setVisibility(View.INVISIBLE);
            vViewContacts.setVisibility(View.VISIBLE);

        }
    }

    // Contato na tela de contatos
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

    // Compara os ContactItems de acordo com o nome do usuário
    private class ContactItemComparator implements Comparator<ContactItem> {

        @Override
        public int compare(ContactItem ci1, ContactItem ci2) {
            return ci1.contact.getName().compareTo(ci2.contact.getName());
        }
    }

    // Contato na caixa de entrada
    private class InboxItem extends Item<GroupieViewHolder>{

        private final User contact;
        private Message lastMessage;

        private InboxItem(final User contact) { this.contact = contact; }

        public void setLastMessage(Message lastMessage) {
            this.lastMessage = lastMessage;
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {
            TextView username = viewHolder.itemView.findViewById(R.id.txtUserName2);
            TextView message = viewHolder.itemView.findViewById(R.id.txtLastMessage2);
            ImageView imgPhoto = viewHolder.itemView.findViewById(R.id.imgUserPhoto2);

            Picasso.get().load(contact.getProfileUrl()).into(imgPhoto);
            username.setText(contact.getName());
            message.setText(lastMessage.getText());
        }

        @Override
        public int getLayout() {
            return R.layout.card_user_inbox;
        }
    }
}


