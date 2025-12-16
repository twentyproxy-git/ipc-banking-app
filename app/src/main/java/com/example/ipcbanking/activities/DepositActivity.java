package com.example.ipcbanking.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.WriteBatch;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

public class DepositActivity extends AppCompatActivity {

    private static final long HIGH_VALUE_TRANSACTION_THRESHOLD = 10000000;

    private ImageView btnBack;
    private TextView tvAccountNumber, tvAccountBalance;
    private AutoCompleteTextView spinnerAccounts;
    private TextInputEditText etAmount;

    private Button btnMomo, btnMbBank, btnVcb;
    private Button btnConfirm;
    private FrameLayout loadingOverlay;

    private FirebaseFirestore db;
    private String customerId;
    private List<AccountItem> accountList = new ArrayList<>();
    private AccountItem currentAccount;
    private String selectedSource = "System";
    private String fullName = "Me";
    private FirebaseUser firebaseUser;

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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_deposit);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        notificationHelper = new NotificationHelper(this);

        if (firebaseUser != null) {
            db.collection("users")
                    .document(firebaseUser.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            fullName = doc.getString("full_name");
                        }
                    });
        }
        customerId = getIntent().getStringExtra("CUSTOMER_ID");

        if (customerId == null) {
            Toast.makeText(this, "Error: User not identified!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        setupMoneyFormatter(etAmount);
        loadUserAccounts();
        setupBiometricPrompt();
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
        btnBack = findViewById(R.id.btn_back);
        tvAccountNumber = findViewById(R.id.tv_account_number);
        tvAccountBalance = findViewById(R.id.tv_account_balance);

        spinnerAccounts = findViewById(R.id.spinner_accounts);
        etAmount = findViewById(R.id.et_deposit_amount);

        btnMomo = findViewById(R.id.btn_source_momo);
        btnMbBank = findViewById(R.id.btn_source_mbbank);
        btnVcb = findViewById(R.id.btn_source_vcb);

        btnConfirm = findViewById(R.id.btn_deposit_confirm);
        loadingOverlay = findViewById(R.id.loading_overlay);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnMomo.setOnClickListener(v -> {
            selectedSource = "MOMO Wallet";
            Toast.makeText(this, "Selected Source: MOMO", Toast.LENGTH_SHORT).show();
        });
        btnMbBank.setOnClickListener(v -> {
            selectedSource = "MB Bank";
            Toast.makeText(this, "Selected Source: MB Bank", Toast.LENGTH_SHORT).show();
        });
        btnVcb.setOnClickListener(v -> {
            selectedSource = "Vietcombank";
            Toast.makeText(this, "Selected Source: Vietcombank", Toast.LENGTH_SHORT).show();
        });

        btnConfirm.setOnClickListener(v -> processDeposit());

        spinnerAccounts.setOnItemClickListener((parent, view, position, id) -> {
            currentAccount = accountList.get(position);
            updateAccountInfoUI();
        });
    }

    private void loadUserAccounts() {
        loadingOverlay.setVisibility(View.VISIBLE);

        db.collection("accounts")
                .whereEqualTo("owner_id", customerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    loadingOverlay.setVisibility(View.GONE);
                    accountList.clear();
                    List<String> labels = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {

                        AccountItem acc = doc.toObject(AccountItem.class);
                        acc.setId(doc.getId());

                        if (!acc.getAccountType().equalsIgnoreCase("CHECKING")
                                && !acc.getAccountType().equalsIgnoreCase("SAVING"))
                            continue;

                        accountList.add(acc);
                        labels.add(acc.getAccountNumber() + " (" + acc.getAccountType() + ")");
                    }

                    if (accountList.isEmpty()) {
                        Toast.makeText(this, "No valid accounts found!", Toast.LENGTH_SHORT).show();
                        btnConfirm.setEnabled(false);
                        return;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            labels
                    );

                    spinnerAccounts.setAdapter(adapter);
                    spinnerAccounts.setText(labels.get(0), false);

                    currentAccount = accountList.get(0);
                    updateAccountInfoUI();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading account list", Toast.LENGTH_SHORT).show()
                );
    }

    private void updateAccountInfoUI() {
        if (currentAccount == null) return;

        tvAccountNumber.setText("Account No: " + currentAccount.getAccountNumber());

        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        String balanceStr = decimalFormat.format(currentAccount.getBalance()) + " VND";
        tvAccountBalance.setText(balanceStr);
    }

    private void processDeposit() {
        if (currentAccount == null) {
            Toast.makeText(this, "Please select an account", Toast.LENGTH_SHORT).show();
            return;
        }

        String rawAmount = Objects.requireNonNull(etAmount.getText()).toString().replace(".", "");

        if (rawAmount.isEmpty()) {
            etAmount.setError("Please enter amount");
            return;
        }

        long amount = Long.parseLong(rawAmount);

        if (amount <= 0) {
            etAmount.setError("Amount must be greater than 0");
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
                long amount = Long.parseLong(Objects.requireNonNull(etAmount.getText()).toString().replace(".", ""));
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

    private void showOtpDialog(long amount) {
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
            if (otp.equals("123456")) { // Hardcoded OTP for demonstration
                dialog.dismiss();
                simulatePaymentGateway(amount);
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancelOtp.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void simulatePaymentGateway(long amount) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_payment_simulation, null);
        builder.setView(view);
        builder.setCancelable(false);
        final AlertDialog simulationDialog = builder.create();
        simulationDialog.show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            simulationDialog.dismiss();
            executeDepositTransaction(amount);
        }, 3000); // Simulate a 3-second delay
    }

    private void executeDepositTransaction(long amount) {
        loadingOverlay.setVisibility(View.VISIBLE);

        WriteBatch batch = db.batch();

        DocumentReference accRef = db.collection("accounts").document(currentAccount.getId());
        batch.update(accRef, "balance", FieldValue.increment(amount));

        DocumentReference transactionRef = db.collection("transactions").document();
        Map<String, Object> transactionData = new HashMap<>();
        String bankName = selectedSource.contains("MOMO") ? "MoMo" : selectedSource;

        transactionData.put("type", "DEPOSIT");
        transactionData.put("sender_account", "EXTERNAL");
        transactionData.put("sender_name", fullName + " (" + bankName + ")");
        transactionData.put("receiver_account", currentAccount.getAccountNumber());
        transactionData.put("receiver_name", fullName);
        transactionData.put("counterparty_bank", bankName);
        transactionData.put("amount", (double) amount);
        transactionData.put("message", "Deposit via " + bankName);
        transactionData.put("status", "SUCCESS");
        transactionData.put("created_at", FieldValue.serverTimestamp());

        batch.set(transactionRef, transactionData);

        batch.commit().addOnSuccessListener(aVoid -> {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Deposit successful!", Toast.LENGTH_SHORT).show();
            
            currentAccount.setBalance(currentAccount.getBalance() + amount);
            updateAccountInfoUI();
            etAmount.setText("");

        }).addOnFailureListener(e -> {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Deposit Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void setupMoneyFormatter(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    editText.removeTextChangedListener(this);

                    String clean = s.toString().replace(".", "");

                    if (clean.isEmpty()) {
                        editText.addTextChangedListener(this);
                        return;
                    }

                    try {
                        long val = Long.parseLong(clean);

                        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
                        symbols.setGroupingSeparator('.');
                        DecimalFormat formatter = new DecimalFormat("#,###", symbols);

                        String formatted = formatter.format(val);

                        current = formatted;
                        editText.setText(formatted);
                        editText.setSelection(formatted.length());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    editText.addTextChangedListener(this);
                }
            }
        });
    }
}
