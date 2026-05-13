package com.example.explit.util;

import com.example.explit.model.ExpenseItem;
import com.example.explit.model.ItemAssignment;
import com.example.explit.model.Receipt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class SplitCalculator {

    public static class PersonTotal {
        private final double subtotal;
        private final double extras;

        public PersonTotal(double subtotal, double extras) {
            this.subtotal = subtotal;
            this.extras = extras;
        }

        public double getSubtotal() {
            return subtotal;
        }

        public double getExtras() {
            return extras;
        }

        public double getTotal() {
            return subtotal + extras;
        }
    }

    public static class Settlement {
        public final long fromId;
        public final long toId;
        public final double amount;

        public Settlement(long fromId, long toId, double amount) {
            this.fromId = fromId;
            this.toId = toId;
            this.amount = amount;
        }
    }

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

    public static List<Settlement> calculateSettlements(Map<Long, PersonTotal> owedMap, Map<Long, Double> paidMap) {
        List<Settlement> settlements = new ArrayList<>();
        Map<Long, Double> netBalances = new HashMap<>();

        // Initialize with 0
        for (Long id : owedMap.keySet()) netBalances.put(id, 0.0);
        for (Long id : paidMap.keySet()) netBalances.put(id, 0.0);

        for (Map.Entry<Long, Double> entry : paidMap.entrySet()) {
            netBalances.put(entry.getKey(), netBalances.get(entry.getKey()) + entry.getValue());
        }
        for (Map.Entry<Long, PersonTotal> entry : owedMap.entrySet()) {
            netBalances.put(entry.getKey(), netBalances.get(entry.getKey()) - entry.getValue().getTotal());
        }

        PriorityQueue<Pair> debtors = new PriorityQueue<>((a, b) -> Double.compare(a.val, b.val));
        PriorityQueue<Pair> creditors = new PriorityQueue<>((a, b) -> Double.compare(b.val, a.val));

        for (Map.Entry<Long, Double> entry : netBalances.entrySet()) {
            if (entry.getValue() < -0.01) debtors.add(new Pair(entry.getKey(), entry.getValue()));
            else if (entry.getValue() > 0.01) creditors.add(new Pair(entry.getKey(), entry.getValue()));
        }

        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Pair debtor = debtors.poll();
            Pair creditor = creditors.poll();
            double amount = Math.min(-debtor.val, creditor.val);
            settlements.add(new Settlement(debtor.id, creditor.id, amount));
            debtor.val += amount;
            creditor.val -= amount;
            if (debtor.val < -0.01) debtors.add(debtor);
            if (creditor.val > 0.01) creditors.add(creditor);
        }
        return settlements;
    }

    private static class Pair {
        long id;
        double val;
        Pair(long id, double val) { this.id = id; this.val = val; }
    }

    public static List<String> toDisplayLines(Map<Long, String> participantNames, Map<Long, PersonTotal> totals, String currency) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<Long, PersonTotal> entry : totals.entrySet()) {
            String name = participantNames.getOrDefault(entry.getKey(), "Participant " + entry.getKey());
            lines.add(name + ": " + currency + String.format("%.2f", entry.getValue().getTotal()));
        }
        return lines;
    }
}
