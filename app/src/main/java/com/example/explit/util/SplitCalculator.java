package com.example.explit.util;

import com.example.explit.model.ExpenseItem;
import com.example.explit.model.ItemAssignment;
import com.example.explit.model.Receipt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitCalculator {

    public static class PersonTotal {
        private final double subtotal;
        private final double extras;

        // ---------------
        // PersonTotal
        public PersonTotal(double subtotal, double extras) {
            this.subtotal = subtotal;
            this.extras = extras;
        }

        // ---------------
        // getSubtotal
        public double getSubtotal() {
            return subtotal;
        }

        // ---------------
        // getExtras
        public double getExtras() {
            return extras;
        }

        // ---------------
        // getTotal
        public double getTotal() {
            return subtotal + extras;
        }
    }

    // ---------------
    // calculateTotals
    public static Map<Long, PersonTotal> calculateTotals(List<ExpenseItem> items,
                                                          Map<Long, List<ItemAssignment>> assignmentsByItem,
                                                          List<Receipt> receiptsByEvent) {
        Map<Long, Double> subtotals = new HashMap<>();
        Map<Long, Double> extras = new HashMap<>();

        for (ExpenseItem item : items) {
            List<ItemAssignment> assignments = assignmentsByItem.get(item.getId());
            if (assignments == null || assignments.isEmpty()) {
                continue;
            }

            double explicitPercentTotal = 0;
            int equalCount = 0;
            for (ItemAssignment assignment : assignments) {
                if (assignment.getPercent() == null) {
                    equalCount++;
                } else {
                    explicitPercentTotal += assignment.getPercent();
                }
            }
            double remainingPercent = Math.max(0, 100d - explicitPercentTotal);
            double equalPercent = equalCount > 0 ? remainingPercent / equalCount : 0;

            for (ItemAssignment assignment : assignments) {
                double percent = assignment.getPercent() == null ? equalPercent : assignment.getPercent();
                double share = item.getAmount() * (percent / 100d);
                subtotals.put(assignment.getParticipantId(), subtotals.getOrDefault(assignment.getParticipantId(), 0d) + share);
            }
        }

        double subtotalSum = 0;
        for (double value : subtotals.values()) {
            subtotalSum += value;
        }

        double eventExtras = 0;
        for (Receipt receipt : receiptsByEvent) {
            eventExtras += receipt.getTax() + receipt.getTip() + receipt.getServiceCharge();
        }

        if (subtotalSum > 0) {
            for (Map.Entry<Long, Double> entry : subtotals.entrySet()) {
                double ratio = entry.getValue() / subtotalSum;
                extras.put(entry.getKey(), ratio * eventExtras);
            }
        }

        Map<Long, PersonTotal> totals = new HashMap<>();
        for (Map.Entry<Long, Double> entry : subtotals.entrySet()) {
            long participantId = entry.getKey();
            totals.put(participantId, new PersonTotal(entry.getValue(), extras.getOrDefault(participantId, 0d)));
        }
        return totals;
    }

    // ---------------
    // toDisplayLines
    public static List<String> toDisplayLines(Map<Long, String> participantNames, Map<Long, PersonTotal> totals, String currency) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<Long, PersonTotal> entry : totals.entrySet()) {
            String name = participantNames.getOrDefault(entry.getKey(), "Participant " + entry.getKey());
            lines.add(name + ": " + currency + String.format("%.2f", entry.getValue().getTotal()));
        }
        return lines;
    }
}
