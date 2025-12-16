package com.example.ipcbanking.activities;

import android.app.AlertDialog;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.ipcbanking.R;
import com.example.ipcbanking.models.AccountItem;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

import de.hdodenhof.circleimageview.CircleImageView;

public class TransferActivity extends AppCompatActivity {

    private static final long HIGH_VALUE_TRANSACTION_THRESHOLD = 10000000;

    private MaterialToolbar toolbar;
    private AutoCompleteTextView spinnerSourceAccounts, spinnerBanks;
    private TextInputEditText etDestinationAccount, etAmount, etMessage;
    private TextView tvBeneficiaryName;
    private Button btnConfirmTransfer;
    private FrameLayout loadingOverlay;
    private LinearLayout layoutBeneficiaryInfo;
    private CircleImageView ivBeneficiaryAvatar;

    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private String customerId;
    private List<AccountItem> sourceAccountList = new ArrayList<>();
    private AccountItem selectedSourceAccount;
    private AccountItem destinationAccount;
    private String beneficiaryName = "";
    private String selectedBank = "IPC Bank";

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        customerId = Objects.requireNonNull(firebaseUser).getUid();

        initViews();
        setupListeners();
        loadSourceAccounts();
        setupBankSpinner();
        setupBiometricPrompt();
        setupDestinationAccountTextWatcher();
        setupMoneyFormatter(etAmount);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerSourceAccounts = findViewById(R.id.spinner_source_accounts);
        spinnerBanks = findViewById(R.id.spinner_banks);
        etDestinationAccount = findViewById(R.id.et_destination_account);
        etAmount = findViewById(R.id.et_transfer_amount);
        etMessage = findViewById(R.id.et_message);
        tvBeneficiaryName = findViewById(R.id.tv_beneficiary_name);
        btnConfirmTransfer = findViewById(R.id.btn_confirm_transfer);
        loadingOverlay = findViewById(R.id.loading_overlay);
        layoutBeneficiaryInfo = findViewById(R.id.layout_beneficiary_info);
        ivBeneficiaryAvatar = findViewById(R.id.iv_beneficiary_avatar);

        btnConfirmTransfer.setEnabled(false); // Disable button by default
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());
        spinnerSourceAccounts.setOnItemClickListener((parent, view, position, id) -> {
            selectedSourceAccount = sourceAccountList.get(position);
        });
        spinnerBanks.setOnItemClickListener((parent, view, position, id) -> {
            selectedBank = (String) parent.getItemAtPosition(position);
            etDestinationAccount.setText("");
            layoutBeneficiaryInfo.setVisibility(View.GONE);
            destinationAccount = null;
            btnConfirmTransfer.setEnabled(false);
        });
        btnConfirmTransfer.setOnClickListener(v -> processTransfer());
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
                        btnConfirmTransfer.setEnabled(false);
                    }
                })
                .addOnFailureListener(e -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load source accounts", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupBankSpinner() {
        List<String> banks = Arrays.asList("IPC Bank", "Vietcombank", "Techcombank", "MB Bank", "ACB");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, banks);
        spinnerBanks.setAdapter(adapter);
        spinnerBanks.setText(banks.get(0), false);
    }

    private void setupDestinationAccountTextWatcher() {
        etDestinationAccount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // As user types, disable button and clear old data
                btnConfirmTransfer.setEnabled(false);
                destinationAccount = null;
                layoutBeneficiaryInfo.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() >= 8) {
                    fetchBeneficiaryName(s.toString());
                } else {
                    btnConfirmTransfer.setEnabled(false);
                }
            }
        });
    }

    private void fetchBeneficiaryName(String accountNumber) {
        if (selectedBank.equals("IPC Bank")) {
            db.collection("accounts")
                    .whereEqualTo("account_number", accountNumber.toUpperCase())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            destinationAccount = queryDocumentSnapshots.getDocuments().get(0).toObject(AccountItem.class);
                            assert destinationAccount != null;
                            destinationAccount.setId(queryDocumentSnapshots.getDocuments().get(0).getId());

                            db.collection("users").document(destinationAccount.getOwnerId()).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            beneficiaryName = documentSnapshot.getString("full_name");
                                            String avatarUrl = documentSnapshot.getString("avatar_url");

                                            tvBeneficiaryName.setText(beneficiaryName);
                                            Glide.with(this).load(avatarUrl).placeholder(R.drawable.ic_unknown).into(ivBeneficiaryAvatar);
                                            layoutBeneficiaryInfo.setVisibility(View.VISIBLE);
                                            btnConfirmTransfer.setEnabled(true);
                                        }
                                    });
                        } else {
                            layoutBeneficiaryInfo.setVisibility(View.VISIBLE);
                            tvBeneficiaryName.setText("Account not found");
                            ivBeneficiaryAvatar.setImageResource(R.drawable.ic_unknown);
                            destinationAccount = null;
                            btnConfirmTransfer.setEnabled(false);
                        }
                    }).addOnFailureListener(e -> {
                        layoutBeneficiaryInfo.setVisibility(View.VISIBLE);
                        tvBeneficiaryName.setText("Error verifying account");
                        ivBeneficiaryAvatar.setImageResource(R.drawable.ic_unknown);
                        destinationAccount = null;
                        btnConfirmTransfer.setEnabled(false);
                    });
        } else {
            beneficiaryName = "External User"; // Placeholder
            tvBeneficiaryName.setText("External Account: " + selectedBank);
            ivBeneficiaryAvatar.setImageResource(R.drawable.ic_unknown);
            layoutBeneficiaryInfo.setVisibility(View.VISIBLE);
            destinationAccount = null;
            btnConfirmTransfer.setEnabled(true);
        }
    }

    private void processTransfer() {
        if (selectedSourceAccount == null) {
            Toast.makeText(this, "Please select a source account", Toast.LENGTH_SHORT).show();
            return;
        }

        // This check is the final safeguard. The button should be disabled in this state anyway.
        if (selectedBank.equals("IPC Bank") && destinationAccount == null) {
            Toast.makeText(this, "Invalid or unverified destination account", Toast.LENGTH_SHORT).show();
            return;
        }

        if (destinationAccount != null && selectedSourceAccount.getId().equals(destinationAccount.getId())) {
            Toast.makeText(this, "Cannot transfer to the same account", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = Objects.requireNonNull(etAmount.getText()).toString().trim().replace(".", "");
        if (amountStr.isEmpty()) {
            etAmount.setError("Required");
            return;
        }
        double amount = Double.parseDouble(amountStr);
        if (amount <= 0) {
            etAmount.setError("Amount must be greater than 0");
            return;
        }
        if (amount > selectedSourceAccount.getBalance()) {
            etAmount.setError("Insufficient balance");
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
                double amount = Double.parseDouble(Objects.requireNonNull(etAmount.getText()).toString().replace(".", ""));
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
                executeTransfer(amount);
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancelOtp.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void executeTransfer(double amount) {
        loadingOverlay.setVisibility(View.VISIBLE);

        WriteBatch batch = db.batch();

        // 1. Decrement sender's balance
        DocumentReference sourceRef = db.collection("accounts").document(selectedSourceAccount.getId());
        batch.update(sourceRef, "balance", FieldValue.increment(-amount));

        // 2. Increment receiver's balance (if internal)
        if (selectedBank.equals("IPC Bank") && destinationAccount != null) {
            DocumentReference destRef = db.collection("accounts").document(destinationAccount.getId());
            batch.update(destRef, "balance", FieldValue.increment(amount));
        }

        // 3. Create transaction record
        DocumentReference transactionRef = db.collection("transactions").document();
        Map<String, Object> txData = new HashMap<>();
        txData.put("type", "TRANSFER");
        txData.put("amount", amount);
        txData.put("message", Objects.requireNonNull(etMessage.getText()).toString());
        txData.put("status", "SUCCESS");
        txData.put("created_at", FieldValue.serverTimestamp());
        txData.put("sender_account", selectedSourceAccount.getAccountNumber());
        txData.put("sender_name", firebaseUser.getDisplayName()); // Assuming the user's name is set in Firebase Auth
        txData.put("receiver_account", Objects.requireNonNull(etDestinationAccount.getText()).toString());
        txData.put("receiver_name", beneficiaryName);
        txData.put("counterparty_bank", selectedBank);

        batch.set(transactionRef, txData);

        batch.commit().addOnSuccessListener(aVoid -> {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Transfer successful!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Transfer failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void setupMoneyFormatter(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

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