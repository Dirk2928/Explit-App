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
import java.util.List;

public class EventHistoryAdapter extends RecyclerView.Adapter<EventHistoryAdapter.HistoryViewHolder> {
    private final List<Event> events = new ArrayList<>();
    private final OnEventClickListener listener;

    public interface OnEventClickListener {
        // ---------------
        // onClick
        void onClick(Event event);
    }

    // ---------------
    // EventHistoryAdapter
    public EventHistoryAdapter(OnEventClickListener listener) {
        this.listener = listener;
    }

    // ---------------
    // setEvents
    public void setEvents(List<Event> list) {
        events.clear();
        events.addAll(list);
        notifyDataSetChanged();
    }

    // ---------------
    // onCreateViewHolder
    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_history, parent, false);
        return new HistoryViewHolder(view);
    }

    // ---------------
    // onBindViewHolder
    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Event event = events.get(position);
        holder.name.setText(event.getName());
        holder.meta.setText("Currency: " + event.getCurrency());
        holder.itemView.setOnClickListener(v -> listener.onClick(event));
    }

    // ---------------
    // getItemCount
    @Override
    public int getItemCount() {
        return events.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView meta;

        // ---------------
        // HistoryViewHolder
        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_history_name);
            meta = itemView.findViewById(R.id.text_history_meta);
        }
    }
}
