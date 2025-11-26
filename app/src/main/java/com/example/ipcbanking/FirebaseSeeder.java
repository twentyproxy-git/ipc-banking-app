package com.example.ipcbanking;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

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
        createIfNotExists("topaz@ipc.com", "topaz123", "Topaz Numby", "0901000111", "OFFICER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764142149/urizqa3znxqdujax2eea.png");

        createIfNotExists("aventurine@ipc.com", "aventurine123", "Aventurine Stratos", "0901000222", "OFFICER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764142267/tw8lblesl115pnn4dn03.png");

        createIfNotExists("sunday@ipc.com", "sunday123", "Sunday Halovian", "0901000333", "OFFICER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764143194/mrlrd87gjlwubod9ltku.png");

        // 2. CUSTOMER
        createIfNotExists("kafka@gmail.com", "kafka123", "Kafka", "0909666777", "CUSTOMER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764142585/nwawkaoucf9mq0n1rtcp.png");

        createIfNotExists("silverwolf@gmail.com", "silverwolf123", "Silver Wolf", "0909888999", "CUSTOMER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764142596/bx1wlvf6qdjbwurbz0sb.png");

        createIfNotExists("firefly@gmail.com", "firefly123", "Firefly", "0909111222", "CUSTOMER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764142600/rhufwnt3zyvtr7xtuzyq.png");
    }

    private void createIfNotExists(String email, String password, String fullName, String phoneNumber, String role, String avatarUrl) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        Log.d("FirebaseSeeder", "✅ Auth created: " + email);
                        saveUserToFirestore(user.getUid(), email, fullName, phoneNumber, role, avatarUrl);
                    }
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        Log.d("FirebaseSeeder", "ℹ️ User already exists (Skipping): " + email);
                    } else {
                        Log.e("FirebaseSeeder", "❌ Auth Error: " + email + ": " + e.getMessage());
                    }
                });
    }

    private void saveUserToFirestore(String uid, String email, String fullName, String phoneNumber, String role, String avatarUrl) {
        Map<String, Object> userData = new HashMap<>();

        userData.put("uid", uid);
        userData.put("full_name", fullName);       // fullName -> full_name
        userData.put("email", email);
        userData.put("phone_number", phoneNumber); // phoneNumber -> phone_number
        userData.put("role", role);
        userData.put("created_at", FieldValue.serverTimestamp()); // createdAt -> created_at
        userData.put("device_token", "");          // deviceToken -> device_token

        // 2. Avatar
        if (avatarUrl == null || avatarUrl.isEmpty() || avatarUrl.contains("DÁN_LINK")) {
            userData.put("avatar_url", "https://ui-avatars.com/api/?name=" + fullName.replace(" ", "+"));
        } else {
            userData.put("avatar_url", avatarUrl); // avatarUrl -> avatar_url
        }

        Map<String, Object> kycDataMap = new HashMap<>();

        if (role.equals("OFFICER")) {
            // Nhân viên: Đã xác thực
            userData.put("is_kyced", true);
            userData.put("kyc_status", "VERIFIED");

            // Điền dữ liệu giả lập
            kycDataMap.put("face_image_url", userData.get("avatar_url"));
            kycDataMap.put("id_card_number", "001099998888");
            kycDataMap.put("verified_at", FieldValue.serverTimestamp());

        } else {
            // Khách hàng: Chưa xác thực
            userData.put("is_kyced", false);
            userData.put("kyc_status", "UNVERIFIED");

            // [QUAN TRỌNG] Tạo sẵn 3 trường nhưng để null
            kycDataMap.put("face_image_url", null);
            kycDataMap.put("id_card_number", null);
            kycDataMap.put("verified_at", null);
        }

        // Đưa map con kyc_data vào map cha
        userData.put("kyc_data", kycDataMap);

        // 4. Lưu Firestore
        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d("FirebaseSeeder", "🔥 Firestore saved (snake_case): " + email))
                .addOnFailureListener(e -> Log.e("FirebaseSeeder", "❌ Firestore Error: " + e.getMessage()));
    }
}