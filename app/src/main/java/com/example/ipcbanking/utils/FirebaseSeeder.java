package com.example.ipcbanking.utils;

import android.content.Context;
import android.util.Log;

import com.example.ipcbanking.models.UserSeedData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseSeeder {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final List<UserSeedData> seedList = new ArrayList<>();

    // Map để lưu UID tạm thời (Email -> UID) dùng cho việc tạo Transaction
    private final Map<String, String> createdUserIds = new HashMap<>();

    public FirebaseSeeder(Context context) {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public void seedUsers() {
        // 1. Seed Bank Config (Lãi suất)
        seedBankConfig();

        // 2. Seed Bank Branches (Chi nhánh)
        seedBankBranches();

        seedList.clear();
        createdUserIds.clear();

        // --- OFFICERS DATA ---
        seedList.add(new UserSeedData("topaz@ipc.com", "topaz123", "Topaz Numby", "0901000111",
                "Pier Point, Trụ sở IPC", "OFFICER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319532/qmr7tdydrnneiiflkxhb.png"));

        seedList.add(new UserSeedData("aventurine@ipc.com", "aventurine123", "Aventurine Stratos", "0901000222",
                "Sigonia-IV, Khu Tài Chính", "OFFICER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319490/jvn1en9l5g1dxzqx7twg.png"));

        seedList.add(new UserSeedData("sunday@ipc.com", "sunday123", "Sunday Halovian", "0901000333",
                "Khách sạn Reverie, Penacony", "OFFICER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319526/eouerxvjagmdisvagc6d.png"));

        // --- CUSTOMERS DATA ---
        seedList.add(new UserSeedData("kafka@gmail.com", "kafka123", "Kafka", "0909666777",
                "Pteruges-V, Hầm trú ẩn Stellaron", "CUSTOMER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319508/rnbwc3kipbbtg4y7puhr.png"));

        seedList.add(new UserSeedData("silverwolf@gmail.com", "silverwolf123", "Silver Wolf", "0909888999",
                "Punklorde, Tiệm Net 8-bit", "CUSTOMER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319517/mgbqst17h39kfukr8zsf.png"));

        seedList.add(new UserSeedData("firefly@gmail.com", "firefly123", "Firefly", "0909111222",
                "Glamoth, Căn cứ quân sự", "CUSTOMER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319501/hbqh21rwcyn5rorjlz2h.png"));

        processUserAtIndex(0);
    }

    private void seedBankConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("savings_rate", 5.5);
        config.put("loan_rate", 7.5);

        db.collection("bank_config").document("rates")
                .set(config, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("FirebaseSeeder", "✅ Bank Config Seeded"));
    }

    private void seedBankBranches() {
        Log.d("FirebaseSeeder", "📍 Seeding Bank Branches...");
        List<Map<String, Object>> branches = new ArrayList<>();

        // Tọa độ các điểm nổi tiếng ở TP.HCM
        branches.add(createBranchMap("IPC Main HQ (Bitexco)", "2 Hai Trieu, Ben Nghe, Q1", 10.771661, 106.704372, "8:00 - 17:00"));
        branches.add(createBranchMap("IPC Landmark 81", "720A Dien Bien Phu, Binh Thanh", 10.795005, 106.721846, "9:00 - 20:00"));
        branches.add(createBranchMap("IPC Independence Palace", "135 Nam Ky Khoi Nghia, Q1", 10.776996, 106.695333, "8:00 - 16:00"));
        branches.add(createBranchMap("IPC Ben Thanh Market", "Cho Ben Thanh, Le Loi, Q1", 10.772109, 106.698275, "8:00 - 18:00"));

        for (int i = 0; i < branches.size(); i++) {
            String docId = "branch_0" + (i + 1);
            db.collection("bank_branches").document(docId)
                    .set(branches.get(i), SetOptions.merge());
        }
        Log.d("FirebaseSeeder", "✅ Bank Branches Seeded.");
    }

    private Map<String, Object> createBranchMap(String name, String address, double lat, double lng, String hours) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("address", address);
        map.put("latitude", lat);
        map.put("longitude", lng);
        map.put("opening_hours", hours);
        return map;
    }

    private void processUserAtIndex(int index) {
        if (index >= seedList.size()) {
            Log.d("FirebaseSeeder", "✅ Users & Accounts Seeded. Starting Transactions...");
            seedTransactions();
            return;
        }

        UserSeedData user = seedList.get(index);
        Log.d("FirebaseSeeder", "⏳ Processing " + (index + 1) + "/" + seedList.size() + ": " + user.getEmail());

        createOrUpdateUser(user, () -> processUserAtIndex(index + 1));
    }

    private void createOrUpdateUser(UserSeedData u, Runnable onComplete) {
        auth.createUserWithEmailAndPassword(u.getEmail(), u.getPassword())
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        saveUserToFirestore(user.getUid(), u, onComplete);
                    } else {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        auth.signInWithEmailAndPassword(u.getEmail(), u.getPassword())
                                .addOnSuccessListener(authResult -> {
                                    FirebaseUser user = authResult.getUser();
                                    if (user != null) {
                                        saveUserToFirestore(user.getUid(), u, onComplete);
                                    } else {
                                        onComplete.run();
                                    }
                                })
                                .addOnFailureListener(e2 -> onComplete.run());
                    } else {
                        onComplete.run();
                    }
                });
    }

    private void saveUserToFirestore(String uid, UserSeedData u, Runnable onComplete) {
        createdUserIds.put(u.getEmail(), uid);

        Map<String, Object> userData = new HashMap<>();
        userData.put("full_name", u.getFullName());
        userData.put("email", u.getEmail());
        userData.put("phone_number", u.getPhoneNumber());
        userData.put("address", u.getAddress());
        userData.put("role", u.getRole());
        userData.put("created_at", FieldValue.serverTimestamp());
        userData.put("device_token", "");

        String avatar = u.getAvatarUrl();
        if (avatar == null || avatar.isEmpty()) {
            userData.put("avatar_url", "https://ui-avatars.com/api/?name=" + u.getFullName().replace(" ", "+"));
        } else {
            userData.put("avatar_url", avatar);
        }

        Map<String, Object> kycDataMap = new HashMap<>();
        if (u.getRole().equals("OFFICER")) {
            userData.put("is_kyced", true);
            userData.put("kyc_status", "VERIFIED");
            kycDataMap.put("id_card_number", "079099123456");
            kycDataMap.put("face_image_url", u.getAvatarUrl());
            kycDataMap.put("id_card_url", "https://res.cloudinary.com/ipc-media/image/upload/v1/samples/id_card_sample");
            kycDataMap.put("verified_at", FieldValue.serverTimestamp());
        } else {
            // KYC Customers
            boolean isVerified = false;
            String status = "UNVERIFIED";
            String idCardNum = null;
            String faceUrl = null;
            String idCardUrl = null;
            Object verifiedAt = null;

            if (u.getEmail().contains("kafka")) {
                isVerified = true; status = "VERIFIED"; idCardNum = "079199000001";
                faceUrl = "https://res.cloudinary.com/ipc-media/image/upload/v1764142585/nwawkaoucf9mq0n1rtcp.png";
                idCardUrl = "https://res.cloudinary.com/ipc-media/image/upload/v1764318945/zuzey4lxwoeavdmrhlmo.png";
                verifiedAt = FieldValue.serverTimestamp();
            } else if (u.getEmail().contains("firefly")) {
                isVerified = true; status = "VERIFIED"; idCardNum = "079199000002";
                faceUrl = "https://res.cloudinary.com/ipc-media/image/upload/v1764142600/rhufwnt3zyvtr7xtuzyq.png";
                idCardUrl = "https://res.cloudinary.com/ipc-media/image/upload/v1764320933/hokhd098u0iicbivmti2.png";
                verifiedAt = FieldValue.serverTimestamp();
            }
            userData.put("is_kyced", isVerified);
            userData.put("kyc_status", status);
            kycDataMap.put("id_card_number", idCardNum);
            kycDataMap.put("face_image_url", faceUrl);
            kycDataMap.put("id_card_url", idCardUrl);
            kycDataMap.put("verified_at", verifiedAt);
        }
        userData.put("kyc_data", kycDataMap);

        db.collection("users").document(uid)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (u.getRole().equals("CUSTOMER")) {
                        seedAccountsForCustomer(uid, u.getEmail());
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> onComplete.run());
    }

    private void seedAccountsForCustomer(String uid, String email) {
        createAccount(uid, uid + "_CHECKING", "101" + uid.substring(0, 5).toUpperCase(), "CHECKING", 50000000.0, 0, 0);
        if (email.startsWith("kafka")) createAccount(uid, uid + "_SAVING", "202" + uid.substring(0, 5).toUpperCase(), "SAVING", 200000000.0, 5.5, 0);
        if (email.startsWith("firefly")) createAccount(uid, uid + "_MORTGAGE", "303" + uid.substring(0, 5).toUpperCase(), "MORTGAGE", -1000000000.0, 7.5, 15000000.0);
    }

    private void createAccount(String ownerId, String docId, String accNum, String type, double balance, double rate, double monthlyPay) {
        Map<String, Object> accData = new HashMap<>();
        accData.put("owner_id", ownerId);
        accData.put("account_number", accNum);
        accData.put("account_type", type);
        accData.put("balance", balance);
        accData.put("created_at", FieldValue.serverTimestamp());
        if (type.equals("SAVING")) accData.put("profit_rate", rate);
        if (type.equals("MORTGAGE")) { accData.put("profit_rate", rate); accData.put("monthly_payment", monthlyPay); }
        db.collection("accounts").document(docId).set(accData, SetOptions.merge());
    }

    private void seedTransactions() {
        Log.d("FirebaseSeeder", "💸 Seeding Transactions...");
        String kafkaUid = createdUserIds.get("kafka@gmail.com");
        String swUid = createdUserIds.get("silverwolf@gmail.com");
        String fireflyUid = createdUserIds.get("firefly@gmail.com");

        if (kafkaUid == null || swUid == null || fireflyUid == null) return;

        String kafkaAcc = "101" + kafkaUid.substring(0, 5).toUpperCase();
        String swAcc = "101" + swUid.substring(0, 5).toUpperCase();
        String fireflyAcc = "101" + fireflyUid.substring(0, 5).toUpperCase();

        List<Map<String, Object>> transactions = new ArrayList<>();
        transactions.add(createTransactionMap(kafkaUid, kafkaAcc, swAcc, "Silver Wolf", 500000, "Buy Stellaron Games", "INTERNAL"));
        transactions.add(createTransactionMap(swUid, swAcc, kafkaAcc, "Kafka", 150000, "Pay back dinner", "INTERNAL"));
        transactions.add(createTransactionMap(fireflyUid, fireflyAcc, kafkaAcc, "Kafka", 2000000, "Glamoth budget", "INTERNAL"));
        transactions.add(createTransactionMap(kafkaUid, kafkaAcc, fireflyAcc, "Firefly", 300000, "Coffee money", "INTERNAL"));

        for (Map<String, Object> trans : transactions) {
            db.collection("transactions").add(trans);
        }
        Log.d("FirebaseSeeder", "🎉 DONE! All transactions seeded.");
    }

    private Map<String, Object> createTransactionMap(String senderId, String senderAcc, String receiverAcc, String receiverName, double amount, String msg, String type) {
        Map<String, Object> map = new HashMap<>();
        map.put("sender_id", senderId);
        map.put("sender_account", senderAcc);
        map.put("receiver_account", receiverAcc);
        map.put("receiver_name", receiverName);
        map.put("amount", amount);
        map.put("message", msg);
        map.put("type", type);
        map.put("status", "SUCCESS");
        map.put("created_at", FieldValue.serverTimestamp());
        return map;
    }
}