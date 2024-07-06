package com.example.trackmate;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackmate.adapter.InvitationsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class InvitationsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private InvitationsAdapter adapter;
    private List<Invitation> invitationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        invitationList = new ArrayList<>();
        adapter = new InvitationsAdapter(invitationList);
        recyclerView.setAdapter(adapter);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            DatabaseReference invitationsRef = FirebaseDatabase.getInstance().getReference().child("Invitations");
            invitationsRef.orderByChild("receiverId").equalTo(currentUserId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    invitationList.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Invitation invitation = snapshot.getValue(Invitation.class);
                        if (invitation != null) {
                            invitationList.add(invitation);
                        }
                    }
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("InvitationsActivity", "Error fetching invitations", databaseError.toException());
                }
            });
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }
}
