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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WithdrawActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvAccountNumber, tvAccountBalance;
    private AutoCompleteTextView spinnerAccounts;
    private TextInputEditText etAmount;

    private Button btnDestMomo, btnDestMb, btnDestVcb;

    private Button btnConfirm;
    private FrameLayout loadingOverlay;

    private FirebaseFirestore db;
    private String customerId;
    private List<AccountItem> accountList = new ArrayList<>();
    private AccountItem currentAccount;
    private String selectedDestination = "ATM/Cash"; // Đích mặc định

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_withdraw);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        // Nhận CustomerID
        if (getIntent().hasExtra("CUSTOMER_ID")) {
            customerId = getIntent().getStringExtra("CUSTOMER_ID");
        } else {
            AccountItem item = (AccountItem) getIntent().getSerializableExtra("ACCOUNT_DATA");
            if (item != null) customerId = item.getOwnerId();
        }

        if (customerId == null) {
            Toast.makeText(this, "Error: User not identified!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadUserAccounts();
        setupMoneyFormatter(etAmount);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvAccountNumber = findViewById(R.id.tv_account_number);
        tvAccountBalance = findViewById(R.id.tv_account_balance);

        spinnerAccounts = findViewById(R.id.spinner_accounts);
        etAmount = findViewById(R.id.et_withdraw_amount);

        btnDestMomo = findViewById(R.id.btn_dest_momo);
        btnDestMb = findViewById(R.id.btn_dest_mbbank);
        btnDestVcb = findViewById(R.id.btn_dest_vcb);

        btnConfirm = findViewById(R.id.btn_withdraw_confirm);
        loadingOverlay = findViewById(R.id.loading_overlay);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // [CẬP NHẬT] Xử lý chọn Destination
        btnDestMomo.setOnClickListener(v -> {
            selectedDestination = "MOMO Wallet";
            Toast.makeText(this, "Destination: MOMO Wallet", Toast.LENGTH_SHORT).show();
        });

        btnDestMb.setOnClickListener(v -> {
            selectedDestination = "MB Bank";
            Toast.makeText(this, "Destination: MB Bank", Toast.LENGTH_SHORT).show();
        });

        btnDestVcb.setOnClickListener(v -> {
            selectedDestination = "Vietcombank";
            Toast.makeText(this, "Destination: Vietcombank", Toast.LENGTH_SHORT).show();
        });

        btnConfirm.setOnClickListener(v -> processWithdraw());

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
                    List<String> accountLabels = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        AccountItem account = doc.toObject(AccountItem.class);
                        account.setId(doc.getId());

                        // Lọc bỏ tài khoản MORTGAGE
                        if ("MORTGAGE".equals(account.getAccountType())) {
                            continue;
                        }

                        accountList.add(account);
                        String label = account.getAccountNumber() + " (" + account.getAccountType() + ")";
                        accountLabels.add(label);
                    }

                    if (!accountList.isEmpty()) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, accountLabels);
                        spinnerAccounts.setAdapter(adapter);

                        spinnerAccounts.setText(accountLabels.get(0), false);
                        currentAccount = accountList.get(0);
                        updateAccountInfoUI();
                    } else {
                        Toast.makeText(this, "No withdrawable accounts found!", Toast.LENGTH_SHORT).show();
                        btnConfirm.setEnabled(false);
                    }
                })
                .addOnFailureListener(e -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load accounts", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateAccountInfoUI() {
        if (currentAccount == null) return;
        tvAccountNumber.setText("Account No: " + currentAccount.getAccountNumber());
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        String balanceStr = decimalFormat.format(currentAccount.getBalance()) + " VND";
        tvAccountBalance.setText(balanceStr);
    }

    private void processWithdraw() {
        if (currentAccount == null) {
            Toast.makeText(this, "Please select an account first", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            etAmount.setError("Required");
            return;
        }

        double amount = Double.parseDouble(amountStr);
        if (amount <= 0) {
            etAmount.setError("Amount must be greater than 0");
            return;
        }

        if (amount > currentAccount.getBalance()) {
            etAmount.setError("Insufficient balance");
            return;
        }

        loadingOverlay.setVisibility(View.VISIBLE);
        final DocumentReference accountRef = db.collection("accounts").document(currentAccount.getId());

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(accountRef);
            double currentBalance = snapshot.getDouble("balance");

            if (currentBalance < amount) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "Insufficient funds",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED
                );
            }

            double newBalance = currentBalance - amount;
            transaction.update(accountRef, "balance", newBalance);
            return newBalance;
        }).addOnSuccessListener(newBalance -> {
            currentAccount.setBalance(newBalance);
            updateAccountInfoUI();
            saveTransactionHistory(amount);
        }).addOnFailureListener(e -> {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Withdraw Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveTransactionHistory(double amount) {
        TransactionItem trans = new TransactionItem(
                currentAccount.getAccountNumber(),
                currentAccount.getAccountNumber(),
                "SYSTEM_WITHDRAW",
                selectedDestination,
                amount,
                "Withdraw to " + selectedDestination,
                "WITHDRAWAL",
                "SUCCESS",
                new Date()
        );

        db.collection("transactions").add(trans)
                .addOnSuccessListener(doc -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Withdraw Successful!", Toast.LENGTH_SHORT).show();
                    etAmount.setText("");
                })
                .addOnFailureListener(e -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Withdraw Successful (History Error)", Toast.LENGTH_SHORT).show();
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