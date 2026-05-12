package com.example.explit.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.explit.R;

public class EventActivity extends AppCompatActivity {
    public static final String EXTRA_GROUP_ID = "extra_group_id";
    public static final String EXTRA_EVENT_ID = "extra_event_id";

    // ---------------
    // onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        if (savedInstanceState == null) {
            long groupId = getIntent().getLongExtra(EXTRA_GROUP_ID, -1);
            long eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, -1);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.event_container, EventDetailFragment.newInstance(groupId, eventId))
                    .commit();
        }
    }
}
