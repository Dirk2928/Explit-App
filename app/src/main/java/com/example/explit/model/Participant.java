package com.example.explit.model;

public class Participant {
    private long id;
    private long groupId;
    private String name;
    private String nickname;

    public Participant(long id, long groupId, String name, String nickname) {
        this.id = id;
        this.groupId = groupId;
        this.name = name;
        this.nickname = nickname;
    }

    public long getId() {
        return id;
    }

    public long getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public String getNickname() {
        return nickname;
    }
    public String getDisplayName() {
        return nickname == null || nickname.isEmpty() ? name : name + " (" + nickname + ")";
    }
}
