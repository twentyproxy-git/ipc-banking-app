package com.example.ipcbanking.models;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class CustomerItem implements Serializable {

    private String uid; // ID Document

    @PropertyName("full_name")
    private String fullName;

    @PropertyName("phone_number")
    private String phoneNumber;

    // [MỚI] Thêm trường address
    @PropertyName("address")
    private String address;

    @PropertyName("avatar_url")
    private String avatarUrl;

    @PropertyName("email")
    private String email;

    @PropertyName("role")
    private String role;

    @PropertyName("kyc_status")
    private String kycStatus;

    @PropertyName("is_kyced")
    private boolean isKyced;

    @PropertyName("kyc_data")
    private Map<String, Object> kycData;

    @PropertyName("created_at")
    private Date createdAt;

    @PropertyName("device_token")
    private String deviceToken;

    public CustomerItem() { }

    // --- GETTERS & SETTERS ---

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    @PropertyName("full_name")
    public String getFullName() { return fullName; }
    @PropertyName("full_name")
    public void setFullName(String fullName) { this.fullName = fullName; }

    @PropertyName("phone_number")
    public String getPhoneNumber() { return phoneNumber; }
    @PropertyName("phone_number")
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    // [MỚI] Getter/Setter cho Address
    @PropertyName("address")
    public String getAddress() { return address; }
    @PropertyName("address")
    public void setAddress(String address) { this.address = address; }

    @PropertyName("avatar_url")
    public String getAvatarUrl() { return avatarUrl; }
    @PropertyName("avatar_url")
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    @PropertyName("email")
    public String getEmail() { return email; }
    @PropertyName("email")
    public void setEmail(String email) { this.email = email; }

    @PropertyName("role")
    public String getRole() { return role; }
    @PropertyName("role")
    public void setRole(String role) { this.role = role; }

    @PropertyName("kyc_status")
    public String getKycStatus() { return kycStatus; }
    @PropertyName("kyc_status")
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }

    @PropertyName("is_kyced")
    public boolean isKyced() { return isKyced; }
    @PropertyName("is_kyced")
    public void setKyced(boolean kyced) { isKyced = kyced; }

    @PropertyName("kyc_data")
    public Map<String, Object> getKycData() { return kycData; }
    @PropertyName("kyc_data")
    public void setKycData(Map<String, Object> kycData) { this.kycData = kycData; }

    @PropertyName("created_at")
    public Date getCreatedAt() { return createdAt; }
    @PropertyName("created_at")
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @PropertyName("device_token")
    public String getDeviceToken() { return deviceToken; }
    @PropertyName("device_token")
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
}