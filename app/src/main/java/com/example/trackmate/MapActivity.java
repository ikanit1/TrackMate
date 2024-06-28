package com.example.trackmate;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

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
    private BitmapDescriptor userIcon; // Иконка пользователя
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean firstTimeZoom = true; // Флаг для отслеживания первого захода

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Инициализация TextView
        TextView welcomeMessage = findViewById(R.id.tvWelcomeMessage);

        // Установка приветственного сообщения
        if (Global.me != null) {
            String welcomeText = "Welcome, " + Global.me.getNickname() + "!";
            welcomeMessage.setText(welcomeText);
        }

        // Остальная инициализация
        Log.e(TAG, "onCreate");
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest.Builder(200)
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
                }
            }
        };

        // Инициализация карты
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Загружаем маленькую иконку пользователя
        String profilePictureUrl = Global.me.getProfilePictureUrl();
        loadUserIcon(profilePictureUrl, bitmapDescriptor -> userIcon = bitmapDescriptor);

        // Обработчики кнопок
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
    }

    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to send your location to all friends?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendLocationNotification();
                Toast.makeText(MapActivity.this, "Sending location...", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
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
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            currMarker = mMap.addMarker(new MarkerOptions()
                                    .position(userLatLng)
                                    .icon(userIcon));
                            if (firstTimeZoom) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));
                                firstTimeZoom = false;
                            }
                        }
                    }
                });

        // Update friends' markers
        updateFriendsMarkers();

        // Check for friend focus from intent
        Intent intent = getIntent();
        String friendNickname = intent.getStringExtra("friendNickname");
        if (friendNickname != null) {
            focusOnFriendMarker(friendNickname);
        }
    }


    private void updateFriendsMarkers() {
        Log.e(TAG, "updateFriendsMarkers");
        if (mMap != null) {
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
    }
    private Bitmap resizeBitmap(Bitmap original, int width, int height) {
        return Bitmap.createScaledBitmap(original, width, height, false);
    }



    private void focusOnFriendMarker(String friendNickname) {
        Log.e(TAG, "focusOnFriendMarker: " + friendNickname);
        for (Marker marker : friendsMarkers) {
            if (Objects.equals(marker.getTitle(), friendNickname)) {
                LatLng friendLocation = marker.getPosition();
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(friendLocation, 15));
                break;
            }
        }
    }

    private void loadUserIcon(String url, final OnIconLoadedListener listener) {
        Glide.with(this)
                .asBitmap()
                .load(url)
                .circleCrop()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        // Resize the bitmap to a smaller size, e.g., 100x100 pixels
                        Bitmap resizedBitmap = resizeBitmap(resource, 100, 100);
                        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(resizedBitmap);
                        listener.onIconLoaded(icon);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private Bitmap createCustomMarker(Context context, Bitmap bitmap) {
        View markerLayout = LayoutInflater.from(context).inflate(R.layout.marker_layout, null);
        ImageView markerImage = markerLayout.findViewById(R.id.marker_image);
        markerImage.setImageBitmap(bitmap);

        // Measure and layout the view
        markerLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        markerLayout.layout(0, 0, markerLayout.getMeasuredWidth(), markerLayout.getMeasuredHeight());

        // Create the bitmap
        Bitmap returnedBitmap = Bitmap.createBitmap(markerLayout.getMeasuredWidth(), markerLayout.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        markerLayout.draw(canvas);

        return returnedBitmap;
    }


    private void loadFriendIcon(String url, final LatLng position, final String nickname) {
        Glide.with(this)
                .asBitmap()
                .load(url)
                .circleCrop()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Bitmap customMarkerBitmap = createCustomMarker(MapActivity.this, resource);
                        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(customMarkerBitmap);
                        Marker friendMarker = mMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(nickname)
                                .icon(icon));
                        friendsMarkers.add(friendMarker);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }


    interface OnIconLoadedListener {
        void onIconLoaded(BitmapDescriptor icon);
    }
}
