package com.example.chatfirebase.data;

public class CallInfo {

    private String callerId;
    private long timestamp;
    private boolean answered;
    private String contactId;
    private String contactName;
    private String contactProfileUrl;

    public CallInfo() { }

    public CallInfo(String callerId, boolean answered) {
        this.callerId = callerId;
        this.answered = answered;
        timestamp = System.currentTimeMillis();
    }

    public void setContact(String contactId, String contactName, String contactProfileUrl) {
        this.contactId = contactId;
        this.contactName = contactName;
        this.contactProfileUrl = contactProfileUrl;
    }

    public String getCallerId() {
        return callerId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isAnswered() {
        return answered;
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
