package com.example.chatfirebase.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatfirebase.R;
import com.example.chatfirebase.data.CallInfo;
import com.example.chatfirebase.interfaces.CallsFragmentListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.sinch.android.rtc.calling.CallEndCause;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.GroupieViewHolder;
import com.xwray.groupie.Item;

import java.text.DateFormat;
import java.util.LinkedList;
import java.util.List;

public class CallsFragment extends Fragment {

    private static final String TAG = "CallsFragment";

    private final List<CallItem> linkedCallItems = new LinkedList<>();

    private CallsFragmentListener fragmentListener;
    private Query callsQuery;
    private EventListener<QuerySnapshot> callsEventListener;
    private ListenerRegistration callsRegistration;
    private int numberNotViewedCalls;

    private Context context;
    private RecyclerView recyclerView;
    private GroupAdapter<GroupieViewHolder> callsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fragmentListener = (CallsFragmentListener) getActivity();

        callsQuery = FirebaseFirestore.getInstance().collection(getString(R.string.collection_talks))
                .document(fragmentListener.getUid())
                .collection(getString(R.string.collection_talks_calls))
                .orderBy(getString(R.string.timestamp), Query.Direction.ASCENDING);

        setCallsEventListener();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calls, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        context = view.getContext();
        recyclerView = view.findViewById(R.id.rv_calls);
        callsAdapter = new GroupAdapter<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(callsAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        numberNotViewedCalls = 0;
        callsRegistration = callsQuery.addSnapshotListener(callsEventListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
        callsRegistration.remove();
        linkedCallItems.clear();
    }

    private void setCallsEventListener() {
        callsEventListener = (snapshots, e) -> {
            if (e == null) {
                if (snapshots != null) {

                    for (DocumentChange doc : snapshots.getDocumentChanges()) {

                        if (doc.getType() == DocumentChange.Type.ADDED) {
                            Log.d(TAG, "Call " + doc.getDocument().getId() + " ADDED");
                            CallInfo callInfo = doc.getDocument().toObject(CallInfo.class);
                            linkedCallItems.add(0, new CallItem(callInfo));
                            if (!callInfo.isViewed()) {
                                numberNotViewedCalls = fragmentListener.updateCallsTab(++numberNotViewedCalls);
                            }
                        }
                    }

                    callsAdapter.replaceAll(linkedCallItems);
                }
                else {
                    Log.e(TAG, "Null calls snapshot");
                    Toast.makeText(context, getString(R.string.failure_calls), Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Log.e(TAG, "Calls snapshot listener failed", e);
                Toast.makeText(context, getString(R.string.failure_calls), Toast.LENGTH_SHORT).show();
            }
        };
    }

    private class CallItem extends Item<GroupieViewHolder> {

        private final CallInfo callInfo;

        public CallItem(CallInfo callInfo) {
            this.callInfo = callInfo;
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {
            ImageView civPhoto = viewHolder.itemView.findViewById(R.id.civ_card_call_photo);
            ImageView ivCallStatus = viewHolder.itemView.findViewById(R.id.iv_card_call_status);
            ImageView ivCall = viewHolder.itemView.findViewById(R.id.iv_card_call);
            TextView tvContactName = viewHolder.itemView.findViewById(R.id.tv_call_username);
            TextView tvDate = viewHolder.itemView.findViewById(R.id.tv_call_timestamp);

            Picasso.get().load(callInfo.getContactProfileUrl()).fit().centerCrop()
                    .placeholder(R.drawable.profile_placeholder).into(civPhoto);
            ivCall.setImageDrawable(ContextCompat.getDrawable(viewHolder.itemView.getContext(), R.drawable.ic_call_green_icon));
            tvContactName.setText(callInfo.getContactName());
            tvDate.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(callInfo.getTimestamp()));

            if (callInfo.getEndCause() == CallEndCause.HUNG_UP.getValue()) {
                if (fragmentListener.getUid().equals(callInfo.getCallerId())) {
                    ivCallStatus.setImageDrawable(ContextCompat.getDrawable(viewHolder.itemView.getContext(),
                            R.drawable.ic_call_made));
                }
                else {
                    ivCallStatus.setImageDrawable(ContextCompat.getDrawable(viewHolder.itemView.getContext(),
                            R.drawable.ic_call_received));
                }
            }
            else {
                if (fragmentListener.getUid().equals(callInfo.getCallerId())) {
                    ivCallStatus.setImageDrawable(ContextCompat.getDrawable(viewHolder.itemView.getContext(),
                            R.drawable.ic_call_made_missed));
                }
                else {
                    ivCallStatus.setImageDrawable(ContextCompat.getDrawable(viewHolder.itemView.getContext(),
                            R.drawable.ic_call_received_missed));
                }
            }

            ivCall.setOnClickListener(view -> fragmentListener
                    .callContact(callInfo.getContactId(), callInfo.getContactName(), callInfo.getContactProfileUrl()));
        }

        @Override
        public int getLayout() {
            return R.layout.card_call;
        }
    }
}