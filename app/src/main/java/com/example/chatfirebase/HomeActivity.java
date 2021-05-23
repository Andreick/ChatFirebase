package com.example.chatfirebase;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

public class HomeActivity extends AppCompatActivity {

    private static final int NUM_PAGES = 3;

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

        imgProfile = findViewById(R.id.imgProfile);
        buttonLogout = findViewById(R.id.btLogout);
        tabLayout = findViewById(R.id.tab_bar);
        viewPager = findViewById(R.id.view_pager);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Failed to load user", Toast.LENGTH_SHORT).show();
            goToLoginActivity();
            return;
        }

        Uri profileUri = currentUser.getPhotoUrl();
        Picasso.get().load(profileUri).into(imgProfile);

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
        ChatFirebaseApplication application = (ChatFirebaseApplication) getApplication();
        application.close();
        goToLoginActivity();
    }

    private class TabSelectedListener implements TabLayout.OnTabSelectedListener {

        @Override
        public void onTabSelected(TabLayout.Tab tab) {

            switch (tab.getPosition()) {
                case 0:
                    tvChats.setTextColor(ContextCompat.getColor(HomeActivity.this, R.color.cinza));
                    break;
                case 1:
                    tvContacts.setTextColor(ContextCompat.getColor(HomeActivity.this, R.color.cinza));
                    break;
                case 2:
                    tvCalls.setTextColor(ContextCompat.getColor(HomeActivity.this, R.color.cinza));
                    break;
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

            switch (tab.getPosition()) {
                case 0:
                    tvChats.setTextColor(ContextCompat.getColor(HomeActivity.this, R.color.cor_primaria));
                    break;
                case 1:
                    tvContacts.setTextColor(ContextCompat.getColor(HomeActivity.this, R.color.cor_primaria));
                    break;
                case 2:
                    tvCalls.setTextColor(ContextCompat.getColor(HomeActivity.this, R.color.cor_primaria));
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

        @NotNull
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


