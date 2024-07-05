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
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.squareup.picasso.Picasso;

public class SearchUserRecyclerAdapter extends FirebaseRecyclerAdapter<Users, SearchUserRecyclerAdapter.UserModelViewHolder> {

    private Context context;
    private OnImageClick mListener;

    public SearchUserRecyclerAdapter(@NonNull FirebaseRecyclerOptions<Users> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull UserModelViewHolder holder, int position, @NonNull Users model) {
        holder.usernameText.setText(model.getNickname());
        holder.phoneText.setText(model.getPhone());
        loadProfilePicture(model.getProfilePictureUrl(), holder.imageView);
    }

    @NonNull
    @Override
    public UserModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend_request, parent, false);
        return new UserModelViewHolder(view);
    }

    class UserModelViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        TextView phoneText;
        ImageView imageView;

        public UserModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            phoneText = itemView.findViewById(R.id.phone_text);
            imageView = itemView.findViewById(R.id.profile_pic_image_view);

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (mListener != null) {
                        if (position != RecyclerView.NO_POSITION) {
                            v.setBackgroundColor(Color.RED);
                            mListener.onImageClick(usernameText.getText().toString());
                        }
                    }
                }
            });
        }
    }

    public interface OnImageClick {
        void onImageClick(String nickName);
    }

    public void setOnImageClick(OnImageClick listener) {
        mListener = listener;
    }

    private void loadProfilePicture(String profilePictureUrl, ImageView imageView) {
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            // Загрузка изображения с использованием Picasso
            Picasso.get().load(profilePictureUrl).placeholder(R.drawable.user_pic).into(imageView);
        } else {
            // Установка изображения по умолчанию, если нет изображения профиля
            imageView.setImageResource(R.drawable.user_pic);
        }
    }
}
