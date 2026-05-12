package com.example.explit.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.data.ExplitRepository;
import com.example.explit.model.Group;

import java.util.List;

public class DashboardFragment extends Fragment {

    private ExplitRepository repository;
    private TextView textWelcome;
    private TextView textTotalOwe;
    private TextView textTotalOwed;
    private RecyclerView recyclerRecentOutings;
    private RecyclerView recyclerGroups;
    private GroupAdapter groupAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new ExplitRepository(requireContext());

        textWelcome = view.findViewById(R.id.text_welcome);
        textTotalOwe = view.findViewById(R.id.text_total_owe);
        textTotalOwed = view.findViewById(R.id.text_total_owed);
        recyclerRecentOutings = view.findViewById(R.id.recycler_recent_outings);
        recyclerGroups = view.findViewById(R.id.recycler_groups);

        // Mocking user name for now
        textWelcome.setText(getString(R.string.welcome_message, "User"));

        setupGroupsRecyclerView();
        loadData();

        view.findViewById(R.id.button_new_split).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new CreateEventFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.text_see_all_groups).setOnClickListener(v -> {
            // Navigate to group list if needed
        });
    }

    private void setupGroupsRecyclerView() {
        recyclerGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        groupAdapter = new GroupAdapter(group -> {
            // Navigate to create a new event for this specific group
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, EventDetailFragment.newInstance(group.getId(), -1))
                    .addToBackStack(null)
                    .commit();
        });
        recyclerGroups.setAdapter(groupAdapter);
    }

    private void loadData() {
        List<Group> groups = repository.getGroups();
        // Limit to top 3 for dashboard
        if (groups.size() > 3) {
            groupAdapter.setGroups(groups.subList(0, 3));
        } else {
            groupAdapter.setGroups(groups);
        }
        
        // In a real app, calculate totals from repository
        textTotalOwe.setText("₱0.00");
        textTotalOwed.setText("₱0.00");
    }
}
