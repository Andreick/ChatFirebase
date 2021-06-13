package com.example.chatfirebase.data;

public class Message {

    private String senderId;
    private String text;
    private long timestamp;
    private boolean read;

    public Message() { }

    public Message(String senderId, String text) {
        this.senderId = senderId;
        this.text = text;
        timestamp = System.currentTimeMillis();
        read = false;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isRead() {
        return read;
    }
}