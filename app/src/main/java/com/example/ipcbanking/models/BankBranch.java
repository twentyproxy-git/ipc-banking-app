package com.example.ipcbanking.models;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

public class BankBranch implements Serializable {
    private String id;

    @PropertyName("name")
    private String name;

    @PropertyName("address")
    private String address;

    @PropertyName("latitude")
    private double latitude;

    @PropertyName("longitude")
    private double longitude;

    @PropertyName("opening_hours")
    private String openingHours;

    // Constructor rỗng bắt buộc cho Firestore
    public BankBranch() { }

    public BankBranch(String id, String name, String address, double latitude, double longitude, String openingHours) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.openingHours = openingHours;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("name")
    public String getName() { return name; }
    @PropertyName("name")
    public void setName(String name) { this.name = name; }

    @PropertyName("address")
    public String getAddress() { return address; }
    @PropertyName("address")
    public void setAddress(String address) { this.address = address; }

    @PropertyName("latitude")
    public double getLatitude() { return latitude; }
    @PropertyName("latitude")
    public void setLatitude(double latitude) { this.latitude = latitude; }

    @PropertyName("longitude")
    public double getLongitude() { return longitude; }
    @PropertyName("longitude")
    public void setLongitude(double longitude) { this.longitude = longitude; }

    @PropertyName("opening_hours")
    public String getOpeningHours() { return openingHours; }
    @PropertyName("opening_hours")
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }
}