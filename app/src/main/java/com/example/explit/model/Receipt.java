package com.example.explit.model;

public class Receipt {
    private long id;
    private long eventId;
    private String title;
    private double tax;
    private double tip;
    private double serviceCharge;
    private String photoPath;

    // ---------------
    // Receipt
    public Receipt(long id, long eventId, String title, double tax, double tip, double serviceCharge, String photoPath) {
        this.id = id;
        this.eventId = eventId;
        this.title = title;
        this.tax = tax;
        this.tip = tip;
        this.serviceCharge = serviceCharge;
        this.photoPath = photoPath;
    }

    // ---------------
    // getId
    public long getId() {
        return id;
    }

    // ---------------
    // getEventId
    public long getEventId() {
        return eventId;
    }

    // ---------------
    // getTitle
    public String getTitle() {
        return title;
    }

    // ---------------
    // getTax
    public double getTax() {
        return tax;
    }

    // ---------------
    // getTip
    public double getTip() {
        return tip;
    }

    // ---------------
    // getServiceCharge
    public double getServiceCharge() {
        return serviceCharge;
    }

    // ---------------
    // getPhotoPath
    public String getPhotoPath() {
        return photoPath;
    }
}
