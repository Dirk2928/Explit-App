package com.example.explit.model;

public class Event {
    private long id;
    private long groupId;
    private String name;
    private String currency;
    private long paidByParticipantId;
    private long lastModified;

    public Event(long id, long groupId, String name, String currency, long paidByParticipantId) {
        this(id, groupId, name, currency, paidByParticipantId, 0);
    }

    public Event(long id, long groupId, String name, String currency, long paidByParticipantId, long lastModified) {
        this.id = id;
        this.groupId = groupId;
        this.name = name;
        this.currency = currency;
        this.paidByParticipantId = paidByParticipantId;
        this.lastModified = lastModified;
    }

    public long getId() { return id; }

    public long getGroupId() { return groupId; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getCurrency() { return currency; }

    public void setCurrency(String currency) { this.currency = currency; }

    public long getPaidByParticipantId() { return paidByParticipantId; }

    public void setPaidByParticipantId(long paidByParticipantId) { this.paidByParticipantId = paidByParticipantId; }

    public long getLastModified() { return lastModified; }

    public void setLastModified(long lastModified) { this.lastModified = lastModified; }
}