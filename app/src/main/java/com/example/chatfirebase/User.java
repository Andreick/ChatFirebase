package com.example.chatfirebase;

import android.os.Parcel;
import android.os.Parcelable;

public class User {

    private String name;
    private String profileUrl;
    private int connectionStatus;

    public User() { }

    public User(String name, String profileUrl) {
        this.name = name;
        this.profileUrl = profileUrl;
    }

    public String getName() {
        return name;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public int getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(int connectionStatus) {
        this.connectionStatus = connectionStatus;
    }
}
