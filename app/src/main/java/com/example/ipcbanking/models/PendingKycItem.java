package com.example.ipcbanking.models;

import com.google.firebase.firestore.PropertyName; // [QUAN TRỌNG]
import java.util.Map;

public class PendingKycItem {
    private String uid;

    // Khai báo biến
    private String fullName;
    private String kycStatus;
    private Map<String, Object> kycData;

    public PendingKycItem() { }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    // --- MAPPING QUAN TRỌNG ---

    // 1. Full Name
    // Nếu trên Firestore là "full_name" (gạch dưới)
    @PropertyName("full_name")
    public String getFullName() { return fullName; }

    @PropertyName("full_name")
    public void setFullName(String fullName) { this.fullName = fullName; }

    // 2. KYC Status
    // Nếu trên Firestore là "kyc_status"
    @PropertyName("kyc_status")
    public String getKycStatus() { return kycStatus; }

    @PropertyName("kyc_status")
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }

    // 3. KYC Data (Map chứa ảnh)
    // Nếu trên Firestore là "kyc_data"
    @PropertyName("kyc_data")
    public Map<String, Object> getKycData() { return kycData; }

    @PropertyName("kyc_data")
    public void setKycData(Map<String, Object> kycData) { this.kycData = kycData; }
}