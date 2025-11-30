package com.example.ipcbanking.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ipcbanking.R;
import com.example.ipcbanking.models.AccountItem;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AccountDetailActivity extends AppCompatActivity {

    private TextView tvCardBalance, tvCardNumber, tvCardHolderName;
    private TextView tvDetailAccNum, tvDetailType, tvDetailDate;

    // Declare extra detail rows
    private LinearLayout layoutExtraDetail, layoutExtraDetail2, layoutExtraDetail3;
    private TextView tvExtraLabel, tvExtraValue;
    private TextView tvExtraLabel2, tvExtraValue2;
    private TextView tvExtraLabel3, tvExtraValue3;

    private ImageView btnBack;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        initViews();

        btnBack.setOnClickListener(v -> finish());

        AccountItem account = (AccountItem) getIntent().getSerializableExtra("ACCOUNT_DATA");
        if (account != null) {
            displayData(account, account.getProfitRate());
            loadOwnerName(account.getOwnerId());

            loadLiveBankConfig(account);
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);

        tvCardBalance = findViewById(R.id.tv_card_balance);
        tvCardNumber = findViewById(R.id.tv_card_number);
        tvCardHolderName = findViewById(R.id.tv_card_holder_name);

        tvDetailAccNum = findViewById(R.id.tv_detail_acc_num);
        tvDetailType = findViewById(R.id.tv_detail_type);
        tvDetailDate = findViewById(R.id.tv_detail_date);

        // Row 1
        layoutExtraDetail = findViewById(R.id.layout_extra_detail);
        tvExtraLabel = findViewById(R.id.tv_extra_label);
        tvExtraValue = findViewById(R.id.tv_extra_value);

        // Row 2
        layoutExtraDetail2 = findViewById(R.id.layout_extra_detail_2);
        tvExtraLabel2 = findViewById(R.id.tv_extra_label_2);
        tvExtraValue2 = findViewById(R.id.tv_extra_value_2);

        // Row 3
        layoutExtraDetail3 = findViewById(R.id.layout_extra_detail_3);
        tvExtraLabel3 = findViewById(R.id.tv_extra_label_3);
        tvExtraValue3 = findViewById(R.id.tv_extra_value_3);
    }

    // Fetch latest interest rate from Firestore
    private void loadLiveBankConfig(AccountItem account) {
        String type = account.getAccountType();
        if (!"SAVING".equals(type) && !"MORTGAGE".equals(type)) return;

        db.collection("bank_config").document("rates").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double liveRate = null;

                        if ("SAVING".equals(type)) {
                            liveRate = documentSnapshot.getDouble("savings_rate");
                        } else if ("MORTGAGE".equals(type)) {
                            liveRate = documentSnapshot.getDouble("loan_rate");
                        }

                        // If new rate exists, update UI
                        if (liveRate != null) {
                            displayData(account, liveRate);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Ignore error, keep using old rate
                });
    }

    private void displayData(AccountItem item, double currentRate) {
        // Use DecimalFormat for "VND" suffix
        DecimalFormat decimalFormat = new DecimalFormat("#,###");

        tvCardBalance.setText(decimalFormat.format(item.getBalance()) + " VND");

        String rawNum = item.getAccountNumber();
        if (rawNum != null && rawNum.length() > 4) {
            String last4 = rawNum.substring(rawNum.length() - 4);
            tvCardNumber.setText("**** **** **** " + last4);
        } else {
            tvCardNumber.setText(rawNum);
        }

        tvDetailAccNum.setText(item.getAccountNumber());
        tvDetailType.setText(item.getAccountType());

        Date date = item.getCreatedAt();
        if (date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvDetailDate.setText(sdf.format(date));
        } else {
            tvDetailDate.setText("N/A");
        }

        String type = item.getAccountType();

        if ("SAVING".equals(type)) {
            layoutExtraDetail.setVisibility(View.VISIBLE);
            layoutExtraDetail2.setVisibility(View.GONE);
            layoutExtraDetail3.setVisibility(View.GONE);

            tvExtraLabel.setText("Interest Rate");

            // Calculate monthly profit with NEW rate
            double profitPerMonth = (item.getBalance() * (currentRate / 100.0)) / 12.0;
            String profitStr = decimalFormat.format(profitPerMonth) + " VND";

            tvExtraValue.setText(currentRate + "% / year\n(+" + profitStr + "/mo)");

        } else if ("MORTGAGE".equals(type)) {
            layoutExtraDetail.setVisibility(View.VISIBLE);
            layoutExtraDetail2.setVisibility(View.VISIBLE);
            layoutExtraDetail3.setVisibility(View.VISIBLE);

            // Row 1: Loan Rate
            tvExtraLabel.setText("Loan Interest Rate");
            tvExtraValue.setText(currentRate + "% / year");

            // Row 2: Monthly Payment
            tvExtraLabel2.setText("Monthly Payment");
            tvExtraValue2.setText(decimalFormat.format(item.getMonthlyPayment()) + " VND");

            // Row 3: Bi-weekly Payment
            tvExtraLabel3.setText("Bi-weekly Payment");
            double biWeeklyPayment = item.getMonthlyPayment() / 2;
            tvExtraValue3.setText(decimalFormat.format(biWeeklyPayment) + " VND");

        } else {
            // CHECKING
            layoutExtraDetail.setVisibility(View.GONE);
            layoutExtraDetail2.setVisibility(View.GONE);
            layoutExtraDetail3.setVisibility(View.GONE);
        }
    }

    private void loadOwnerName(String ownerId) {
        if (ownerId == null || ownerId.isEmpty()) {
            tvCardHolderName.setText("UNKNOWN");
            return;
        }
        tvCardHolderName.setText("LOADING...");
        db.collection("users").document(ownerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("full_name");
                        if (fullName != null) {
                            tvCardHolderName.setText(fullName.toUpperCase());
                        } else {
                            tvCardHolderName.setText("NO NAME");
                        }
                    }
                })
                .addOnFailureListener(e -> tvCardHolderName.setText("ERROR"));
    }
}