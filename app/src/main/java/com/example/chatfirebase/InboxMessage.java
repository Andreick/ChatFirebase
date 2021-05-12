package com.example.chatfirebase;

class InboxMessage extends Message {

    private String contactId;
    private String contactName;
    private String contactProfileUrl;

    public InboxMessage() { }

    public InboxMessage(User contact, Message message) {
        super(message.getSenderId(), message.getText(), message.getTimestamp());
        contactId = contact.getId();
        contactName = contact.getName();
        contactProfileUrl = contact.getProfileUrl();
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
}
