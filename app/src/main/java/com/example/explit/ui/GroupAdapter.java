package com.example.explit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.model.Group;

import java.util.ArrayList;
import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {
    private final List<Group> groups = new ArrayList<>();
    private final OnGroupClickListener listener;

    public interface OnGroupClickListener {
        // ---------------
        // onGroupClick
        void onGroupClick(Group group);
    }

    // ---------------
    // GroupAdapter
    public GroupAdapter(OnGroupClickListener listener) {
        this.listener = listener;
    }

    // ---------------
    // setGroups
    public void setGroups(List<Group> list) {
        groups.clear();
        groups.addAll(list);
        notifyDataSetChanged();
    }

    // ---------------
    // onCreateViewHolder
    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    // ---------------
    // onBindViewHolder
    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groups.get(position);
        holder.name.setText(group.getName());
        holder.category.setText(group.getCategory());
        holder.itemView.setOnClickListener(v -> listener.onGroupClick(group));
    }

    // ---------------
    // getItemCount
    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView category;

        // ---------------
        // GroupViewHolder
        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_group_name);
            category = itemView.findViewById(R.id.text_group_category);
        }
    }
}
