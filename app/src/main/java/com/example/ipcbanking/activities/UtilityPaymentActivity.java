package com.example.ipcbanking.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

public class UtilityPaymentActivity extends AppCompatActivity {

    public static final String EXTRA_UTILITY_TYPE = "utility_type";
    private static final long HIGH_VALUE_TRANSACTION_THRESHOLD = 10000000;

    private MaterialToolbar toolbar;
    private AutoCompleteTextView spinnerSourceAccounts, spinnerProvider;
    private TextInputEditText etServiceCode, etAmount;
    private TextInputLayout layoutProvider, layoutServiceCode;
    private Button btnConfirmPayment;
    private FrameLayout loadingOverlay;
    private ImageView ivUtilityIcon;

    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private String customerId;
    private List<AccountItem> sourceAccountList = new ArrayList<>();
    private AccountItem selectedSourceAccount;
    private String utilityType;
    private String selectedProvider;
    private DocumentReference billToPayRef;

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
        setContentView(R.layout.activity_utility_payment);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        customerId = Objects.requireNonNull(firebaseUser).getUid();

        utilityType = getIntent().getStringExtra(EXTRA_UTILITY_TYPE);
        notificationHelper = new NotificationHelper(this);

        initViews();
        setupToolbar();
        setupListeners();
        loadSourceAccounts();
        setupBiometricPrompt();
        configureUiForUtilityType();
        requestNotificationPermission();
    }
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU is API 33
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
        etAmount = findViewById(R.id.et_amount);
        layoutProvider = findViewById(R.id.layout_provider);
        layoutServiceCode = findViewById(R.id.layout_service_code);
        btnConfirmPayment = findViewById(R.id.btn_confirm_payment);
        loadingOverlay = findViewById(R.id.loading_overlay);
        ivUtilityIcon = findViewById(R.id.iv_utility_icon);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(utilityType);
    }

    private void setupListeners() {
        spinnerSourceAccounts.setOnItemClickListener((parent, view, position, id) -> {
            selectedSourceAccount = sourceAccountList.get(position);
        });
        spinnerProvider.setOnItemClickListener((parent, view, position, id) -> {
            selectedProvider = (String) parent.getItemAtPosition(position);
            clearBillData();
        });
        btnConfirmPayment.setOnClickListener(v -> processPayment());

        etServiceCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearBillData();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() > 3) { 
                    fetchBillAmount(s.toString());
                }
            }
        });
    }
    
    private void clearBillData(){
        btnConfirmPayment.setEnabled(false);
        etAmount.setText("");
        billToPayRef = null;
        ivUtilityIcon.setVisibility(View.GONE);
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

    private void configureUiForUtilityType() {
        List<String> providers = new ArrayList<>();
        if ("Pay Bill".equals(utilityType)) {
            layoutProvider.setHint("Bill Type");
            layoutServiceCode.setHint("Customer Code");
            providers.addAll(Arrays.asList("Electricity", "Water", "Internet"));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, providers);
        spinnerProvider.setAdapter(adapter);
    }

    private void updateUtilityIcon() {
        ivUtilityIcon.setVisibility(View.VISIBLE);
        switch (selectedProvider) {
            case "Electricity":
                ivUtilityIcon.setImageResource(R.drawable.electricity);
                break;
            case "Water":
                ivUtilityIcon.setImageResource(R.drawable.water);
                break;
            case "Internet":
                ivUtilityIcon.setImageResource(R.drawable.internet);
                break;
            default:
                ivUtilityIcon.setVisibility(View.GONE);
                break;
        }
    }

    private void fetchBillAmount(String customerCode) {
        if (selectedProvider == null || selectedProvider.isEmpty()) {
            return;
        }
        db.collection("bills")
                .whereEqualTo("type", selectedProvider)
                .whereEqualTo("customer_code", customerCode.toUpperCase())
                .whereEqualTo("status", "UNPAID")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot billDoc = queryDocumentSnapshots.getDocuments().get(0);
                        Double amount = billDoc.getDouble("amount");
                        if (amount != null) {
                            billToPayRef = billDoc.getReference();
                            DecimalFormat formatter = new DecimalFormat("#,###");
                            etAmount.setText(formatter.format(amount));
                            btnConfirmPayment.setEnabled(true);
                            updateUtilityIcon(); // Show icon on success
                        }
                    } else {
                        clearBillData();
                        Toast.makeText(this, "No unpaid bill found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void processPayment() {
        String amountStr = Objects.requireNonNull(etAmount.getText()).toString().trim().replace(",", "");
        double amount = Double.parseDouble(amountStr);
        if (amount <= 0) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedSourceAccount.getBalance() < amount) {
             Toast.makeText(this, "Insufficient balance", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount >= HIGH_VALUE_TRANSACTION_THRESHOLD) {
            biometricPrompt.authenticate(promptInfo);
        } else {
            showOtpDialog(amount);
        }
    }

    private void setupBiometricPrompt() {
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                double amount = Double.parseDouble(Objects.requireNonNull(etAmount.getText()).toString().replace(",", ""));
                showOtpDialog(amount);
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Authenticate to proceed with your transaction")
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
        
        if (billToPayRef != null) {
            batch.update(billToPayRef, "status", "PAID");
        }

        DocumentReference transactionRef = db.collection("transactions").document();
        Map<String, Object> txData = new HashMap<>();
        txData.put("type", "UTILITY");
        txData.put("amount", amount);
        txData.put("message", "Payment for " + selectedProvider + ": " + Objects.requireNonNull(etServiceCode.getText()).toString());
        txData.put("status", "SUCCESS");
        txData.put("created_at", FieldValue.serverTimestamp());
        txData.put("sender_account", selectedSourceAccount.getAccountNumber());
        txData.put("receiver_name", selectedProvider);

        batch.set(transactionRef, txData);

        batch.commit().addOnSuccessListener(aVoid -> {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Payment failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
