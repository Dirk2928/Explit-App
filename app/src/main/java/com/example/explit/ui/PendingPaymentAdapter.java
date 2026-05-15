package com.example.explit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.data.ExplitRepository;
import com.example.explit.model.Participant;
import com.example.explit.model.SettlementStatus;

import java.util.ArrayList;
import java.util.List;

public class PendingPaymentAdapter extends RecyclerView.Adapter<PendingPaymentAdapter.ViewHolder> {

    private final List<SettlementStatus> items = new ArrayList<>();
    private ExplitRepository repository;

    public void setRepository(ExplitRepository repository) {
        this.repository = repository;
    }

    public void setItems(List<SettlementStatus> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_payment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SettlementStatus s = items.get(position);

        String fromName = getParticipantName(s.eventId, s.fromId);
        String toName = getParticipantName(s.eventId, s.toId);

        holder.payerName.setText(fromName);
        holder.recipientName.setText(toName);
        holder.amount.setText(String.format("₱%.2f", s.amount));
        holder.eventInfo.setText(s.eventName + " • " + s.groupName);
    }

    private String getParticipantName(long eventId, long participantId) {
        if (repository == null) return "ID: " + participantId;
        List<Participant> participants = repository.getEventParticipants(eventId);
        for (Participant p : participants) {
            if (p.getId() == participantId) return p.getDisplayName();
        }
        return "ID: " + participantId;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView payerName, recipientName, amount, eventInfo;

        ViewHolder(View v) {
            super(v);
            payerName = v.findViewById(R.id.text_payer);
            recipientName = v.findViewById(R.id.text_recipient);
            amount = v.findViewById(R.id.text_amount);
            eventInfo = v.findViewById(R.id.text_event_info);
        }
    }
}
