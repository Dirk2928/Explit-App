package com.example.explit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.model.ExpenseItem;

import java.util.ArrayList;
import java.util.List;

public class ExpenseItemAdapter extends RecyclerView.Adapter<ExpenseItemAdapter.ExpenseViewHolder> {
    private final List<ExpenseItem> items = new ArrayList<>();
    private final OnItemLongClickListener listener;

    public interface OnItemLongClickListener {
        // ---------------
        // onLongClick
        void onLongClick(ExpenseItem item);
    }

    // ---------------
    // ExpenseItemAdapter
    public ExpenseItemAdapter(OnItemLongClickListener listener) {
        this.listener = listener;
    }

    // ---------------
    // setItems
    public void setItems(List<ExpenseItem> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    // ---------------
    // onCreateViewHolder
    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    // ---------------
    // onBindViewHolder
    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        ExpenseItem item = items.get(position);
        holder.name.setText(item.getName());
        holder.amount.setText(String.format("%.2f", item.getAmount()));
        holder.shared.setVisibility(item.isShared() ? View.VISIBLE : View.GONE);
        holder.itemView.setOnLongClickListener(v -> {
            listener.onLongClick(item);
            return true;
        });
    }

    // ---------------
    // getItemCount
    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView amount;
        TextView shared;

        // ---------------
        // ExpenseViewHolder
        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_item_name);
            amount = itemView.findViewById(R.id.text_item_amount);
            shared = itemView.findViewById(R.id.text_item_shared);
        }
    }
}
