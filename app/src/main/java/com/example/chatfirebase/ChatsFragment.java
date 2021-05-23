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

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

public class ChatsFragment extends Fragment {

    private static final String TAG = "ChatsFragment";

    private final Map<String, ChatItem> chatItemMap = new HashMap<>();
    private final LinkedList<ChatItem> linkedChatItems = new LinkedList<>();

    private Query chatsQuery;
    private EventListener<QuerySnapshot> chatsEventListener;
    private ListenerRegistration chatsRegistration;

    private Context context;
    private RecyclerView recyclerView;
    private GroupAdapter<GroupieViewHolder> chatsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String currentUid = FirebaseAuth.getInstance().getUid();

        chatsQuery = FirebaseFirestore.getInstance().collection(getString(R.string.collection_inboxes))
                .document(currentUid)
                .collection(getString(R.string.collection_inbox_message))
                .orderBy(getString(R.string.message_timestamp), Query.Direction.ASCENDING);

        setChatsEventListener();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
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
    }

    // Atualiza as conversas em tempo real
    private void setChatsEventListener() {
        chatsEventListener = (snapshots, e) -> {
            if (e == null) {
                if (snapshots != null) {
                    for (DocumentChange doc : snapshots.getDocumentChanges()) {
                        InboxMessage inboxMessage = doc.getDocument().toObject(InboxMessage.class);

                        switch (doc.getType()) {
                            case ADDED:
                                Log.d(TAG, "Document Change ADDED");
                                ChatItem newChatItem = new ChatItem(inboxMessage);
                                chatItemMap.put(inboxMessage.getContactId(), newChatItem);
                                linkedChatItems.addFirst(newChatItem);
                                break;
                            case MODIFIED:
                                Log.d(TAG, "Document Change MODIFIED");
                                ChatItem chatItem = Objects.requireNonNull(chatItemMap.get(inboxMessage.getContactId()));
                                linkedChatItems.remove(chatItem);
                                chatItem.setMessage(inboxMessage);
                                linkedChatItems.addFirst(chatItem);
                                break;
                        }

                        chatsAdapter.update(linkedChatItems, false);
                        chatsAdapter.notifyDataSetChanged();
                    }
                }
            }
            else {
                Log.e(TAG, "Chats snapshot listener failed", e);
                Toast.makeText(context, "Failed to load chats", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private class OnChatItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(@NotNull Item item, @NotNull View view) {
            ChatItem chatItem = (ChatItem) item;

            Intent chatIntent = new Intent(context, ChatActivity.class);
            chatIntent.putExtra(getString(R.string.user_id), chatItem.contactId);
            startActivity(chatIntent);
        }
    }

    private static class ChatItem extends Item<GroupieViewHolder> {

        private final String contactId;
        private final String contactProfileUrl;
        private final String contactName;
        private String senderId;
        private String lastMessage;

        public ChatItem(InboxMessage inboxMessage) {
            contactId = inboxMessage.getContactId();
            contactProfileUrl = inboxMessage.getContactProfileUrl();
            contactName = inboxMessage.getContactName();
            senderId = inboxMessage.getSenderId();
            lastMessage = inboxMessage.getText();
        }

        private void setMessage(Message message) {
            senderId = message.getSenderId();
            lastMessage = message.getText();
        }

        @Override
        public void bind(GroupieViewHolder viewHolder, int position) {
            ImageView imgPhoto = viewHolder.itemView.findViewById(R.id.imgUserPhoto2);
            TextView username = viewHolder.itemView.findViewById(R.id.txtUserName2);
            TextView message = viewHolder.itemView.findViewById(R.id.txtLastMessage2);

            Picasso.get().load(contactProfileUrl).into(imgPhoto);
            username.setText(contactName);

            String text = lastMessage;

            if (contactId.equals(senderId)) {
                text = contactName + ": " + text;
            }

            message.setText(text);
        }

        @Override
        public int getLayout() {
            return R.layout.card_user_chats;
        }
    }
}