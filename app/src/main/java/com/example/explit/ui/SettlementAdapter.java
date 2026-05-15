package com.example.explit.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.util.CurrencyHelper;
import com.example.explit.util.SplitCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettlementAdapter extends RecyclerView.Adapter<SettlementAdapter.ViewHolder> {

    private final List<SplitCalculator.Settlement> settlements = new ArrayList<>();
    private Map<Long, String> participantNames;
    private Map<String, Double> paidStatus;
    private OnPaidToggleListener paidToggleListener;
    private Context context;

    public interface OnPaidToggleListener {
        void onPay(long fromId, long toId, double totalAmount, double paidSoFar);
    }

    public void setData(List<SplitCalculator.Settlement> settlements, Map<Long, String> participantNames,
                        Map<String, Double> paidStatus, OnPaidToggleListener listener, Context context) {
        this.settlements.clear();
        this.settlements.addAll(settlements);
        this.participantNames = participantNames;
        this.paidStatus = paidStatus;
        this.paidToggleListener = listener;
        this.context = context;
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

        String key = s.fromId + "_" + s.toId;
        double paidSoFar = paidStatus != null && paidStatus.containsKey(key) ? paidStatus.get(key) : 0;
        boolean isFullyPaid;
        if (context != null && CurrencyHelper.shouldRound(context)) {
            isFullyPaid = Math.floor(paidSoFar) >= Math.floor(s.amount);
        } else {
            isFullyPaid = Math.abs(paidSoFar - s.amount) < 0.01;
        }

        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(isFullyPaid);
        holder.checkbox.setEnabled(false);

        String displayAmount;
        if (paidSoFar > 0 && !isFullyPaid) {
            displayAmount = String.format("₱%.2f (₱%.2f paid, ₱%.2f left)", s.amount, paidSoFar, s.amount - paidSoFar);
        } else if (isFullyPaid) {
            displayAmount = String.format("₱%.2f ✓ Paid", s.amount);
        } else {
            displayAmount = String.format("₱%.2f", s.amount);
        }
        holder.amount.setText(displayAmount);

        holder.itemView.setOnClickListener(v -> {
            if (!isFullyPaid && paidToggleListener != null) {
                paidToggleListener.onPay(s.fromId, s.toId, s.amount, paidSoFar);
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