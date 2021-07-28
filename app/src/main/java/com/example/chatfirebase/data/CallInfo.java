package com.example.chatfirebase.data;

public class CallInfo {

    private String callerId;
    private long timestamp;
    private int endCause;
    private boolean viewed;
    private String contactId;
    private String contactName;
    private String contactProfileUrl;

    public CallInfo() { }

    public CallInfo(String callerId, int endCause, boolean viewed) {
        this.callerId = callerId;
        this.endCause = endCause;
        this.viewed = viewed;
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

    public int getEndCause() {
        return endCause;
    }

    public boolean isViewed() {
        return viewed;
    }

    public void setViewed(boolean viewed) {
        this.viewed = viewed;
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
