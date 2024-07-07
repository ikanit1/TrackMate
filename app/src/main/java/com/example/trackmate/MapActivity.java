package com.example.trackmate;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
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
    private final String TAG = "MapActivity";
    private DatabaseReference refLocations;
    private ArrayList<Marker> friendsMarkers = new ArrayList<>();
    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Marker currMarker;
    private BitmapDescriptor userIcon;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean firstTimeZoom = true;
    private Handler handler;
    private Runnable mapUpdateRunnable;
    private StorageReference storageRef;
    private BroadcastReceiver nicknameChangeReceiver;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Log.d(TAG, "onCreate");

        refLocations = FirebaseDatabase.getInstance().getReference("Locations");
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
                Log.d(TAG, "onLocationResult");
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, location.toString());
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

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        storageRef = FirebaseStorage.getInstance().getReference();

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
        sosButton.setOnClickListener(v -> showConfirmationDialog());

        handler = new Handler(Looper.getMainLooper());
        mapUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateFriendsMarkers();
                handler.postDelayed(this, 10000); // 10 seconds
            }
        };

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
        IntentFilter filter = new IntentFilter("com.example.trackmate.NICKNAME_UPDATED");
        registerReceiver(nicknameChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nicknameChangeReceiver);
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
        Log.d(TAG, "onResume");
        if (mMap != null) {
            updateFriendsMarkers();
        }
        handler.post(mapUpdateRunnable); // Start periodic updates
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        handler.removeCallbacks(mapUpdateRunnable); // Stop periodic updates
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    Log.d(TAG, "onSuccess");
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        LatLng latLng = new LatLng(latitude, longitude);
                        Global.myLoc = new UserLocation(String.valueOf(latitude), String.valueOf(longitude), Global.me.getNickname());
                        Log.d("MAP", Global.myLoc.toString());
                        refLocations.child(Global.me.getNickname()).setValue(Global.myLoc);

                        StorageReference userPicRef = storageRef.child("profile_pictures/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + ".jpg");
                        userPicRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(bytes -> {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            Bitmap circularBitmap = getCircularBitmap(bitmap);
                            BitmapDescriptor userIcon = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(circularBitmap, 150, 150, false));

                            mMap.clear();
                            currMarker = mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(Global.me.getNickname())
                                    .icon(userIcon));
                            currMarker.showInfoWindow();
                            if (firstTimeZoom) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                                firstTimeZoom = false;
                            }
                            addFriendsMarkers();
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

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    private void focusOnFriendMarker(String friendNickname) {
        for (Marker marker : friendsMarkers) {
            if (marker.getTitle().equals(friendNickname)) {
                Toast.makeText(MapActivity.this, "Focusing on marker: " + friendNickname, Toast.LENGTH_SHORT).show();
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 17));
                marker.showInfoWindow();
                return;
            }
        }
        Toast.makeText(MapActivity.this, "No marker found for: " + friendNickname, Toast.LENGTH_SHORT).show();
    }

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

    private void updateFriendsMarkers() {
        for (Marker marker : friendsMarkers) {
            marker.remove();
        }
        friendsMarkers.clear();

        for (UserLocation userLocation : Global.myFriendsLocation) {
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

    private void updateFriendMarkersForNicknameChange(String oldNickname, String newNickname) {
        DatabaseReference friendLocationRef = refLocations.child(oldNickname);
        friendLocationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserLocation friendLocation = dataSnapshot.getValue(UserLocation.class);
                if (friendLocation != null) {
                    // Remove the old marker
                    for (Iterator<Marker> iterator = friendsMarkers.iterator(); iterator.hasNext(); ) {
                        Marker marker = iterator.next();
                        if (marker.getTitle().equals(oldNickname)) {
                            marker.remove();
                            iterator.remove();
                            break;
                        }
                    }

                    // Update Firebase with new nickname
                    refLocations.child(newNickname).setValue(friendLocation).addOnSuccessListener(aVoid -> {
                        refLocations.child(oldNickname).removeValue();
                        // Add a new marker with the updated nickname
                        LatLng position = new LatLng(Double.parseDouble(friendLocation.getLatitude()), Double.parseDouble(friendLocation.getLongitude()));
                        loadFriendIcon(Global.getUserIconUrl(newNickname), position, newNickname);
                        Toast.makeText(MapActivity.this, "Nickname and marker updated", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> Toast.makeText(MapActivity.this, "Failed to update new location reference", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, "Failed to fetch old location data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}