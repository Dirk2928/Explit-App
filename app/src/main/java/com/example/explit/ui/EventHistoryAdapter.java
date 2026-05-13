package com.example.explit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.model.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventHistoryAdapter extends RecyclerView.Adapter<EventHistoryAdapter.HistoryViewHolder> {
    private final List<Event> events = new ArrayList<>();
    private final Map<Long, String> totals = new HashMap<>();
    private final Map<Long, String> balances = new HashMap<>();
    private final OnEventClickListener listener;

    public interface OnEventClickListener {
        void onClick(Event event);
    }

    public EventHistoryAdapter(OnEventClickListener listener) {
        this.listener = listener;
    }

    public void setEvents(List<Event> list, Map<Long, String> eventTotals, Map<Long, String> userBalances) {
        events.clear();
        events.addAll(list);
        totals.clear();
        totals.putAll(eventTotals);
        balances.clear();
        balances.putAll(userBalances);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Event event = events.get(position);
        holder.name.setText(event.getName());
        holder.meta.setText("Currency: " + event.getCurrency());
        
        String total = totals.get(event.getId());
        holder.total.setText(total != null ? total : "₱0.00");
        
        String balance = balances.get(event.getId());
        if (balance != null && !balance.isEmpty()) {
            holder.balance.setVisibility(View.VISIBLE);
            holder.balance.setText(balance);
        } else {
            holder.balance.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView name, meta, total, balance;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_history_name);
            meta = itemView.findViewById(R.id.text_history_meta);
            total = itemView.findViewById(R.id.text_history_total);
            balance = itemView.findViewById(R.id.text_history_balance);
        }
    }
}
