package com.example.trackmate;

public class FriendRequest {
    private String nickname;
    private String status;
    private String sender;

    public FriendRequest() {
        // Пустой конструктор для Firebase
    }

    public FriendRequest(String nickname, String sender) {
        this.nickname = nickname;
        this.sender = sender;
        this.status = "pending";
    }

    public FriendRequest(String nickname, String sender, String status) {
        this.nickname = nickname;
        this.sender = sender;
        this.status = status;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}