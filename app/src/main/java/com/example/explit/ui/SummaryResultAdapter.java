package com.example.explit.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.model.Participant;
import com.example.explit.util.SplitCalculator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SummaryResultAdapter extends RecyclerView.Adapter<SummaryResultAdapter.ViewHolder> {

    private final List<Participant> participants = new ArrayList<>();
    private Map<Long, SplitCalculator.PersonTotal> totals;
    private double grandTotal = 0;
    private Context context;

    public void setData(List<Participant> participants, Map<Long, SplitCalculator.PersonTotal> totals, Context context) {
        this.participants.clear();
        this.participants.addAll(participants);
        this.totals = totals;
        this.context = context;
        this.grandTotal = 0;
        if (totals != null) {
            for (SplitCalculator.PersonTotal pt : totals.values()) {
                grandTotal += pt.getTotal();
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_person_result, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Participant p = participants.get(position);
        String displayName = p.getDisplayName();
        holder.name.setText(displayName);

        String initials = "";
        if (displayName != null && !displayName.isEmpty()) {
            initials = displayName.substring(0, 1).toUpperCase();
        }
        holder.initials.setText(initials);

        if (totals != null && totals.containsKey(p.getId())) {
            double amount = totals.get(p.getId()).getTotal();
            holder.amount.setText(String.format("₱%.2f", amount));
            int progress = grandTotal > 0 ? (int)((amount / grandTotal) * 100) : 0;
            holder.progress.setProgress(progress);
        } else {
            holder.amount.setText("₱0.00");
            holder.progress.setProgress(0);
        }

        holder.paidAmount.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, initials, amount, paidAmount;
        LinearProgressIndicator progress;

        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.text_name);
            initials = v.findViewById(R.id.text_initials);
            amount = v.findViewById(R.id.text_status_amount);
            paidAmount = v.findViewById(R.id.text_paid_amount);
            progress = v.findViewById(R.id.progress_share);
        }
    }
}