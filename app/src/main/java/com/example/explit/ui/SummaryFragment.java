package com.example.explit.ui;

import android.content.Intent;
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

public class SummaryFragment extends Fragment {

    private static final String ARG_EVENT_ID = "arg_event_id";
    private long eventId;
    private ExplitRepository repository;
    
    private TextView textTotalBill;
    private RecyclerView recyclerPerPerson;
    private RecyclerView recyclerSettlements;
    
    private SummaryResultAdapter personAdapter;
    private SettlementAdapter settlementAdapter;

    public static SummaryFragment newInstance(long eventId) {
        SummaryFragment fragment = new SummaryFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getLong(ARG_EVENT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new ExplitRepository(requireContext());

        textTotalBill = view.findViewById(R.id.text_total_bill_amount);
        recyclerPerPerson = view.findViewById(R.id.recycler_per_person);
        recyclerSettlements = view.findViewById(R.id.recycler_settlements);

        recyclerPerPerson.setLayoutManager(new LinearLayoutManager(requireContext()));
        personAdapter = new SummaryResultAdapter();
        recyclerPerPerson.setAdapter(personAdapter);

        recyclerSettlements.setLayoutManager(new LinearLayoutManager(requireContext()));
        settlementAdapter = new SettlementAdapter();
        recyclerSettlements.setAdapter(settlementAdapter);

        calculateAndDisplay();

        view.findViewById(R.id.button_save_event).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Event Saved", Toast.LENGTH_SHORT).show();
            // Go back to Dashboard
            getParentFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        });

        view.findViewById(R.id.button_share_summary).setOnClickListener(v -> shareSummaryText());
        
        view.findViewById(R.id.button_new_split_bottom).setOnClickListener(v -> {
             getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new CreateEventFragment())
                    .commit();
        });
    }

    private void calculateAndDisplay() {
        Event event = repository.getEvent(eventId);
        if (event == null) return;

        List<Participant> participants = repository.getParticipants(event.getGroupId());
        List<ExpenseItem> items = repository.getExpenseItemsForEvent(eventId);
        Map<Long, List<ItemAssignment>> assignments = repository.getAssignmentsByItem(eventId);
        List<Receipt> receipts = repository.getReceipts(eventId);

        // 1. Calculate totals per person
        Map<Long, SplitCalculator.PersonTotal> totals = SplitCalculator.calculateTotals(items, assignments, receipts);
        
        // 2. Display Grand Total
        double grandTotal = 0;
        for (SplitCalculator.PersonTotal pt : totals.values()) {
            grandTotal += pt.getTotal();
        }
        textTotalBill.setText(String.format("₱%.2f", grandTotal));

        // 3. Update Person List
        personAdapter.setData(participants, totals);

        // 4. Calculate Settlements
        // In this simple version, assume one person paid the whole bill (the "payer" set in Event)
        // If paidByParticipantId is -1, we assume the first participant paid for the sake of calculation
        long payerId = event.getPaidByParticipantId();
        if (payerId == -1 && !participants.isEmpty()) {
            payerId = participants.get(0).getId();
        }

        Map<Long, Double> paidMap = new HashMap<>();
        if (payerId != -1) {
            paidMap.put(payerId, grandTotal);
        }

        List<SplitCalculator.Settlement> settlements = SplitCalculator.calculateSettlements(totals, paidMap);
        
        Map<Long, String> names = new HashMap<>();
        for (Participant p : participants) names.put(p.getId(), p.getDisplayName());
        
        settlementAdapter.setData(settlements, names);
    }

    private void shareSummaryText() {
        // Simple sharing logic
        StringBuilder sb = new StringBuilder("Explit Summary:\n");
        sb.append("Total: ").append(textTotalBill.getText()).append("\n\n");
        
        // Add settlements to text
        // (Accessing adapters directly for simplicity in this example)
        // ...
        
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share summary via"));
    }
}
