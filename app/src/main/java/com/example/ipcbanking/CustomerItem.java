package com.example.ipcbanking;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

public class CustomerItem {
    private String uid;

    private String fullName;

    private String phoneNumber;

    private String avatarUrl;

    private boolean isKyced;

    public CustomerItem() { }

    @Exclude
    public String getUid() { return uid; }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFullName() { return fullName; }

    public String getPhoneNumber() { return phoneNumber; }

    public String getAvatarUrl() { return avatarUrl; }

    public boolean isKyced() { return isKyced; }
}