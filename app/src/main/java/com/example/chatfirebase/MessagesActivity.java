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

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.Item;
import com.xwray.groupie.OnItemClickListener;
import com.xwray.groupie.ViewHolder;

import java.util.List;

public class MessagesActivity extends AppCompatActivity {

    private Button vButtonLogout;
    private RadioGroup vRadioOptions;
    private RadioButton vInbox, vGroups, vCalls;
    private RecyclerView vViewContacts, vViewInbox;
    private GroupAdapter<ViewHolder> adapter, adapter2;
    private ImageView vimgProfile;
    private User user;

    public MessagesActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);


        vButtonLogout = findViewById(R.id.btLogout);
        vRadioOptions = findViewById(R.id.rdOptions);
        vInbox = findViewById(R.id.btInbox);
        vGroups = findViewById(R.id.btContacts);
        vCalls = findViewById(R.id.btCalls);
        vViewContacts = findViewById(R.id.lsContacts);
        vViewInbox = findViewById(R.id.lsInbox);
        vimgProfile = findViewById(R.id.imgProfile);

        //Picasso.get().load(user.getProfileUrl()).into(vimgProfile);


        adapter = new GroupAdapter<>();
        vViewContacts.setLayoutManager(new LinearLayoutManager(this));
        vViewContacts.setAdapter(adapter);

        adapter2 = new GroupAdapter<>();
        vViewInbox.setLayoutManager(new LinearLayoutManager(this));
        vViewInbox.setAdapter(adapter2);




//------------------------------------------------------------
        // Deslogar
        vButtonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MessagesActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
//------------------------------------------------------------
        // Configuracao RadioButtons
        vRadioOptions.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.btInbox) {
                    vViewInbox.setVisibility(View.VISIBLE);
                    vViewContacts.setVisibility(View.INVISIBLE);

                }
                if (checkedId == R.id.btContacts){
                    vViewInbox.setVisibility(View.INVISIBLE);
                    vViewContacts.setVisibility(View.VISIBLE);

                }

            }
        });

        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull Item item, @NonNull View view) {
                UserItem userItem = (UserItem) item;
                Intent intent = new Intent(MessagesActivity.this, ChatActivity.class);
                intent.putExtra("user", userItem.user);
                startActivity(intent);
            }
        });


        verifyAuth();
        fetchUsers();
        fetchMessages();

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
                                    Contact contact = doc.getDocument().toObject(Contact.class);

                                    adapter2.add(new ContactItem(contact));
                                }
                            }
                        }
                    }
                });
    }

    // Verifica se o usuario esta conectado
    private void verifyAuth() {
        if (FirebaseAuth.getInstance().getUid() == null) {
            Intent intent = new Intent(MessagesActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }


    private void fetchUsers(){

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
                        adapter.add(new UserItem(user));
                        adapter.notifyDataSetChanged();
                    }
                }
            });
    }

    private class ContactItem extends Item<ViewHolder>{

        private final Contact contact;

        private ContactItem(Contact contact) { this.contact = contact; }

        @Override
        public void bind(@NonNull ViewHolder viewHolder, int position) {
            TextView username = viewHolder.itemView.findViewById(R.id.txtUserName2);
            TextView message = viewHolder.itemView.findViewById(R.id.txtLastMessage2);
            ImageView imgPhoto = viewHolder.itemView.findViewById(R.id.imgUserPhoto2);

            username.setText(contact.getUsername());
            message.setText(contact.getLastMessage());
            Picasso.get().load(contact.getPhotoUrl()).into(imgPhoto);
        }

        @Override
        public int getLayout() {
            return R.layout.card_user_inbox;
        }
    }



    private class UserItem extends Item<ViewHolder>{
        private final User user;

        public UserItem(User user) {
            this.user = user;
        }

        @Override
        public void bind(@NonNull ViewHolder viewHolder, int position) {

            TextView txtUserNm = viewHolder.itemView.findViewById(R.id.txtUserName);
            ImageView imgPhoto = viewHolder.itemView.findViewById(R.id.imgUserPhoto);

            txtUserNm.setText(user.getUsername());

            Picasso.get().load(user.getProfileUrl()).into(imgPhoto);
        }

        @Override
        public int getLayout() {
            return R.layout.card_user;
        }
    }
    }


