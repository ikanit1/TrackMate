package com.example.trackmate;

import java.util.ArrayList;

public class Users {
    private String phone;
    private String email;
    private String password;
    private String nickname;
    private String fcmToken;
    private String profilePictureUrl; // Add this field
    private ArrayList<String> friends;

    public Users() {
    }

    public Users(String phone, String email, String password, String nickname, ArrayList<String> friends, String fcmToken) {
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.fcmToken = fcmToken;
        this.profilePictureUrl = ""; // Default value
        this.friends = friends;
    }

    public Users(String phone, String email, String password, String nickname, ArrayList<String> friends, String fcmToken, String profilePictureUrl) {
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.fcmToken = fcmToken;
        this.profilePictureUrl = profilePictureUrl; // Initialize this field
        this.friends = friends;
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

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getProfilePictureUrl() { // Add this method
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) { // Add this method
        this.profilePictureUrl = profilePictureUrl;
    }

    public ArrayList<String> getFriends() {
        return friends;
    }

    public void setFriends(ArrayList<String> friends) {
        this.friends = friends;
    }

    // Method to add a friend to the friends list
    public void addFriend(String friendNickname) {
        if (friends == null) {
            friends = new ArrayList<>();
        }
        if (!friends.contains(friendNickname)) {
            friends.add(friendNickname);
        }
    }

    // Method to remove a friend from the friends list
    public void removeFriend(String friendNickname) {
        if (friends != null) {
            friends.remove(friendNickname);
        }
    }
}
