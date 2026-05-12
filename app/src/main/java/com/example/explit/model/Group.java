package com.example.explit.model;

public class Group {
    private long id;
    private String name;
    private String category;

    // ---------------
    // Group
    public Group(long id, String name, String category) {
        this.id = id;
        this.name = name;
        this.category = category;
    }

    // ---------------
    // getId
    public long getId() {
        return id;
    }

    // ---------------
    // getName
    public String getName() {
        return name;
    }

    // ---------------
    // getCategory
    public String getCategory() {
        return category;
    }
}
