package com.example.explit.model;

public class Receipt {
    private long id;
    private long eventId;
    private String title;
    private double tax;
    private double tip;
    private double serviceCharge;
    private String photoPath;

    public Receipt(long id, long eventId, String title, double tax, double tip, double serviceCharge, String photoPath) {
        this.id = id;
        this.eventId = eventId;
        this.title = title;
        this.tax = tax;
        this.tip = tip;
        this.serviceCharge = serviceCharge;
        this.photoPath = photoPath;
    }

    public long getId() {
        return id;
    }
    public long getEventId() {
        return eventId;
    }
    public String getTitle() {
        return title;
    }

    public double getTax() {
        return tax;
    }

    public double getTip() {
        return tip;
    }

    public double getServiceCharge() {
        return serviceCharge;
    }

    public String getPhotoPath() {
        return photoPath;
    }
}
