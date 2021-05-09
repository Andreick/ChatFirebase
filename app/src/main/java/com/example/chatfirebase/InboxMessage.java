package com.example.chatfirebase;

class InboxMessage extends Message {

    private String contactId;
    private String contactName;
    private String profileUrl;

    public InboxMessage() { }

    public InboxMessage(String contactId, Message message) {
        super(message.getSenderId(), message.getText(), message.getTimestamp());
        this.contactId = contactId;
    }

    public String getContactId() {
        return contactId;
    }

    /*public String getContactName() {
        return contactName;
    }

    public String getProfileUrl() {
        return profileUrl;
    }*/
}
