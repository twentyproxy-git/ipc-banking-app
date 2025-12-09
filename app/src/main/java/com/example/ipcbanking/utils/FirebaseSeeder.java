package com.example.ipcbanking.utils;

import android.content.Context;
import android.util.Log;

import com.example.ipcbanking.models.UserData;
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
    private final List<UserData> seedList = new ArrayList<>();

    private final Map<String, String> createdUserIds = new HashMap<>();

    public FirebaseSeeder(Context context) {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    // ==================== PUBLIC CALL ====================
    public void seedUsers() {
        seedBankConfig();
        seedBankBranches();

        seedList.clear();
        createdUserIds.clear();

        // OFFICERS
        seedList.add(new UserData("topaz@ipc.com", "topaz123", "Topaz Numby", "0901000111",
                "Pier Point, Trụ sở IPC", "OFFICER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319532/qmr7tdydrnneiiflkxhb.png"));

        seedList.add(new UserData("aventurine@ipc.com", "aventurine123", "Aventurine Stratos", "0901000222",
                "Sigonia-IV, Khu Tài Chính", "OFFICER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319490/jvn1en9l5g1dxzqx7twg.png"));

        seedList.add(new UserData("sunday@ipc.com", "sunday123", "Sunday Halovian", "0901000333",
                "Khách sạn Reverie, Penacony", "OFFICER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319526/eouerxvjagmdisvagc6d.png"));

        // CUSTOMERS
        seedList.add(new UserData("kafka@gmail.com", "kafka123", "Kafka", "0909666777",
                "Pteruges-V, Hầm trú ẩn Stellaron", "CUSTOMER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319508/rnbwc3kipbbtg4y7puhr.png"));

        seedList.add(new UserData("silverwolf@gmail.com", "silverwolf123", "Silver Wolf", "0909888999",
                "Punklorde, Tiệm Net 8-bit", "CUSTOMER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319517/mgbqst17h39kfukr8zsf.png"));

        seedList.add(new UserData("firefly@gmail.com", "firefly123", "Firefly", "0909111222",
                "Glamoth, Căn cứ quân sự", "CUSTOMER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319501/hbqh21rwcyn5rorjlz2h.png"));

        processUserAtIndex(0);
    }

    // ==================== BANK CONFIG ====================
    private void seedBankConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("savings_rate", 5.5);
        config.put("loan_rate", 7.5);

        db.collection("bank_config").document("rates")
                .set(config, SetOptions.merge());
    }

    private void seedBankBranches() {
        List<Map<String, Object>> branches = new ArrayList<>();
        branches.add(createBranchMap("IPC Main HQ (Bitexco)", "2 Hai Trieu, Ben Nghe, Q1", 10.771661, 106.704372, "8:00 - 17:00"));
        branches.add(createBranchMap("IPC Landmark 81", "720A Dien Bien Phu, Binh Thanh", 10.795005, 106.721846, "9:00 - 20:00"));
        branches.add(createBranchMap("IPC Independence Palace", "135 Nam Ky Khoi Nghia, Q1", 10.776996, 106.695333, "8:00 - 16:00"));
        branches.add(createBranchMap("IPC Ben Thanh Market", "Cho Ben Thanh, Le Loi, Q1", 10.772109, 106.698275, "8:00 - 18:00"));

        for (int i = 0; i < branches.size(); i++) {
            String docId = "branch_0" + (i + 1);
            db.collection("bank_branches").document(docId)
                    .set(branches.get(i), SetOptions.merge());
        }
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

    // ==================== USER SEED ====================
    private void processUserAtIndex(int index) {
        if (index >= seedList.size()) {
            Log.d("FirebaseSeeder", "Users Seeded → Seeding Transactions...");
            seedTransactions();
            return;
        }
        UserData user = seedList.get(index);
        createOrUpdateUser(user, () -> processUserAtIndex(index + 1));
    }

    private void createOrUpdateUser(UserData u, Runnable onComplete) {
        auth.createUserWithEmailAndPassword(u.getEmail(), u.getPassword())
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) saveUserToFirestore(user.getUid(), u, onComplete);
                    else onComplete.run();
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        auth.signInWithEmailAndPassword(u.getEmail(), u.getPassword())
                                .addOnSuccessListener(authResult -> {
                                    FirebaseUser user = authResult.getUser();
                                    if (user != null) saveUserToFirestore(user.getUid(), u, onComplete);
                                    else onComplete.run();
                                })
                                .addOnFailureListener(e2 -> onComplete.run());
                    } else onComplete.run();
                });
    }

    private void saveUserToFirestore(String uid, UserData u, Runnable onComplete) {
        createdUserIds.put(u.getEmail(), uid);

        Map<String, Object> data = new HashMap<>();
        data.put("full_name", u.getFullName());
        data.put("email", u.getEmail());
        data.put("phone_number", u.getPhoneNumber());
        data.put("address", u.getAddress());
        data.put("role", u.getRole());
        data.put("created_at", FieldValue.serverTimestamp());
        data.put("device_token", "");
        data.put("avatar_url", u.getAvatarUrl());

        boolean verified = u.getRole().equals("OFFICER") ||
                u.getEmail().contains("kafka") ||
                u.getEmail().contains("firefly");

        data.put("is_kyced", verified);
        data.put("kyc_status", verified ? "VERIFIED" : "UNVERIFIED");

        // ==================== KYC DATA ====================
        Map<String, Object> kyc = new HashMap<>();

        if (verified) {
            if (u.getEmail().contains("kafka")) {
                kyc.put("face_image_url", "https://res.cloudinary.com/ipc-media/image/upload/v1764142585/nwawkaoucf9mq0n1rtcp.png");
                kyc.put("id_card_url", "https://res.cloudinary.com/ipc-media/image/upload/v1764318945/zuzey4lxwoeavdmrhlmo.png");
                kyc.put("id_card_number", "079200040001");
            } else if (u.getEmail().contains("firefly")) {
                kyc.put("face_image_url", "https://res.cloudinary.com/ipc-media/image/upload/v1764142600/rhufwnt3zyvtr7xtuzyq.png");
                kyc.put("id_card_url", "https://res.cloudinary.com/ipc-media/image/upload/v1764320933/hokhd098u0iicbivmti2.png");
                kyc.put("id_card_number", "079200040002");
            } else {
                kyc.put("face_image_url", null);
                kyc.put("id_card_url", null);
                kyc.put("id_card_number", null);
            }
        } else {
            kyc.put("face_image_url", null);
            kyc.put("id_card_url", null);
            kyc.put("id_card_number", null);
        }

        data.put("kyc_data", kyc);

        db.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (u.getRole().equals("CUSTOMER")) {
                        seedAccountsForCustomer(uid, u.getEmail());
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> onComplete.run());
    }

    private void seedAccountsForCustomer(String uid, String email) {
        createAccount(uid, uid + "_CHECKING", "101" + uid.substring(0, 5).toUpperCase(),
                "CHECKING", 50000000.0, 0, 0);

        if (email.startsWith("kafka"))
            createAccount(uid, uid + "_SAVING", "202" + uid.substring(0, 5).toUpperCase(),
                    "SAVING", 200000000.0, 5.5, 0);

        if (email.startsWith("firefly"))
            createAccount(uid, uid + "_MORTGAGE", "303" + uid.substring(0, 5).toUpperCase(),
                    "MORTGAGE", -1000000000.0, 7.5, 15000000.0);
    }

    private void createAccount(String ownerId, String docId, String accNum, String type,
                               double balance, double rate, double monthlyPay) {

        Map<String, Object> map = new HashMap<>();
        map.put("owner_id", ownerId);
        map.put("account_number", accNum);
        map.put("account_type", type);
        map.put("balance", balance);
        map.put("created_at", FieldValue.serverTimestamp());

        if (!type.equals("CHECKING")) map.put("profit_rate", rate);
        if (type.equals("MORTGAGE")) map.put("monthly_payment", monthlyPay);

        db.collection("accounts").document(docId)
                .set(map, SetOptions.merge());
    }

    // ==================== TRANSACTION SEED ====================
    private void seedTransactions() {
        Log.d("FirebaseSeeder", "Seeding Transactions...");

        String kafkaUid = createdUserIds.get("kafka@gmail.com");
        String swUid = createdUserIds.get("silverwolf@gmail.com");
        String fireflyUid = createdUserIds.get("firefly@gmail.com");
        if (kafkaUid == null || swUid == null || fireflyUid == null) return;

        String kafkaAcc = "101" + kafkaUid.substring(0, 5).toUpperCase();
        String swAcc = "101" + swUid.substring(0, 5).toUpperCase();
        String fireflyAcc = "101" + fireflyUid.substring(0, 5).toUpperCase();

        List<Map<String, Object>> list = new ArrayList<>();

        // Internal Transfer
        list.add(createTransaction("TRANSFER", kafkaAcc, "Kafka",
                swAcc, "Silver Wolf",
                "IPC Bank", 500000, "Buy Stellaron Games"));

        list.add(createTransaction("TRANSFER", swAcc, "Silver Wolf",
                kafkaAcc, "Kafka",
                "IPC Bank", 150000, "Pay back dinner"));

        // Deposit from MoMo to IPC
        list.add(createTransaction("DEPOSIT", "EXTERNAL", "Kafka (MoMo)",
                kafkaAcc, "Kafka",
                "MoMo", 1000000, "Top up from MoMo"));

        // Withdrawal from IPC to Vietcombank
        list.add(createTransaction("WITHDRAWAL", swAcc, "Silver Wolf",
                "EXTERNAL", "Silver Wolf (Vietcombank)",
                "Vietcombank", 2000000, "Cash out to VCB"));

        for (Map<String, Object> t : list) {
            db.collection("transactions").add(t);
        }
        Log.d("FirebaseSeeder", "DONE!");
    }

    private Map<String, Object> createTransaction(
            String type,
            String senderAcc, String senderName,
            String receiverAcc, String receiverName,
            String counterpartyBank,
            double amount, String msg) {

        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("sender_account", senderAcc);
        map.put("sender_name", senderName);
        map.put("receiver_account", receiverAcc);
        map.put("receiver_name", receiverName);
        map.put("counterparty_bank", counterpartyBank);
        map.put("amount", amount);
        map.put("message", msg);
        map.put("status", "SUCCESS");
        map.put("created_at", FieldValue.serverTimestamp());
        return map;
    }
}
