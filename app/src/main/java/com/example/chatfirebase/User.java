package com.example.chatfirebase;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {

    private String id;
    private String name;
    private String profileUrl;

    public User() { }

    public User(String id, String name, String profileUrl) {
        this.id = id;
        this.name = name;
        this.profileUrl = profileUrl;
    }

    public User(String id, String name, Uri profileUrl) {
        this.id = id;
        this.name = name;
        this.profileUrl = profileUrl.toString();
    }

    protected User(Parcel in) {
        id = in.readString();
        name = in.readString();
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

    public String getName(){
        return name;
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
        parcel.writeString(name);
        parcel.writeString(profileUrl);
    }
}
