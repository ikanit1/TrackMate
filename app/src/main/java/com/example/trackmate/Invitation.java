package com.example.trackmate;

public class Invitation {
    private String senderId;
    private String senderName;

    public Invitation() {
        // Пустой конструктор необходим для Firebase
    }

    public Invitation(String senderId, String senderName) {
        this.senderId = senderId;
        this.senderName = senderName;
    }

    // Геттеры и сеттеры

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
}
