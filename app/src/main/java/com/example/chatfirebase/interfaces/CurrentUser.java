package com.example.chatfirebase.interfaces;

public interface CurrentUser {

    String getUid();
    void goToTalkActivity(String contactId, String contactName, String contactProfileUrl);
}
