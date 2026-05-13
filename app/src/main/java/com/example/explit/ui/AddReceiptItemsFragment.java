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
    private long groupId;
    private long eventId = -1;
    private long receiptId = -1;
    private long payerId = -1;
    
    private ExplitRepository repository;
    private ExpenseItemAdapter adapter;
    
    private EditText editTax, editTip;
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

    public static AddReceiptItemsFragment newInstance(long groupId) {
        AddReceiptItemsFragment fragment = new AddReceiptItemsFragment();
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
        return inflater.inflate(R.layout.fragment_add_receipt_items, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new ExplitRepository(requireContext());

        editTax = view.findViewById(R.id.edit_tax);
        editTip = view.findViewById(R.id.edit_tip);
        textSubtotalPreview = view.findViewById(R.id.text_subtotal_preview);
        buttonSelectPayer = view.findViewById(R.id.button_select_payer);
        recyclerItems = view.findViewById(R.id.recycler_items);

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
            saveReceiptAndEventDetails();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, SummaryFragment.newInstance(eventId))
                    .addToBackStack(null)
                    .commit();
        });
        
        loadItems();
    }

    private void initializeEventAndReceipt() {
        List<Event> events = repository.getEventsByGroup(groupId);
        Event event;
        if (events.isEmpty()) {
            eventId = repository.createEvent(groupId, "New Split", "₱", -1);
            event = repository.getEvent(eventId);
        } else {
            event = events.get(0);
            eventId = event.getId();
        }
        
        payerId = event.getPaidByParticipantId();
        updatePayerButtonText();

        List<Receipt> receipts = repository.getReceipts(eventId);
        if (receipts.isEmpty()) {
            receiptId = repository.addReceipt(eventId, "Receipt 1", 0, 0, 0, "");
        } else {
            receiptId = receipts.get(0).getId();
            Receipt r = receipts.get(0);
            editTax.setText(String.valueOf(r.getTax()));
            editTip.setText(String.valueOf(r.getTip()));
            
            if (r.getPhotoPath() != null && !r.getPhotoPath().isEmpty()) {
                MaterialButton btnPhoto = getView().findViewById(R.id.button_add_photo);
                btnPhoto.setText("Receipt Attached ✅");
            }
        }
    }

    private void saveReceiptPhoto(Uri uri) {
        if (receiptId != -1) {
            repository.updateReceiptPhoto(receiptId, uri.toString());
            MaterialButton btnPhoto = getView().findViewById(R.id.button_add_photo);
            btnPhoto.setText("Receipt Attached ✅");
            Toast.makeText(getContext(), "Receipt photo saved locally", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPayerDialog() {
        List<Participant> participants = repository.getParticipants(groupId);
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
            List<Participant> participants = repository.getParticipants(groupId);
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
        
        if (!receipts.isEmpty()) {
            Receipt r = receipts.get(0);
            r.setTax(parseDouble(editTax.getText().toString()));
            r.setTip(parseDouble(editTip.getText().toString()));
        }

        Map<Long, SplitCalculator.PersonTotal> totals = SplitCalculator.calculateTotals(items, assignments, receipts);
        double grandTotal = 0;
        for (SplitCalculator.PersonTotal pt : totals.values()) grandTotal += pt.getTotal();
        textSubtotalPreview.setText(String.format("Total: ₱%.2f", grandTotal));
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
        List<Participant> participants = repository.getParticipants(groupId);
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
        List<Participant> allParticipants = repository.getParticipants(groupId);
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
