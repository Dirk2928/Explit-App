package com.example.explit.model;

public class ExpenseItem {
    private long id;
    private long receiptId;
    private String name;
    private double amount;
    private boolean shared;
    private long payerParticipantId;

    // ---------------
    // ExpenseItem
    public ExpenseItem(long id, long receiptId, String name, double amount, boolean shared, long payerParticipantId) {
        this.id = id;
        this.receiptId = receiptId;
        this.name = name;
        this.amount = amount;
        this.shared = shared;
        this.payerParticipantId = payerParticipantId;
    }

    // ---------------
    // getId
    public long getId() {
        return id;
    }

    // ---------------
    // getReceiptId
    public long getReceiptId() {
        return receiptId;
    }

    // ---------------
    // getName
    public String getName() {
        return name;
    }

    // ---------------
    // getAmount
    public double getAmount() {
        return amount;
    }

    // ---------------
    // isShared
    public boolean isShared() {
        return shared;
    }

    // ---------------
    // getPayerParticipantId
    public long getPayerParticipantId() {
        return payerParticipantId;
    }
}
