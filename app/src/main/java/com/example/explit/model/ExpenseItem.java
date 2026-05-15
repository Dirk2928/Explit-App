package com.example.explit.model;

public class ExpenseItem {
    private long id;
    private long receiptId;
    private String name;
    private double amount;
    private boolean shared;
    private long payerParticipantId;

    public ExpenseItem(long id, long receiptId, String name, double amount, boolean shared, long payerParticipantId) {
        this.id = id;
        this.receiptId = receiptId;
        this.name = name;
        this.amount = amount;
        this.shared = shared;
        this.payerParticipantId = payerParticipantId;
    }

    public long getId() {
        return id;
    }

    public long getReceiptId() {
        return receiptId;
    }

    public String getName() {
        return name;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isShared() {
        return shared;
    }

    public long getPayerParticipantId() {
        return payerParticipantId;
    }
}
