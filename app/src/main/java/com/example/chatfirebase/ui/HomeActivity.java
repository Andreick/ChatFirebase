package com.example.chatfirebase.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.chatfirebase.ChatFirebaseApplication;
import com.example.chatfirebase.R;
import com.example.chatfirebase.data.User;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.squareup.picasso.Picasso;

public class HomeActivity extends AppCompatActivity {

    private static final int NUM_PAGES = 3;

    private ChatFirebaseApplication application;

    private ImageView imgProfile;
    private Button buttonLogout;
    private TextView tvChats, tvContacts, tvCalls;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FragmentStateAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        imgProfile = findViewById(R.id.civ_home_photo);
        buttonLogout = findViewById(R.id.btn_logout);
        tabLayout = findViewById(R.id.tab_bar);
        viewPager = findViewById(R.id.vp_home);

        application = (ChatFirebaseApplication) getApplication();
        User currentUser = application.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Failed to load user", Toast.LENGTH_SHORT).show();
            goToLoginActivity();
            return;
        }

        pagerAdapter = new PagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        tabLayout.addOnTabSelectedListener(new TabSelectedListener());

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {

            tab.setCustomView(R.layout.custom_tab);
            View view = tab.getCustomView();

            if (view != null) {

                switch (position) {
                    case 0:
                        tvChats = view.findViewById(R.id.tv_tab);
                        tvChats.setText(R.string.tab_chats);
                        break;
                    case 1:
                        tvContacts = view.findViewById(R.id.tv_tab);
                        tvContacts.setText(R.string.tab_contacts);
                        break;
                    case 2:
                        tvCalls = view.findViewById(R.id.tv_tab);
                        tvCalls.setText(R.string.tab_calls);
                        break;
                }
            }
        }).attach();

        String profileUri = currentUser.getProfileUrl();
        Picasso.get().load(profileUri).placeholder(R.drawable.profile_placeholder).into(imgProfile);

        buttonLogout.setOnClickListener(view -> logout());
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) super.onBackPressed();
        else viewPager.setCurrentItem(0);
    }

    private void goToLoginActivity() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);
    }

    private void logout() {
        application.close();
        goToLoginActivity();
    }

    private class TabSelectedListener implements TabLayout.OnTabSelectedListener {

        @Override
        public void onTabSelected(TabLayout.Tab tab) {

            switch (tab.getPosition()) {
                case 0:
                    tvChats.setTextColor(ContextCompat.getColor(HomeActivity.this, R.color.color_primary));
                    break;
                case 1:
                    tvContacts.setTextColor(ContextCompat.getColor(HomeActivity.this, R.color.color_primary));
                    break;
                case 2:
                    tvCalls.setTextColor(ContextCompat.getColor(HomeActivity.this, R.color.color_primary));
                    break;
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

            switch (tab.getPosition()) {
                case 0:
                    tvChats.setTextColor(ContextCompat.getColor(HomeActivity.this, R.color.gray));
                    break;
                case 1:
                    tvContacts.setTextColor(ContextCompat.getColor(HomeActivity.this, R.color.gray));
                    break;
                case 2:
                    tvCalls.setTextColor(ContextCompat.getColor(HomeActivity.this, R.color.gray));
                    break;
            }
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


