package com.example.explit.model;

public class Participant {
    private long id;
    private long groupId;
    private String name;
    private String nickname;

    // ---------------
    // Participant
    public Participant(long id, long groupId, String name, String nickname) {
        this.id = id;
        this.groupId = groupId;
        this.name = name;
        this.nickname = nickname;
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
    // getNickname
    public String getNickname() {
        return nickname;
    }

    // ---------------
    // getDisplayName
    public String getDisplayName() {
        return nickname == null || nickname.isEmpty() ? name : name + " (" + nickname + ")";
    }
}
