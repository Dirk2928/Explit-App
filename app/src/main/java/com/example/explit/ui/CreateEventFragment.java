package com.example.explit.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.explit.R;
import com.example.explit.data.ExplitRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CreateEventFragment extends Fragment {

    private EditText editName;
    private ChipGroup chipGroupCategory;
    private EditText editDate;
    private ExplitRepository repository;
    private final Calendar calendar = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new ExplitRepository(requireContext());

        editName = view.findViewById(R.id.edit_event_name);
        chipGroupCategory = view.findViewById(R.id.chip_group_category);
        editDate = view.findViewById(R.id.edit_date);
        
        updateDateLabel();

        editDate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (view1, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateLabel();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        view.findViewById(R.id.button_continue).setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            if (name.isEmpty()) {
                editName.setError("Name is required");
                return;
            }

            String category = "General";
            int checkedId = chipGroupCategory.getCheckedChipId();
            if (checkedId != View.NO_ID) {
                Chip chip = chipGroupCategory.findViewById(checkedId);
                category = chip.getText().toString();
            }

            // Create a group for this split session
            long groupId = repository.createGroup(name, category);
            
            // Navigate to Screen 3: Add Participants (EventDetailFragment)
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, EventDetailFragment.newInstance(groupId, -1))
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void updateDateLabel() {
        String myFormat = "MM/dd/yy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        editDate.setText(sdf.format(calendar.getTime()));
    }
}
