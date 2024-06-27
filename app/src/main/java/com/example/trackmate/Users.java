package com.example.trackmate;

import java.util.ArrayList;

public class Users {

    private String phone, email, password, nickname, fcmToken;
    private ArrayList<String> friends;

    public Users() {
    }

    public Users(String phone, String email, String password, String nickname, ArrayList<String> friends, String fcmToken) {
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.friends = friends;
        this.fcmToken = fcmToken;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public ArrayList<String> getFriends() {
        return friends;
    }

    public void setFriends(ArrayList<String> friends) {
        this.friends = friends;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
