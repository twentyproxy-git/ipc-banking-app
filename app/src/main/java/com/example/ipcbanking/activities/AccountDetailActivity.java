package com.example.ipcbanking.activities;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AccountDetailActivity extends AppCompatActivity {

    private TextView tvCardBalance, tvCardNumber, tvCardHolderName;
    private TextView tvDetailAccNum, tvDetailType, tvDetailDate;
    private ImageView imgVisibilityDetail; // [MỚI] Icon mắt

    // Declare extra detail rows
    private LinearLayout layoutExtraDetail, layoutExtraDetail2, layoutExtraDetail3;
    private TextView tvExtraLabel, tvExtraValue;
    private TextView tvExtraLabel2, tvExtraValue2;
    private TextView tvExtraLabel3, tvExtraValue3;

    private ImageView btnBack;
    private FirebaseFirestore db;

    private boolean isDetailVisible = false; // [MỚI] Trạng thái ẩn/hiện
    private AccountItem currentAccount; // Lưu biến toàn cục để dùng lại
    private double currentLiveRate = 0; // Lưu tỷ giá lấy từ server

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
        setupListeners(); // [MỚI]

        currentAccount = (AccountItem) getIntent().getSerializableExtra("ACCOUNT_DATA");
        if (currentAccount != null) {
            // Mặc định hiển thị chế độ ẩn (Masked)
            updateUIState();

            loadOwnerName(currentAccount.getOwnerId());
            loadLiveBankConfig(currentAccount);
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);

        // Card Header
        tvCardBalance = findViewById(R.id.tv_card_balance);
        tvCardNumber = findViewById(R.id.tv_card_number);
        tvCardHolderName = findViewById(R.id.tv_card_holder_name);
        imgVisibilityDetail = findViewById(R.id.img_visibility_detail); // [MỚI]

        // Detail Body
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

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // [MỚI] Sự kiện bấm vào mắt
        imgVisibilityDetail.setOnClickListener(v -> {
            if (isDetailVisible) {
                // Đang hiện -> Ẩn ngay (Không cần pass)
                isDetailVisible = false;
                updateUIState();
            } else {
                // Đang ẩn -> Hiện (Cần pass)
                showPasswordConfirmationDialog();
            }
        });
    }

    // [MỚI] Hàm cập nhật giao diện dựa trên isDetailVisible
    private void updateUIState() {
        if (currentAccount == null) return;

        DecimalFormat decimalFormat = new DecimalFormat("#,###");

        if (isDetailVisible) {
            // HIỆN THÔNG TIN THẬT
            imgVisibilityDetail.setImageResource(R.drawable.ic_eye);

            // 1. Số dư
            tvCardBalance.setText(decimalFormat.format(currentAccount.getBalance()) + " VND");

            // 2. Số tài khoản (Trên thẻ & Dưới List)
            tvCardNumber.setText(currentAccount.getAccountNumber());
            tvDetailAccNum.setText(currentAccount.getAccountNumber());

            // 3. Ngày mở
            if (currentAccount.getCreatedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                tvDetailDate.setText(sdf.format(currentAccount.getCreatedAt()));
            }

            // 4. Thông tin phụ (Lãi suất, Trả góp)
            updateExtraDetails(true, decimalFormat);

        } else {
            // CHE THÔNG TIN (MASKED)
            imgVisibilityDetail.setImageResource(R.drawable.ic_eye_off);

            tvCardBalance.setText("****** VND");

            // Mask số tài khoản
            String rawNum = currentAccount.getAccountNumber();
            String maskedNum = "**** **** **** ****";
            if (rawNum != null && rawNum.length() > 4) {
                maskedNum = "**** **** **** " + rawNum.substring(rawNum.length() - 4);
            }
            tvCardNumber.setText(maskedNum);
            tvDetailAccNum.setText("************"); // Che hoàn toàn ở dưới

            tvDetailDate.setText("**/**/****");

            // Che thông tin phụ
            updateExtraDetails(false, decimalFormat);
        }

        // Loại tài khoản luôn hiện
        tvDetailType.setText(currentAccount.getAccountType());
    }

    private void updateExtraDetails(boolean showRealData, DecimalFormat df) {
        String type = currentAccount.getAccountType();
        double rateToUse = (currentLiveRate > 0) ? currentLiveRate : currentAccount.getProfitRate();

        if ("SAVING".equals(type)) {
            layoutExtraDetail.setVisibility(View.VISIBLE);
            tvExtraLabel.setText("Interest Rate");

            if (showRealData) {
                double profitPerMonth = (currentAccount.getBalance() * (rateToUse / 100.0)) / 12.0;
                tvExtraValue.setText(rateToUse + "% / year\n(+" + df.format(profitPerMonth) + " VND/mo)");
            } else {
                tvExtraValue.setText("*.*% / year\n(*******)");
            }

            layoutExtraDetail2.setVisibility(View.GONE);
            layoutExtraDetail3.setVisibility(View.GONE);

        } else if ("MORTGAGE".equals(type)) {
            layoutExtraDetail.setVisibility(View.VISIBLE);
            layoutExtraDetail2.setVisibility(View.VISIBLE);
            layoutExtraDetail3.setVisibility(View.VISIBLE);

            tvExtraLabel.setText("Loan Interest Rate");
            tvExtraValue.setText(showRealData ? (rateToUse + "% / year") : "*.*%");

            tvExtraLabel2.setText("Monthly Payment");
            tvExtraValue2.setText(showRealData ? (df.format(currentAccount.getMonthlyPayment()) + " VND") : "*** VND");

            tvExtraLabel3.setText("Bi-weekly Payment");
            if (showRealData) {
                double biWeekly = currentAccount.getMonthlyPayment() / 2;
                tvExtraValue3.setText(df.format(biWeekly) + " VND");
            } else {
                tvExtraValue3.setText("*** VND");
            }
        } else {
            layoutExtraDetail.setVisibility(View.GONE);
            layoutExtraDetail2.setVisibility(View.GONE);
            layoutExtraDetail3.setVisibility(View.GONE);
        }
    }

    private void showPasswordConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_password_confirm, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etPassword = dialogView.findViewById(R.id.et_password_confirm);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_password);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_password);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String password = etPassword.getText().toString();
            if (!password.isEmpty()) {
                verifyPasswordWithFirebase(password, dialog);
            } else {
                Toast.makeText(this, "Password required!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void verifyPasswordWithFirebase(String password, AlertDialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

            user.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Verified Successfully", Toast.LENGTH_SHORT).show();
                        isDetailVisible = true;
                        updateUIState(); // Cập nhật lại UI để hiện thông tin
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Incorrect Password!", Toast.LENGTH_SHORT).show();
                    });
        }
    }

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

                        if (liveRate != null) {
                            currentLiveRate = liveRate;
                            updateUIState();
                        }
                    }
                });
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