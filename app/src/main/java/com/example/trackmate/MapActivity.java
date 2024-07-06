package com.example.trackmate;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private final String TAG = "MyTag";
    private DatabaseReference refLocations;
    private ArrayList<Marker> friendsMarkers = new ArrayList<>();
    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Marker currMarker;
    private BitmapDescriptor userIcon; // User icon
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean firstTimeZoom = true; // Flag for first-time zoom
    private Handler handler;
    private Runnable mapUpdateRunnable;
    private StorageReference storageRef;
    private LocationViewModel locationViewModel;
    private BroadcastReceiver nicknameChangeReceiver;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Log.e(TAG, "onCreate");

        // Set welcome message
        locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        addFriendLocationListener();
        refLocations = FirebaseDatabase.getInstance().getReference("Locations");

        // Add Firebase listeners for location and nickname changes
        DatabaseReference locationsRef = FirebaseDatabase.getInstance().getReference("Locations");
        locationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Object value = snapshot.getValue();
                    if (value instanceof Map) {
                        Map<String, Object> locationData = (Map<String, Object>) value;
                        if (locationData != null) {
                            Object latitude = locationData.get("latitude");
                            Object longitude = locationData.get("longitude");
                            if (latitude instanceof Double) {
                                locationData.put("latitude", String.valueOf(latitude));
                            } else if (latitude instanceof String) {
                                locationData.put("latitude", (String) latitude);
                            }
                            if (longitude instanceof Double) {
                                locationData.put("longitude", String.valueOf(longitude));
                            } else if (longitude instanceof String) {
                                locationData.put("longitude", (String) longitude);
                            }
                            snapshot.getRef().setValue(locationData);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        TextView welcomeMessageTextView = findViewById(R.id.tvWelcomeMessage);
        String welcomeMessage = "Welcome, " + Global.me.getNickname() + "!";
        welcomeMessageTextView.setText(welcomeMessage);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest.Builder(5000)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .build();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Log.e(TAG, "onLocationResult");
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    Log.e(TAG, location.toString());
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    if (currMarker != null) {
                        currMarker.setPosition(latLng);
                        if (firstTimeZoom) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                            firstTimeZoom = false;
                        }
                    }

                    // Update Firebase with the new location
                    if (refLocations != null) {
                        Global.myLoc = new UserLocation(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()), Global.me.getNickname());
                        refLocations.child(Global.me.getNickname()).setValue(Global.myLoc)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Location updated in Firebase"))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update location in Firebase", e));
                    }
                }
            }
        };

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize Firebase Storage reference
        storageRef = FirebaseStorage.getInstance().getReference();

        // Button handlers
        ImageButton friendsButton = findViewById(R.id.friendsButton);
        friendsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MapActivity.this, FriendsActivity.class);
            startActivity(intent);
            finish();
        });
        ImageButton settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MapActivity.this, SettingsActivity.class);
            startActivity(intent);
            finish();
        });
        ImageButton sosButton = findViewById(R.id.sosButton);
        sosButton.setOnClickListener(v -> {
            showConfirmationDialog();
        });

        // Initialize handler and runnable for periodic updates
        handler = new Handler(Looper.getMainLooper());
        mapUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateFriendsMarkers();
                handler.postDelayed(this, 10000); // 10 seconds
            }
        };

        // BroadcastReceiver for nickname changes
        nicknameChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.trackmate.NICKNAME_UPDATED".equals(intent.getAction())) {
                    String oldNickname = intent.getStringExtra("oldNickname");
                    String newNickname = intent.getStringExtra("newNickname");
                    updateFriendMarkersForNicknameChange(oldNickname, newNickname);
                }
            }
        };
        // Register the receiver
        IntentFilter filter = new IntentFilter("com.example.trackmate.NICKNAME_UPDATED");
        registerReceiver(nicknameChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver
        unregisterReceiver(nicknameChangeReceiver);
    }

    private void updateFriendMarkersForNicknameChange(String oldNickname, String newNickname) {
        refLocations.child(oldNickname).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Object locationData = dataSnapshot.getValue();
                if (locationData != null) {
                    // Remove the old location reference
                    refLocations.child(oldNickname).removeValue().addOnSuccessListener(aVoid -> {
                        // Set the new location reference
                        refLocations.child(newNickname).setValue(locationData).addOnSuccessListener(aVoid1 -> {
                            // Update the marker on the map
                            for (Marker marker : friendsMarkers) {
                                if (marker.getTitle().equals(oldNickname)) {
                                    LatLng position = marker.getPosition();
                                    marker.remove(); // Remove the old marker

                                    // Add a new marker with the updated nickname
                                    Marker newMarker = mMap.addMarker(new MarkerOptions()
                                            .position(position)
                                            .title(newNickname)
                                            .icon(getUserIcon(newNickname)));
                                    friendsMarkers.add(newMarker);
                                    break;
                                }
                            }
                            Toast.makeText(MapActivity.this, "Nickname and marker updated", Toast.LENGTH_SHORT).show();
                        }).addOnFailureListener(e ->
                                Toast.makeText(MapActivity.this, "Failed to update new location reference", Toast.LENGTH_SHORT).show());
                    }).addOnFailureListener(e ->
                            Toast.makeText(MapActivity.this, "Failed to remove old location reference", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, "Failed to fetch old location data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to send your location to all friends?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            sendLocationNotification();
            Toast.makeText(MapActivity.this, "Sending location...", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private Users findUserByNickname(String nickname) {
        for (Users user : Global.allUsers) {
            if (user.getNickname().equals(nickname)) {
                return user;
            }
        }
        return null;
    }

    private void sendLocationNotification() {
        LatLng myLocation = new LatLng(Double.parseDouble(Global.myLoc.getLatitude()), Double.parseDouble(Global.myLoc.getLongitude()));

        Log.d(TAG, "Sending location notification. My location: " + myLocation.toString());

        for (UserLocation friendLoc : Global.myFriendsLocation) {
            Users friend = findUserByNickname(friendLoc.getNickName());
            if (friend != null) {
                Log.d(TAG, "Sending notification to friend: " + friend.getNickname());
                sendNotificationToFriend(friend.getFcmToken(), Global.me.getNickname(), myLocation);
            }
        }
    }

    private void sendNotificationToFriend(String fcmToken, String senderNickname, LatLng location) {
        if (fcmToken == null || location == null) {
            Log.e(TAG, "Invalid fcmToken or location");
            return;
        }

        double latitude = location.latitude;
        double longitude = location.longitude;

        Log.d(TAG, "Sending notification with location. Sender: " + senderNickname + ", Location: (" + latitude + ", " + longitude + ")");

        Map<String, String> data = new HashMap<>();
        data.put("senderNickname", senderNickname);
        data.put("latitude", String.valueOf(latitude));
        data.put("longitude", String.valueOf(longitude));

        RemoteMessage message = new RemoteMessage.Builder(fcmToken)
                .setMessageId(String.valueOf(System.currentTimeMillis()))
                .addData("senderNickname", senderNickname)
                .addData("latitude", String.valueOf(latitude))
                .addData("longitude", String.valueOf(longitude))
                .build();

        FirebaseMessaging.getInstance().send(message);
        Log.d(TAG, "Notification sent to friend with token: " + fcmToken);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        if (mMap != null) {
            updateFriendsMarkers();
        }
        handler.post(mapUpdateRunnable); // Start periodic updates
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        handler.removeCallbacks(mapUpdateRunnable); // Stop periodic updates
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.e(TAG, "onMapReady");
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        // Check location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Get the last known user location
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    Log.e(TAG, "onSuccess");
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        LatLng latLng = new LatLng(latitude, longitude);
                        Global.myLoc = new UserLocation(String.valueOf(latitude), String.valueOf(longitude), Global.me.getNickname());
                        Log.e("MAP", Global.myLoc.toString());
                        refLocations = FirebaseDatabase.getInstance().getReference("Locations");
                        refLocations.child(Global.me.getNickname()).setValue(Global.myLoc); // Use current user's nickname

                        // Fetch the user's profile picture from Firebase Storage
                        StorageReference userPicRef = storageRef.child("profile_pictures/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + ".jpg");
                        userPicRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(bytes -> {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            Bitmap circularBitmap = getCircularBitmap(bitmap); // Make the bitmap circular
                            BitmapDescriptor userIcon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(circularBitmap, 150, 150, false));

                            mMap.clear();
                            currMarker = mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(Global.me.getNickname()) // Set user's nickname as marker title
                                    .icon(userIcon));
                            currMarker.showInfoWindow();
                            if (firstTimeZoom) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                                firstTimeZoom = false;
                            }
                            addFriendsMarkers(); // Add friends' markers to the map
                            if (getIntent().hasExtra("friendNickname")) {
                                String friendNickname = getIntent().getStringExtra("friendNickname");
                                Toast.makeText(MapActivity.this, friendNickname + " go focus", Toast.LENGTH_SHORT).show();
                                focusOnFriendMarker(friendNickname);
                            }
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to fetch user profile picture", e);
                        });
                    } else {
                        Toast.makeText(MapActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                    }
                });

        // Request location updates
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    // Method to focus on a friend's marker by nickname
    private void focusOnFriendMarker(String friendNickname) {
        for (Marker marker : friendsMarkers) {
            if (marker.getTitle().equals(friendNickname)) {
                Toast.makeText(MapActivity.this, "Focusing on marker: " + friendNickname, Toast.LENGTH_SHORT).show();
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 17));
                marker.showInfoWindow(); // Show info window for the marker
                return;
            }
        }
        Toast.makeText(MapActivity.this, "No marker found for: " + friendNickname, Toast.LENGTH_SHORT).show();
    }

    // Method to add friends' markers to the map
    private void addFriendsMarkers() {
        for (UserLocation userLoc : Global.myFriendsLocation) {
            loadFriendIcon(Global.getUserIconUrl(userLoc.getNickName()), userLoc.getLatLng(), userLoc.getNickName());
        }
    }

    private BitmapDescriptor getUserIcon(String nickname) {
        String iconUrl = Global.getUserIconUrl(nickname);
        if (iconUrl != null && !iconUrl.isEmpty()) {
            Bitmap bitmap = getBitmapFromURL(iconUrl);
            if (bitmap != null) {
                return BitmapDescriptorFactory.fromBitmap(getCircularBitmap(bitmap));
            }
        }
        return userIcon;
    }

    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newSize = Math.min(width, height);

        Bitmap output = Bitmap.createBitmap(newSize, newSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, newSize, newSize);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(newSize / 2, newSize / 2, newSize / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    private Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e(TAG, "Error loading image from URL", e);
            return null;
        }
    }

    // Method to update friends' markers on the map
    private void updateFriendsMarkers() {
        for (Marker marker : friendsMarkers) {
            marker.remove();
        }
        friendsMarkers.clear();

        Iterator<UserLocation> iterator = Global.myFriendsLocation.iterator();
        while (iterator.hasNext()) {
            UserLocation userLocation = iterator.next();
            Users friend = findUserByNickname(userLocation.getNickName());
            if (friend != null) {
                LatLng friendLatLng = new LatLng(Double.parseDouble(userLocation.getLatitude()), Double.parseDouble(userLocation.getLongitude()));
                String profilePictureUrl = friend.getProfilePictureUrl();
                loadFriendIcon(profilePictureUrl, friendLatLng, userLocation.getNickName());
            }
        }
    }

    private void loadFriendIcon(String url, final LatLng position, final String nickname) {
        if (url == null || url.isEmpty()) {
            // Use default image if URL is null or empty
            useDefaultImage(position, nickname);
            return;
        }

        Glide.with(this)
                .asBitmap()
                .load(url)
                .circleCrop()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(createCustomMarker(MapActivity.this, resource));
                        Marker friendMarker = mMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(nickname)
                                .icon(icon));
                        friendsMarkers.add(friendMarker);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        // Use default image if loading fails
                        useDefaultImage(position, nickname);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private void useDefaultImage(LatLng position, String nickname) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.user_pic);
        BitmapDescriptor friendIcon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, 150, 150, false));
        Marker friendMarker = mMap.addMarker(new MarkerOptions()
                .position(position)
                .title(nickname)
                .icon(friendIcon));
        friendsMarkers.add(friendMarker);
    }


    private Bitmap createCustomMarker(Context context, Bitmap bitmap) {
        View markerLayout = LayoutInflater.from(context).inflate(R.layout.marker_layout, null);
        ImageView markerImage = markerLayout.findViewById(R.id.marker_image);
        markerImage.setImageBitmap(bitmap);

        markerLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        markerLayout.layout(0, 0, markerLayout.getMeasuredWidth(), markerLayout.getMeasuredHeight());

        Bitmap returnedBitmap = Bitmap.createBitmap(markerLayout.getMeasuredWidth(), markerLayout.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        markerLayout.draw(canvas);

        return returnedBitmap;
    }


    // Method to use the default image
    private void useDefaultImage(UserLocation userLoc) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.user_pic);
        BitmapDescriptor friendIcon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, 150, 150, false));
        MarkerOptions mo = new MarkerOptions()
                .position(userLoc.getLatLng())
                .title(userLoc.getNickName())
                .icon(friendIcon);
        Marker m = mMap.addMarker(mo);
        Objects.requireNonNull(m).showInfoWindow();
        friendsMarkers.add(m);
    }
    private void addFriendLocationListener() {
        DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("friends");

        friendsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot friendSnapshot : dataSnapshot.getChildren()) {
                    String friendNickname = friendSnapshot.getValue(String.class);
                    if (friendNickname != null) {
                        listenToFriendLocation(friendNickname);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching friends", databaseError.toException());
            }
        });
    }
    private void listenToFriendLocation(String friendNickname) {
        DatabaseReference friendLocationRef = FirebaseDatabase.getInstance().getReference("Locations").child(friendNickname);
        friendLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserLocation friendLocation = dataSnapshot.getValue(UserLocation.class);
                if (friendLocation != null) {
                    updateFriendMarker(friendLocation);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching friend's location", databaseError.toException());
            }
        });
    }
    private void updateFriendMarker(UserLocation friendLocation) {
        LatLng friendLatLng = new LatLng(Double.parseDouble(friendLocation.getLatitude()), Double.parseDouble(friendLocation.getLongitude()));
        String profilePictureUrl = Global.getUserIconUrl(friendLocation.getNickName());

        // Remove existing marker for this friend if it exists
        for (Marker marker : friendsMarkers) {
            if (marker.getTitle().equals(friendLocation.getNickName())) {
                marker.remove();
                friendsMarkers.remove(marker);
                break;
            }
        }

        loadFriendIcon(profilePictureUrl, friendLatLng, friendLocation.getNickName());
    }

    // Handler for location permission requests
    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onMapReady(mMap);
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void removeFriend(String friendNickname) {
        Iterator<UserLocation> iterator = Global.myFriendsLocation.iterator();
        while (iterator.hasNext()) {
            UserLocation userLoc = iterator.next();
            if (userLoc.getNickName().equals(friendNickname)) {
                iterator.remove();
                break;
            }
        }
        updateFriendsMarkers();
    }
}
