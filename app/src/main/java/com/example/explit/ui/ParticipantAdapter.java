package com.example.explit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.model.Participant;

import java.util.ArrayList;
import java.util.List;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder> {
    private final List<Participant> participants = new ArrayList<>();

    // ---------------
    // setParticipants
    public void setParticipants(List<Participant> list) {
        participants.clear();
        participants.addAll(list);
        notifyDataSetChanged();
    }

    // ---------------
    // onCreateViewHolder
    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant, parent, false);
        return new ParticipantViewHolder(view);
    }

    // ---------------
    // onBindViewHolder
    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        holder.label.setText(participants.get(position).getDisplayName());
    }

    // ---------------
    // getItemCount
    @Override
    public int getItemCount() {
        return participants.size();
    }

    static class ParticipantViewHolder extends RecyclerView.ViewHolder {
        TextView label;

        // ---------------
        // ParticipantViewHolder
        ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.text_participant);
        }
    }
}
