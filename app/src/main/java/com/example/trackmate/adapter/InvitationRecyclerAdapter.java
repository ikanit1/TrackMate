package com.example.trackmate.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackmate.R;
import com.example.trackmate.FriendRequest;

import java.util.ArrayList;

public class InvitationRecyclerAdapter extends RecyclerView.Adapter<InvitationRecyclerAdapter.InvitationViewHolder> {

    private ArrayList<FriendRequest> invitations;

    public InvitationRecyclerAdapter(ArrayList<FriendRequest> invitations) {
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
        // Установите данные для элементов списка здесь
        FriendRequest invitation = invitations.get(position);
        // Пример: holder.senderName.setText(invitation.getSenderName());
    }

    @Override
    public int getItemCount() {
        // Добавляем проверку на null
        return (invitations != null) ? invitations.size() : 0;
    }

    public static class InvitationViewHolder extends RecyclerView.ViewHolder {
        // Определите элементы ViewHolder здесь

        public InvitationViewHolder(@NonNull View itemView) {
            super(itemView);
            // Инициализируйте элементы UI здесь
            // Пример: TextView senderName = itemView.findViewById(R.id.sender_name);
        }
    }
}
