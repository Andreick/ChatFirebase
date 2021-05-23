package com.example.chatfirebase;

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

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
/*import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;*/
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.GroupieViewHolder;
import com.xwray.groupie.Item;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class ContactsFragment extends Fragment {

    private static final String TAG = "ContactsFragment";

    private final Set<ContactItem> contactItemSet = new TreeSet<>(new ContactItemComparator());

    private Query contactsQuery;
    private EventListener<QuerySnapshot> contactsEventListener;
    private ListenerRegistration contactsRegistration;

    /*private DatabaseReference contactsReference;
    private ChildEventListener contactsEventListener;*/

    private Context context;
    private RecyclerView recyclerView;
    private GroupAdapter<GroupieViewHolder> contactsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String currentUid = FirebaseAuth.getInstance().getUid();

        contactsQuery = FirebaseFirestore.getInstance().collection(getString(R.string.collection_users))
                .whereNotEqualTo(getString(R.string.user_id), currentUid);

        setContactsEventListener();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
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
        contactsRegistration = contactsQuery.addSnapshotListener(contactsEventListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        contactsRegistration.remove();
    }

    private void setContactsEventListener() {
        contactsEventListener = (snapshots, e) -> {
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
                Log.e(TAG, "Contacts snapshot listener failed", e);
                Toast.makeText(context, "Failed to load contacts", Toast.LENGTH_SHORT).show();
            }
        };
    }

    /*private void fetchContacts() {
        contactsEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull @NotNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {
                Log.d(TAG, "onChildAdded: " + snapshot.getKey());

                User contact = snapshot.getValue(User.class);

                if (contact != null && !contact.getId().equals(currentUid)) {
                    contactItemSet.add(new ContactItem(contact));
                    contactsAdapter.update(contactItemSet);
                    contactsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(@NonNull @NotNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {
                Log.d(TAG, "onChildChanged: " + snapshot.getKey());
            }

            @Override
            public void onChildRemoved(@NonNull @NotNull DataSnapshot snapshot) {
                Log.d(TAG, "onChildRemoved: " + snapshot.getKey());
            }

            @Override
            public void onChildMoved(@NonNull @NotNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {
                Log.d(TAG, "onChildMoved: " + snapshot.getKey());
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {
                Log.e(TAG, "Contacts child event listener failed", error.toException());
                Toast.makeText(HomeActivity.this, "Failed to load contacts", Toast.LENGTH_SHORT).show();
            }
        };
    }*/

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
        public void bind(GroupieViewHolder viewHolder, int position) {
            ImageView imgPhoto = viewHolder.itemView.findViewById(R.id.imgUserPhoto);
            TextView txtUserNm = viewHolder.itemView.findViewById(R.id.txtUserName);

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

    private class OnContactItemClickListener implements com.xwray.groupie.OnItemClickListener {

        @Override
        public void onItemClick(@NotNull Item item, @NotNull View view) {
            ContactItem contactItem = (ContactItem) item;

            Intent chatIntent = new Intent(context, ChatActivity.class);
            chatIntent.putExtra(getString(R.string.user_id), contactItem.contactId);
            startActivity(chatIntent);
        }
    }
}