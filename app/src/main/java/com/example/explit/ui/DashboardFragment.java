package com.example.explit.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import com.example.explit.model.ItemAssignment;
import com.example.explit.model.Receipt;
import com.example.explit.model.SettlementStatus;
import com.example.explit.util.SplitCalculator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private ExplitRepository repository;
    private RecyclerView recyclerGroups;
    private GroupAdapter groupAdapter;
    private SharedPreferences prefs;
    private RecyclerView recyclerRecentOutings;
    private EventHistoryAdapter recentOutingsAdapter;
    private RecyclerView recyclerPendingPayments;
    private PendingPaymentAdapter pendingPaymentAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new ExplitRepository(requireContext());
        prefs = requireContext().getSharedPreferences("explit_prefs", Context.MODE_PRIVATE);

        recyclerGroups = view.findViewById(R.id.recycler_groups);

        setupGroupsRecyclerView();
        setupRecentOutingsRecyclerView(view);
        setupPendingPayments(view);

        view.findViewById(R.id.text_see_all_groups).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new GroupListFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void setupPendingPayments(View view) {
        recyclerPendingPayments = view.findViewById(R.id.recycler_pending_payments);
        recyclerPendingPayments.setLayoutManager(new LinearLayoutManager(requireContext()));
        pendingPaymentAdapter = new PendingPaymentAdapter();
        recyclerPendingPayments.setAdapter(pendingPaymentAdapter);
        pendingPaymentAdapter.setRepository(repository);
    }

    private void setupGroupsRecyclerView() {
        recyclerGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        groupAdapter = new GroupAdapter(
                group -> {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.main_container, EventDetailFragment.newInstance(group.getId(), -1))
                            .addToBackStack(null)
                            .commit();
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
                                loadData();
                                Toast.makeText(getContext(), "Group deleted", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
        );
        recyclerGroups.setAdapter(groupAdapter);
    }

    private void setupRecentOutingsRecyclerView(View view) {
        recyclerRecentOutings = view.findViewById(R.id.recycler_recent_outings);
        recyclerRecentOutings.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recentOutingsAdapter = new EventHistoryAdapter(event -> {
            List<ExpenseItem> items = repository.getExpenseItemsForEvent(event.getId());
            if (items.isEmpty()) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, AddReceiptItemsFragment.newInstance(event.getGroupId(), event.getId()))
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
                            .replace(R.id.main_container, AddReceiptItemsFragment.newInstance(event.getGroupId(), event.getId()))
                            .addToBackStack(null)
                            .commit();
                }
            }
        }, null);
        recyclerRecentOutings.setAdapter(recentOutingsAdapter);
    }

    private void loadData() {
        List<Group> pinnedGroups = repository.getPinnedGroups();
        List<Group> recentGroups = repository.getRecentGroups(2);
        List<Group> displayGroups = new ArrayList<>(pinnedGroups);
        for (Group rg : recentGroups) {
            boolean alreadyAdded = false;
            for (Group pg : pinnedGroups) {
                if (pg.getId() == rg.getId()) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) displayGroups.add(rg);
        }

        Map<Long, Boolean> groupStatusMap = new HashMap<>();
        for (Group g : displayGroups) {
            if (repository.groupHasUnpaidSettlements(g.getId())) {
                groupStatusMap.put(g.getId(), false);
            } else if (repository.groupHasIncompleteEvents(g.getId())) {
                groupStatusMap.put(g.getId(), true);
            }
        }
        groupAdapter.setGroups(displayGroups, groupStatusMap);
        List<Event> allEvents = repository.getAllEvents();
        List<Event> recentEvents = allEvents.size() > 5 ? allEvents.subList(0, 5) : allEvents;
        Map<Long, String> recentTotals = new HashMap<>();
        for (Event e : recentEvents) {
            List<ExpenseItem> items = repository.getExpenseItemsForEvent(e.getId());
            Map<Long, List<ItemAssignment>> assignments = repository.getAssignmentsByItem(e.getId());
            List<Receipt> receipts = repository.getReceipts(e.getId());
            Map<Long, SplitCalculator.PersonTotal> totals = SplitCalculator.calculateTotals(items, assignments, receipts);
            double grandTotal = 0;
            for (SplitCalculator.PersonTotal pt : totals.values()) grandTotal += pt.getTotal();
            recentTotals.put(e.getId(), String.format("₱%.2f", grandTotal));
        }
        Map<Long, Boolean> recentUnpaidMap = new HashMap<>();
        for (Event e : recentEvents) {
            recentUnpaidMap.put(e.getId(), repository.hasUnpaidSettlements(e.getId()));
        }
        Map<Long, Boolean> recentIncompleteMap = new HashMap<>();
        for (Event e : recentEvents) {
            recentIncompleteMap.put(e.getId(), repository.isEventIncomplete(e.getId()));
        }
        recentOutingsAdapter.setEvents(recentEvents, recentTotals, new HashMap<>(), recentUnpaidMap, recentIncompleteMap);
        List<SettlementStatus> unpaidList = repository.getUnpaidSettlements();
        pendingPaymentAdapter.setItems(unpaidList);
        if (unpaidList.isEmpty()) {
            recyclerPendingPayments.setVisibility(View.GONE);
        } else {
            recyclerPendingPayments.setVisibility(View.VISIBLE);
        }
    }
}
