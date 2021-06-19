package com.example.chatfirebase.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatfirebase.R;
import com.example.chatfirebase.data.User;
import com.example.chatfirebase.data.UserConnectionStatus;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.GroupieViewHolder;
import com.xwray.groupie.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class ContactsFragment extends Fragment {

    private static final String TAG = "ContactsFragment";

    private final Map<String, ContactItem> contactItemMap = new HashMap<>();
    private final TreeSet<ContactItem> contactItemSet = new TreeSet<>();
    private final List<ContactItem> contactItems = new ArrayList<>();

    private static String currentUid;
    private DatabaseReference usersReference;
    private ChildEventListener contactsEventListener;

    private Context context;
    private RecyclerView recyclerView;
    private GroupAdapter<GroupieViewHolder> contactsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentUid = FirebaseAuth.getInstance().getUid();
        usersReference = FirebaseDatabase.getInstance().getReference(getString(R.string.database_users));
        setContactsEventListener();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        context = view.getContext();
        recyclerView = view.findViewById(R.id.rv_contacts);
        contactsAdapter = new GroupAdapter<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(contactsAdapter);

        contactsAdapter.setOnItemClickListener(new OnContactItemClickListener());
    }

    @Override
    public void onStart() {
        super.onStart();
        usersReference.addChildEventListener(contactsEventListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        usersReference.removeEventListener(contactsEventListener);
        contactItemSet.clear();
        contactsAdapter.clear();
    }

    private void setContactsEventListener() {
        contactsEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "onChildAdded: " + snapshot.getKey());
                String uid = snapshot.getKey();

                if (uid != null && !uid.equals(currentUid)) {
                    User contact = snapshot.getValue(User.class);
                    if (contact != null) {
                        ContactItem contactItem = new ContactItem(uid, contact);
                        contactItemMap.put(uid, contactItem);
                        contactItemSet.add(contactItem);
                        Collections.sort(contactItems);
                        contactsAdapter.update(contactItemSet);
                        contactsAdapter.notifyDataSetChanged();
                    }
                    else {
                        Log.e(TAG, "Null contact");
                        Toast.makeText(context, getString(R.string.failure_contact), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "onChildChanged: " + snapshot.getKey());
                String uid = snapshot.getKey();

                if (uid != null && !uid.equals(currentUid)) {
                    User changedContact = snapshot.getValue(User.class);
                    ContactItem contactItem = contactItemMap.get(uid);
                    if (changedContact != null && contactItem != null) {
                        contactItemSet.remove(contactItem);
                        contactItem.update(changedContact);
                        contactItemSet.add(contactItem);
                        contactsAdapter.update(contactItemSet);
                        contactsAdapter.notifyDataSetChanged();
                    }
                    else {
                        Log.e(TAG, "Null contact or null contact item");
                        Toast.makeText(context, getString(R.string.failure_contact), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onChildRemoved: " + snapshot.getKey());
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "onChildMoved: " + snapshot.getKey());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Contacts child event listener failed", error.toException());
                Toast.makeText(context, getString(R.string.failure_contact), Toast.LENGTH_SHORT).show();
            }
        };
    }

    private static class ContactItem extends Item<GroupieViewHolder> implements Comparable<ContactItem> {

        private final String contactId;
        private final String contactProfileUrl;
        private final String contactName;
        private int contactConnStatus;

        public ContactItem(String uid, User contact) {
            contactId = uid;
            contactProfileUrl = contact.getProfileUrl();
            contactName = contact.getName();
            contactConnStatus = contact.getConnectionStatus();
        }

        public void update(User contact) {
            contactConnStatus = contact.getConnectionStatus();
        }

        @Override
        public void bind(GroupieViewHolder viewHolder, int position) {
            ImageView imgPhoto = viewHolder.itemView.findViewById(R.id.civ_card_contact_photo);
            ImageView imgConnStatus = viewHolder.itemView.findViewById(R.id.civ_contact_conn_status);
            TextView txtUserNm = viewHolder.itemView.findViewById(R.id.tv_contact_username);
            TextView txtConnStatus = viewHolder.itemView.findViewById(R.id.iv_contact_conn_status);

            Picasso.get().load(contactProfileUrl).placeholder(R.drawable.profile_placeholder).into(imgPhoto);
            txtUserNm.setText(contactName);

            Context context = viewHolder.itemView.getContext();
            int connColor;

            if (contactConnStatus == UserConnectionStatus.ONLINE.ordinal()) {
                txtConnStatus.setText(context.getText(R.string.user_online));
                connColor = R.color.green;
            }
            else if (contactConnStatus == UserConnectionStatus.ABSENT.ordinal()) {
                txtConnStatus.setText(context.getText(R.string.user_absent));
                connColor = R.color.orange;
            }
            else {
                txtConnStatus.setText(context.getText(R.string.user_offline));
                connColor = R.color.red;
            }

            txtConnStatus.setTextColor(ContextCompat.getColor(context, connColor));
            imgConnStatus.setImageDrawable(ContextCompat.getDrawable(context, connColor));
        }

        @Override
        public int getLayout() {
            return R.layout.card_contact;
        }

        @Override
        public int compareTo(ContactItem ci) {
            if (this == ci) return 0;

            int connStatusDiff = ci.contactConnStatus - contactConnStatus;
            if (connStatusDiff != 0) return connStatusDiff;

            int contactNameDiff = contactName.compareTo(ci.contactName);
            if (contactNameDiff != 0) return contactNameDiff;

            return contactId.compareTo(ci.contactId);
        }
    }

    private static class OnContactItemClickListener implements com.xwray.groupie.OnItemClickListener {

        @Override
        public void onItemClick(@NonNull Item item, @NonNull View view) {
            ContactItem contactItem = (ContactItem) item;
            Context context = view.getContext();

            Intent chatIntent = new Intent(context, TalkActivity.class);
            chatIntent.putExtra(context.getString(R.string.extra_user_id), currentUid);
            chatIntent.putExtra(context.getString(R.string.extra_contact_id), contactItem.contactId);
            chatIntent.putExtra(context.getString(R.string.extra_contact_name), contactItem.contactName);
            chatIntent.putExtra(context.getString(R.string.extra_contact_profile_url), contactItem.contactProfileUrl);
            context.startActivity(chatIntent);
        }
    }
}