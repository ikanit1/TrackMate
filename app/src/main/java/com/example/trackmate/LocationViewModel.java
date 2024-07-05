package com.example.trackmate;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

// Define a ViewModel class to hold your location data
public class LocationViewModel extends ViewModel {
    private final MutableLiveData<Map<String, UserLocation>> locations = new MutableLiveData<>(new HashMap<>());

    public LiveData<Map<String, UserLocation>> getLocations() {
        return locations;
    }

    public void updateLocation(String nickname, UserLocation location) {
        Map<String, UserLocation> currentLocations = locations.getValue();
        if (currentLocations != null) {
            currentLocations.put(nickname, location);
            locations.setValue(currentLocations);
        }
    }

    public void removeLocation(String nickname) {
        Map<String, UserLocation> currentLocations = locations.getValue();
        if (currentLocations != null) {
            currentLocations.remove(nickname);
            locations.setValue(currentLocations);
        }
    }
}
