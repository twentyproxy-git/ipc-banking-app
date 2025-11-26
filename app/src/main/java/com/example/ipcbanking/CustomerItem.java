package com.example.ipcbanking;

import com.google.firebase.firestore.PropertyName;

public class CustomerItem {
    private String uid;

    @PropertyName("full_name")
    private String fullName;

    @PropertyName("phone_number")
    private String phoneNumber;

    @PropertyName("avatar_url")
    private String avatarUrl;

    @PropertyName("is_kyced")
    private boolean isKyced;

    public CustomerItem() { }

    // Getters cũng phải thêm Annotation để khi đẩy ngược lên DB nó hiểu
    public String getUid() { return uid; }

    @PropertyName("full_name")
    public String getFullName() { return fullName; }

    @PropertyName("phone_number")
    public String getPhoneNumber() { return phoneNumber; }

    @PropertyName("avatar_url")
    public String getAvatarUrl() { return avatarUrl; }

    @PropertyName("is_kyced")
    public boolean isKyced() { return isKyced; }
}