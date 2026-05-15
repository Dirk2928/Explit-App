package com.example.explit.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.data.ExplitRepository;
import com.example.explit.model.Event;
import java.util.HashMap;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private ExplitRepository repository;
    private EventHistoryAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        repository = new ExplitRepository(this);
        RecyclerView recyclerView = findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventHistoryAdapter(event -> {
            Intent intent = new Intent(this, EventActivity.class);
            intent.putExtra(EventActivity.EXTRA_GROUP_ID, event.getGroupId());
            intent.putExtra(EventActivity.EXTRA_EVENT_ID, event.getId());
            startActivity(intent);
        }, null);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<Event> events = repository.getAllEvents();
        adapter.setEvents(events, new HashMap<>(), new HashMap<>(), null, null);
    }
}
