package com.example.explit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.model.Participant;

import java.util.ArrayList;
import java.util.List;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder> {

    public interface OnParticipantRemoveListener {
        void onRemove(Participant participant, int position);
    }

    private final List<Participant> participants = new ArrayList<>();
    private OnParticipantRemoveListener removeListener;

    public void setOnParticipantRemoveListener(OnParticipantRemoveListener listener) {
        this.removeListener = listener;
    }

    public void setParticipants(List<Participant> list) {
        participants.clear();
        participants.addAll(list);
        notifyDataSetChanged();
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant_added, parent, false);
        return new ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        Participant participant = participants.get(position);
        holder.name.setText(participant.getName());

        String initials = "";
        if (!participant.getName().isEmpty()) {
            initials = participant.getName().substring(0, 1).toUpperCase();
        }
        holder.initials.setText(initials);

        holder.removeButton.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemove(participant, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }
    static class ParticipantViewHolder extends RecyclerView.ViewHolder {
        TextView initials;
        EditText name;
        ImageButton removeButton;
        ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            initials = itemView.findViewById(R.id.text_initials);
            name = itemView.findViewById(R.id.edit_participant_name);
            removeButton = itemView.findViewById(R.id.button_remove);
        }
    }
}