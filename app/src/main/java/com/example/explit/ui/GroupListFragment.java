package com.example.explit.ui;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.data.ExplitDbHelper;
import com.example.explit.data.ExplitRepository;
import com.example.explit.model.Event;
import com.example.explit.model.ExpenseItem;
import com.example.explit.model.Group;
import com.example.explit.model.Receipt;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupListFragment extends Fragment {
    private ExplitRepository repository;
    private GroupAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new ExplitRepository(requireContext());

        RecyclerView recyclerView = view.findViewById(R.id.recycler_groups);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new GroupAdapter(
                group -> {
                    Intent intent = new Intent(requireContext(), EventActivity.class);
                    intent.putExtra(EventActivity.EXTRA_GROUP_ID, group.getId());
                    startActivity(intent);
                },
                group -> {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Delete Group")
                            .setMessage("Delete \"" + group.getName() + "\" and all its data?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                List<Event> events = repository.getEventsByGroup(group.getId());
                                for (Event e : events) {
                                    List<ExpenseItem> items = repository.getExpenseItemsForEvent(e.getId());
                                    for (ExpenseItem item : items) {
                                        repository.deleteExpenseItem(item.getId());
                                    }
                                }
                                SQLiteDatabase db = new ExplitDbHelper(requireContext()).getWritableDatabase();
                                db.delete("receipts", "event_id IN (SELECT id FROM events WHERE group_id=?)", new String[]{String.valueOf(group.getId())});
                                db.delete("events", "group_id=?", new String[]{String.valueOf(group.getId())});
                                db.delete("participants", "group_id=?", new String[]{String.valueOf(group.getId())});
                                db.delete("groups", "id=?", new String[]{String.valueOf(group.getId())});
                                db.close();
                                loadGroups();
                                Toast.makeText(getContext(), "Group deleted", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
        );
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.button_add_group).setOnClickListener(v -> showAddGroupDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadGroups();
    }

    private void loadGroups() {
        List<Group> groups = repository.getGroups();
        Map<Long, Boolean> groupUnpaidMap = new HashMap<>();
        for (Group g : groups) {
            groupUnpaidMap.put(g.getId(), repository.groupHasUnpaidSettlements(g.getId()));
        }
        adapter.setGroups(groups, groupUnpaidMap);
    }

    private void showAddGroupDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        EditText nameInput = new EditText(requireContext());
        nameInput.setHint(getString(R.string.group_name));
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(nameInput);

        Spinner categorySpinner = new Spinner(requireContext());
        categorySpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, new String[]{"Family", "Friends", "Business", "School", "Normal"}));
        layout.addView(categorySpinner);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_group)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (!name.isEmpty()) {
                        repository.createGroup(name, (String) categorySpinner.getSelectedItem());
                        loadGroups();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}