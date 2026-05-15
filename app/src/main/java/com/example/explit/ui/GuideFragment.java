package com.example.explit.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.explit.R;

public class GuideFragment extends Fragment {

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_guide, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences("explit_prefs", Context.MODE_PRIVATE);

        View conversionRow = view.findViewById(R.id.row_conversion);
        TextView conversionValue = view.findViewById(R.id.text_conversion_value);

        boolean roundDecimals = prefs.getBoolean("round_decimals", false);
        conversionValue.setText(roundDecimals ? "Round Whole" : "Keep Decimals");

        conversionRow.setOnClickListener(v -> {
            boolean current = prefs.getBoolean("round_decimals", false);
            prefs.edit().putBoolean("round_decimals", !current).apply();
            conversionValue.setText(!current ? "Round Whole" : "Keep Decimals");
            Toast.makeText(getContext(), !current ? "Switched to Whole Numbers" : "Switched to Keep Decimals", Toast.LENGTH_SHORT).show();
        });
    }
}