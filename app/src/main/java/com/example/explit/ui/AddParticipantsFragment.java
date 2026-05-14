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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.data.ExplitRepository;
import com.example.explit.model.Participant;

import java.util.List;

public class AddParticipantsFragment extends Fragment {

    private static final String ARG_GROUP_ID = "arg_group_id";
    private long groupId;
    private ExplitRepository repository;
    private ParticipantAdapter adapter;
    private EditText editName;

    public static AddParticipantsFragment newInstance(long groupId) {
        AddParticipantsFragment fragment = new AddParticipantsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_GROUP_ID, groupId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            groupId = getArguments().getLong(ARG_GROUP_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_participants, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new ExplitRepository(requireContext());

        editName = view.findViewById(R.id.edit_search_add);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_added_participants);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ParticipantAdapter();
        recyclerView.setAdapter(adapter);

        loadParticipants();

        view.findViewById(R.id.button_add_direct).setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            if (!name.isEmpty()) {
                repository.addParticipant(groupId, name, "");
                editName.setText("");
                loadParticipants();
            } else {
                Toast.makeText(getContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.button_next_to_receipt).setOnClickListener(v -> {
            // Check if there are participants
            List<Participant> currentList = repository.getParticipants(groupId);
            if (currentList.isEmpty()) {
                Toast.makeText(getContext(), "Add at least one participant", Toast.LENGTH_SHORT).show();
                return;
            }

            // Phase 1, Step 2: Navigate to Add Receipts screen
            // We pass the groupId to find/create an event for it
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AddReceiptItemsFragment.newInstance(groupId, -1))
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void loadParticipants() {
        List<Participant> participants = repository.getParticipants(groupId);
        adapter.setParticipants(participants);
    }
}
