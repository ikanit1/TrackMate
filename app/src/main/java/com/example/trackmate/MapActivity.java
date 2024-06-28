package com.example.trackmate;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
        userIcon = getSmallUserIcon();

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

        // Проверка разрешений на местоположение
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Получение последнего известного местоположения пользователя
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
                        refLocations.child(Global.me.getNickname()).setValue(Global.myLoc); // Используем ник текущего пользователя

                        mMap.clear();
                        currMarker = mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(Global.me.getNickname()) // Устанавливаем ник пользователя в качестве заголовка маркера
                                .icon(userIcon));
                        currMarker.showInfoWindow();
                        if (firstTimeZoom) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                            firstTimeZoom = false;
                        }
                        addFriendsMarkers();// Добавляем маркеры друзей на карту
                        if (getIntent().hasExtra("friendNickname")) {
                            String friendNickname = getIntent().getStringExtra("friendNickname");
                            Toast.makeText(MapActivity.this, friendNickname + "go focus" , Toast.LENGTH_SHORT).show();
                            focusOnFriendMarker(friendNickname);

                        }
                    } else {
                        Toast.makeText(MapActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                    }
                });

        // Запрос обновлений местоположения пользователя
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    // Метод для фокусировки на маркере друга по его нику
    private void focusOnFriendMarker(String friendNickname) {
        for (Marker marker : friendsMarkers) {
            if (marker.getTitle().equals(friendNickname)) {
                Toast.makeText(MapActivity.this, "Focusing on marker: " + friendNickname, Toast.LENGTH_SHORT).show();
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 17));
                marker.showInfoWindow(); // Показать информационное окно для маркера
                return;
            }
        }
        Toast.makeText(MapActivity.this, "No marker found for: " + friendNickname, Toast.LENGTH_SHORT).show();
    }

    // Метод для добавления маркеров друзей на карту
    private void addFriendsMarkers() {
        for (UserLocation userLoc : Global.myFriendsLocation) {
            MarkerOptions mo = new MarkerOptions()
                    .position(userLoc.getLatLng())
                    .title(userLoc.getNickName()) // Устанавливаем ник друга в качестве заголовка маркера
                    .icon(userIcon); // Используем маленькую иконку пользователя

            Marker m = mMap.addMarker(mo);
            Objects.requireNonNull(m).showInfoWindow();
            friendsMarkers.add(m);
        }
    }

    // Метод для обновления маркеров друзей на карте
    private void updateFriendsMarkers() {
        // Удаляем все существующие маркеры друзей с карты
        Iterator<Marker> iterator = friendsMarkers.iterator();
        while (iterator.hasNext()) {
            Marker marker = iterator.next();
            marker.remove();
            iterator.remove();
        }

        // Добавляем обновленные маркеры друзей на карту
        for (UserLocation userLoc : Global.myFriendsLocation) {
            MarkerOptions mo = new MarkerOptions()
                    .position(userLoc.getLatLng())
                    .title(userLoc.getNickName())
                    .icon(userIcon);

            Marker m = mMap.addMarker(mo);
            Objects.requireNonNull(m).showInfoWindow();
            friendsMarkers.add(m);
        }
    }

    private BitmapDescriptor getSmallUserIcon() {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.user_pic); // Загружаем изображение
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = 150;
        int newHeight = 150;
        Bitmap smallMarker = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false); // Создаем уменьшенное изображение
        return BitmapDescriptorFactory.fromBitmap(smallMarker); // Создаем BitmapDescriptor из уменьшенного изображения
    }

    // Обработчик запроса разрешений на местоположение
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


