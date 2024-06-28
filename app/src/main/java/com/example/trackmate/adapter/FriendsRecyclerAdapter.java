package com.example.trackmate.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackmate.FriendRequest;
import com.example.trackmate.R;
import com.example.trackmate.Users;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class FriendsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_FRIEND = 0;
    private static final int TYPE_REQUEST = 1;

    private List<Object> items;
    private Context context;
    private OnImageClick onImageClick;

    public interface OnImageClick {
        void onImageClick(String nickName);
    }

    public FriendsRecyclerAdapter(List<Object> items, Context context) {
        this.items = items;
        this.context = context;
    }

    public void setOnImageClick(OnImageClick onImageClick) {
        this.onImageClick = onImageClick;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof Users) {
            return TYPE_FRIEND;
        } else {
            return TYPE_REQUEST;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FRIEND) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
            return new FriendViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_friend_request, parent, false);
            return new RequestViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_FRIEND) {
            ((FriendViewHolder) holder).bind((Users) items.get(position));
        } else {
            ((RequestViewHolder) holder).bind((FriendRequest) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;

        FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onImageClick != null) {
                        Users user = (Users) items.get(getAdapterPosition());
                        onImageClick.onImageClick(user.getNickname());
                    }
                }
            });
        }

        void bind(Users user) {
            nameTextView.setText(user.getNickname());
        }
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        Button acceptButton;
        Button declineButton;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            declineButton = itemView.findViewById(R.id.declineButton);
        }

        void bind(FriendRequest request) {
            nameTextView.setText(request.getNickname());

            acceptButton.setOnClickListener(v -> handleAccept(request));
            declineButton.setOnClickListener(v -> handleDecline(request));
        }

        private void handleAccept(FriendRequest request) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
                usersRef.child(currentUser.getUid()).child("friends").push().setValue(request.getSender());
                usersRef.child(request.getSender()).child("friends").push().setValue(currentUser.getUid());
                removeRequest(request);
            }
        }

        private void handleDecline(FriendRequest request) {
            removeRequest(request);
        }

        private void removeRequest(FriendRequest request) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUser.getUid()).child("friendRequests");
                requestsRef.orderByChild("nickname").equalTo(request.getNickname()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            snapshot.getRef().removeValue();
                        }
                        items.remove(request);
                        notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Обработка ошибки
                    }
                });
            }
        }
    }
}
