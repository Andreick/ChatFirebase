package com.example.chatfirebase.interfaces;

public interface CallsFragmentListener extends CurrentUser {

    int updateCallsTab(int numberCalls);

    void callContact(String contactId, String contactName, String contactProfileUrl);
}
