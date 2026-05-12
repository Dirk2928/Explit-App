package com.example.explit.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.data.ExplitRepository;
import com.example.explit.model.Group;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class GroupListFragment extends Fragment {
    private ExplitRepository repository;
    private GroupAdapter adapter;

    // ---------------
    // onCreateView
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_list, container, false);
    }

    // ---------------
    // onViewCreated
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new ExplitRepository(requireContext());

        RecyclerView recyclerView = view.findViewById(R.id.recycler_groups);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new GroupAdapter(group -> {
            Intent intent = new Intent(requireContext(), EventActivity.class);
            intent.putExtra(EventActivity.EXTRA_GROUP_ID, group.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.button_add_group).setOnClickListener(v -> showAddGroupDialog());
    }

    // ---------------
    // onResume
    @Override
    public void onResume() {
        super.onResume();
        loadGroups();
    }

    // ---------------
    // loadGroups
    private void loadGroups() {
        List<Group> groups = repository.getGroups();
        adapter.setGroups(groups);
    }

    // ---------------
    // showAddGroupDialog
    private void showAddGroupDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        EditText nameInput = new EditText(requireContext());
        nameInput.setHint(getString(R.string.group_name));
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(nameInput);

        Spinner categorySpinner = new Spinner(requireContext());
        categorySpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, new String[]{"Family", "Friends", "Business"}));
        layout.addView(categorySpinner);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_group)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (!name.isEmpty()) {
                        repository.createGroup(name, (String) categorySpinner.getSelectedItem());
                        loadGroups();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
