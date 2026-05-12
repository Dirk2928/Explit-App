package com.example.explit.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;

public class SummaryFragment extends Fragment {

    private static final String ARG_EVENT_ID = "arg_event_id";
    private long eventId;

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

        TextView textTotalBill = view.findViewById(R.id.text_total_bill_amount);
        RecyclerView recyclerPerPerson = view.findViewById(R.id.recycler_per_person);
        RecyclerView recyclerSettlements = view.findViewById(R.id.recycler_settlements);

        // In a real app, calculate and display data from the event/receipts
        textTotalBill.setText("₱0.00");

        view.findViewById(R.id.button_save_event).setOnClickListener(v -> {
            // Save and return to dashboard
        });

        view.findViewById(R.id.button_share_summary).setOnClickListener(v -> {
            // Logic to export as text
        });
    }
}
