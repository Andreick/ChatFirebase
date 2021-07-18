package com.example.chatfirebase.data;

public class CallInfo {

    private String callerId;
    private String contactName;
    private String contactProfileUrl;
    private long timestamp;
    private boolean answered;

    public CallInfo() { }

    public CallInfo(String callerId, String contactName, String contactProfileUrl, long timestamp, boolean answered) {
        this.callerId = callerId;
        this.contactName = contactName;
        this.contactProfileUrl = contactProfileUrl;
        this.timestamp = timestamp;
        this.answered = answered;
    }

    public String getCallerId() {
        return callerId;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactProfileUrl() {
        return contactProfileUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isAnswered() {
        return answered;
    }
}
