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
        Map<Long, PersonTotal> totals = new HashMap<>();

        for (ExpenseItem item : items) {
            List<ItemAssignment> assignments = assignmentsByItem.get(item.getId());
            if (assignments == null || assignments.isEmpty()) {
                continue;
            }

            double share = item.getAmount() / assignments.size();
            for (ItemAssignment a : assignments) {
                long pid = a.getParticipantId();
                PersonTotal existing = totals.get(pid);
                double newSubtotal = (existing != null ? existing.getSubtotal() : 0) + share;
                totals.put(pid, new PersonTotal(newSubtotal, 0));
            }
        }
        return totals;
    }

    public static List<Settlement> calculateSettlements(Map<Long, PersonTotal> owedMap, Map<Long, Double> paidMap) {
        Map<Long, Double> netBalances = SettlementCalculator.buildNetBalances(owedMap, paidMap);
        List<SettlementCalculator.Payment> payments = SettlementCalculator.minimizeTransactions(netBalances);

        List<Settlement> settlements = new ArrayList<>();
        for (SettlementCalculator.Payment payment : payments) {
            settlements.add(new Settlement(
                    payment.getFromParticipantId(),
                    payment.getToParticipantId(),
                    payment.getAmount()
            ));
        }
        return settlements;
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
