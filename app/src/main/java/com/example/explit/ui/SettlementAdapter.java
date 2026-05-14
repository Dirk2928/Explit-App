package com.example.explit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.util.SplitCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettlementAdapter extends RecyclerView.Adapter<SettlementAdapter.ViewHolder> {

    private final List<SplitCalculator.Settlement> settlements = new ArrayList<>();
    private Map<Long, String> participantNames;
    private Map<String, Integer> paidStatus;
    private OnPaidToggleListener paidToggleListener;

    public interface OnPaidToggleListener {
        void onToggle(long fromId, long toId, boolean paid);
    }

    public void setData(List<SplitCalculator.Settlement> settlements, Map<Long, String> participantNames,
                        Map<String, Integer> paidStatus, OnPaidToggleListener listener) {
        this.settlements.clear();
        this.settlements.addAll(settlements);
        this.participantNames = participantNames;
        this.paidStatus = paidStatus;
        this.paidToggleListener = listener;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_settlement, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SplitCalculator.Settlement s = settlements.get(position);
        String from = participantNames.getOrDefault(s.fromId, "Unknown");
        String to = participantNames.getOrDefault(s.toId, "Unknown");

        holder.instruction.setText(from + " pays " + to);
        holder.amount.setText(String.format("₱%.2f", s.amount));

        String key = s.fromId + "_" + s.toId;
        boolean isPaid = paidStatus != null && paidStatus.containsKey(key) && paidStatus.get(key) == 1;
        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(isPaid);
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (paidToggleListener != null) {
                paidToggleListener.onToggle(s.fromId, s.toId, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return settlements.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView instruction, amount;
        CheckBox checkbox;

        ViewHolder(View v) {
            super(v);
            instruction = v.findViewById(R.id.text_settlement_instruction);
            amount = v.findViewById(R.id.text_settlement_amount);
            checkbox = v.findViewById(R.id.checkbox_paid);
        }
    }
}