package com.example.trackmate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.trackmate.adapter.FriendsRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class FriendsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FriendsRecyclerAdapter adapter;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;
    private ArrayList<Users> friendsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        friendsList = new ArrayList<>();
        adapter = new FriendsRecyclerAdapter(friendsList, this);
        recyclerView.setAdapter(adapter);

        adapter.setOnImageClick(new FriendsRecyclerAdapter.OnImageClick() {
            @Override
            public void onImageClick(String nickName) {
                // Переход к UserActivity при клике на изображение
                Intent intent = new Intent(FriendsActivity.this, UserActivity.class);
                intent.putExtra("nickname", nickName); // Передача никнейма
                intent.putExtra("phone", getPhoneByNickname(nickName)); // Передача телефона
                startActivity(intent);
            }
        });

        ImageButton homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FriendsActivity.this, MapActivity.class);
                startActivity(intent);
            }
        });
        ImageButton addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FriendsActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });
        ImageButton settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FriendsActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        loadFriends();
    }

    private void loadFriends() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            DatabaseReference currentUserRef = databaseReference.child(currentUserId).child("friends");

            currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    ArrayList<String> friendNicknames = new ArrayList<>();
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String nickname = snapshot.getValue(String.class);
                            if (nickname != null) {
                                friendNicknames.add(nickname);
                            }
                        }
                        fetchFriendsDetails(friendNicknames);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(FriendsActivity.this, "Error loading friends", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void fetchFriendsDetails(ArrayList<String> friendNicknames) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                friendsList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Users user = snapshot.getValue(Users.class);
                    if (user != null && friendNicknames.contains(user.getNickname())) {
                        friendsList.add(user);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(FriendsActivity.this, "Error fetching friends details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getPhoneByNickname(String nickname) {
        for (Users user : friendsList) {
            if (user.getNickname().equals(nickname)) {
                return user.getPhone();
            }
        }
        return "";
    }
}
