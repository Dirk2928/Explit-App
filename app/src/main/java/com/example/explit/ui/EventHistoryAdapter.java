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
    private final OnEventLongClickListener longClickListener;
    private final Map<Long, Boolean> unpaidMap = new HashMap<>();
    private final Map<Long, Boolean> incompleteMap = new HashMap<>();

    public interface OnEventClickListener {
        void onClick(Event event);
    }
    public interface OnEventLongClickListener {
        void onLongClick(Event event);
    }

    public EventHistoryAdapter(OnEventClickListener listener, OnEventLongClickListener longClickListener) {
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    public void setEvents(List<Event> list, Map<Long, String> eventTotals, Map<Long, String> userBalances, Map<Long, Boolean> unpaidStatus, Map<Long, Boolean> incompleteStatus) {
        incompleteMap.clear();
        if (incompleteStatus != null) incompleteMap.putAll(incompleteStatus);        unpaidMap.clear();
        if (unpaidStatus != null) unpaidMap.putAll(unpaidStatus);
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
        if (event.getLastModified() > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
            holder.meta.setText(sdf.format(new java.util.Date(event.getLastModified())));
        } else {
            holder.meta.setText(event.getCurrency());
        }

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
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(event);
            }
            return true;
        });
        if (unpaidMap.containsKey(event.getId()) && unpaidMap.get(event.getId())) {
            holder.itemView.setBackgroundColor(0xFFFFEBEE);
        } else if (incompleteMap.containsKey(event.getId()) && incompleteMap.get(event.getId())) {
            holder.itemView.setBackgroundColor(0xFFFFFDE7);
        } else {
            holder.itemView.setBackgroundColor(0xFFFFFFFF);
        }
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
