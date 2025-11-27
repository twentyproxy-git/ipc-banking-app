package com.example.ipcbanking;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class FirebaseSeeder {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public FirebaseSeeder(Context context) {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public void seedUsers() {
        // 1. OFFICER
        createOrUpdateUser("topaz@ipc.com", "topaz123", "Topaz Numby", "0901000111", "OFFICER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764142149/urizqa3znxqdujax2eea.png");

        createOrUpdateUser("aventurine@ipc.com", "aventurine123", "Aventurine Stratos", "0901000222", "OFFICER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764142267/tw8lblesl115pnn4dn03.png");

        // 2. CUSTOMER
        createOrUpdateUser("kafka@gmail.com", "kafka123", "Kafka", "0909666777", "CUSTOMER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764142585/nwawkaoucf9mq0n1rtcp.png");

        createOrUpdateUser("silverwolf@gmail.com", "silverwolf123", "Silver Wolf", "0909888999", "CUSTOMER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764142596/bx1wlvf6qdjbwurbz0sb.png");

        createOrUpdateUser("firefly@gmail.com", "firefly123", "Firefly", "0909111222", "CUSTOMER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764142600/rhufwnt3zyvtr7xtuzyq.png");
    }

    private void createOrUpdateUser(String email, String password, String fullName, String phoneNumber, String role, String avatarUrl) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        Log.d("FirebaseSeeder", "✅ Created New Auth: " + email);
                        saveUserToFirestore(user.getUid(), email, fullName, phoneNumber, role, avatarUrl);
                    }
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        Log.d("FirebaseSeeder", "⚠️ User exists, attempting update: " + email);
                        // Đăng nhập để lấy UID rồi update
                        auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener(authResult -> {
                                    FirebaseUser user = authResult.getUser();
                                    if (user != null) {
                                        saveUserToFirestore(user.getUid(), email, fullName, phoneNumber, role, avatarUrl);
                                    }
                                });
                    }
                });
    }

    private void saveUserToFirestore(String uid, String email, String fullName, String phoneNumber, String role, String avatarUrl) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("fullName", fullName);
        userData.put("email", email);
        userData.put("phoneNumber", phoneNumber);
        userData.put("role", role);
        userData.put("createdAt", FieldValue.serverTimestamp());

        if (avatarUrl == null || avatarUrl.isEmpty()) {
            userData.put("avatarUrl", "https://ui-avatars.com/api/?name=" + fullName.replace(" ", "+"));
        } else {
            userData.put("avatarUrl", avatarUrl);
        }

        // ... (Code KYC giữ nguyên như cũ) ...
        Map<String, Object> kycDataMap = new HashMap<>();
        // ... (Giản lược đoạn KYC cho gọn, bạn giữ nguyên logic cũ nhé) ...
        userData.put("kycData", kycDataMap);

        db.collection("users").document(uid)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirebaseSeeder", "🔥 User Saved: " + email);

                    // [MỚI] NẾU LÀ CUSTOMER THÌ TẠO TÀI KHOẢN NGÂN HÀNG MẪU
                    if (role.equals("CUSTOMER")) {
                        seedAccountsForCustomer(uid, email);
                    }
                });
    }

    // === HÀM TẠO TÀI KHOẢN NGÂN HÀNG ===
    private void seedAccountsForCustomer(String uid, String email) {
        // 1. Tạo tài khoản CHECKING (Thanh toán) - Ai cũng có
        // ID document = uid + "_CHECKING" để tránh trùng lặp khi chạy lại
        createAccount(uid, uid + "_CHECKING", "101" + uid.substring(0, 5).toUpperCase(),
                "CHECKING", 50000000.0, 0, 0);

        // 2. Tạo tài khoản SAVING (Tiết kiệm) - Chỉ cho Kafka (Ví dụ)
        if (email.startsWith("kafka")) {
            createAccount(uid, uid + "_SAVING", "202" + uid.substring(0, 5).toUpperCase(),
                    "SAVING", 200000000.0, 5.5, 0); // Lãi suất 5.5%
        }

        // 3. Tạo tài khoản MORTGAGE (Vay) - Chỉ cho Firefly (Ví dụ mua thuốc/nhà)
        if (email.startsWith("firefly")) {
            createAccount(uid, uid + "_MORTGAGE", "303" + uid.substring(0, 5).toUpperCase(),
                    "MORTGAGE", -1000000000.0, 0, 15000000.0); // Nợ 1 tỷ, trả mỗi tháng 15tr
        }
    }

    private void createAccount(String ownerId, String docId, String accNum, String type, double balance, double rate, double monthlyPay) {
        Map<String, Object> accData = new HashMap<>();
        accData.put("ownerId", ownerId);
        accData.put("accountNumber", accNum);
        accData.put("accountType", type); // CHECKING, SAVING, MORTGAGE
        accData.put("balance", balance);
        accData.put("createdAt", FieldValue.serverTimestamp());

        // Các trường riêng biệt theo yêu cầu đề bài
        if (type.equals("SAVING")) {
            accData.put("profitRate", rate); // % Lãi suất
        }
        if (type.equals("MORTGAGE")) {
            accData.put("monthlyPayment", monthlyPay); // Số tiền phải trả hàng tháng
        }

        db.collection("accounts").document(docId)
                .set(accData, SetOptions.merge())
                .addOnSuccessListener(v -> Log.d("FirebaseSeeder", "💰 Account Created: " + type + " for " + ownerId));
    }
}