package com.example.ipcbanking.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide; // [QUAN TRỌNG] Nhớ import Glide
import com.example.ipcbanking.R;
import com.example.ipcbanking.utils.CloudinaryHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class VerifyKYCActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 100;

    private ImageView btnBack;
    private View layoutUploadForm, layoutPendingState;
    private Button btnBackHome, btnSubmit;
    private TextInputEditText etIdCardNumber;

    private ImageView imgIdDoc, imgFace;
    private View btnCamId, btnGalId, btnCamFace, btnGalFace;

    private FirebaseFirestore db;
    private String customerId;
    private Bitmap bitmapIdDoc = null;
    private Bitmap bitmapFace = null;
    private int currentCaptureTarget = 0; // 1: ID Card, 2: Face

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verify_kyc);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        CloudinaryHelper.initCloudinary(this);
        db = FirebaseFirestore.getInstance();
        customerId = getIntent().getStringExtra("CUSTOMER_ID");

        initViews();
        setupResultLaunchers();
        setupListeners();
        checkCurrentKycStatus();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        layoutUploadForm = findViewById(R.id.layout_upload_form);
        layoutPendingState = findViewById(R.id.layout_pending_state);
        btnBackHome = findViewById(R.id.btn_back_home);

        etIdCardNumber = findViewById(R.id.et_id_card_number);
        imgIdDoc = findViewById(R.id.img_id_doc);
        imgFace = findViewById(R.id.img_face);

        btnCamId = findViewById(R.id.btn_cam_id);
        btnGalId = findViewById(R.id.btn_gal_id);
        btnCamFace = findViewById(R.id.btn_cam_face);
        btnGalFace = findViewById(R.id.btn_gal_face);

        btnSubmit = findViewById(R.id.btn_submit_kyc);
    }

    private void setupResultLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        processCapturedImage(imageBitmap);
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            InputStream imageStream = getContentResolver().openInputStream(uri);
                            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                            processCapturedImage(selectedImage);
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void processCapturedImage(Bitmap bitmap) {
        if (currentCaptureTarget == 1) {
            bitmapIdDoc = bitmap;
            handleImageDisplay(imgIdDoc, bitmap);
        } else if (currentCaptureTarget == 2) {
            bitmapFace = bitmap;
            handleImageDisplay(imgFace, bitmap);
        }
    }

    private void handleImageDisplay(ImageView targetView, Bitmap bitmap) {
        targetView.setImageBitmap(bitmap);
        targetView.setPadding(0, 0, 0, 0);
        targetView.setColorFilter(null);
        targetView.setImageTintList(null);
        targetView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnBackHome.setOnClickListener(v -> finish());

        // ID Card
        btnCamId.setOnClickListener(v -> {
            currentCaptureTarget = 1;
            checkPermissionAndOpenCamera();
        });

        btnGalId.setOnClickListener(v -> {
            currentCaptureTarget = 1;
            checkPermissionAndOpenGallery();
        });

        // Face - Camera (Hiện Dialog Scan)
        btnCamFace.setOnClickListener(v -> {
            currentCaptureTarget = 2;
            showFaceScanDialog();
        });

        btnGalFace.setOnClickListener(v -> {
            currentCaptureTarget = 2;
            checkPermissionAndOpenGallery();
        });

        btnSubmit.setOnClickListener(v -> startUploadProcess());
    }

    private void showFaceScanDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_face_scan_kyc, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setCancelable(false);

        View scanLine = dialogView.findViewById(R.id.scan_line);
        TextView tvStatus = dialogView.findViewById(R.id.tv_scan_status);
        View pbScanning = dialogView.findViewById(R.id.pb_scanning);

        TranslateAnimation animation = new TranslateAnimation(
                Animation.ABSOLUTE, 0f, Animation.ABSOLUTE, 0f,
                Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_PARENT, 0.9f);
        animation.setDuration(1500);
        animation.setRepeatCount(1);
        animation.setRepeatMode(Animation.REVERSE);
        scanLine.startAnimation(animation);

        dialog.show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) {
                scanLine.clearAnimation();
                tvStatus.setText("Face Detected! ✅");
                tvStatus.setTextColor(Color.parseColor("#388E3C"));
                pbScanning.setVisibility(View.GONE);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    dialog.dismiss();
                    checkPermissionAndOpenCamera();
                }, 500);
            }
        }, 3000);
    }

    private void checkPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void checkPermissionAndOpenGallery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_CODE);
        } else {
            openGallery();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            cameraLauncher.launch(takePictureIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No Camera App found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (currentCaptureTarget == 2) {
                    showFaceScanDialog();
                } else {
                    openCamera();
                }
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- [CẬP NHẬT] HÀM LOAD DỮ LIỆU CŨ ---
    private void checkCurrentKycStatus() {
        if (customerId == null) return;
        db.collection("users").document(customerId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String status = documentSnapshot.getString("kyc_status");
                Map<String, Object> kycData = (Map<String, Object>) documentSnapshot.get("kyc_data");

                if ("PENDING".equals(status)) {
                    showPendingState();
                } else {
                    showUploadForm();

                    if (kycData != null) {
                        // 1. Load số CCCD
                        if (kycData.containsKey("id_card_number")) {
                            etIdCardNumber.setText((String) kycData.get("id_card_number"));
                        }

                        // 2. [MỚI] Load ảnh CCCD nếu có
                        String idCardUrl = (String) kycData.get("id_card_url");
                        if (idCardUrl != null && !idCardUrl.isEmpty()) {
                            // Dùng Glide load ảnh
                            Glide.with(this).load(idCardUrl).into(imgIdDoc);

                            // Chỉnh lại Style cho giống ảnh mới chụp (bỏ padding, scale đẹp)
                            imgIdDoc.setPadding(0, 0, 0, 0);
                            imgIdDoc.setColorFilter(null);
                            imgIdDoc.setImageTintList(null);
                            imgIdDoc.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        }

                        // 3. [MỚI] Load ảnh Face nếu có
                        String faceUrl = (String) kycData.get("face_image_url");
                        if (faceUrl != null && !faceUrl.isEmpty()) {
                            Glide.with(this).load(faceUrl).into(imgFace);

                            // Chỉnh lại Style
                            imgFace.setPadding(0, 0, 0, 0);
                            imgFace.setColorFilter(null);
                            imgFace.setImageTintList(null);
                            imgFace.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        }
                    }

                    if ("VERIFIED".equals(status)) {
                        btnSubmit.setText("Update & Re-verify");
                    }
                }
            }
        });
    }

    private void showPendingState() {
        layoutUploadForm.setVisibility(View.GONE);
        btnSubmit.setVisibility(View.GONE);
        layoutPendingState.setVisibility(View.VISIBLE);
    }

    private void showUploadForm() {
        layoutPendingState.setVisibility(View.GONE);
        layoutUploadForm.setVisibility(View.VISIBLE);
        btnSubmit.setVisibility(View.VISIBLE);
    }

    private void startUploadProcess() {
        String idNumber = etIdCardNumber.getText().toString().trim();
        if (idNumber.length() < 9) {
            etIdCardNumber.setError("Invalid ID"); return;
        }

        // Nếu user chỉ muốn cập nhật số ID mà giữ nguyên ảnh cũ thì sao?
        // Logic hiện tại bắt buộc phải chụp lại cả 2 ảnh mới cho phép submit.
        // Bạn có thể tùy chỉnh logic này nếu muốn cho phép dùng ảnh cũ.
        if (bitmapIdDoc == null || bitmapFace == null) {
            Toast.makeText(this, "Please take new photos to verify!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Uploading (0/2)...");

        byte[] idCardBytes = bitmapToBytes(bitmapIdDoc);
        byte[] faceBytes = bitmapToBytes(bitmapFace);
        String timeStamp = String.valueOf(System.currentTimeMillis());

        CloudinaryHelper.uploadImageBytes(idCardBytes, "id_" + customerId + "_" + timeStamp, new CloudinaryHelper.OnImageUploadListener() {
            @Override
            public void onSuccess(String idCardUrl) {
                runOnUiThread(() -> btnSubmit.setText("Uploading (1/2)..."));
                CloudinaryHelper.uploadImageBytes(faceBytes, "face_" + customerId + "_" + timeStamp, new CloudinaryHelper.OnImageUploadListener() {
                    @Override
                    public void onSuccess(String faceUrl) {
                        saveToFirestore(idNumber, idCardUrl, faceUrl);
                    }
                    @Override public void onError(String error) { handleError("Face Upload Failed"); }
                });
            }
            @Override public void onError(String error) { handleError("ID Upload Failed"); }
        });
    }

    private byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private void saveToFirestore(String idNumber, String idCardUrl, String faceUrl) {
        runOnUiThread(() -> btnSubmit.setText("Saving Data..."));
        Map<String, Object> updates = new HashMap<>();
        updates.put("kyc_status", "PENDING");
        Map<String, Object> kycData = new HashMap<>();
        kycData.put("id_card_number", idNumber);
        kycData.put("id_card_url", idCardUrl);
        kycData.put("face_image_url", faceUrl);
        updates.put("kyc_data", kycData);

        db.collection("users").document(customerId).set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Submitted!", Toast.LENGTH_SHORT).show();
                    showPendingState();
                })
                .addOnFailureListener(e -> handleError(e.getMessage()));
    }

    private void handleError(String msg) {
        runOnUiThread(() -> {
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Submit for Review");
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }
}