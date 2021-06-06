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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatfirebase.R;
import com.example.chatfirebase.data.Chat;
import com.google.firebase.auth.FirebaseAuth;
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
import com.xwray.groupie.OnItemClickListener;

import java.util.LinkedList;

public class ChatsFragment extends Fragment {

    private static final String TAG = "ChatsFragment";

    private final LinkedList<ChatItem> linkedChatItems = new LinkedList<>();

    private String currentUid;
    private Query chatsQuery;
    private EventListener<QuerySnapshot> chatsEventListener;
    private ListenerRegistration chatsRegistration;

    private Context context;
    private RecyclerView recyclerView;
    private GroupAdapter<GroupieViewHolder> chatsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentUid = FirebaseAuth.getInstance().getUid();

        chatsQuery = FirebaseFirestore.getInstance().collection(getString(R.string.collection_chats))
                .document(currentUid)
                .collection(getString(R.string.collection_chat))
                .orderBy(getString(R.string.chat_last_message_timestamp), Query.Direction.ASCENDING);

        setChatsEventListener();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        context = view.getContext();
        recyclerView = view.findViewById(R.id.rv_chats);
        chatsAdapter = new GroupAdapter<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(chatsAdapter);

        chatsAdapter.setOnItemClickListener(new OnChatItemClickListener());
    }

    @Override
    public void onStart() {
        super.onStart();
        chatsRegistration = chatsQuery.addSnapshotListener(chatsEventListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        chatsRegistration.remove();
        linkedChatItems.clear();
        chatsAdapter.clear();
    }

    // Atualiza as conversas em tempo real
    private void setChatsEventListener() {
        chatsEventListener = (snapshots, e) -> {
            if (e == null) {
                if (snapshots != null) {
                    for (DocumentChange doc : snapshots.getDocumentChanges()) {
                        Chat chat = doc.getDocument().toObject(Chat.class);
                        ChatItem chatItem = new ChatItem(chat);

                        switch (doc.getType()) {
                            case ADDED:
                                Log.d(TAG, "Document ADDED");
                                linkedChatItems.addFirst(chatItem);
                                break;
                            case MODIFIED:
                                Log.d(TAG, "Document MODIFIED");
                                linkedChatItems.remove(chatItem);
                                linkedChatItems.addFirst(chatItem);
                                break;
                        }
                    }
                    chatsAdapter.update(linkedChatItems, false);
                    chatsAdapter.notifyDataSetChanged();
                }
                else {
                    Log.e(TAG, "Null chats snapshot");
                    Toast.makeText(context, getString(R.string.failure_chats), Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Log.e(TAG, "Chats snapshot listener failed", e);
                Toast.makeText(context, getString(R.string.failure_chats), Toast.LENGTH_SHORT).show();
            }
        };
    }

    private static class ChatItem extends Item<GroupieViewHolder> {

        private final String contactId;
        private final String contactProfileUrl;
        private final String contactName;
        private final String senderId;
        private final String lastMessage;

        public ChatItem(Chat chat) {
            contactId = chat.getContactId();
            contactProfileUrl = chat.getContactProfileUrl();
            contactName = chat.getContactName();
            senderId = chat.getLastMessage().getSenderId();
            lastMessage = chat.getLastMessage().getText();
        }

        @Override
        public void bind(GroupieViewHolder viewHolder, int position) {
            ImageView imgPhoto = viewHolder.itemView.findViewById(R.id.civ_card_chat_photo);
            TextView username = viewHolder.itemView.findViewById(R.id.tv_chat_username);
            TextView message = viewHolder.itemView.findViewById(R.id.tv_chat_last_message);

            Picasso.get().load(contactProfileUrl).placeholder(R.drawable.profile_placeholder).into(imgPhoto);
            username.setText(contactName);

            String text = lastMessage;

            if (contactId.equals(senderId)) {
                text = contactName + ": " + text;
            }

            message.setText(text);
        }

        @Override
        public int getLayout() {
            return R.layout.card_chat;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChatItem chatItem = (ChatItem) o;
            return contactId.equals(chatItem.contactId);
        }

        @Override
        public int hashCode() {
            return contactId.hashCode();
        }
    }

    private class OnChatItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(@NonNull Item item, @NonNull View view) {
            ChatItem chatItem = (ChatItem) item;

            Intent chatIntent = new Intent(context, ChatActivity.class);
            chatIntent.putExtra(getString(R.string.extra_user_id), currentUid);
            chatIntent.putExtra(getString(R.string.extra_contact_id), chatItem.contactId);
            chatIntent.putExtra(getString(R.string.extra_contact_name), chatItem.contactName);
            chatIntent.putExtra(getString(R.string.extra_contact_profile_url), chatItem.contactProfileUrl);
            startActivity(chatIntent);
        }
    }
}