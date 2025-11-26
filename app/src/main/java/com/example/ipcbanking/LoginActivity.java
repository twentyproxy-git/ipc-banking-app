package com.example.ipcbanking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private final String emailPattern = "^[A-Za-z0-9._%+-]+@(ipc\\.com|gmail\\.com)$";
    private TextInputEditText emailEditText, passwordEditText;
    private MaterialButton loginButton;
    private FirebaseHelper firebaseHelper;
    private FirebaseSeeder firebaseSeeder;

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

        firebaseSeeder = new FirebaseSeeder(this);
        firebaseSeeder.seedUsers();

        firebaseHelper = new FirebaseHelper(this);

        // Ánh xạ views
        initViews();

        // Click login
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            boolean isValid = true;

            // Check email
            if (!email.matches(emailPattern)) {
                emailEditText.setError("Email must end with @ipc.com or @gmail.com");
                isValid = false;
            } else {
                emailEditText.setError(null);
            }

            // Check password
            if (password.isEmpty()) {
                passwordEditText.setError("Password cannot be empty");
                isValid = false;
            } else if (password.length() < 6) {
                passwordEditText.setError("Password must be at least 6 characters");
                isValid = false;
            } else {
                passwordEditText.setError(null);
            }

            // Stop the call if input is INVALID format
            if (!isValid) return;

            // Make a call to Firebase Authentication
            firebaseHelper.loginUser(email, password, new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    Toast.makeText(LoginActivity.this, "Login successful: " + user.getEmail(), Toast.LENGTH_SHORT).show();

                    // --- CHUYỂN MÀN HÌNH TẠI ĐÂY ---
                    Intent intent = new Intent(LoginActivity.this, BankOfficerActivity.class);

                    // Xóa Activity hiện tại và các activity trước đó khỏi ngăn xếp (Stack)
                    // Để user ấn Back không quay lại màn Login
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void initViews() {
        emailEditText = findViewById(R.id.input_email);
        passwordEditText = findViewById(R.id.input_password);
        loginButton = findViewById(R.id.btn_login);
    }
}
