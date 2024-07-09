package com.example.trackmate;

import com.google.android.gms.maps.model.LatLng;

public class UserLocation {
    private String latitude;
    private String longitude;
    private String nickName;

    // Default constructor required for calls to DataSnapshot.getValue(UserLocation.class)
    public UserLocation() {
    }

    public UserLocation(String latitude, String longitude, String nickName) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.nickName = nickName;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public LatLng getLatLng() {
        return new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
    }

    @Override
    public String toString() {
        return "UserLocation{" +
                "latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                ", nickName='" + nickName + '\'' +
                '}';
    }
}
