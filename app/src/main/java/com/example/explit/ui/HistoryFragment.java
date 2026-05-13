package com.example.explit.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryFragment extends Fragment {

    private ExplitRepository repository;
    private RecyclerView recyclerView;
    private EventHistoryAdapter adapter;
    private SharedPreferences prefs;

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
        });
        recyclerView.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        List<Event> events = repository.getAllEvents();
        String userName = prefs.getString("user_name", "");

        Map<Long, String> eventTotals = new HashMap<>();
        Map<Long, String> userBalances = new HashMap<>();

        for (Event event : events) {
            List<Participant> participants = repository.getParticipants(event.getGroupId());
            List<ExpenseItem> items = repository.getExpenseItemsForEvent(event.getId());
            Map<Long, List<ItemAssignment>> assignments = repository.getAssignmentsByItem(event.getId());
            List<Receipt> receipts = repository.getReceipts(event.getId());

            Map<Long, SplitCalculator.PersonTotal> personTotals = SplitCalculator.calculateTotals(items, assignments, receipts);
            
            double grandTotal = 0;
            for (SplitCalculator.PersonTotal pt : personTotals.values()) {
                grandTotal += pt.getTotal();
            }
            eventTotals.put(event.getId(), String.format("₱%.2f", grandTotal));

            if (!userName.isEmpty()) {
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

                    List<SplitCalculator.Settlement> settlements = SplitCalculator.calculateSettlements(personTotals, paidMap);
                    
                    double myBalance = 0;
                    for (SplitCalculator.Settlement s : settlements) {
                        if (s.fromId == myId) {
                            myBalance -= s.amount;
                        } else if (s.toId == myId) {
                            myBalance += s.amount;
                        }
                    }

                    if (myBalance < -0.01) {
                        userBalances.put(event.getId(), String.format("You owe ₱%.2f", Math.abs(myBalance)));
                    } else if (myBalance > 0.01) {
                        userBalances.put(event.getId(), String.format("You are owed ₱%.2f", myBalance));
                    } else {
                        userBalances.put(event.getId(), "Settled up");
                    }
                }
            }
        }

        adapter.setEvents(events, eventTotals, userBalances);
    }
}
