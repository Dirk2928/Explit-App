package com.example.explit.util;

import com.example.explit.model.ExpenseItem;
import com.example.explit.model.ItemAssignment;
import com.example.explit.model.Receipt;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

class SplitCalculatorTest {

    // ---------------
    // calculateTotals_distributesExtrasProportionally
    @Test
    void calculateTotals_distributesExtrasProportionally() {
        ExpenseItem itemA = new ExpenseItem(10, 1, "Food", 60, true, -1);
        ExpenseItem itemB = new ExpenseItem(11, 1, "Drink", 40, true, -1);

        Map<Long, List<ItemAssignment>> assignments = Map.of(
                10L, List.of(new ItemAssignment(10, 1, 50d), new ItemAssignment(10, 2, 50d)),
                11L, List.of(new ItemAssignment(11, 1, 100d))
        );

        List<Receipt> receipts = List.of(new Receipt(1, 1, "R1", 10, 10, 0, null));

        Map<Long, SplitCalculator.PersonTotal> totals = SplitCalculator.calculateTotals(List.of(itemA, itemB), assignments, receipts);

        assertEquals(62.0, round2(totals.get(1L).getTotal()));
        assertEquals(38.0, round2(totals.get(2L).getTotal()));
    }

    // ---------------
    // minimizeTransactions_generatesSingleSettlement
    @Test
    void minimizeTransactions_generatesSingleSettlement() {
        Map<Long, Double> net = Map.of(1L, 50d, 2L, -30d, 3L, -20d);
        List<SettlementCalculator.Payment> payments = SettlementCalculator.minimizeTransactions(net);

        assertEquals(2, payments.size());
        assertEquals(50.0, round2(payments.get(0).getAmount() + payments.get(1).getAmount()));
    }

    // ---------------
    // round2
    private double round2(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
