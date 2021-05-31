package com.example.chatfirebase;

class Chat {

    private String contactId;
    private String contactName;
    private String contactProfileUrl;
    private Message lastMessage;

    public Chat() { }

    public Chat(String uid, User contact, Message message) {
        contactId = uid;
        contactName = contact.getName();
        contactProfileUrl = contact.getProfileUrl();
        lastMessage = message;
    }

    public String getContactId() {
        return contactId;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactProfileUrl() {
        return contactProfileUrl;
    }

    public Message getLastMessage() {
        return lastMessage;
    }
}
