package com.example.explit.model;

public class ItemAssignment {
    private long itemId;
    private long participantId;
    private Double percent;

    // ---------------
    // ItemAssignment
    public ItemAssignment(long itemId, long participantId, Double percent) {
        this.itemId = itemId;
        this.participantId = participantId;
        this.percent = percent;
    }

    // ---------------
    // getItemId
    public long getItemId() {
        return itemId;
    }

    // ---------------
    // getParticipantId
    public long getParticipantId() {
        return participantId;
    }

    // ---------------
    // getPercent
    public Double getPercent() {
        return percent;
    }
}
