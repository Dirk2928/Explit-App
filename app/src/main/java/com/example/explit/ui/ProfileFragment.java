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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.explit.R;

public class ProfileFragment extends Fragment {

    private EditText editUserName;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        prefs = requireContext().getSharedPreferences("explit_prefs", Context.MODE_PRIVATE);
        editUserName = view.findViewById(R.id.edit_user_name); // I need to make sure this ID exists in XML or add it

        String savedName = prefs.getString("user_name", "");
        editUserName.setText(savedName);

        editUserName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                prefs.edit().putString("user_name", s.toString().trim()).apply();
            }
        });

        view.findViewById(R.id.button_clear_history).setOnClickListener(v -> {
            // Logic to clear DB could go here
            Toast.makeText(getContext(), "Feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }
}
