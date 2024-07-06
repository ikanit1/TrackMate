package com.example.trackmate;

public class Invitation {
    private String senderId;
    private String receiverId;
    private String status;
    private String timestamp;
    private String invitationId;

    // Default constructor required for calls to DataSnapshot.getValue(Invitation.class)
    public Invitation() {
    }

    public Invitation(String senderId, String receiverId, String status, String timestamp, String invitationId) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.status = status;
        this.timestamp = timestamp;
        this.invitationId = invitationId;
    }

    public Invitation(String senderId, String receiverId, String status) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.status = status;
    }

    // Getters
    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getStatus() {
        return status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getInvitationId() {
        return invitationId;
    }

    // Setters
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setInvitationId(String invitationId) {
        this.invitationId = invitationId;
    }
}
