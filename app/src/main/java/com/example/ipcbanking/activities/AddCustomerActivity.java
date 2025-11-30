package com.example.ipcbanking.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ipcbanking.R;
import com.example.ipcbanking.utils.CloudinaryHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddCustomerActivity extends AppCompatActivity {

    private ImageView btnBack, imgAvatar;
    private TextInputEditText etFullName, etEmail, etPassword, etPhone;

    // [UPDATE] Address fields
    private TextInputEditText etAddressStreet, etAddressWard, etAddressDistrict, etAddressCity, etAddressState;

    private Button btnSave;

    private Uri selectedImageUri = null;
    private FirebaseFirestore db;
    private FirebaseAuth secondaryAuth;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        imgAvatar.setImageBitmap(bitmap);
                        imgAvatar.setPadding(0, 0, 0, 0);
                        imgAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Cannot load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_customer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        CloudinaryHelper.initCloudinary(this);

        db = FirebaseFirestore.getInstance();
        initSecondaryAuth();
        initViews();
        setupListeners();
    }

    private void initSecondaryAuth() {
        String secondaryAppName = "SecondaryApp";
        FirebaseApp secondaryApp;
        try {
            secondaryApp = FirebaseApp.getInstance(secondaryAppName);
        } catch (IllegalStateException e) {
            FirebaseOptions options = FirebaseApp.getInstance().getOptions();
            secondaryApp = FirebaseApp.initializeApp(this, options, secondaryAppName);
        }
        secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        imgAvatar = findViewById(R.id.img_avatar);
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etPhone = findViewById(R.id.et_phone);

        // [UPDATE] Map address fields
        etAddressStreet = findViewById(R.id.et_address_street);
        etAddressWard = findViewById(R.id.et_address_ward);
        etAddressDistrict = findViewById(R.id.et_address_district);
        etAddressCity = findViewById(R.id.et_address_city);
        etAddressState = findViewById(R.id.et_address_state);

        btnSave = findViewById(R.id.btn_save_customer);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        imgAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnSave.setOnClickListener(v -> validateAndSubmit());
    }

    private void validateAndSubmit() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // Get address data
        String street = etAddressStreet.getText().toString().trim();
        String ward = etAddressWard.getText().toString().trim();
        String district = etAddressDistrict.getText().toString().trim();
        String city = etAddressCity.getText().toString().trim();
        String state = etAddressState.getText().toString().trim();

        if (fullName.isEmpty()) { etFullName.setError("Required"); return; }
        if (email.isEmpty()) { etEmail.setError("Required"); return; }
        if (password.isEmpty()) { etPassword.setError("Required"); return; }
        if (password.length() < 6) { etPassword.setError("Min 6 characters"); return; }
        if (phone.isEmpty()) { etPhone.setError("Required"); return; }

        // Validate Address Fields
        if (street.isEmpty()) { etAddressStreet.setError("Required"); return; }
        if (ward.isEmpty()) { etAddressWard.setError("Required"); return; }
        if (district.isEmpty()) { etAddressDistrict.setError("Required"); return; }
        if (city.isEmpty()) { etAddressCity.setError("Required"); return; }
        if (state.isEmpty()) { etAddressState.setError("Required"); return; }

        // Combine address
        String fullAddress = street + ", " + ward + ", " + district + ", " + city + ", " + state;

        btnSave.setEnabled(false);
        btnSave.setText("Processing...");

        if (selectedImageUri != null) {
            uploadImageAndCreateUser(fullName, email, password, phone, fullAddress);
        } else {
            // Use default Cloudinary avatar if no image selected
            String defaultAvatar = "https://res.cloudinary.com/ipc-media/image/upload/v1764413617/lai2fjastozoacpeuols.png";
            createAuthUser(fullName, email, password, phone, fullAddress, defaultAvatar);
        }
    }

    private void uploadImageAndCreateUser(String name, String email, String password, String phone, String address) {
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

        String publicId = "cus_" + UUID.randomUUID().toString().substring(0, 8);

        CloudinaryHelper.uploadImageUri(selectedImageUri, publicId, new CloudinaryHelper.OnImageUploadListener() {
            @Override
            public void onSuccess(String imageUrl) {
                runOnUiThread(() -> createAuthUser(name, email, password, phone, address, imageUrl));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(AddCustomerActivity.this, "Image upload failed: " + error, Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("SAVE CUSTOMER");
                });
            }
        });
    }

    private void createAuthUser(String name, String email, String password, String phone, String address, String avatarUrl) {
        secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser newUser = authResult.getUser();
                    if (newUser != null) {
                        saveUserToFirestore(newUser.getUid(), name, email, phone, address, avatarUrl);
                        secondaryAuth.signOut();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Account creation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("SAVE CUSTOMER");
                });
    }

    private void saveUserToFirestore(String uid, String name, String email, String phone, String address, String avatarUrl) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("full_name", name);
        userMap.put("email", email);
        userMap.put("phone_number", phone);
        userMap.put("address", address);
        userMap.put("avatar_url", avatarUrl);
        userMap.put("role", "CUSTOMER");
        userMap.put("is_kyced", false);
        userMap.put("kyc_status", "UNVERIFIED");
        userMap.put("created_at", FieldValue.serverTimestamp());

        db.collection("users").document(uid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> createDefaultAccount(uid))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Data save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("SAVE CUSTOMER");
                });
    }

    private void createDefaultAccount(String uid) {
        String accNum = "101" + uid.substring(0, 5).toUpperCase();
        Map<String, Object> accMap = new HashMap<>();
        accMap.put("account_number", accNum);
        accMap.put("account_type", "CHECKING");
        accMap.put("balance", 0.0);
        accMap.put("owner_id", uid);
        accMap.put("created_at", FieldValue.serverTimestamp());

        db.collection("accounts").document(uid + "_CHECKING")
                .set(accMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Customer added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Bank account creation failed", Toast.LENGTH_SHORT).show();
                });
    }
}