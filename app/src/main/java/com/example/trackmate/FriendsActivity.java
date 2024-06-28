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
    private ArrayList<Object> items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        items = new ArrayList<>();
        adapter = new FriendsRecyclerAdapter(items, this);
        recyclerView.setAdapter(adapter);
        setupFriendListListener();

        adapter.setOnImageClick(new FriendsRecyclerAdapter.OnImageClick() {
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
        ImageButton invitationsButton = findViewById(R.id.invitationsButton);
        invitationsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FriendsActivity.this, InvitationsActivity.class);
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

        loadFriendsAndRequests();
    }
    // В FriendsActivity добавить слушатель изменений в базе данных Firebase для обновления списка друзей
    private void setupFriendListListener() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            DatabaseReference currentUserRef = databaseReference.child(currentUserId).child("friends");

            currentUserRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    items.clear();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String friendNickname = snapshot.getValue(String.class);
                        if (friendNickname != null) {
                            fetchFriendDetails(friendNickname);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(FriendsActivity.this, "Error loading friends", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    private void fetchFriendDetails(String friendNickname) {
        databaseReference.child(friendNickname).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Users user = dataSnapshot.getValue(Users.class);
                if (user != null) {
                    items.add(user);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(FriendsActivity.this, "Error fetching friend details", Toast.LENGTH_SHORT).show();
            }
        });
    }




    private void loadFriendsAndRequests() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            DatabaseReference currentUserRef = databaseReference.child(currentUserId);

            currentUserRef.child("friends").addListenerForSingleValueEvent(new ValueEventListener() {
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

            currentUserRef.child("friendRequests").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    ArrayList<FriendRequest> friendRequests = new ArrayList<>();
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            FriendRequest request = snapshot.getValue(FriendRequest.class);
                            if (request != null) {
                                friendRequests.add(request);
                            }
                        }
                        items.addAll(friendRequests);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(FriendsActivity.this, "Error loading friend requests", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void fetchFriendsDetails(ArrayList<String> friendNicknames) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Users user = snapshot.getValue(Users.class);
                    if (user != null && friendNicknames.contains(user.getNickname())) {
                        items.add(user);
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
        for (Object item : items) {
            if (item instanceof Users) {
                Users user = (Users) item;
                if (user.getNickname().equals(nickname)) {
                    return user.getPhone();
                }
            }
        }
        return "";
    }
}
