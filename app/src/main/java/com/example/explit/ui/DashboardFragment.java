package com.example.explit.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.data.ExplitRepository;
import com.example.explit.model.Event;
import com.example.explit.model.ExpenseItem;
import com.example.explit.model.Group;
import com.example.explit.model.ItemAssignment;
import com.example.explit.model.Participant;
import com.example.explit.model.Receipt;
import com.example.explit.util.SplitCalculator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private ExplitRepository repository;
    private TextView textWelcome;
    private TextView textTotalOwe;
    private TextView textTotalOwed;
    private RecyclerView recyclerGroups;
    private GroupAdapter groupAdapter;
    private SharedPreferences prefs;

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

        textWelcome = view.findViewById(R.id.text_welcome);
        textTotalOwe = view.findViewById(R.id.text_total_owe);
        textTotalOwed = view.findViewById(R.id.text_total_owed);
        recyclerGroups = view.findViewById(R.id.recycler_groups);

        String userName = prefs.getString("user_name", "User");
        textWelcome.setText(getString(R.string.welcome_message, userName));

        setupGroupsRecyclerView();
        
        view.findViewById(R.id.button_new_split).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new CreateEventFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void setupGroupsRecyclerView() {
        recyclerGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        groupAdapter = new GroupAdapter(group -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, EventDetailFragment.newInstance(group.getId(), -1))
                    .addToBackStack(null)
                    .commit();
        });
        recyclerGroups.setAdapter(groupAdapter);
    }

    private void loadData() {
        List<Group> groups = repository.getGroups();
        if (groups.size() > 3) {
            groupAdapter.setGroups(groups.subList(0, 3));
        } else {
            groupAdapter.setGroups(groups);
        }

        calculateGlobalBalance();
    }

    private void calculateGlobalBalance() {
        String userName = prefs.getString("user_name", "");
        if (userName.isEmpty()) {
            textTotalOwe.setText("₱0.00");
            textTotalOwed.setText("₱0.00");
            return;
        }

        double totalOwe = 0;
        double totalOwed = 0;

        List<Event> allEvents = repository.getAllEvents();
        for (Event event : allEvents) {
            List<Participant> participants = repository.getParticipants(event.getGroupId());
            long myId = -1;
            for (Participant p : participants) {
                if (p.getName().equalsIgnoreCase(userName) || (p.getNickname() != null && p.getNickname().equalsIgnoreCase(userName))) {
                    myId = p.getId();
                    break;
                }
            }

            if (myId == -1) continue;

            List<ExpenseItem> items = repository.getExpenseItemsForEvent(event.getId());
            Map<Long, List<ItemAssignment>> assignments = repository.getAssignmentsByItem(event.getId());
            List<Receipt> receipts = repository.getReceipts(event.getId());

            Map<Long, SplitCalculator.PersonTotal> totals = SplitCalculator.calculateTotals(items, assignments, receipts);
            
            double grandTotal = 0;
            for (SplitCalculator.PersonTotal pt : totals.values()) grandTotal += pt.getTotal();

            long payerId = event.getPaidByParticipantId();
            if (payerId == -1 && !participants.isEmpty()) payerId = participants.get(0).getId();

            Map<Long, Double> paidMap = new HashMap<>();
            if (payerId != -1) paidMap.put(payerId, grandTotal);

            List<SplitCalculator.Settlement> settlements = SplitCalculator.calculateSettlements(totals, paidMap);

            for (SplitCalculator.Settlement s : settlements) {
                if (s.fromId == myId) {
                    totalOwe += s.amount;
                } else if (s.toId == myId) {
                    totalOwed += s.amount;
                }
            }
        }

        textTotalOwe.setText(String.format("₱%.2f", totalOwe));
        textTotalOwed.setText(String.format("₱%.2f", totalOwed));
    }
}
