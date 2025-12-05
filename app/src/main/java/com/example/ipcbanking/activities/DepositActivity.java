package com.example.ipcbanking.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ipcbanking.R;
import com.example.ipcbanking.models.AccountItem;
import com.example.ipcbanking.models.TransactionItem;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DepositActivity extends AppCompatActivity {

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
    private String fullName = "Me";  // fallback nếu chưa fetch xong
    private FirebaseUser firebaseUser;


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

        String rawAmount = etAmount.getText().toString().replace(".", "");

        if (rawAmount.isEmpty()) {
            etAmount.setError("Please enter amount");
            return;
        }

        long amount = Long.parseLong(rawAmount);

        if (amount <= 0) {
            etAmount.setError("Amount must be greater than 0");
            return;
        }

        loadingOverlay.setVisibility(View.VISIBLE);

        DocumentReference accRef = db.collection("accounts").document(currentAccount.getId());

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(accRef);

            double currentBalance = snapshot.getDouble("balance");
            double newBalance = currentBalance + amount;

            transaction.update(accRef, "balance", newBalance);

            return newBalance;
        }).addOnSuccessListener(newBalance -> {
            currentAccount.setBalance(newBalance);
            updateAccountInfoUI();
            saveTransactionHistory(amount);
        }).addOnFailureListener(e -> {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Deposit Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveTransactionHistory(double amount) {
        Map<String, Object> transactionData = new HashMap<>();

        // Chuẩn hoá tên ngân hàng
        String bankName;
        if (selectedSource.contains("MOMO")) {
            bankName = "MoMo";
        } else {
            bankName = selectedSource;
        }

        transactionData.put("type", "DEPOSIT");

        // BÊN GỬI: EXTERNAL
        transactionData.put("sender_account", "EXTERNAL");
        transactionData.put("sender_name", fullName + " (" + bankName + ")");

        // BÊN NHẬN: USER
        transactionData.put("receiver_account", currentAccount.getAccountNumber());
        transactionData.put("receiver_name", fullName);

        // CHỈ GIỮ counterparty_bank
        transactionData.put("counterparty_bank", bankName);

        transactionData.put("amount", amount);
        transactionData.put("message", "Deposit via " + bankName);
        transactionData.put("status", "SUCCESS");
        transactionData.put("created_at", FieldValue.serverTimestamp());

        db.collection("transactions")
                .add(transactionData)
                .addOnSuccessListener(doc -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Deposit successful!", Toast.LENGTH_SHORT).show();
                    etAmount.setText("");
                })
                .addOnFailureListener(e -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Deposit saved partially!", Toast.LENGTH_SHORT).show();
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