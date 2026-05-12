package com.example.explit.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;

public class AddReceiptItemsFragment extends Fragment {

    private EditText editTotalBill;
    private TextView textSubtotalPreview;
    private RecyclerView recyclerItems;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_receipt_items, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editTotalBill = view.findViewById(R.id.edit_total_bill);
        textSubtotalPreview = view.findViewById(R.id.text_subtotal_preview);
        recyclerItems = view.findViewById(R.id.recycler_items);

        view.findViewById(R.id.button_add_item).setOnClickListener(v -> {
            // Show Screen 5: Assign Item Dialog
        });

        view.findViewById(R.id.button_calculate).setOnClickListener(v -> {
            // Navigate to Screen 6: Summary
        });
    }
}
