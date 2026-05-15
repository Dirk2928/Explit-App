package com.example.explit.ui;

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
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.main_container, EventDetailFragment.newInstance(group.getId(), -1))
                            .addToBackStack(null)
                            .commit();
                },
                group -> {
                    String pinText = group.isPinned() ? "Unpin Group" : "Pin Group";
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(group.getName())
                            .setItems(new String[]{pinText, "Delete Group"}, (dialog, which) -> {
                                if (which == 0) {
                                    boolean newPinned = !group.isPinned();
                                    repository.setGroupPinned(group.getId(), newPinned);
                                    group.setPinned(newPinned);
                                    loadGroups();
                                    Toast.makeText(getContext(), newPinned ? "Group pinned" : "Group unpinned", Toast.LENGTH_SHORT).show();
                                } else {
                                    new MaterialAlertDialogBuilder(requireContext())
                                            .setTitle("Delete Group")
                                            .setMessage("Delete \"" + group.getName() + "\" and all its data?")
                                            .setPositiveButton("Delete", (d, w) -> {
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
                            })
                            .show();
                }
        );
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadGroups();
    }

    private void loadGroups() {
        List<Group> groups = repository.getGroups();
        Map<Long, Boolean> groupStatusMap = new HashMap<>();
        for (Group g : groups) {
            if (repository.groupHasUnpaidSettlements(g.getId())) {
                groupStatusMap.put(g.getId(), false);
            } else if (repository.groupHasIncompleteEvents(g.getId())) {
                groupStatusMap.put(g.getId(), true);
            }
        }
        adapter.setGroups(groups, groupStatusMap);
    }
}