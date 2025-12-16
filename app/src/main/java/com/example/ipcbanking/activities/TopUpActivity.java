package com.example.ipcbanking.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ipcbanking.R;
import com.example.ipcbanking.models.AccountItem;
import com.example.ipcbanking.utils.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

public class TopUpActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private AutoCompleteTextView spinnerSourceAccounts, spinnerProvider;
    private TextInputEditText etServiceCode;
    private Button btnConfirmPayment;
    private FrameLayout loadingOverlay;
    private GridLayout gridDenominations;

    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private String customerId;
    private List<AccountItem> sourceAccountList = new ArrayList<>();
    private AccountItem selectedSourceAccount;
    private String selectedProvider;
    private long selectedAmount = 0;
    private MaterialButton selectedDenominationButton = null;


    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private NotificationHelper notificationHelper;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // Handle permission grant/denial if needed
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_up);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        customerId = Objects.requireNonNull(firebaseUser).getUid();
        notificationHelper = new NotificationHelper(this);

        initViews();
        setupToolbar();
        setupListeners();
        loadSourceAccounts();
        setupBiometricPrompt();
        setupProviderSpinner();
        setupDenominationListeners();
        requestNotificationPermission();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerSourceAccounts = findViewById(R.id.spinner_source_accounts);
        spinnerProvider = findViewById(R.id.spinner_provider);
        etServiceCode = findViewById(R.id.et_service_code);
        btnConfirmPayment = findViewById(R.id.btn_confirm_payment);
        loadingOverlay = findViewById(R.id.loading_overlay);
        gridDenominations = findViewById(R.id.grid_denominations);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        spinnerSourceAccounts.setOnItemClickListener((parent, view, position, id) -> {
            selectedSourceAccount = sourceAccountList.get(position);
        });
        spinnerProvider.setOnItemClickListener((parent, view, position, id) -> {
            selectedProvider = (String) parent.getItemAtPosition(position);
        });
        btnConfirmPayment.setOnClickListener(v -> processPayment());
    }

    private void setupDenominationListeners() {
        for (int i = 0; i < gridDenominations.getChildCount(); i++) {
            View child = gridDenominations.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                button.setOnClickListener(v -> {
                    if (selectedDenominationButton != null) {
                        selectedDenominationButton.setChecked(false);
                    }
                    button.setChecked(true);
                    selectedDenominationButton = button;
                    selectedAmount = Long.parseLong(button.getTag().toString());
                });
            }
        }
    }

    private void loadSourceAccounts() {
        loadingOverlay.setVisibility(View.VISIBLE);
        db.collection("accounts")
                .whereEqualTo("owner_id", customerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loadingOverlay.setVisibility(View.GONE);
                    sourceAccountList.clear();
                    List<String> accountLabels = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AccountItem account = doc.toObject(AccountItem.class);
                        account.setId(doc.getId());
                        if (!"MORTGAGE".equals(account.getAccountType())) {
                            sourceAccountList.add(account);
                            accountLabels.add(account.getAccountNumber() + " (" + account.getAccountType() + ")");
                        }
                    }
                    if (!sourceAccountList.isEmpty()) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, accountLabels);
                        spinnerSourceAccounts.setAdapter(adapter);
                        spinnerSourceAccounts.setText(accountLabels.get(0), false);
                        selectedSourceAccount = sourceAccountList.get(0);
                    } else {
                        Toast.makeText(this, "No source accounts found!", Toast.LENGTH_SHORT).show();
                        btnConfirmPayment.setEnabled(false);
                    }
                })
                .addOnFailureListener(e -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load source accounts", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupProviderSpinner() {
        List<String> providers = Arrays.asList("Viettel", "MobiFone", "VinaPhone", "Vietnamobile");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, providers);
        spinnerProvider.setAdapter(adapter);
    }

    private void processPayment() {
        if (selectedSourceAccount == null) {
            Toast.makeText(this, "Please select a source account", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedProvider == null || selectedProvider.isEmpty()) {
            Toast.makeText(this, "Please select a mobile network", Toast.LENGTH_SHORT).show();
            return;
        }
        String serviceCode = Objects.requireNonNull(etServiceCode.getText()).toString().trim();
        if (serviceCode.isEmpty()) {
            etServiceCode.setError("Phone number is required");
            return;
        }
        if (selectedAmount <= 0) {
            Toast.makeText(this, "Please select an amount", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedAmount > selectedSourceAccount.getBalance()) {
            Toast.makeText(this, "Insufficient balance", Toast.LENGTH_SHORT).show();
            return;
        }

        showOtpDialog(selectedAmount);
    }

    private void setupBiometricPrompt() {
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                showOtpDialog(selectedAmount);
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Authenticate for your transaction")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Cancel")
                .build();
    }

    private void showOtpDialog(double amount) {
        notificationHelper.sendOtpNotification("123456");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_otp_verification, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();

        EditText etOtp = view.findViewById(R.id.et_otp);
        Button btnConfirmOtp = view.findViewById(R.id.btn_confirm_otp);
        Button btnCancelOtp = view.findViewById(R.id.btn_cancel_otp);

        btnConfirmOtp.setOnClickListener(v -> {
            String otp = etOtp.getText().toString();
            if (otp.equals("123456")) { // Hardcoded OTP
                dialog.dismiss();
                executePayment(amount);
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
            }
        });
        btnCancelOtp.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void executePayment(double amount) {
        loadingOverlay.setVisibility(View.VISIBLE);

        WriteBatch batch = db.batch();

        DocumentReference sourceRef = db.collection("accounts").document(selectedSourceAccount.getId());
        batch.update(sourceRef, "balance", FieldValue.increment(-amount));

        DocumentReference transactionRef = db.collection("transactions").document();
        Map<String, Object> txData = new HashMap<>();
        txData.put("type", "TOP_UP");
        txData.put("amount", amount);
        txData.put("message", "Top-up for " + selectedProvider + ": " + Objects.requireNonNull(etServiceCode.getText()).toString());
        txData.put("status", "SUCCESS");
        txData.put("created_at", FieldValue.serverTimestamp());
        txData.put("sender_account", selectedSourceAccount.getAccountNumber());
        txData.put("receiver_name", selectedProvider);

        batch.set(transactionRef, txData);

        batch.commit().addOnSuccessListener(aVoid -> {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Top-up successful!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Top-up failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
