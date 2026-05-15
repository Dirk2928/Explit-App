package com.example.explit.ui;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.ContentValues;

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
import com.example.explit.model.ItemAssignment;
import com.example.explit.model.Participant;
import com.example.explit.model.Receipt;
import com.example.explit.util.SplitCalculator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventDetailFragment extends Fragment {
    private static final String ARG_GROUP_ID = "arg_group_id";
    private static final String ARG_EVENT_ID = "arg_event_id";

    private long groupId;
    private ExplitRepository repository;
    private boolean isNewGroup;

    private EditText groupNameInput;
    private Spinner categorySpinner;
    private RecyclerView participantRecycler;
    private RecyclerView eventRecycler;

    private ParticipantAdapter participantAdapter;
    private EventHistoryAdapter eventAdapter;

    private List<Participant> participants = new ArrayList<>();
    private List<Event> events = new ArrayList<>();

    public static EventDetailFragment newInstance(long groupId, long eventId) {
        Bundle args = new Bundle();
        args.putLong(ARG_GROUP_ID, groupId);
        args.putLong(ARG_EVENT_ID, eventId);
        EventDetailFragment fragment = new EventDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            groupId = getArguments().getLong(ARG_GROUP_ID, -1);
        }
        isNewGroup = (groupId == -1);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new ExplitRepository(requireContext());

        groupNameInput = view.findViewById(R.id.edit_event_name);
        groupNameInput.setHint("Group Name");

        categorySpinner = view.findViewById(R.id.spinner_category);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Normal", "Family", "Friends", "Business", "School"});
        categorySpinner.setAdapter(categoryAdapter);

        participantRecycler = view.findViewById(R.id.recycler_participants);
        eventRecycler = view.findViewById(R.id.recycler_expenses);

        participantAdapter = new ParticipantAdapter();
        participantAdapter.setOnParticipantRemoveListener((participant, position) -> {
            if (isNewGroup) {
                participants.remove(position);
                participantAdapter.setParticipants(participants);
            } else {
                SQLiteDatabase db = new ExplitDbHelper(requireContext()).getWritableDatabase();
                db.delete("participants", "id=?", new String[]{String.valueOf(participant.getId())});
                ContentValues v = new ContentValues();
                v.put("last_modified", System.currentTimeMillis());
                db.update("groups", v, "id=?", new String[]{String.valueOf(groupId)});
                db.close();
                loadData();
                Toast.makeText(getContext(), "Participant removed. Past events unchanged.", Toast.LENGTH_SHORT).show();
            }
        });
        participantRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        participantRecycler.setAdapter(participantAdapter);

        eventAdapter = new EventHistoryAdapter(
                event -> {
                    List<ExpenseItem> items = repository.getExpenseItemsForEvent(event.getId());
                    if (items.isEmpty()) {
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.main_container, AddReceiptItemsFragment.newInstance(groupId, event.getId()))
                                .addToBackStack(null)
                                .commit();
                    } else {
                        Map<Long, List<ItemAssignment>> assignments = repository.getAssignmentsByItem(event.getId());
                        List<Receipt> receipts = repository.getReceipts(event.getId());
                        Map<Long, SplitCalculator.PersonTotal> totals = SplitCalculator.calculateTotals(items, assignments, receipts);
                        double grandTotal = 0;
                        for (SplitCalculator.PersonTotal pt : totals.values()) grandTotal += pt.getTotal();

                        double savedBillTotal = 0;
                        if (!receipts.isEmpty()) {
                            savedBillTotal = receipts.get(0).getServiceCharge();
                        }

                        if (Math.abs(grandTotal - savedBillTotal) < 0.01 && savedBillTotal > 0) {
                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.main_container, SummaryFragment.newInstance(event.getId()))
                                    .addToBackStack(null)
                                    .commit();
                        } else {
                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.main_container, AddReceiptItemsFragment.newInstance(groupId, event.getId()))
                                    .addToBackStack(null)
                                    .commit();
                        }
                    }
                },
                event -> {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(event.getName())
                            .setItems(new String[]{"Edit Items", "Delete Event"}, (dialog, which) -> {
                                if (which == 0) {
                                    getParentFragmentManager().beginTransaction()
                                            .replace(R.id.main_container, AddReceiptItemsFragment.newInstance(groupId, event.getId()))
                                            .addToBackStack(null)
                                            .commit();
                                } else {
                                    new MaterialAlertDialogBuilder(requireContext())
                                            .setTitle("Delete Event")
                                            .setMessage("Delete \"" + event.getName() + "\" and all its data?")
                                            .setPositiveButton("Delete", (d, w) -> {
                                                List<ExpenseItem> items = repository.getExpenseItemsForEvent(event.getId());
                                                for (ExpenseItem item : items) {
                                                    repository.deleteExpenseItem(item.getId());
                                                }
                                                SQLiteDatabase db = new ExplitDbHelper(requireContext()).getWritableDatabase();
                                                db.delete("receipts", "event_id=?", new String[]{String.valueOf(event.getId())});
                                                db.delete("events", "id=?", new String[]{String.valueOf(event.getId())});
                                                db.close();
                                                loadData();
                                                Toast.makeText(getContext(), "Event deleted", Toast.LENGTH_SHORT).show();
                                            })
                                            .setNegativeButton("Cancel", null)
                                            .show();
                                }
                            })
                            .show();
                }
        );
        eventRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        eventRecycler.setAdapter(eventAdapter);

        view.findViewById(R.id.button_add_participant).setOnClickListener(v -> {
            showAddParticipantDialog();
        });

        if (isNewGroup) {
            view.findViewById(R.id.button_add_receipt).setVisibility(View.VISIBLE);
            ((android.widget.Button) view.findViewById(R.id.button_add_receipt)).setText("Save Group");
            view.findViewById(R.id.button_add_receipt).setOnClickListener(v -> saveNewGroup());
        } else {
            view.findViewById(R.id.button_add_receipt).setVisibility(View.GONE);
        }

        view.findViewById(R.id.button_add_item).setOnClickListener(v -> {
            if (isNewGroup) {
                Toast.makeText(getContext(), "Save the group first", Toast.LENGTH_SHORT).show();
                return;
            }
            showAddEventDialog();
        });

        if (!isNewGroup) {
            loadData();
        } else {
            eventRecycler.setVisibility(View.GONE);
            view.findViewById(R.id.button_add_item).setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!isNewGroup && groupId > 0) {
            String name = groupNameInput.getText().toString().trim();
            String category = categorySpinner.getSelectedItem().toString();
            if (!name.isEmpty()) {
                SQLiteDatabase db = new ExplitDbHelper(requireContext()).getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("name", name);
                values.put("category", category);
                db.update("groups", values, "id=?", new String[]{String.valueOf(groupId)});
                ContentValues v = new ContentValues();
                v.put("last_modified", System.currentTimeMillis());
                db.update("groups", v, "id=?", new String[]{String.valueOf(groupId)});
                db.close();
            }
        }
    }

    private void saveNewGroup() {
        String name = groupNameInput.getText().toString().trim();
        if (name.isEmpty()) {
            groupNameInput.setError("Group name is required");
            return;
        }

        String category = categorySpinner.getSelectedItem().toString();
        groupId = repository.createGroup(name, category);

        for (Participant p : participants) {
            repository.addParticipant(groupId, p.getName(), p.getNickname());
        }

        Toast.makeText(getContext(), "Group saved!", Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, new DashboardFragment())
                .commit();
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).setNavSelected(R.id.nav_home);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isNewGroup) {
            loadData();
        }
    }

    private void loadData() {
        participants = repository.getParticipants(groupId);
        participantAdapter.setParticipants(participants);

        events = repository.getEventsByGroup(groupId);
        Map<Long, String> eventTotals = new HashMap<>();
        Map<Long, String> userBalances = new HashMap<>();

        for (Event event : events) {
            List<ExpenseItem> items = repository.getExpenseItemsForEvent(event.getId());
            Map<Long, List<ItemAssignment>> assignments = repository.getAssignmentsByItem(event.getId());
            List<Receipt> receipts = repository.getReceipts(event.getId());
            Map<Long, SplitCalculator.PersonTotal> totals = SplitCalculator.calculateTotals(items, assignments, receipts);
            double grandTotal = 0;
            for (SplitCalculator.PersonTotal pt : totals.values()) grandTotal += pt.getTotal();
            eventTotals.put(event.getId(), String.format("₱%.2f", grandTotal));
        }
        Map<Long, Boolean> unpaidMap = new HashMap<>();
        for (Event event : events) {
            unpaidMap.put(event.getId(), repository.hasUnpaidSettlements(event.getId()));
        }
        Map<Long, Boolean> incompleteMap = new HashMap<>();
        for (Event event : events) {
            incompleteMap.put(event.getId(), repository.isEventIncomplete(event.getId()));
        }
        eventAdapter.setEvents(events, eventTotals, userBalances, unpaidMap, incompleteMap);
        com.example.explit.model.Group group = null;
        List<com.example.explit.model.Group> groups = repository.getGroups();
        for (com.example.explit.model.Group g : groups) {
            if (g.getId() == groupId) {
                group = g;
                break;
            }
        }
        if (group != null) {
            groupNameInput.setText(group.getName());
            String cat = group.getCategory();
            for (int i = 0; i < categorySpinner.getCount(); i++) {
                if (categorySpinner.getItemAtPosition(i).toString().equals(cat)) {
                    categorySpinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private void showAddParticipantDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        EditText nameInput = new EditText(requireContext());
        nameInput.setHint("Name");
        layout.addView(nameInput);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Participant")
                .setView(layout)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (!name.isEmpty()) {
                        if (isNewGroup) {
                            participants.add(new Participant(-1, -1, name, ""));                            participantAdapter.setParticipants(participants);
                        } else {
                            repository.addParticipant(groupId, name, "");                            loadData();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    private void showAddEventDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        EditText eventNameInput = new EditText(requireContext());
        eventNameInput.setHint("Event name (e.g., Saturday Dinner)");
        layout.addView(eventNameInput);

        String[] categories = {"Food", "Party", "Travel", "Shopping", "Bills", "Other"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("New Event")
                .setView(layout)
                .setSingleChoiceItems(categories, -1, null)
                .setPositiveButton("Next", (dialog, which) -> {
                    String name = eventNameInput.getText().toString().trim();
                    if (name.isEmpty()) name = "New Event";

                    int selectedIndex = ((androidx.appcompat.app.AlertDialog) dialog).getListView().getCheckedItemPosition();
                    String category = selectedIndex >= 0 ? categories[selectedIndex] : "Other";

                    showParticipantChecklist(name, category);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showParticipantChecklist(String eventName, String eventCategory) {
        List<Participant> groupParticipants = repository.getParticipants(groupId);
        if (groupParticipants.isEmpty()) {
            Toast.makeText(getContext(), "Add participants to the group first", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[groupParticipants.size()];
        boolean[] checked = new boolean[groupParticipants.size()];
        for (int i = 0; i < groupParticipants.size(); i++) {
            names[i] = groupParticipants.get(i).getDisplayName();
            checked[i] = true;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Participants")
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Create Event", (dialog, which) -> {
                    long newEventId = repository.createEvent(groupId, eventName, "₱", -1);
                    for (int i = 0; i < groupParticipants.size(); i++) {
                        if (checked[i]) {
                            Participant p = groupParticipants.get(i);
                            repository.addEventParticipant(newEventId, p.getId(), p.getName(), p.getNickname());
                        }
                    }
                    Toast.makeText(getContext(), "Event created: " + eventName, Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}