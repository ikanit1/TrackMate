package com.example.trackmate.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackmate.R;
import com.example.trackmate.Users;

import java.util.ArrayList;

public class FriendsRecyclerAdapter extends RecyclerView.Adapter<FriendsRecyclerAdapter.ViewHolder> {

    private ArrayList<Users> friendsList;
    private Context context;
    private OnImageClick mListener;

    public FriendsRecyclerAdapter(ArrayList<Users> friendsList, Context context) {
        this.friendsList = friendsList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_recycler_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Users user = friendsList.get(position);
        holder.usernameText.setText(user.getNickname());
        holder.phoneText.setText(user.getPhone());

        // Пример загрузки изображения (замените на свой способ загрузки)
        // Picasso.get().load(user.getImageUrl()).into(holder.imageView);

        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onImageClick(user.getNickname());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        TextView phoneText;
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            phoneText = itemView.findViewById(R.id.phone_text);
            imageView = itemView.findViewById(R.id.profile_pic_image_view);
        }
    }

    public interface OnImageClick {
        void onImageClick(String nickName);
    }

    public void setOnImageClick(OnImageClick listener) {
        mListener = listener;
    }
}
