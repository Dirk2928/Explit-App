package com.example.explit.model;

public class Group {
    private long id;
    private String name;
    private String category;
    private boolean pinned;

    public Group(long id, String name, String category) {
        this(id, name, category, false);
    }

    public Group(long id, String name, String category, boolean pinned) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.pinned = pinned;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }
}