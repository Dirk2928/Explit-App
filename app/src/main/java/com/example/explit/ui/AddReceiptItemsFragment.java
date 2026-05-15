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
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import com.example.explit.data.ExplitDbHelper;


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
import com.example.explit.util.CurrencyHelper;
import com.example.explit.util.SplitCalculator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddReceiptItemsFragment extends Fragment {

    private static final String ARG_GROUP_ID = "arg_group_id";
    private static final String ARG_EVENT_ID = "arg_event_id";
    private long groupId;
    private long eventId = -1;
    private long receiptId = -1;
    private Map<Long, Double> payerMap = new HashMap<>();

    private ExplitRepository repository;
    private ExpenseItemAdapter adapter;
    private EditText editTotalBill;
    private TextView textSubtotalPreview;
    private MaterialButton buttonSelectPayer;
    private RecyclerView recyclerItems;

    private final ActivityResultLauncher<String[]> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) {
                        saveReceiptPhoto(uri);
                    }
                    Toast.makeText(getContext(), uris.size() + " photos added", Toast.LENGTH_SHORT).show();
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
        editTotalBill.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                updatePayerButtonState();
            }
        });

        adapter = new ExpenseItemAdapter(this::showItemActions);
        recyclerItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerItems.setAdapter(adapter);

        initializeEventAndReceipt();
        updatePayerButtonState();

        buttonSelectPayer.setOnClickListener(v -> showPayerDialog());
        view.findViewById(R.id.button_add_item).setOnClickListener(v -> showAddItemDialog());

        view.findViewById(R.id.button_add_photo).setOnClickListener(v -> {
            List<Receipt> receipts = repository.getReceipts(eventId);
            String photoPath = "";
            if (!receipts.isEmpty()) {
                photoPath = receipts.get(0).getPhotoPath();
            }

            if (photoPath != null && !photoPath.isEmpty()) {
                String[] photos = photoPath.split(",");
                String[] items = new String[photos.length + 1];
                for (int i = 0; i < photos.length; i++) {
                    items[i] = "Photo " + (i + 1) + " (tap to delete)";
                }
                items[photos.length] = "Add New Photo";

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Receipt Photos (" + photos.length + ")")
                        .setItems(items, (dialog, which) -> {
                            if (which == photos.length) {
                                photoPickerLauncher.launch(new String[]{"image/*"});                            } else {
                                StringBuilder newPath = new StringBuilder();
                                for (int i = 0; i < photos.length; i++) {
                                    if (i != which) {
                                        if (newPath.length() > 0) newPath.append(",");
                                        newPath.append(photos[i].trim());
                                    }
                                }
                                repository.updateReceiptPhoto(receiptId, newPath.toString());
                                MaterialButton btnPhoto = getView().findViewById(R.id.button_add_photo);
                                if (newPath.length() == 0) {
                                    btnPhoto.setText("Add Receipt Photo");
                                } else {
                                    btnPhoto.setText("Receipts Attached ✅");
                                }
                                Toast.makeText(getContext(), "Photo removed", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();
            } else {
                photoPickerLauncher.launch(new String[]{"image/*"});
            }
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

    private void updatePayerButtonState() {
        double billTotal = parseDouble(editTotalBill.getText().toString().trim());
        buttonSelectPayer.setEnabled(billTotal > 0);
        if (billTotal == 0) {
            buttonSelectPayer.setText("Enter bill total first");
        } else if (payerMap.isEmpty()) {
            buttonSelectPayer.setText("Select Payer");
        }
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
        payerMap = repository.getPayments(eventId);
        updatePayerButtonText();

        List<Receipt> receipts = repository.getReceipts(eventId);
        if (receipts.isEmpty()) {
            receiptId = repository.addReceipt(eventId, "Receipt 1", 0, 0, 0, "");
        } else {
            receiptId = receipts.get(0).getId();
            Receipt r = receipts.get(0);
            double savedBillTotal = r.getServiceCharge();
            if (savedBillTotal > 0) {
                editTotalBill.setText(String.valueOf(savedBillTotal));
            }
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
        boolean[] checked = new boolean[participants.size()];
        for (int i = 0; i < participants.size(); i++) {
            names[i] = participants.get(i).getDisplayName();
            checked[i] = payerMap.containsKey(participants.get(i).getId());
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Who paid? (select all)")
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Next", (dialog, which) -> {
                    List<Participant> selected = new ArrayList<>();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) selected.add(participants.get(i));
                    }
                    if (selected.isEmpty()) {
                        Toast.makeText(getContext(), "Select at least one payer", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showPayerAmounts(selected);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPayerAmounts(List<Participant> payers) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        List<EditText> amountInputs = new ArrayList<>();
        for (Participant p : payers) {
            TextView label = new TextView(requireContext());
            label.setText(p.getDisplayName() + " paid:");
            layout.addView(label);
            EditText input = new EditText(requireContext());
            input.setHint("Amount");
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            if (payerMap.containsKey(p.getId())) {
                input.setText(String.valueOf(payerMap.get(p.getId())));
            }
            layout.addView(input);
            amountInputs.add(input);
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Enter amounts paid")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    double billTotal = parseDouble(editTotalBill.getText().toString().trim());
                    payerMap.clear();
                    double totalPaid = 0;
                    for (int i = 0; i < payers.size(); i++) {
                        String amtStr = amountInputs.get(i).getText().toString().trim();
                        if (!amtStr.isEmpty()) {
                            double amt = parseDouble(amtStr);
                            payerMap.put(payers.get(i).getId(), amt);
                            totalPaid += amt;
                        }
                    }
                    if (Math.abs(totalPaid - billTotal) > 0.01) {
                        Toast.makeText(getContext(), "Total paid (₱" + String.format("%.2f", totalPaid) + ") must match bill total (₱" + String.format("%.2f", billTotal) + ")", Toast.LENGTH_LONG).show();
                        return;
                    }
                    updatePayerButtonText();
                })                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updatePayerButtonText() {
        if (payerMap.isEmpty()) {
            buttonSelectPayer.setText("Select Payer");
        } else {
            List<Participant> participants = repository.getEventParticipants(eventId);
            StringBuilder sb = new StringBuilder("Paid by: ");
            boolean first = true;
            for (Map.Entry<Long, Double> entry : payerMap.entrySet()) {
                for (Participant p : participants) {
                    if (p.getId() == entry.getKey()) {
                        if (!first) sb.append(", ");
                        sb.append(p.getName());
                        first = false;
                        break;
                    }
                }
            }
            buttonSelectPayer.setText(sb.toString());
        }
    }

    private void loadItems() {
        List<ExpenseItem> items = repository.getExpenseItemsForEvent(eventId);
        Map<Long, List<ItemAssignment>> assignments = repository.getAssignmentsByItem(eventId);
        List<Participant> participants = repository.getEventParticipants(eventId);
        adapter.setItemsWithAssignments(items, assignments, participants);
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
            if (CurrencyHelper.shouldRound(requireContext())) {
                grandTotal = Math.floor(grandTotal);
                expectedTotal = Math.floor(expectedTotal);
            }
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
                    loadItems();
                })
                .show();
    }

    private void saveReceiptAndEventDetails() {
        Event event = repository.getEvent(eventId);
        if (event != null) {
            long firstPayer = payerMap.isEmpty() ? -1 : payerMap.keySet().iterator().next();
            repository.updateEvent(eventId, event.getName(), event.getCurrency(), firstPayer);
            repository.savePayments(eventId, payerMap);
            SQLiteDatabase db = new ExplitDbHelper(requireContext()).getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("last_modified", System.currentTimeMillis());
            db.update("groups", values, "id=?", new String[]{String.valueOf(event.getGroupId())});
            db.close();
        }
    }
    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }
    @Override
    public void onPause() {
        super.onPause();
        if (receiptId != -1 && editTotalBill != null) {
            double billTotal = parseDouble(editTotalBill.getText().toString().trim());
            repository.updateReceiptDetails(receiptId, 0, 0, billTotal);
        }
        if (eventId != -1 && !payerMap.isEmpty()) {
            repository.savePayments(eventId, payerMap);
        }
        if (eventId != -1) {
            Event event = repository.getEvent(eventId);
            if (event != null) {
                android.database.sqlite.SQLiteDatabase db = new com.example.explit.data.ExplitDbHelper(requireContext()).getWritableDatabase();
                android.content.ContentValues v = new android.content.ContentValues();
                v.put("last_modified", System.currentTimeMillis());
                db.update("groups", v, "id=?", new String[]{String.valueOf(event.getGroupId())});
                db.update("events", v, "id=?", new String[]{String.valueOf(eventId)});
                db.close();
            }
        }
    }
}
