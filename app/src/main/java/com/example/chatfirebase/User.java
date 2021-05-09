package com.example.chatfirebase;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {

    private String id;
    private String username;
    private String profileUrl;

    public User() { }

    public User(String id, String username, String profileUrl) {
        this.id = id;
        this.username = username;
        this.profileUrl = profileUrl;
    }

    protected User(Parcel in) {
        id = in.readString();
        username = in.readString();
        profileUrl = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getId(){
        return id;
    }

    public String getUsername(){
        return username;
    }

    public String getProfileUrl(){
        return profileUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(username);
        parcel.writeString(profileUrl);
    }
}
