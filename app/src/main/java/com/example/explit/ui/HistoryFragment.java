package com.example.explit.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.data.ExplitRepository;
import com.example.explit.model.Event;
import com.example.explit.model.ExpenseItem;
import com.example.explit.model.ItemAssignment;
import com.example.explit.model.Participant;
import com.example.explit.model.Receipt;
import com.example.explit.util.SplitCalculator;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryFragment extends Fragment {

    private ExplitRepository repository;
    private RecyclerView recyclerView;
    private EventHistoryAdapter adapter;
    private SharedPreferences prefs;
    private EditText editSearch;
    private ChipGroup chipGroupFilters;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new ExplitRepository(requireContext());
        prefs = requireContext().getSharedPreferences("explit_prefs", Context.MODE_PRIVATE);

        recyclerView = view.findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EventHistoryAdapter(event -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, SummaryFragment.newInstance(event.getId()))
                    .addToBackStack(null)
                    .commit();
        }, null);
        recyclerView.setAdapter(adapter);

        editSearch = view.findViewById(R.id.edit_search_history);
        chipGroupFilters = view.findViewById(R.id.chip_group_filters);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                filterEvents();
            }
        });

        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> filterEvents());

        loadHistory();
    }

    private void loadHistory() {
        filterEvents();
    }

    private void filterEvents() {
        String query = editSearch.getText().toString().trim().toLowerCase();
        List<Event> filtered = repository.getAllEvents();

        if (!query.isEmpty()) {
            List<Event> searchResults = new ArrayList<>();
            for (Event e : filtered) {
                if (e.getName().toLowerCase().contains(query)) {
                    searchResults.add(e);
                }
            }
            filtered = searchResults;
        }

        Map<Long, String> eventTotals = new HashMap<>();
        Map<Long, String> userBalances = new HashMap<>();
        String userName = prefs.getString("user_name", "");
        for (Event event : filtered) {
            List<ExpenseItem> items = repository.getExpenseItemsForEvent(event.getId());
            Map<Long, List<ItemAssignment>> assignments = repository.getAssignmentsByItem(event.getId());
            List<Receipt> receipts = repository.getReceipts(event.getId());
            Map<Long, SplitCalculator.PersonTotal> totals = SplitCalculator.calculateTotals(items, assignments, receipts);
            double grandTotal = 0;
            for (SplitCalculator.PersonTotal pt : totals.values()) grandTotal += pt.getTotal();
            eventTotals.put(event.getId(), String.format("₱%.2f", grandTotal));

            if (!userName.isEmpty()) {
                List<Participant> participants = repository.getEventParticipants(event.getId());
                long myId = -1;
                for (Participant p : participants) {
                    if (p.getName().equalsIgnoreCase(userName) || (p.getNickname() != null && p.getNickname().equalsIgnoreCase(userName))) {
                        myId = p.getId();
                        break;
                    }
                }
                if (myId != -1) {
                    long payerId = event.getPaidByParticipantId();
                    if (payerId == -1 && !participants.isEmpty()) payerId = participants.get(0).getId();
                    Map<Long, Double> paidMap = new HashMap<>();
                    if (payerId != -1) paidMap.put(payerId, grandTotal);
                    List<SplitCalculator.Settlement> settlements = SplitCalculator.calculateSettlements(totals, paidMap);
                    double myBalance = 0;
                    for (SplitCalculator.Settlement s : settlements) {
                        if (s.fromId == myId) myBalance -= s.amount;
                        else if (s.toId == myId) myBalance += s.amount;
                    }
                    if (myBalance < -0.01) userBalances.put(event.getId(), String.format("You owe ₱%.2f", Math.abs(myBalance)));
                    else if (myBalance > 0.01) userBalances.put(event.getId(), String.format("You are owed ₱%.2f", myBalance));
                    else userBalances.put(event.getId(), "Settled up");
                }
            }
        }
        adapter.setEvents(filtered, eventTotals, userBalances, null);
    }
}