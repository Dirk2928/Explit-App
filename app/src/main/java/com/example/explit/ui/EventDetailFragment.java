package com.example.explit.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.explit.R;
import com.example.explit.data.ExplitRepository;
import com.example.explit.model.Event;
import com.example.explit.model.ExpenseItem;
import com.example.explit.model.ItemAssignment;
import com.example.explit.model.Participant;
import com.example.explit.model.Receipt;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventDetailFragment extends Fragment {
    private static final String ARG_GROUP_ID = "arg_group_id";
    private static final String ARG_EVENT_ID = "arg_event_id";

    private static class EventInput {
        private final String rawName;
        private final String name;
        private final String currency;
        private final long paidByParticipantId;

        private EventInput(String rawName, String name, String currency, long paidByParticipantId) {
            this.rawName = rawName;
            this.name = name;
            this.currency = currency;
            this.paidByParticipantId = paidByParticipantId;
        }
    }

    private long groupId;
    private long eventId;
    private ExplitRepository repository;

    private EditText eventNameInput;
    private Spinner currencySpinner;
    private RecyclerView participantRecycler;
    private RecyclerView expenseRecycler;

    private ParticipantAdapter participantAdapter;
    private ExpenseItemAdapter expenseAdapter;

    private List<Participant> participants = new ArrayList<>();
    private List<Receipt> receipts = new ArrayList<>();
    private List<ExpenseItem> items = new ArrayList<>();
    private Map<Long, List<ItemAssignment>> assignmentsByItem = new HashMap<>();

    private Event event;
    private EditText pendingPhotoPathInput;

    private final ActivityResultLauncher<String> photoPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && pendingPhotoPathInput != null) {
                    pendingPhotoPathInput.setText(uri.toString());
                }
            }
    );

    // ---------------
    // newInstance
    public static EventDetailFragment newInstance(long groupId, long eventId) {
        Bundle args = new Bundle();
        args.putLong(ARG_GROUP_ID, groupId);
        args.putLong(ARG_EVENT_ID, eventId);
        EventDetailFragment fragment = new EventDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    // ---------------
    // onCreate
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            groupId = getArguments().getLong(ARG_GROUP_ID, -1);
            eventId = getArguments().getLong(ARG_EVENT_ID, -1);
        }
    }

    // ---------------
    // onCreateView
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_detail, container, false);
    }

    // ---------------
    // onViewCreated
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new ExplitRepository(requireContext());

        eventNameInput = view.findViewById(R.id.edit_event_name);
        currencySpinner = view.findViewById(R.id.spinner_currency);
        participantRecycler = view.findViewById(R.id.recycler_participants);
        expenseRecycler = view.findViewById(R.id.recycler_expenses);

        currencySpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, new String[]{getString(R.string.peso), getString(R.string.dollar)}));

        participantAdapter = new ParticipantAdapter();
        participantRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        participantRecycler.setAdapter(participantAdapter);

        expenseAdapter = new ExpenseItemAdapter(this::showItemActionsDialog);
        expenseRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        expenseRecycler.setAdapter(expenseAdapter);

        view.findViewById(R.id.button_add_participant).setOnClickListener(v -> showAddParticipantDialog());
        view.findViewById(R.id.button_add_receipt).setOnClickListener(v -> showAddReceiptDialog());
        view.findViewById(R.id.button_add_item).setOnClickListener(v -> showAddItemDialog());

        loadData();
    }

    // ---------------
    // loadData
    private void loadData() {
        if (eventId > 0) {
            event = repository.getEvent(eventId);
        }
        if (event == null && groupId > 0) {
            event = new Event(-1, groupId, "New Event", getString(R.string.peso), -1);
        }
        if (event != null) {
            groupId = event.getGroupId();
            eventNameInput.setText(event.getName());
            currencySpinner.setSelection(getString(R.string.dollar).equals(event.getCurrency()) ? 1 : 0);
        }

        participants = repository.getParticipants(groupId);
        participantAdapter.setParticipants(participants);

        if (eventId > 0) {
            receipts = repository.getReceipts(eventId);
            items = repository.getExpenseItemsForEvent(eventId);
            assignmentsByItem = repository.getAssignmentsByItem(eventId);
        } else {
            receipts = new ArrayList<>();
            items = new ArrayList<>();
            assignmentsByItem = new HashMap<>();
        }
        expenseAdapter.setItems(items);
    }

    // ---------------
    // saveEvent
    private void saveEvent() {
        EventInput input = readEventInput();

        if (eventId <= 0) {
            eventId = repository.createEvent(groupId, input.name, input.currency, input.paidByParticipantId);
        } else {
            repository.updateEvent(eventId, input.name, input.currency, input.paidByParticipantId);
        }

        event = repository.getEvent(eventId);
        loadData();
    }

    // ---------------
    // onPause
    @Override
    public void onPause() {
        super.onPause();
        persistEventUpdates();
    }

    // ---------------
    // persistEventUpdates
    private void persistEventUpdates() {
        EventInput input = readEventInput();
        if (eventId <= 0) {
            Event currentEvent = event;
            if (currentEvent == null) {
                return;
            }
            boolean nameChanged = !input.rawName.isEmpty() && !input.rawName.equals(currentEvent.getName());
            boolean currencyChanged = !input.currency.equals(currentEvent.getCurrency());
            if (!nameChanged && !currencyChanged) {
                return;
            }
            eventId = repository.createEvent(groupId, input.name, input.currency, input.paidByParticipantId);
            event = repository.getEvent(eventId);
            return;
        }
        repository.updateEvent(eventId, input.name, input.currency, input.paidByParticipantId);
        if (event != null) {
            event.setName(input.name);
            event.setCurrency(input.currency);
            event.setPaidByParticipantId(input.paidByParticipantId);
        }
    }

    // ---------------
    // ensureEventExists
    private boolean ensureEventExists() {
        if (eventId > 0) {
            return true;
        }
        saveEvent();
        return eventId > 0;
    }

    // ---------------
    // showAddParticipantDialog
    private void showAddParticipantDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        EditText nameInput = new EditText(requireContext());
        nameInput.setHint("Name");
        layout.addView(nameInput);

        EditText nicknameInput = new EditText(requireContext());
        nicknameInput.setHint("Nickname (optional)");
        layout.addView(nicknameInput);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_participant)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (!name.isEmpty()) {
                        repository.addParticipant(groupId, name, nicknameInput.getText().toString().trim());
                        participants = repository.getParticipants(groupId);
                        participantAdapter.setParticipants(participants);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ---------------
    // showAddReceiptDialog
    private void showAddReceiptDialog() {
        if (!ensureEventExists()) {
            return;
        }

        LinearLayout layout = buildVerticalLayout();
        EditText titleInput = addInput(layout, "Receipt title", InputType.TYPE_CLASS_TEXT);
        EditText taxInput = addInput(layout, "Tax", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText tipInput = addInput(layout, "Tip", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText serviceInput = addInput(layout, "Service charge", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText photoInput = addInput(layout, "Photo URI/path", InputType.TYPE_CLASS_TEXT);

        TextView attachButton = new TextView(requireContext());
        attachButton.setText("Attach Photo");
        attachButton.setTextSize(16f);
        int pad = (int) (10 * getResources().getDisplayMetrics().density);
        attachButton.setPadding(0, pad, 0, pad);
        attachButton.setOnClickListener(v -> {
            pendingPhotoPathInput = photoInput;
            photoPicker.launch("image/*");
        });
        layout.addView(attachButton);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_receipt)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    repository.addReceipt(
                            eventId,
                            safeText(titleInput, "Receipt"),
                            parseDouble(taxInput.getText().toString()),
                            parseDouble(tipInput.getText().toString()),
                            parseDouble(serviceInput.getText().toString()),
                            photoInput.getText().toString().trim()
                    );
                    loadData();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ---------------
    // showAddItemDialog
    private void showAddItemDialog() {
        if (!ensureEventExists()) {
            return;
        }
        receipts = repository.getReceipts(eventId);
        if (receipts.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setMessage("Please add a receipt first.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        LinearLayout layout = buildVerticalLayout();
        EditText nameInput = addInput(layout, "Item name", InputType.TYPE_CLASS_TEXT);
        EditText amountInput = addInput(layout, "Amount", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText percentagesInput = addInput(layout, "Custom split % (comma-separated, optional)", InputType.TYPE_CLASS_TEXT);

        Spinner receiptSpinner = new Spinner(requireContext());
        List<String> receiptTitles = new ArrayList<>();
        for (Receipt receipt : receipts) {
            receiptTitles.add(receipt.getTitle() + " (#" + receipt.getId() + ")");
        }
        receiptSpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, receiptTitles));
        layout.addView(receiptSpinner);

        CheckBox sharedCheck = new CheckBox(requireContext());
        sharedCheck.setText("Shared item");
        layout.addView(sharedCheck);

        boolean[] selected = new boolean[participants.size()];
        for (int i = 0; i < selected.length; i++) {
            selected[i] = true;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_item)
                .setView(layout)
                .setMultiChoiceItems(getParticipantNames(), selected, (dialog, which, isChecked) -> selected[which] = isChecked)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    int receiptIndex = receiptSpinner.getSelectedItemPosition();
                    long receiptId = receipts.get(receiptIndex).getId();
                    long itemId = repository.addExpenseItem(
                            receiptId,
                            safeText(nameInput, "Item"),
                            parseDouble(amountInput.getText().toString()),
                            sharedCheck.isChecked(),
                            -1
                    );

                    List<ItemAssignment> assignments = new ArrayList<>();
                    List<Participant> selectedParticipants = getSelectedParticipants(selected);
                    List<Double> customPercents = parsePercentages(percentagesInput.getText().toString(), selectedParticipants.size());
                    for (int i = 0; i < selectedParticipants.size(); i++) {
                        Participant participant = selectedParticipants.get(i);
                        Double percent = customPercents != null ? customPercents.get(i) : null;
                        assignments.add(new ItemAssignment(itemId, participant.getId(), percent));
                    }
                    repository.replaceAssignments(itemId, assignments);
                    loadData();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ---------------
    // showItemActionsDialog
    private void showItemActionsDialog(ExpenseItem item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.getName())
                .setItems(new String[]{"Edit", "Edit Split", "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditItemDialog(item);
                    } else if (which == 1) {
                        showEditSplitDialog(item);
                    } else {
                        repository.deleteExpenseItem(item.getId());
                        loadData();
                    }
                })
                .show();
    }

    // ---------------
    // showEditItemDialog
    private void showEditItemDialog(ExpenseItem item) {
        LinearLayout layout = buildVerticalLayout();
        EditText nameInput = addInput(layout, "Item name", InputType.TYPE_CLASS_TEXT);
        nameInput.setText(item.getName());
        EditText amountInput = addInput(layout, "Amount", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountInput.setText(String.valueOf(item.getAmount()));
        CheckBox sharedCheck = new CheckBox(requireContext());
        sharedCheck.setText("Shared item");
        sharedCheck.setChecked(item.isShared());
        layout.addView(sharedCheck);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Item")
                .setView(layout)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    repository.updateExpenseItem(item.getId(), safeText(nameInput, item.getName()), parseDouble(amountInput.getText().toString()), sharedCheck.isChecked(), item.getPayerParticipantId());
                    loadData();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ---------------
    // showEditSplitDialog
    private void showEditSplitDialog(ExpenseItem item) {
        boolean[] selected = new boolean[participants.size()];
        List<ItemAssignment> existing = assignmentsByItem.getOrDefault(item.getId(), new ArrayList<>());
        Map<Long, Double> existingPercentByParticipant = new HashMap<>();
        for (ItemAssignment assignment : existing) {
            existingPercentByParticipant.put(assignment.getParticipantId(), assignment.getPercent());
        }
        for (int i = 0; i < participants.size(); i++) {
            selected[i] = existingPercentByParticipant.containsKey(participants.get(i).getId());
        }

        LinearLayout layout = buildVerticalLayout();
        EditText percentagesInput = addInput(layout, "Custom split % (comma-separated, optional)", InputType.TYPE_CLASS_TEXT);
        percentagesInput.setText(getExistingPercentCsv(existingPercentByParticipant));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Split")
                .setView(layout)
                .setMultiChoiceItems(getParticipantNames(), selected, (dialog, which, isChecked) -> selected[which] = isChecked)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    List<Participant> selectedParticipants = getSelectedParticipants(selected);
                    List<Double> customPercents = parsePercentages(percentagesInput.getText().toString(), selectedParticipants.size());
                    List<ItemAssignment> assignments = new ArrayList<>();
                    for (int i = 0; i < selectedParticipants.size(); i++) {
                        Participant selectedParticipant = selectedParticipants.get(i);
                        Double percent = customPercents != null ? customPercents.get(i) : null;
                        assignments.add(new ItemAssignment(item.getId(), selectedParticipant.getId(), percent));
                    }
                    repository.replaceAssignments(item.getId(), assignments);
                    loadData();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // buildVerticalLayout
    private LinearLayout buildVerticalLayout() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        return layout;
    }

    // ---------------
    // addInput
    private EditText addInput(LinearLayout root, String hint, int inputType) {
        EditText input = new EditText(requireContext());
        input.setHint(hint);
        input.setInputType(inputType);
        root.addView(input);
        return input;
    }

    // ---------------
    // readEventInput
    private EventInput readEventInput() {
        String rawName = eventNameInput.getText().toString().trim();
        String name = rawName.isEmpty() ? "Event" : rawName;
        String currency = String.valueOf(currencySpinner.getSelectedItem());
        long paidByParticipantId = event != null ? event.getPaidByParticipantId() : -1;
        return new EventInput(rawName, name, currency, paidByParticipantId);
    }

    // ---------------
    // parseDouble
    private double parseDouble(String value) {
        try {
            return value == null || value.trim().isEmpty() ? 0d : Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    // ---------------
    // safeText
    private String safeText(EditText input, String fallback) {
        String value = input.getText().toString().trim();
        return value.isEmpty() ? fallback : value;
    }

    // ---------------
    // getParticipantNames
    private String[] getParticipantNames() {
        String[] names = new String[participants.size()];
        for (int i = 0; i < participants.size(); i++) {
            names[i] = participants.get(i).getDisplayName();
        }
        return names;
    }

    // getSelectedParticipants
    private List<Participant> getSelectedParticipants(boolean[] selected) {
        List<Participant> selectedParticipants = new ArrayList<>();
        for (int i = 0; i < selected.length && i < participants.size(); i++) {
            if (selected[i]) {
                selectedParticipants.add(participants.get(i));
            }
        }
        return selectedParticipants;
    }

    // ---------------
    // parsePercentages
    private List<Double> parsePercentages(String csv, int expectedSize) {
        String trimmed = csv == null ? "" : csv.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String[] parts = trimmed.split(",");
        if (parts.length != expectedSize || expectedSize == 0) {
            return null;
        }
        List<Double> values = new ArrayList<>();
        for (String part : parts) {
            try {
                values.add(Double.parseDouble(part.trim()));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return values;
    }

    // ---------------
    // getExistingPercentCsv
    private String getExistingPercentCsv(Map<Long, Double> existingPercentByParticipant) {
        List<String> values = new ArrayList<>();
        for (Participant participant : participants) {
            if (existingPercentByParticipant.containsKey(participant.getId())) {
                Double percent = existingPercentByParticipant.get(participant.getId());
                if (percent != null) {
                    values.add(String.valueOf(percent));
                }
            }
        }
        return joinComma(values);
    }

    // ---------------
    // joinComma
    private String joinComma(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            sb.append(values.get(i));
            if (i < values.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    // ---------------
    // joinLines
    private String joinLines(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(lines.get(i));
            if (i < lines.size() - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
