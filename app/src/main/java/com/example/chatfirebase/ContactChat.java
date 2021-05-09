package com.example.chatfirebase;

class ContactChat extends Message {

    private String contactId;
    private String contactName;
    private String profileUrl;

    public ContactChat() { }

    public ContactChat(String contactId, String contactName, String profileUrl, Message message) {
        super(message.getSenderId(), message.getText(), message.getTimestamp());
        this.contactId = contactId;
        this.contactName = contactName;
        this.profileUrl = profileUrl;
    }

    public String getContactId() {
        return contactId;
    }

    public String getContactName() {
        return contactName;
    }

    public String getProfileUrl() {
        return profileUrl;
    }
}
