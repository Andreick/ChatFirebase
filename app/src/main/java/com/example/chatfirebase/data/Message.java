package com.example.chatfirebase.data;

public class Message {

    private String senderId;
    private String text;
    private long timestamp;

    public Message() { }

    public Message(String senderId, String text) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }

    public Message(String senderId, String text, long timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
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
}