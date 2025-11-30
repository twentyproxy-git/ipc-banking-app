package com.example.ipcbanking.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ipcbanking.utils.CloudinaryHelper;
import com.example.ipcbanking.utils.FirebaseHelper;
import com.example.ipcbanking.utils.FirebaseSeeder;
import com.example.ipcbanking.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private final String emailPattern = "^[A-Za-z0-9._%+-]+@(ipc\\.com|gmail\\.com)$";
    private TextInputEditText emailEditText, passwordEditText;
    private MaterialButton loginButton;
    private FirebaseHelper firebaseHelper;
    private FirebaseSeeder firebaseSeeder;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Init Cloudinary
        CloudinaryHelper.initCloudinary(this);

        // Init Helpers
        firebaseHelper = new FirebaseHelper(this);
        db = FirebaseFirestore.getInstance();
        firebaseSeeder = new FirebaseSeeder(this);

        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);

        boolean isSeeded = prefs.getBoolean("is_data_seeded_v2", false);

        if (!isSeeded) {
            Log.d("FirebaseSeeder", "🌱 First run (v2) detected. Seeding data...");
            Toast.makeText(this, "Initializing sample data...", Toast.LENGTH_SHORT).show();

            firebaseSeeder.seedUsers();

            // Lưu lại trạng thái đã seed
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("is_data_seeded_v2", true);
            editor.apply();
        } else {
            Log.d("FirebaseSeeder", "✅ Data already seeded. Skipping to protect data.");
        }
        // ------------------------------------------------------

        initViews();

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (!validateInputs(email, password)) return;

            firebaseHelper.loginUser(email, password, new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    checkUserRoleAndRedirect(user.getUid());
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void checkUserRoleAndRedirect(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");

                        if (role != null) {
                            Intent intent;
                            if (role.equals("OFFICER")) {
                                Toast.makeText(LoginActivity.this, "Welcome Bank Officer!", Toast.LENGTH_SHORT).show();
                                intent = new Intent(LoginActivity.this, BankOfficerActivity.class);
                                intent.putExtra("OFFICER_ID", uid);
                            } else {
                                Toast.makeText(LoginActivity.this, "Welcome Customer!", Toast.LENGTH_SHORT).show();
                                intent = new Intent(LoginActivity.this, CustomerHomeActivity.class);
                                intent.putExtra("CUSTOMER_ID", uid);
                            }
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error: User role not found!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Error: User data not found in DB!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateInputs(String email, String password) {
        boolean isValid = true;
        if (!email.matches(emailPattern)) {
            emailEditText.setError("Email must end with @ipc.com or @gmail.com");
            isValid = false;
        } else {
            emailEditText.setError(null);
        }

        if (password.isEmpty() || password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            isValid = false;
        } else {
            passwordEditText.setError(null);
        }
        return isValid;
    }

    private void initViews() {
        emailEditText = findViewById(R.id.input_email);
        passwordEditText = findViewById(R.id.input_password);
        loginButton = findViewById(R.id.btn_login);
    }
}