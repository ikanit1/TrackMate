package com.example.trackmate.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.trackmate.R;
import com.example.trackmate.Users;

import java.util.ArrayList;

public class FriendsRecyclerAdapter extends RecyclerView.Adapter<FriendsRecyclerAdapter.FriendsViewHolder> {

    private ArrayList<Object> items;
    private Context context;
    private OnImageClick onImageClick;

    public FriendsRecyclerAdapter(ArrayList<Object> items, Context context) {
        this.items = items;
        this.context = context;
    }

    @NonNull
    @Override
    public FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new FriendsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendsViewHolder holder, int position) {
        Object item = items.get(position);

        if (item instanceof Users) {
            Users user = (Users) item;
            holder.tvNickname.setText(user.getNickname());
            holder.tvPhone.setText(user.getPhone());

            // Load profile picture using Glide
            Glide.with(context)
                    .load(user.getProfilePictureUrl())
                    .circleCrop()
                    .placeholder(R.drawable.user_pic) // Add a placeholder if needed
                    .into(holder.ivProfilePicture);

            holder.ivProfilePicture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onImageClick != null) {
                        onImageClick.onImageClick(user.getNickname());
                    }
                }
            });
        }
        // Handle other item types if needed
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setOnImageClick(OnImageClick onImageClick) {
        this.onImageClick = onImageClick;
    }

    public interface OnImageClick {
        void onImageClick(String nickName);
    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder {
        TextView tvNickname;
        TextView tvPhone;
        ImageView ivProfilePicture;

        public FriendsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNickname = itemView.findViewById(R.id.tvNickname);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            ivProfilePicture = itemView.findViewById(R.id.ivProfilePicture);
        }
    }
}
