package com.example.explit.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.data.ExplitRepository;

public class HistoryFragment extends Fragment {

    private ExplitRepository repository;
    private RecyclerView recyclerView;
    private EventHistoryAdapter adapter;

    // ---------------
    // onCreateView
    // ---------------
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    // ---------------
    // onViewCreated
    // ---------------
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new ExplitRepository(requireContext());

        recyclerView = view.findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EventHistoryAdapter(event -> {
            // Navigate to event details/summary
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, EventDetailFragment.newInstance(event.getGroupId(), event.getId()))
                    .addToBackStack(null)
                    .commit();
        });
        recyclerView.setAdapter(adapter);

        loadHistory();
    }

    // ---------------
    // load history
    // ---------------
    private void loadHistory() {
        adapter.setEvents(repository.getAllEvents());
    }
}
