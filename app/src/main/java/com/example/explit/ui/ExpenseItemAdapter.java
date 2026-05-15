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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.widget.ImageButton;
import com.example.explit.model.ItemAssignment;
import com.example.explit.model.Participant;

public class ExpenseItemAdapter extends RecyclerView.Adapter<ExpenseItemAdapter.ExpenseViewHolder> {
    private final List<ExpenseItem> items = new ArrayList<>();
    private final OnItemLongClickListener listener;
    private Map<Long, List<ItemAssignment>> assignments = new HashMap<>();
    private List<Participant> participants = new ArrayList<>();

    public interface OnItemLongClickListener {
        void onLongClick(ExpenseItem item);
    }
    public ExpenseItemAdapter(OnItemLongClickListener listener) {
        this.listener = listener;
    }

    public void setItemsWithAssignments(List<ExpenseItem> list, Map<Long, List<ItemAssignment>> assignments, List<Participant> participants) {
        items.clear();
        items.addAll(list);
        this.assignments = assignments;
        this.participants = participants;
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense_bill, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        ExpenseItem item = items.get(position);
        holder.name.setText(item.getName());
        holder.amount.setText(String.format("₱%.2f", item.getAmount()));

        List<ItemAssignment> itemAssignments = assignments.get(item.getId());
        if (itemAssignments != null && !itemAssignments.isEmpty()) {
            if (participants != null && itemAssignments.size() == participants.size()) {
                holder.assignedTo.setText("Assigned to: All");
                holder.assignedTo.setVisibility(View.VISIBLE);
            } else {
                StringBuilder names = new StringBuilder("Assigned to: ");
                boolean first = true;
                for (ItemAssignment a : itemAssignments) {
                    String name = null;
                    if (participants != null) {
                        for (Participant p : participants) {
                            if (p.getId() == a.getParticipantId()) {
                                name = p.getName();
                                break;
                            }
                        }
                    }
                    if (name == null) name = "ID:" + a.getParticipantId();
                    if (!first) names.append(", ");
                    names.append(name);
                    first = false;
                }
                holder.assignedTo.setText(names.toString());
                holder.assignedTo.setVisibility(View.VISIBLE);
            }
        } else {
            holder.assignedTo.setVisibility(View.GONE);
        }

        holder.menuButton.setOnClickListener(v -> listener.onLongClick(item));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onLongClick(item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView amount;
        TextView assignedTo;
        ImageButton menuButton;
        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_item_name);
            amount = itemView.findViewById(R.id.text_item_price);
            assignedTo = itemView.findViewById(R.id.text_assigned_to);
            menuButton = itemView.findViewById(R.id.button_item_menu);
        }
    }
}
