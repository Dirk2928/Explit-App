package com.example.explit.model;

public class ItemAssignment {
    private long itemId;
    private long participantId;
    private Double percent;

    public ItemAssignment(long itemId, long participantId, Double percent) {
        this.itemId = itemId;
        this.participantId = participantId;
        this.percent = percent;
    }

    public long getItemId() {
        return itemId;
    }

    public long getParticipantId() {
        return participantId;
    }

    public Double getPercent() {
        return percent;
    }
}
