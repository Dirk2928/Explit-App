package com.example.explit.model;

public class Event {
    private long id;
    private long groupId;
    private String name;
    private String currency;
    private long paidByParticipantId;

    // ---------------
    // Event
    public Event(long id, long groupId, String name, String currency, long paidByParticipantId) {
        this.id = id;
        this.groupId = groupId;
        this.name = name;
        this.currency = currency;
        this.paidByParticipantId = paidByParticipantId;
    }

    // ---------------
    // getId
    public long getId() {
        return id;
    }

    // ---------------
    // getGroupId
    public long getGroupId() {
        return groupId;
    }

    // ---------------
    // getName
    public String getName() {
        return name;
    }

    // ---------------
    // setName
    public void setName(String name) {
        this.name = name;
    }

    // ---------------
    // getCurrency
    public String getCurrency() {
        return currency;
    }

    // ---------------
    // setCurrency
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    // ---------------
    // getPaidByParticipantId
    public long getPaidByParticipantId() {
        return paidByParticipantId;
    }

    // ---------------
    // setPaidByParticipantId
    public void setPaidByParticipantId(long paidByParticipantId) {
        this.paidByParticipantId = paidByParticipantId;
    }
}
