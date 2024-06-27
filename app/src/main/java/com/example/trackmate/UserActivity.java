package com.example.trackmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UserActivity extends AppCompatActivity {

    private boolean isFriend; // Переменная для определения, является ли пользователь другом
    private Button addFriendButton;
    private Button removeFriendButton;
    private Button viewLocationButton;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;

    TextView userNick;
    TextView userPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");

        // Инициализация кнопок навигации
        ImageButton homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserActivity.this, MapActivity.class);
                startActivity(intent);
            }
        });

        ImageButton settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        ImageButton friendsButton = findViewById(R.id.friendsButton);
        friendsButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserActivity.this, FriendsActivity.class);
            startActivity(intent);
            finish();
        });

        Intent intent = getIntent();
        if (intent != null) {
            String nickname = intent.getStringExtra("nickname");
            String phone = intent.getStringExtra("phone");

            userNick = findViewById(R.id.user_nickname);
            userNick.setText(nickname);

            userPhone = findViewById(R.id.user_phone);
            userPhone.setText(phone);
        }

        addFriendButton = findViewById(R.id.add_friend_button);
        removeFriendButton = findViewById(R.id.remove_friend_button);
        viewLocationButton = findViewById(R.id.view_location_button);


        isFriend = checkIfUserIsFriend(userNick.getText().toString());

        if (isFriend) {
            addFriendButton.setVisibility(View.GONE);
            removeFriendButton.setVisibility(View.VISIBLE);
            viewLocationButton.setVisibility(View.VISIBLE);
        } else {
            addFriendButton.setVisibility(View.VISIBLE);
            removeFriendButton.setVisibility(View.GONE);
            viewLocationButton.setVisibility(View.GONE);
        }


        addFriendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFriend(userNick.getText().toString());
            }
        });
        removeFriendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeFriend(userNick.getText().toString());
            }
        });
        viewLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Получаем никнейм друга
                String friendNickname = userNick.getText().toString();

                // Обновляем маркеры на карте в MapActivity
                updateMapActivityFriendsMarkers(friendNickname);
            }
        });
    }

    private boolean checkIfUserIsFriend(String friendNickname) {
        ArrayList<String> friends = Global.me.getFriends();
        return friends != null && friends.contains(friendNickname);
    }

    private void addFriend(String friendNickname) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            DatabaseReference currentUserRef = databaseReference.child(currentUserId).child("friends");

            currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<String> friendsList = new ArrayList<>();
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            friendsList.add(snapshot.getValue(String.class));
                        }
                    }

                    if (!friendsList.contains(friendNickname)) {
                        friendsList.add(friendNickname);

                        currentUserRef.setValue(friendsList).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(UserActivity.this, "Friend added", Toast.LENGTH_SHORT).show();
                                addFriendButton.setVisibility(View.GONE);
                                removeFriendButton.setVisibility(View.VISIBLE);
                                viewLocationButton.setVisibility(View.VISIBLE);

                                // Добавляем друга в список друзей в Global
                                Global.myFriendsLocation.add(findUserLocationByNickname(friendNickname));

                                // Обновляем маркеры на карте в MapActivity
                                updateMapActivityFriendsMarkers(friendNickname);
                            } else {
                                Toast.makeText(UserActivity.this, "Error adding friend", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        Toast.makeText(UserActivity.this, "Friend already added", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("UserActivity", "Error fetching user data", databaseError.toException());
                }
            });
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeFriend(String friendNickname) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            DatabaseReference currentUserRef = databaseReference.child(currentUserId).child("friends");

            currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<String> friendsList = new ArrayList<>();
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            friendsList.add(snapshot.getValue(String.class));
                        }
                    }

                    if (friendsList.contains(friendNickname)) {
                        friendsList.remove(friendNickname);

                        currentUserRef.setValue(friendsList).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(UserActivity.this, "Friend removed", Toast.LENGTH_SHORT).show();
                                addFriendButton.setVisibility(View.VISIBLE);
                                removeFriendButton.setVisibility(View.GONE);
                                viewLocationButton.setVisibility(View.GONE);

                                Iterator<UserLocation> iterator = Global.myFriendsLocation.iterator();
                                while (iterator.hasNext()) {
                                    UserLocation userLoc = iterator.next();
                                    if (userLoc.getNickName().equals(friendNickname)) {
                                        iterator.remove();
                                        break; // Выходим из цикла, так как друг удален
                                    }
                                }


                                updateMapActivityFriendsMarkers(friendNickname);
                            } else {
                                Toast.makeText(UserActivity.this, "Error removing friend", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        Toast.makeText(UserActivity.this, "Friend not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("UserActivity", "Error fetching user data", databaseError.toException());
                }
            });
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    // Метод для поиска местоположения пользователя по его нику
    private UserLocation findUserLocationByNickname(String nickname) {
        for (UserLocation userLoc : Global.allLocations) {
            if (userLoc.getNickName().equals(nickname)) {
                return userLoc;
            }
        }
        return null;
    }

    // Метод для обновления маркеров друзей на карте в MapActivity
    private void updateMapActivityFriendsMarkers(String friendNickname) {
        Intent intent = new Intent(UserActivity.this, MapActivity.class);


        intent.putExtra("friendNickname", friendNickname);
        Toast.makeText(UserActivity.this, friendNickname, Toast.LENGTH_SHORT).show();
        startActivity(intent);
        finish(); // Закрываем текущую активити, чтобы вернуться к MapActivity с обновленными данными
    }
}
