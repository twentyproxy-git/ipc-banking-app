package com.example.ipcbanking;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditCustomerActivity extends AppCompatActivity {

    // Views
    private TextInputEditText etName, etPhone, etAddress, etEmail, etIdCard, etVerifiedDate;
    private Button btnEdit, btnSave;
    private ImageView imgAvatar;
    private LinearLayout layoutKycInfo;
    private CardView cardNotVerified;

    // Data & Logic
    private String customerId;
    private FirebaseFirestore db;
    private boolean isEditMode = false;
    private boolean isCustomerKyced = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_customer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        customerId = getIntent().getStringExtra("CUSTOMER_ID");

        initViews();

        // 2. Mặc định khóa Editing
        setEditingEnabled(false);

        // 3. Xử lý nút Edit / Cancel
        btnEdit.setOnClickListener(v -> {
            if (!isEditMode) {
                setEditingEnabled(true);
            } else {
                setEditingEnabled(false);
                loadCustomerData(); // Reset lại dữ liệu cũ nếu hủy
            }
        });

        // 4. Xử lý nút Save
        btnSave.setOnClickListener(v -> saveChanges());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomerData();
    }

    private void initViews() {
        etName = findViewById(R.id.et_full_name);
        etPhone = findViewById(R.id.et_phone);
        etAddress = findViewById(R.id.et_address);
        etEmail = findViewById(R.id.et_email);

        etIdCard = findViewById(R.id.et_id_card);
        etVerifiedDate = findViewById(R.id.et_verified_date);

        layoutKycInfo = findViewById(R.id.layout_kyc_info);
        cardNotVerified = findViewById(R.id.card_not_verified);

        btnEdit = findViewById(R.id.btn_enable_edit);
        btnSave = findViewById(R.id.btn_save_changes);
        imgAvatar = findViewById(R.id.img_avatar);
    }

    private void setEditingEnabled(boolean enabled) {
        this.isEditMode = enabled;

        // Bật tắt các ô cơ bản
        etName.setEnabled(enabled);
        etPhone.setEnabled(enabled);
        etAddress.setEnabled(enabled);

        // Chỉ cho sửa CMND nếu user ĐÃ xác thực (tức là ô này đang hiện)
        if (isCustomerKyced) {
            etIdCard.setEnabled(enabled);
        } else {
            etIdCard.setEnabled(false);
        }

        // Email và Ngày xác thực LUÔN LUÔN KHÓA
        etEmail.setEnabled(false);
        etVerifiedDate.setEnabled(false);

        // UI Nút Save
        btnSave.setEnabled(enabled);
        if (enabled) {
            btnSave.setBackgroundColor(Color.parseColor("#590303")); // Đỏ đậm
            btnSave.setTextColor(Color.WHITE);
            btnEdit.setText("Cancel");
        } else {
            btnSave.setBackgroundColor(Color.parseColor("#BDBDBD")); // Xám
            btnSave.setTextColor(Color.parseColor("#757575"));
            btnEdit.setText("Edit Profile");
        }
    }

    private void loadCustomerData() {
        if (customerId == null) return;

        db.collection("users").document(customerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // --- BASIC INFO ---
                        etName.setText(documentSnapshot.getString("full_name"));
                        etPhone.setText(documentSnapshot.getString("phone_number"));
                        etEmail.setText(documentSnapshot.getString("email"));

                        String addr = documentSnapshot.getString("address");
                        etAddress.setText(addr != null ? addr : "");

                        String avatarUrl = documentSnapshot.getString("avatar_url");
                        if (avatarUrl != null) {
                            Glide.with(this).load(avatarUrl).circleCrop().into(imgAvatar);
                        }

                        // --- KYC LOGIC ---
                        Boolean isKyced = documentSnapshot.getBoolean("is_kyced");
                        this.isCustomerKyced = (isKyced != null && isKyced);

                        if (this.isCustomerKyced) {
                            // Đã xác thực -> Hiện ô nhập
                            layoutKycInfo.setVisibility(View.VISIBLE);
                            cardNotVerified.setVisibility(View.GONE);

                            Map<String, Object> kycData = (Map<String, Object>) documentSnapshot.get("kyc_data");
                            if (kycData != null) {
                                String idCard = (String) kycData.get("id_card_number");
                                etIdCard.setText(idCard);

                                Timestamp timestamp = (Timestamp) kycData.get("verified_at");
                                if (timestamp != null) {
                                    Date date = timestamp.toDate();
                                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                    etVerifiedDate.setText(sdf.format(date));
                                }
                            }
                        } else {
                            // Chưa xác thực -> Ẩn ô nhập, Hiện cảnh báo
                            layoutKycInfo.setVisibility(View.GONE);
                            cardNotVerified.setVisibility(View.VISIBLE);
                        }

                        // Đảm bảo trạng thái UI đúng sau khi load data
                        setEditingEnabled(isEditMode);
                    }
                });
    }

    private void saveChanges() {
        String newName = etName.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();
        String newAddress = etAddress.getText().toString().trim();

        if (newName.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(this, "Name and Phone are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("full_name", newName);
        updates.put("phone_number", newPhone);
        updates.put("address", newAddress);

        // Chỉ update CMND nếu đang hiển thị (đã kyc)
        if (isCustomerKyced) {
            String newIdCard = etIdCard.getText().toString().trim();
            if (!newIdCard.isEmpty()) {
                updates.put("kyc_data.id_card_number", newIdCard);
            }
        }

        db.collection("users").document(customerId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    setEditingEnabled(false); // Khóa lại sau khi lưu
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}