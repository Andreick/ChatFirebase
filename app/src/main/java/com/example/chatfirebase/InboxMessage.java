package com.example.chatfirebase;

class InboxMessage extends Message {

    private String contactId;

    public InboxMessage() { }

    public InboxMessage(String contactId, Message message) {
        super(message.getSenderId(), message.getText(), message.getTimestamp());
        this.contactId = contactId;
    }

    public String getContactId() {
        return contactId;
    }
}
