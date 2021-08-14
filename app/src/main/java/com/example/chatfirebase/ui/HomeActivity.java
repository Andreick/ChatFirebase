package com.example.chatfirebase.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.chatfirebase.ChatFirebaseApplication;
import com.example.chatfirebase.R;
import com.example.chatfirebase.interfaces.CallsFragmentListener;
import com.example.chatfirebase.interfaces.ChatsFragmentListener;
import com.example.chatfirebase.services.SinchService;
import com.example.chatfirebase.util.BaseCallActivity;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.squareup.picasso.Picasso;

public class HomeActivity extends BaseCallActivity implements ChatsFragmentListener, CallsFragmentListener {

    private static final String TAG = "HomeActivity";
    private static final int NUM_PAGES = 3;

    private String currentUid, userName, profileUrl;

    private ImageView imgProfile;
    private Button buttonLogout;
    private ViewPager2 viewPager;
    private FragmentStateAdapter pagerAdapter;
    private TabLayout tabLayout;
    private BadgeDrawable chatsBadge, callsBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            userFailure(getString(R.string.failure_user));
            return;
        }

        userName = currentUser.getDisplayName();
        Uri photoUri = currentUser.getPhotoUrl();

        if (TextUtils.isEmpty(userName) || photoUri == null) {
            userFailure(getString(R.string.failure_user_profile));
            return;
        }

        currentUid = currentUser.getUid();
        profileUrl = photoUri.toString();

        attemptToStartSinchService(new Intent(this, SinchService.class));

        imgProfile = findViewById(R.id.civ_home_photo);
        buttonLogout = findViewById(R.id.btn_logout);
        tabLayout = findViewById(R.id.tab_bar);
        viewPager = findViewById(R.id.vp_home);

        pagerAdapter = new PagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(pagerAdapter.getItemCount());

        tabLayout.addOnTabSelectedListener(new TabSelectedListener());

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {

            if (position == 1) {
                tab.setText(R.string.tab_contacts);
                return;
            }

            BadgeDrawable badge = tab.getOrCreateBadge();
            badge.setVisible(false);
            badge.setBadgeTextColor(ContextCompat.getColor(this, R.color.color_secondary));
            badge.setBackgroundColor(ContextCompat.getColor(this, R.color.yellow));

            switch (position) {
                case 0:
                    tab.setText(R.string.tab_chats);
                    chatsBadge = badge;
                    break;
                case 2:
                    tab.setText(R.string.tab_calls);
                    callsBadge = badge;
                    break;
            }
        }).attach();

        Picasso.get().load(profileUrl).fit().centerCrop()
                .placeholder(R.drawable.profile_placeholder).into(imgProfile);

        buttonLogout.setOnClickListener(view -> logout());
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) super.onBackPressed();
        else viewPager.setCurrentItem(0);
    }

    private void userFailure(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        goToLoginActivity();
    }

    private void goToLoginActivity() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);
    }

    private void logout() {
        viewPager.setAdapter(null);
        ((ChatFirebaseApplication) getApplication()).close();
        goToLoginActivity();
    }

    @Override
    public String getUid() {
        return currentUid;
    }

    @Override
    public void goToTalkActivity(String contactId, String contactName, String contactProfileUrl) {
        Intent talkIntent = new Intent(this, TalkActivity.class);
        talkIntent.putExtra(getString(R.string.user_id), currentUid);
        talkIntent.putExtra(getString(R.string.user_name), userName);
        talkIntent.putExtra(getString(R.string.user_profile_url), profileUrl);
        talkIntent.putExtra(getString(R.string.contact_id), contactId);
        talkIntent.putExtra(getString(R.string.contact_name), contactName);
        talkIntent.putExtra(getString(R.string.contact_profile_url), contactProfileUrl);
        startActivity(talkIntent);
    }

    @Override
    public void callContact(String contactId, String contactName, String contactProfileUrl) {
        Intent sinchServiceIntent = new Intent(this, SinchService.class);
        sinchServiceIntent.putExtra(getString(R.string.contact_id), contactId);
        sinchServiceIntent.putExtra(getString(R.string.contact_name), contactName);
        sinchServiceIntent.putExtra(getString(R.string.contact_profile_url), contactProfileUrl);
        attemptToStartSinchService(sinchServiceIntent);
    }

    @Override
    public void updateChatsTab(int numberUnreadMessages) {
        updateTabBadge(chatsBadge, numberUnreadMessages);
    }

    @Override
    public int updateCallsTab(int numberNotViewedCalls) {
        if (tabLayout.getSelectedTabPosition() == 2) {
            setCallsViewed();
            return 0;
        }
        updateTabBadge(callsBadge, numberNotViewedCalls);
        return numberNotViewedCalls;
    }

    private void updateTabBadge(BadgeDrawable badge, int number) {
        if (number > 0) {
            badge.setNumber(number);
            badge.setVisible(true);
        }
        else badge.setVisible(false);
    }

    private void setCallsViewed() {
        FirebaseFirestore.getInstance().collection(getString(R.string.collection_talks))
                .document(currentUid).collection(getString(R.string.collection_talks_calls))
                .whereEqualTo(getString(R.string.viewed), false)
                .get()
                .addOnSuccessListener(snapshots -> {
                    WriteBatch batch = FirebaseFirestore.getInstance().batch();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        batch.update(doc.getReference(), getString(R.string.viewed), true);
                    }

                    batch.commit().addOnFailureListener(e -> {
                        Log.e(TAG, "Update call batch failed", e);
                        Toast.makeText(HomeActivity.this,
                                getString(R.string.failure_calls), Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query calls not viewed", e);
                    Toast.makeText(HomeActivity.this,
                            getString(R.string.failure_calls), Toast.LENGTH_SHORT).show();
                });
    }

    private class TabSelectedListener implements TabLayout.OnTabSelectedListener {

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            if (tab.getPosition() == 2) {
                setCallsViewed();
                callsBadge.setVisible(false);
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    }

    private static class PagerAdapter extends FragmentStateAdapter {

        public PagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {

            switch (position) {
                case 0:
                    return new ChatsFragment();
                case 1:
                    return new ContactsFragment();
                case 2:
                    return new CallsFragment();
                default:
                    return new Fragment();
            }
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }
}


