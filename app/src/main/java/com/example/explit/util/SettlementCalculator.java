package com.example.explit.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettlementCalculator {

    public static class Payment {
        private final long fromParticipantId;
        private final long toParticipantId;
        private final double amount;

        // ---------------
        // Payment
        public Payment(long fromParticipantId, long toParticipantId, double amount) {
            this.fromParticipantId = fromParticipantId;
            this.toParticipantId = toParticipantId;
            this.amount = amount;
        }

        // ---------------
        // getFromParticipantId
        public long getFromParticipantId() {
            return fromParticipantId;
        }

        // ---------------
        // getToParticipantId
        public long getToParticipantId() {
            return toParticipantId;
        }

        // ---------------
        // getAmount
        public double getAmount() {
            return amount;
        }
    }

    // ---------------
    // minimizeTransactions
    public static List<Payment> minimizeTransactions(Map<Long, Double> netBalances) {
        List<Map.Entry<Long, Double>> debtors = new ArrayList<>();
        List<Map.Entry<Long, Double>> creditors = new ArrayList<>();

        for (Map.Entry<Long, Double> entry : netBalances.entrySet()) {
            if (entry.getValue() < -0.005d) {
                debtors.add(Map.entry(entry.getKey(), -entry.getValue()));
            } else if (entry.getValue() > 0.005d) {
                creditors.add(Map.entry(entry.getKey(), entry.getValue()));
            }
        }

        debtors.sort(Comparator.comparingDouble(Map.Entry::getValue));
        creditors.sort(Comparator.comparingDouble(Map.Entry::getValue));

        List<Payment> settlements = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            Map.Entry<Long, Double> debtor = debtors.get(i);
            Map.Entry<Long, Double> creditor = creditors.get(j);
            double paid = Math.min(debtor.getValue(), creditor.getValue());
            settlements.add(new Payment(debtor.getKey(), creditor.getKey(), paid));

            double debtorLeft = debtor.getValue() - paid;
            double creditorLeft = creditor.getValue() - paid;
            debtors.set(i, Map.entry(debtor.getKey(), debtorLeft));
            creditors.set(j, Map.entry(creditor.getKey(), creditorLeft));

            if (debtorLeft <= 0.005d) {
                i++;
            }
            if (creditorLeft <= 0.005d) {
                j++;
            }
        }
        return settlements;
    }

    // ---------------
    // buildNetBalances
    public static Map<Long, Double> buildNetBalances(Map<Long, SplitCalculator.PersonTotal> owes, Map<Long, Double> paid) {
        Map<Long, Double> net = new HashMap<>();
        for (Map.Entry<Long, SplitCalculator.PersonTotal> entry : owes.entrySet()) {
            long personId = entry.getKey();
            double paidAmount = paid.getOrDefault(personId, 0d);
            net.put(personId, paidAmount - entry.getValue().getTotal());
        }
        for (Map.Entry<Long, Double> entry : paid.entrySet()) {
            if (!net.containsKey(entry.getKey())) {
                net.put(entry.getKey(), entry.getValue());
            }
        }
        return net;
    }
}
