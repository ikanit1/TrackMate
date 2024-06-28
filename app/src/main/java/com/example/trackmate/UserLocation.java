package com.example.trackmate;

import com.google.android.gms.maps.model.LatLng;

public class UserLocation {
    private String Latitude;
    private String Longitude;
    private String NickName;

    public UserLocation() {
    }

    public UserLocation(String latitude, String longitude, String nickName) {
        Latitude = latitude;
        Longitude = longitude;
        NickName = nickName;
    }

    public String getLatitude() {
        return Latitude;
    }

    public void setLatitude(String latitude) {
        Latitude = latitude;
    }

    public String getLongitude() {
        return Longitude;
    }

    public void setLongitude(String longitude) {
        Longitude = longitude;
    }

    public String getNickName() {
        return NickName;
    }

    public void setNickName(String nickName) {
        NickName = nickName;
    }

    public LatLng getLatLng() {
        return new LatLng(Double.parseDouble(Latitude), Double.parseDouble(Longitude));
    }

    @Override
    public String toString() {
        return "UserLocation{" +
                "Latitude='" + Latitude + '\'' +
                ", Longitude='" + Longitude + '\'' +
                ", Nickname='" + NickName + '\'' +
                '}';
    }
}
