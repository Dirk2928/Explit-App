package com.example.explit.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SummaryFragment extends Fragment {

    private static final String ARG_EVENT_ID = "arg_event_id";
    private long eventId;
    private ExplitRepository repository;
    private ImageView imageReceipt;

    private TextView textTotalBill;
    private RecyclerView recyclerPerPerson;
    private RecyclerView recyclerSettlements;

    private SummaryResultAdapter personAdapter;
    private SettlementAdapter settlementAdapter;
    private String cachedSummaryContent;

    public static SummaryFragment newInstance(long eventId) {
        SummaryFragment fragment = new SummaryFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getLong(ARG_EVENT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new ExplitRepository(requireContext());

        textTotalBill = view.findViewById(R.id.text_total_bill_amount);
        recyclerPerPerson = view.findViewById(R.id.recycler_per_person);
        recyclerSettlements = view.findViewById(R.id.recycler_settlements);
        imageReceipt = view.findViewById(R.id.image_receipt);

        recyclerPerPerson.setLayoutManager(new LinearLayoutManager(requireContext()));
        personAdapter = new SummaryResultAdapter();
        recyclerPerPerson.setAdapter(personAdapter);

        recyclerSettlements.setLayoutManager(new LinearLayoutManager(requireContext()));
        settlementAdapter = new SettlementAdapter();
        recyclerSettlements.setAdapter(settlementAdapter);

        calculateAndDisplay();

        view.findViewById(R.id.button_share_summary).setOnClickListener(v -> shareSummaryText());

        view.findViewById(R.id.button_share_summary).setOnLongClickListener(v -> {
            exportSummaryToFile();
            return true;
        });
        view.findViewById(R.id.button_return_home).setOnClickListener(v -> {
            getParentFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        });
    }

    private Map<String, Double> loadPaidStatus() {
        return repository.getSettlementPaidStatus(eventId);
    }

    private void showPaymentDialog(long fromId, long toId, double totalAmount, double paidSoFar, Map<Long, String> names) {
        String fromName = names.getOrDefault(fromId, "Person");
        String toName = names.getOrDefault(toId, "Person");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        TextView info = new TextView(requireContext());
        info.setText(fromName + " pays " + toName + "\nTotal: ₱" + String.format("%.2f", totalAmount) + "\nPaid so far: ₱" + String.format("%.2f", paidSoFar) + "\nRemaining: ₱" + String.format("%.2f", totalAmount - paidSoFar));
        layout.addView(info);

        EditText input = new EditText(requireContext());
        input.setHint("Amount to pay now");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Record Payment")
                .setView(layout)
                .setPositiveButton("Pay", (dialog, which) -> {
                    String amtStr = input.getText().toString().trim();
                    if (!amtStr.isEmpty()) {
                        double newPayment = Double.parseDouble(amtStr);
                        if (newPayment > totalAmount - paidSoFar) {
                            Toast.makeText(getContext(), "Amount exceeds remaining balance", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        double newTotal = paidSoFar + newPayment;
                        repository.setSettlementPaid(eventId, fromId, toId, newTotal);
                        Event event = repository.getEvent(eventId);
                        if (event != null) {
                            repository.updateEvent(eventId, event.getName(), event.getCurrency(), event.getPaidByParticipantId());
                        }
                        calculateAndDisplay();
                        Toast.makeText(getContext(), "Payment recorded", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void calculateAndDisplay() {
        Event event = repository.getEvent(eventId);
        if (event == null) return;

        List<Participant> participants = repository.getEventParticipants(eventId);
        List<ExpenseItem> items = repository.getExpenseItemsForEvent(eventId);
        Map<Long, List<ItemAssignment>> assignments = repository.getAssignmentsByItem(eventId);
        List<Receipt> receipts = repository.getReceipts(eventId);

        if (participants.isEmpty() && items.isEmpty()) {
            textTotalBill.setText("₱0.00");
            return;
        }

        Map<Long, SplitCalculator.PersonTotal> totals = SplitCalculator.calculateTotals(items, assignments, receipts);

        double grandTotal = 0;
        for (SplitCalculator.PersonTotal pt : totals.values()) {
            grandTotal += pt.getTotal();
        }
        textTotalBill.setText(String.format("₱%.2f", grandTotal));
        if (CurrencyHelper.shouldRound(requireContext())) {
            grandTotal = Math.floor(grandTotal);
            textTotalBill.setText(String.format("₱%.0f", grandTotal));
            Map<Long, SplitCalculator.PersonTotal> roundedTotals = new HashMap<>();
            for (Map.Entry<Long, SplitCalculator.PersonTotal> entry : totals.entrySet()) {
                double rounded = Math.floor(entry.getValue().getTotal());
                roundedTotals.put(entry.getKey(), new SplitCalculator.PersonTotal(rounded, 0));
            }
            totals = roundedTotals;
        }

        List<String> allPhotos = new ArrayList<>();
        for (Receipt r : receipts) {
            if (r.getPhotoPath() != null && !r.getPhotoPath().isEmpty()) {
                String[] paths = r.getPhotoPath().split(",");
                for (String path : paths) {
                    allPhotos.add(path.trim());
                }
            }
        }

        if (!allPhotos.isEmpty()) {
            imageReceipt.setVisibility(View.VISIBLE);
            try {
                Uri photoUri = Uri.parse(allPhotos.get(0));
                imageReceipt.setImageURI(photoUri);
            } catch (Exception e) {
                File imgFile = new File(allPhotos.get(0));
                if (imgFile.exists()) {
                    imageReceipt.setImageURI(Uri.fromFile(imgFile));
                }
            }

            imageReceipt.setOnClickListener(v -> {
                if (allPhotos.size() == 1) {
                    showFullImage(allPhotos.get(0));
                } else {
                    String[] photoItems = new String[allPhotos.size()];
                    for (int i = 0; i < allPhotos.size(); i++) photoItems[i] = "Receipt Photo " + (i + 1);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Receipt Photos")
                            .setItems(photoItems, (dialog, which) -> showFullImage(allPhotos.get(which)))
                            .setPositiveButton("Close", null)
                            .show();
                }
            });
        }

        personAdapter.setData(participants, totals, requireContext());

        Map<Long, Double> paidMap = repository.getPayments(eventId);
        if (paidMap.isEmpty() && !participants.isEmpty()) {
            paidMap.put(participants.get(0).getId(), grandTotal);
        }

        List<SplitCalculator.Settlement> settlements = SplitCalculator.calculateSettlements(totals, paidMap);
        Map<Long, String> names = new HashMap<>();
        for (Participant p : participants) names.put(p.getId(), p.getDisplayName());

        repository.saveSettlements(eventId, settlements);
        Map<String, Double> paidStatus = loadPaidStatus();
        settlementAdapter.setData(settlements, names, paidStatus, (fromId, toId, totalAmount, paidSoFar) -> {
            showPaymentDialog(fromId, toId, totalAmount, paidSoFar, names);
        }, requireContext());

        cachedSummaryContent = buildSummaryString();
    }

    private String buildSummaryString() {
        StringBuilder sb = new StringBuilder("Explit Bill Summary\n");
        sb.append("Total: ").append(textTotalBill.getText()).append("\n\n");
        sb.append("Settlements:\n");

        Event event = repository.getEvent(eventId);
        if (event != null) {
            List<Participant> participants = repository.getEventParticipants(eventId);
            List<ExpenseItem> items = repository.getExpenseItemsForEvent(eventId);
            Map<Long, List<ItemAssignment>> assignments = repository.getAssignmentsByItem(eventId);
            List<Receipt> receipts = repository.getReceipts(eventId);
            Map<Long, SplitCalculator.PersonTotal> totals = SplitCalculator.calculateTotals(items, assignments, receipts);

            double grandTotal = 0;
            for (SplitCalculator.PersonTotal pt : totals.values()) grandTotal += pt.getTotal();

            Map<Long, Double> paidMap = repository.getPayments(eventId);
            if (paidMap.isEmpty() && !participants.isEmpty()) {
                paidMap.put(participants.get(0).getId(), grandTotal);
            }

            List<SplitCalculator.Settlement> settlements = SplitCalculator.calculateSettlements(totals, paidMap);
            Map<Long, String> names = new HashMap<>();
            for (Participant p : participants) names.put(p.getId(), p.getDisplayName());

            for (SplitCalculator.Settlement s : settlements) {
                sb.append(names.getOrDefault(s.fromId, "Unknown")).append(" pays ")
                        .append(names.getOrDefault(s.toId, "Unknown"))
                        .append(": ₱").append(String.format("%.2f", s.amount)).append("\n");
            }
        }
        return sb.toString();
    }

    private void shareSummaryText() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, buildSummaryString());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share summary via"));
    }

    private void exportSummaryToFile() {
        if (cachedSummaryContent == null) {
            cachedSummaryContent = buildSummaryString();
        }
        String content = cachedSummaryContent;
        String fileName = "Explit_Summary_" + eventId + ".txt";

        File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir != null && !dir.exists()) dir.mkdirs();
        File file = new File(dir, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes());
            Toast.makeText(getContext(), "Exported to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error exporting file", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFullImage(String path) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        ImageView fullImage = new ImageView(requireContext());
        try {
            fullImage.setImageURI(Uri.parse(path));
        } catch (Exception e) {
            File imgFile = new File(path);
            if (imgFile.exists()) fullImage.setImageURI(Uri.fromFile(imgFile));
        }
        fullImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        builder.setView(fullImage);
        builder.setPositiveButton("Close", null);
        builder.show();
    }
}
