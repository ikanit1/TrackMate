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

    private boolean isFriend; // Flag to check if the user is a friend
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

        // Initialize buttons
        addFriendButton = findViewById(R.id.add_friend_button);
        removeFriendButton = findViewById(R.id.remove_friend_button);
        viewLocationButton = findViewById(R.id.view_location_button);

        // Initialize navigation buttons
        ImageButton homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserActivity.this, MapActivity.class);
            startActivity(intent);
        });

        ImageButton settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        ImageButton friendsButton = findViewById(R.id.friendsButton);
        friendsButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserActivity.this, FriendsActivity.class);
            startActivity(intent);
            finish();
        });

        // Get data from Intent
        Intent intent = getIntent();
        if (intent != null) {
            String nickname = intent.getStringExtra("nickname");
            String phone = intent.getStringExtra("phone");

            userNick = findViewById(R.id.user_nickname);
            userNick.setText(nickname);

            userPhone = findViewById(R.id.user_phone);
            userPhone.setText(phone);

            ivProfilePicture = findViewById(R.id.user_image);

            // Load profile picture from Firebase Storage
            loadProfilePicture(nickname);

            // Check if the user is a friend and update the visibility of the buttons accordingly
            checkIfUserIsFriend(nickname);
        }

        // Set click listeners for the buttons (add friend, remove friend, view location)
        addFriendButton.setOnClickListener(v -> sendFriendInvitation(userNick.getText().toString()));
        removeFriendButton.setOnClickListener(v -> removeFriend(userNick.getText().toString()));
        viewLocationButton.setOnClickListener(v -> {
            String friendNickname = userNick.getText().toString();
            Intent intent1 = new Intent(UserActivity.this, MapActivity.class);
            intent1.putExtra("friendNickname", friendNickname);
            startActivity(intent1);
            finish(); // Close UserActivity to return to MapActivity
        });
    }

    private void sendFriendInvitation(String receiverNickname) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            databaseReference.orderByChild("nickname").equalTo(receiverNickname).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String receiverId = snapshot.getKey();
                            DatabaseReference invitationsRef = FirebaseDatabase.getInstance().getReference().child("Invitations");
                            String invitationId = invitationsRef.push().getKey();
                            String timestamp = String.valueOf(System.currentTimeMillis()); // Example timestamp

                            Invitation invitation = new Invitation(currentUserId, receiverId, "pending", timestamp, invitationId);
                            invitationsRef.child(invitationId).setValue(invitation).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(UserActivity.this, "Invitation sent", Toast.LENGTH_SHORT).show();
                                    listenForInvitationResponse(invitationId, currentUserId, receiverId);
                                } else {
                                    Toast.makeText(UserActivity.this, "Failed to send invitation", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(UserActivity.this, "User not found", Toast.LENGTH_SHORT).show();
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

    private void listenForInvitationResponse(String invitationId, String senderId, String receiverId) {
        DatabaseReference invitationRef = FirebaseDatabase.getInstance().getReference().child("Invitations").child(invitationId);
        invitationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Invitation invitation = dataSnapshot.getValue(Invitation.class);
                if (invitation != null && "accepted".equals(invitation.getStatus())) {
                    databaseReference.child(receiverId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Users receiverUser = snapshot.getValue(Users.class);
                            if (receiverUser != null) {
                                addFriend(receiverUser.getNickname());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("UserActivity", "Error fetching receiver user data", error.toException());
                        }
                    });
                    invitationRef.removeEventListener(this);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("UserActivity", "Error listening for invitation response", databaseError.toException());
            }
        });
    }

    // Load the profile picture
    private void loadProfilePicture(String nickname) {
        databaseReference.orderByChild("nickname").equalTo(nickname).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Users user = snapshot.getValue(Users.class);
                        if (user != null && user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
                            // Load the image using Picasso
                            Picasso.get().load(user.getProfilePictureUrl()).into(ivProfilePicture);
                        } else {
                            // Do nothing if there's no profile picture URL
                            Log.e("UserActivity", "No profile picture URL found");
                        }
                    }
                } else {
                    // Do nothing if no data
                    Log.e("UserActivity", "No data found for user");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("UserActivity", "Error fetching user data", databaseError.toException());
            }
        });
    }

    // Check if the user is a friend
    private void checkIfUserIsFriend(String friendNickname) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            DatabaseReference currentUserRef = databaseReference.child(currentUserId);

            currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Users currentUserData = dataSnapshot.getValue(Users.class);
                    if (currentUserData != null && currentUserData.getFriends() != null) {
                        List<String> friendsList = currentUserData.getFriends();
                        isFriend = friendsList.contains(friendNickname);
                    } else {
                        isFriend = false;
                    }
                    updateFriendshipButtonsVisibility(isFriend);
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

    // Add friend
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

                                // Add friend to Global friends list
                                Global.myFriendsLocation.add(findUserLocationByNickname(friendNickname));

                                // Update friends markers on the map
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

    private UserLocation findUserLocationByNickname(String nickname) {
        for (UserLocation userLoc : Global.allLocations) {
            if (userLoc.getNickName().equals(nickname)) {
                return userLoc;
            }
        }
        return null;
    }

    // Remove friend
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
                                        break; // Exit loop after removing friend
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

    // Method to update friends markers on the map
    private void updateMapActivityFriendsMarkers(String friendNickname) {
        for (UserLocation userLoc : Global.allLocations) {
            if (userLoc.getNickName().equals(friendNickname)) {
                Global.myFriendsLocation.add(userLoc);
                break; // Exit loop after adding friend
            }
        }
    }

    // Method to update visibility of friendship buttons
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
