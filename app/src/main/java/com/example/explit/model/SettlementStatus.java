package com.example.explit.model;

public class SettlementStatus {
    public long eventId;
    public long fromId;
    public long toId;
    public double amount;
    public String eventName;
    public long groupId;
    public String groupName;

    public SettlementStatus(long eventId, long fromId, long toId, double amount, String eventName, long groupId, String groupName) {
        this.eventId = eventId;
        this.fromId = fromId;
        this.toId = toId;
        this.amount = amount;
        this.eventName = eventName;
        this.groupId = groupId;
        this.groupName = groupName;
    }
}