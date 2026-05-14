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

public class ProfileFragment extends Fragment {

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
        String userName = prefs.getString("user_name", "User");

        TextView initialsView = view.findViewById(R.id.text_profile_initials);
        TextView nameView = view.findViewById(R.id.text_profile_name);

        String initial = userName.isEmpty() ? "U" : userName.substring(0, 1).toUpperCase();
        initialsView.setText(initial);
        nameView.setText(userName);

        view.findViewById(R.id.button_clear_history).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }
}