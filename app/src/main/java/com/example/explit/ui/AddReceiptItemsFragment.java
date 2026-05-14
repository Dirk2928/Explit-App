package com.example.explit.ui;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.explit.util.SplitCalculator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddReceiptItemsFragment extends Fragment {

    private static final String ARG_GROUP_ID = "arg_group_id";
    private static final String ARG_EVENT_ID = "arg_event_id";
    private long groupId;
    private long eventId = -1;
    private long receiptId = -1;
    private long payerId = -1;

    private ExplitRepository repository;
    private ExpenseItemAdapter adapter;
    private EditText editTotalBill;
    private TextView textSubtotalPreview;
    private MaterialButton buttonSelectPayer;
    private RecyclerView recyclerItems;

    private final ActivityResultLauncher<String> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    saveReceiptPhoto(uri);
                }
            }
    );

    public static AddReceiptItemsFragment newInstance(long groupId, long eventId) {
        AddReceiptItemsFragment fragment = new AddReceiptItemsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_GROUP_ID, groupId);
        args.putLong(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            groupId = getArguments().getLong(ARG_GROUP_ID);
            eventId = getArguments().getLong(ARG_EVENT_ID, -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_receipt_items, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new ExplitRepository(requireContext());

        textSubtotalPreview = view.findViewById(R.id.text_subtotal_preview);
        buttonSelectPayer = view.findViewById(R.id.button_select_payer);
        recyclerItems = view.findViewById(R.id.recycler_items);
        editTotalBill = view.findViewById(R.id.edit_total_bill);

        adapter = new ExpenseItemAdapter(this::showItemActions);
        recyclerItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerItems.setAdapter(adapter);

        initializeEventAndReceipt();

        buttonSelectPayer.setOnClickListener(v -> showPayerDialog());
        view.findViewById(R.id.button_add_item).setOnClickListener(v -> showAddItemDialog());

        view.findViewById(R.id.button_add_photo).setOnClickListener(v -> {
            photoPickerLauncher.launch("image/*");
        });

        view.findViewById(R.id.button_calculate).setOnClickListener(v -> {
            if (editTotalBill.getText().toString().trim().isEmpty() || parseDouble(editTotalBill.getText().toString().trim()) == 0) {
                Toast.makeText(getContext(), "Enter a bill total amount first", Toast.LENGTH_SHORT).show();
                return;
            }
            List<ExpenseItem> items = repository.getExpenseItemsForEvent(eventId);
            Map<Long, List<ItemAssignment>> assignments = repository.getAssignmentsByItem(eventId);
            List<Receipt> receipts = repository.getReceipts(eventId);
            Map<Long, SplitCalculator.PersonTotal> totals = SplitCalculator.calculateTotals(items, assignments, receipts);
            double grandTotal = 0;
            for (SplitCalculator.PersonTotal pt : totals.values()) grandTotal += pt.getTotal();

            String totalBillStr = editTotalBill.getText().toString().trim();
            if (!totalBillStr.isEmpty()) {
                double expectedTotal = parseDouble(totalBillStr);
                if (Math.abs(grandTotal - expectedTotal) > 0.01) {
                    Toast.makeText(getContext(), "Subtotal (₱" + String.format("%.2f", grandTotal) + ") doesn't match bill total (₱" + String.format("%.2f", expectedTotal) + ")", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            saveReceiptAndEventDetails();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, SummaryFragment.newInstance(eventId))
                    .addToBackStack(null)
                    .commit();
        });

        loadItems();
    }

    private void initializeEventAndReceipt() {
        if (eventId == -1) {
            List<Event> events = repository.getEventsByGroup(groupId);
            if (events.isEmpty()) {
                eventId = repository.createEvent(groupId, "New Split", "₱", -1);
            } else {
                eventId = events.get(0).getId();
            }
        }

        Event event = repository.getEvent(eventId);
        if (event != null) {
            payerId = event.getPaidByParticipantId();
        }
        updatePayerButtonText();

        List<Receipt> receipts = repository.getReceipts(eventId);
        if (receipts.isEmpty()) {
            receiptId = repository.addReceipt(eventId, "Receipt 1", 0, 0, 0, "");
        } else {
            receiptId = receipts.get(0).getId();
            Receipt r = receipts.get(0);
            if (r.getPhotoPath() != null && !r.getPhotoPath().isEmpty()) {
                MaterialButton btnPhoto = getView().findViewById(R.id.button_add_photo);
                btnPhoto.setText("Receipts Attached ✅");
            }
        }
    }

    private void saveReceiptPhoto(Uri uri) {
        if (receiptId != -1) {
            String newPath = uri.toString();
            List<Receipt> receipts = repository.getReceipts(eventId);
            String existingPath = "";
            if (!receipts.isEmpty()) {
                existingPath = receipts.get(0).getPhotoPath();
            }
            String combined = existingPath.isEmpty() ? newPath : existingPath + "," + newPath;
            repository.updateReceiptPhoto(receiptId, combined);
            MaterialButton btnPhoto = getView().findViewById(R.id.button_add_photo);
            btnPhoto.setText("Receipts Attached ✅");
            Toast.makeText(getContext(), "Receipt photo added", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPayerDialog() {
        List<Participant> participants = repository.getEventParticipants(eventId);
        if (participants.isEmpty()) {
            Toast.makeText(getContext(), "No participants in this event", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[participants.size()];
        for (int i = 0; i < participants.size(); i++) names[i] = participants.get(i).getDisplayName();

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Who paid the bill?")
                .setItems(names, (dialog, which) -> {
                    payerId = participants.get(which).getId();
                    updatePayerButtonText();
                })
                .show();
    }

    private void updatePayerButtonText() {
        if (payerId == -1) {
            buttonSelectPayer.setText("Select Payer");
        } else {
            List<Participant> participants = repository.getEventParticipants(eventId);
            for (Participant p : participants) {
                if (p.getId() == payerId) {
                    buttonSelectPayer.setText("Paid by: " + p.getDisplayName());
                    break;
                }
            }
        }
    }

    private void loadItems() {
        List<ExpenseItem> items = repository.getExpenseItemsForEvent(eventId);
        adapter.setItems(items);
        updateRunningBalance();
    }

    private void updateRunningBalance() {
        List<ExpenseItem> items = repository.getExpenseItemsForEvent(eventId);
        Map<Long, List<ItemAssignment>> assignments = repository.getAssignmentsByItem(eventId);
        List<Receipt> receipts = repository.getReceipts(eventId);

        Map<Long, SplitCalculator.PersonTotal> totals = SplitCalculator.calculateTotals(items, assignments, receipts);
        double grandTotal = 0;
        for (SplitCalculator.PersonTotal pt : totals.values()) {
            grandTotal += pt.getTotal();
        }

        String totalBillStr = editTotalBill.getText().toString().trim();
        if (!totalBillStr.isEmpty()) {
            double expectedTotal = parseDouble(totalBillStr);
            if (Math.abs(grandTotal - expectedTotal) > 0.01) {
                textSubtotalPreview.setText(String.format("Subtotal: ₱%.2f (Expected: ₱%.2f)", grandTotal, expectedTotal));
            } else {
                textSubtotalPreview.setText(String.format("Total: ₱%.2f ✓", grandTotal));
            }
        } else {
            textSubtotalPreview.setText(String.format("Subtotal: ₱%.2f", grandTotal));
        }
    }

    private void showAddItemDialog() {
        EditText inputName = new EditText(requireContext());
        inputName.setHint("Item name");
        EditText inputAmount = new EditText(requireContext());
        inputAmount.setHint("Amount");
        inputAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 10);
        layout.addView(inputName);
        layout.addView(inputAmount);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Item")
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = inputName.getText().toString().trim();
                    String amtStr = inputAmount.getText().toString().trim();
                    if (!name.isEmpty() && !amtStr.isEmpty()) {
                        long itemId = repository.addExpenseItem(receiptId, name, Double.parseDouble(amtStr), true, -1);
                        autoAssignToAll(itemId);
                        loadItems();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void autoAssignToAll(long itemId) {
        List<Participant> participants = repository.getEventParticipants(eventId);
        if (participants.isEmpty()) {
            Toast.makeText(getContext(), "No participants in this event", Toast.LENGTH_SHORT).show();
            return;
        }
        List<ItemAssignment> assignments = new ArrayList<>();
        for (Participant p : participants) assignments.add(new ItemAssignment(itemId, p.getId(), null));
        repository.replaceAssignments(itemId, assignments);
    }

    private void showItemActions(ExpenseItem item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.getName())
                .setItems(new String[]{"Edit Split", "Delete"}, (dialog, which) -> {
                    if (which == 0) showEditSplitDialog(item);
                    else if (which == 1) {
                        repository.deleteExpenseItem(item.getId());
                        loadItems();
                    }
                })
                .show();
    }

    private void showEditSplitDialog(ExpenseItem item) {
        List<Participant> allParticipants = repository.getEventParticipants(eventId);
        Map<Long, List<ItemAssignment>> allAssignments = repository.getAssignmentsByItem(eventId);
        List<ItemAssignment> currentAssignments = allAssignments.getOrDefault(item.getId(), new ArrayList<>());

        boolean[] checkedItems = new boolean[allParticipants.size()];
        String[] participantNames = new String[allParticipants.size()];

        for (int i = 0; i < allParticipants.size(); i++) {
            participantNames[i] = allParticipants.get(i).getDisplayName();
            for (ItemAssignment a : currentAssignments) {
                if (a.getParticipantId() == allParticipants.get(i).getId()) {
                    checkedItems[i] = true;
                    break;
                }
            }
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Who shared this?")
                .setMultiChoiceItems(participantNames, checkedItems, (dialog, which, isChecked) -> checkedItems[which] = isChecked)
                .setPositiveButton("Save", (dialog, which) -> {
                    List<ItemAssignment> newAssignments = new ArrayList<>();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) newAssignments.add(new ItemAssignment(item.getId(), allParticipants.get(i).getId(), null));
                    }
                    repository.replaceAssignments(item.getId(), newAssignments);
                    updateRunningBalance();
                })
                .show();
    }

    private void saveReceiptAndEventDetails() {
        Event event = repository.getEvent(eventId);
        if (event != null) {
            repository.updateEvent(eventId, event.getName(), event.getCurrency(), payerId);
        }
    }
    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }
}