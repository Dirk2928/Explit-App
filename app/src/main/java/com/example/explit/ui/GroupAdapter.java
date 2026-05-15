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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    public interface OnGroupLongClickListener {
        void onGroupLongClick(Group group);
    }

    private final List<Group> groups = new ArrayList<>();
    private final OnGroupClickListener listener;
    private final OnGroupLongClickListener longClickListener;
    private final Map<Long, Boolean> unpaidMap = new HashMap<>();

    public GroupAdapter(OnGroupClickListener listener, OnGroupLongClickListener longClickListener) {
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    public void setGroups(List<Group> list) {
        setGroups(list, new HashMap<>());
    }

    public void setGroups(List<Group> list, Map<Long, Boolean> unpaidStatus) {
        groups.clear();
        groups.addAll(list);
        unpaidMap.clear();
        if (unpaidStatus != null) unpaidMap.putAll(unpaidStatus);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groups.get(position);
        holder.name.setText(group.getName());
        if (group.isPinned()) {
            holder.category.setText("📌 " + group.getCategory());
        } else {
            holder.category.setText(group.getCategory());
        }
        holder.itemView.setOnClickListener(v -> listener.onGroupClick(group));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onGroupLongClick(group);
            return true;
        });

        if (unpaidMap.containsKey(group.getId())) {
            Boolean status = unpaidMap.get(group.getId());
            if (status == null || !status) {
                holder.itemView.setBackgroundColor(0xFFFFEBEE);
            } else {
                holder.itemView.setBackgroundColor(0xFFFFFDE7);
            }
        } else {
            holder.itemView.setBackgroundColor(0xFFFFFFFF);
        }
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView category;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_group_name);
            category = itemView.findViewById(R.id.text_group_category);
        }
    }
}