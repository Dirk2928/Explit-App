package com.example.explit.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.explit.R;
import com.example.explit.data.ExplitRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class CreateEventFragment extends Fragment {

    private EditText editGroupName;
    private ChipGroup chipGroupCategory;
    private ExplitRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new ExplitRepository(requireContext());

        editGroupName = view.findViewById(R.id.edit_event_name);
        chipGroupCategory = view.findViewById(R.id.chip_group_category);

        // Update label from "Event Name" to "Group Name"
        editGroupName.setHint("Group Name");

        view.findViewById(R.id.button_continue).setOnClickListener(v -> {
            String name = editGroupName.getText().toString().trim();
            if (name.isEmpty()) {
                editGroupName.setError("Group name is required");
                return;
            }

            String category = "Normal";
            int checkedId = chipGroupCategory.getCheckedChipId();
            if (checkedId != View.NO_ID) {
                Chip chip = chipGroupCategory.findViewById(checkedId);
                category = chip.getText().toString();
            }

            long groupId = repository.createGroup(name, category);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, EventDetailFragment.newInstance(groupId, -1))
                    .addToBackStack(null)
                    .commit();
        });
    }
}