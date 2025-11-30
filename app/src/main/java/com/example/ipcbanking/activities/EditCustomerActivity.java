package com.example.ipcbanking.activities;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.ipcbanking.R;
import com.example.ipcbanking.utils.CloudinaryHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EditCustomerActivity extends AppCompatActivity {

    // Views
    private TextInputEditText etName, etPhone, etAddress, etEmail, etIdCard, etVerifiedDate;
    private Button btnEdit, btnSave;

    // Các nút Upload
    private Button btnGallery;
    private LinearLayout layoutAvatarActions;

    private ImageView imgAvatar;
    private ImageView btnBack;

    private ImageView imgIdCard;
    private ImageView imgFaceKyc;

    private LinearLayout layoutKycInfo;
    private CardView cardNotVerified;

    // Data & Logic
    private String customerId;
    private FirebaseFirestore db;
    private boolean isEditMode = false;
    private boolean isCustomerKyced = false;

    private Uri selectedImageUri = null;
    private Bitmap selectedImageBitmap = null;

    // [MỚI] Launcher chọn ảnh từ Gallery
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    selectedImageBitmap = null; // Xóa bitmap nếu có

                    // Hiển thị ảnh
                    imgAvatar.setImageURI(uri);
                    imgAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imgAvatar.setPadding(0, 0, 0, 0);
                }
            }
    );

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

        // Init Cloudinary
        CloudinaryHelper.initCloudinary(this);

        db = FirebaseFirestore.getInstance();
        customerId = getIntent().getStringExtra("CUSTOMER_ID");

        initViews();
        setupListeners();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Mặc định khóa Editing
        setEditingEnabled(false);
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

        btnGallery = findViewById(R.id.btn_gallery);
        layoutAvatarActions = findViewById(R.id.layout_avatar_actions);

        imgAvatar = findViewById(R.id.img_avatar);
        btnBack = findViewById(R.id.btn_back);

        imgIdCard = findViewById(R.id.img_id_card);
        imgFaceKyc = findViewById(R.id.img_face_kyc);
    }

    private void setupListeners() {
        // Xử lý nút Edit / Cancel
        btnEdit.setOnClickListener(v -> {
            if (!isEditMode) {
                setEditingEnabled(true);
            } else {
                setEditingEnabled(false);
                // Reset lại dữ liệu cũ và xóa ảnh tạm
                selectedImageUri = null;
                selectedImageBitmap = null;
                loadCustomerData();
            }
        });

        btnSave.setOnClickListener(v -> saveChanges());

        btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
    }

    private void setEditingEnabled(boolean enabled) {
        this.isEditMode = enabled;

        etName.setEnabled(enabled);
        etPhone.setEnabled(enabled);
        etAddress.setEnabled(enabled);

        // Ẩn/Hiện nút Upload Avatar
        if (layoutAvatarActions != null) {
            layoutAvatarActions.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }

        // Các thông tin định danh KHÔNG được sửa
        etEmail.setEnabled(false);
        etIdCard.setEnabled(false);
        etVerifiedDate.setEnabled(false);

        btnSave.setEnabled(enabled);
        if (enabled) {
            btnSave.setBackgroundColor(Color.parseColor("#590303"));
            btnSave.setTextColor(Color.WHITE);
            btnEdit.setText("Cancel");
        } else {
            btnSave.setBackgroundColor(Color.parseColor("#BDBDBD"));
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
                        // Chỉ load ảnh từ mạng nếu KHÔNG có ảnh tạm đang chọn (tránh load đè khi xoay màn hình)
                        if (selectedImageUri == null && selectedImageBitmap == null) {
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                Glide.with(this).load(avatarUrl).circleCrop().into(imgAvatar);
                            }
                        }

                        // --- KYC LOGIC ---
                        Boolean isKyced = documentSnapshot.getBoolean("is_kyced");
                        this.isCustomerKyced = (isKyced != null && isKyced);

                        if (this.isCustomerKyced) {
                            layoutKycInfo.setVisibility(View.VISIBLE);
                            cardNotVerified.setVisibility(View.GONE);

                            Map<String, Object> kycData = (Map<String, Object>) documentSnapshot.get("kyc_data");
                            if (kycData != null) {
                                String idCard = (String) kycData.get("id_card_number");
                                etIdCard.setText(idCard != null ? idCard : "N/A");

                                Timestamp timestamp = (Timestamp) kycData.get("verified_at");
                                if (timestamp != null) {
                                    Date date = timestamp.toDate();
                                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                    etVerifiedDate.setText(sdf.format(date));
                                }

                                String idCardUrl = (String) kycData.get("id_card_url");
                                if (idCardUrl != null && !idCardUrl.isEmpty()) {
                                    imgIdCard.setPadding(0, 0, 0, 0);
                                    imgIdCard.setImageTintList(null);
                                    Glide.with(this).load(idCardUrl).into(imgIdCard);
                                } else {
                                    imgIdCard.setImageResource(R.drawable.ic_scan_face);
                                    int p = (int) (32 * getResources().getDisplayMetrics().density);
                                    imgIdCard.setPadding(p, p, p, p);
                                }

                                String faceUrl = (String) kycData.get("face_image_url");
                                if (faceUrl != null && !faceUrl.isEmpty()) {
                                    imgFaceKyc.setPadding(0, 0, 0, 0);
                                    imgFaceKyc.setImageTintList(null);
                                    Glide.with(this).load(faceUrl).into(imgFaceKyc);
                                } else {
                                    imgFaceKyc.setImageResource(R.drawable.ic_scan_face);
                                    int p = (int) (24 * getResources().getDisplayMetrics().density);
                                    imgFaceKyc.setPadding(p, p, p, p);
                                }
                            }
                        } else {
                            layoutKycInfo.setVisibility(View.GONE);
                            cardNotVerified.setVisibility(View.VISIBLE);
                        }

                        setEditingEnabled(isEditMode);
                    }
                });
    }

    // [CẬP NHẬT] Logic lưu thay đổi (có xử lý upload ảnh)
    private void saveChanges() {
        String newName = etName.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();
        String newAddress = etAddress.getText().toString().trim();

        if (newName.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(this, "Name and Phone are required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Processing...");

        // Trường hợp 1: Có chọn ảnh từ Gallery
        if (selectedImageUri != null) {
            String publicId = "avatar_" + UUID.randomUUID().toString();
            CloudinaryHelper.uploadImageUri(selectedImageUri, publicId, new CloudinaryHelper.OnImageUploadListener() {
                @Override
                public void onSuccess(String imageUrl) {
                    runOnUiThread(() -> updateFirestore(newName, newPhone, newAddress, imageUrl));
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditCustomerActivity.this, "Image Upload Failed: " + error, Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Changes");
                    });
                }
            });
        }
        // Trường hợp 2: Có chụp ảnh từ Camera
        else if (selectedImageBitmap != null) {
            String publicId = "avatar_" + UUID.randomUUID().toString();
            byte[] imageData = getBytesFromBitmap(selectedImageBitmap);
            CloudinaryHelper.uploadImageBytes(imageData, publicId, new CloudinaryHelper.OnImageUploadListener() {
                @Override
                public void onSuccess(String imageUrl) {
                    runOnUiThread(() -> updateFirestore(newName, newPhone, newAddress, imageUrl));
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditCustomerActivity.this, "Image Upload Failed: " + error, Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Changes");
                    });
                }
            });
        }
        // Trường hợp 3: Không đổi ảnh
        else {
            updateFirestore(newName, newPhone, newAddress, null);
        }
    }

    // Hàm con để update Firestore sau khi (có hoặc không) upload ảnh
    private void updateFirestore(String name, String phone, String address, String newAvatarUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("full_name", name);
        updates.put("phone_number", phone);
        updates.put("address", address);

        if (newAvatarUrl != null) {
            updates.put("avatar_url", newAvatarUrl);
        }

        db.collection("users").document(customerId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    setEditingEnabled(false);

                    // Reset biến ảnh tạm
                    selectedImageUri = null;
                    selectedImageBitmap = null;

                    btnSave.setEnabled(true);
                    btnSave.setText("Save Changes");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Changes");
                });
    }

    // Helper: Chuyển Bitmap sang byte array cho Cloudinary
    private byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        return stream.toByteArray();
    }
}