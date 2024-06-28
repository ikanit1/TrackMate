package com.example.trackmate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Toast;

import com.example.trackmate.adapter.InvitationRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class InvitationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private InvitationRecyclerAdapter adapter;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;
    private ArrayList<FriendRequest> invitations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        invitations = new ArrayList<>();
        adapter = new InvitationRecyclerAdapter(invitations);
        recyclerView.setAdapter(adapter);

        loadInvitations();
    }

    private void loadInvitations() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            DatabaseReference currentUserRef = databaseReference.child(currentUserId).child("friendRequests");

            currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            FriendRequest request = snapshot.getValue(FriendRequest.class);
                            if (request != null) {
                                invitations.add(request);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(InvitationsActivity.this, "Error loading invitations", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
