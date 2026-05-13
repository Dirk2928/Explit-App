package com.example.explit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.model.Participant;
import com.example.explit.util.SplitCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettlementAdapter extends RecyclerView.Adapter<SettlementAdapter.ViewHolder> {

    private final List<SplitCalculator.Settlement> settlements = new ArrayList<>();
    private Map<Long, String> participantNames;

    public void setData(List<SplitCalculator.Settlement> settlements, Map<Long, String> participantNames) {
        this.settlements.clear();
        this.settlements.addAll(settlements);
        this.participantNames = participantNames;
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
    }

    @Override
    public int getItemCount() {
        return settlements.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView instruction, amount;

        ViewHolder(View v) {
            super(v);
            instruction = v.findViewById(R.id.text_settlement_instruction);
            amount = v.findViewById(R.id.text_settlement_amount);
        }
    }
}
