package com.example.trackmate.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackmate.Invitation;
import com.example.trackmate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class InvitationsAdapter extends RecyclerView.Adapter<InvitationsAdapter.InvitationViewHolder> {
    private List<Invitation> invitations;

    public InvitationsAdapter(List<Invitation> invitations) {
        this.invitations = invitations;
    }

    @NonNull
    @Override
    public InvitationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invitation, parent, false);
        return new InvitationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvitationViewHolder holder, int position) {
        Invitation invitation = invitations.get(position);
        holder.bind(invitation);
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    class InvitationViewHolder extends RecyclerView.ViewHolder {
        TextView senderName;
        Button acceptButton;
        Button rejectButton;

        public InvitationViewHolder(@NonNull View itemView) {
            super(itemView);
            senderName = itemView.findViewById(R.id.sender_name);
            acceptButton = itemView.findViewById(R.id.accept_button);
            rejectButton = itemView.findViewById(R.id.reject_button);
        }

        public void bind(Invitation invitation) {
            // Fetch sender's nickname and display
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(invitation.getSenderId());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String nickname = dataSnapshot.child("nickname").getValue(String.class);
                    senderName.setText(nickname);

                    // Set click listeners after getting nickname
                    acceptButton.setOnClickListener(v -> {
                        updateInvitationStatus(invitation, "accepted");
                        addFriend(nickname);
                    });

                    rejectButton.setOnClickListener(v -> {
                        updateInvitationStatus(invitation, "rejected");
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("InvitationsAdapter", "Error fetching user data", databaseError.toException());
                }
            });
        }

        private void updateInvitationStatus(Invitation invitation, String status) {
            DatabaseReference invitationsRef = FirebaseDatabase.getInstance().getReference().child("Invitations");
            invitationsRef.child(invitation.getInvitationId()).child("status").setValue(status).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    invitations.remove(invitation);
                    notifyDataSetChanged();
                }
            });
        }

        private void addFriend(String friendNickname) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String currentUserId = currentUser.getUid();
                DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId).child("friends");

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
                                    Toast.makeText(itemView.getContext(), "Friend added", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(itemView.getContext(), "Error adding friend", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("InvitationsAdapter", "Error fetching user data", databaseError.toException());
                    }
                });
            }
        }
    }
}
