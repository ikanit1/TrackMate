package com.example.trackmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UserActivity extends AppCompatActivity {

    private boolean isFriend; // Флаг для определения, является ли пользователь другом
    private Button addFriendButton;
    private Button removeFriendButton;
    private Button viewLocationButton;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;

    private TextView userNick;
    private TextView userPhone;
    private ImageView ivProfilePicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");

        // Инициализация кнопок
        addFriendButton = findViewById(R.id.add_friend_button);
        removeFriendButton = findViewById(R.id.remove_friend_button);
        viewLocationButton = findViewById(R.id.view_location_button);

        // Инициализация кнопок навигации
        ImageButton homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserActivity.this, MapActivity.class);
            startActivity(intent);
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

        // Получение данных из Intent
        Intent intent = getIntent();
        if (intent != null) {
            String nickname = intent.getStringExtra("nickname");
            String phone = intent.getStringExtra("phone");

            userNick = findViewById(R.id.user_nickname);
            userNick.setText(nickname);

            userPhone = findViewById(R.id.user_phone);
            userPhone.setText(phone);

            ivProfilePicture = findViewById(R.id.user_image);

            // Загрузка изображения профиля из Firebase Storage
            loadProfilePicture(nickname);

            // Проверка, является ли пользователь другом и отображение соответствующих кнопок
            isFriend = checkIfUserIsFriend(nickname);
            updateFriendshipButtonsVisibility(isFriend);
        }

        // Установка слушателей нажатий на кнопки (добавить друга, удалить друга, показать местоположение)
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
                String friendNickname = userNick.getText().toString();
                Intent intent = new Intent(UserActivity.this, MapActivity.class);
                intent.putExtra("friendNickname", friendNickname);
                startActivity(intent);
                finish(); // Закрыть UserActivity, чтобы вернуться в MapActivity
            }
        });
    }

    // Загрузка изображения профиля
    private void loadProfilePicture(String nickname) {
        DatabaseReference userRef = databaseReference.child(nickname);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String profilePictureUrl = dataSnapshot.child("profilePictureUrl").getValue(String.class);
                    if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                        // Загрузка изображения с использованием Picasso
                        Picasso.get().load(profilePictureUrl).placeholder(R.drawable.user_pic).into(ivProfilePicture);
                    } else {
                        // Установка изображения по умолчанию, если нет изображения профиля
                        ivProfilePicture.setImageResource(R.drawable.user_pic);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("UserActivity", "Ошибка при получении данных пользователя", databaseError.toException());
            }
        });
    }

    // Проверка, является ли пользователь другом
    private boolean checkIfUserIsFriend(String friendNickname) {
        ArrayList<String> friends = Global.me.getFriends();
        return friends != null && friends.contains(friendNickname);
    }

    // Добавить друга
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
                                Toast.makeText(UserActivity.this, "Друг добавлен", Toast.LENGTH_SHORT).show();
                                addFriendButton.setVisibility(View.GONE);
                                removeFriendButton.setVisibility(View.VISIBLE);
                                viewLocationButton.setVisibility(View.VISIBLE);

                                // Добавить друга в список друзей Global
                                Global.myFriendsLocation.add(findUserLocationByNickname(friendNickname));

                                // Обновить маркеры друзей на карте
                                updateMapActivityFriendsMarkers(friendNickname);
                            } else {
                                Toast.makeText(UserActivity.this, "Ошибка при добавлении друга", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        Toast.makeText(UserActivity.this, "Друг уже добавлен", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("UserActivity", "Ошибка при получении данных пользователя", databaseError.toException());
                }
            });
        } else {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
        }
    }

    // Удалить друга
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
                                Toast.makeText(UserActivity.this, "Друг удален", Toast.LENGTH_SHORT).show();
                                addFriendButton.setVisibility(View.VISIBLE);
                                removeFriendButton.setVisibility(View.GONE);
                                viewLocationButton.setVisibility(View.GONE);

                                Iterator<UserLocation> iterator = Global.myFriendsLocation.iterator();
                                while (iterator.hasNext()) {
                                    UserLocation userLoc = iterator.next();
                                    if (userLoc.getNickName().equals(friendNickname)) {
                                        iterator.remove();
                                        break; // Выйти из цикла после удаления друга
                                    }
                                }

                                updateMapActivityFriendsMarkers(friendNickname);
                            } else {
                                Toast.makeText(UserActivity.this, "Ошибка при удалении друга", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        Toast.makeText(UserActivity.this, "Друг не найден", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("UserActivity", "Ошибка при получении данных пользователя", databaseError.toException());
                }
            });
        } else {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
        }
    }

    // Метод для нахождения местоположения пользователя по нику
    private UserLocation findUserLocationByNickname(String nickname) {
        for (UserLocation userLoc : Global.allLocations) {
            if (userLoc.getNickName().equals(nickname)) {
                return userLoc;
            }
        }
        return null;
    }

    // Метод для обновления маркеров друзей на карте
    private void updateMapActivityFriendsMarkers(String friendNickname) {
        for (UserLocation userLoc : Global.allLocations) {
            if (userLoc.getNickName().equals(friendNickname)) {
                Global.myFriendsLocation.add(userLoc);
                break; // Выйти из цикла после добавления друга
            }
        }
    }

    // Метод для обновления видимости кнопок дружбы
    private void updateFriendshipButtonsVisibility(boolean isFriend) {
        if (isFriend) {
            addFriendButton.setVisibility(View.GONE);
            removeFriendButton.setVisibility(View.VISIBLE);
            viewLocationButton.setVisibility(View.VISIBLE);
        } else {
            addFriendButton.setVisibility(View.VISIBLE);
            removeFriendButton.setVisibility(View.GONE);
            viewLocationButton.setVisibility(View.GONE);
        }
    }
}
