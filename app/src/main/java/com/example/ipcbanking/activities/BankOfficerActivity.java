package com.example.ipcbanking.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipcbanking.adapters.CustomerAdapter;
import com.example.ipcbanking.R;
import com.example.ipcbanking.models.CustomerItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankOfficerActivity extends AppCompatActivity {

    // Khai báo Views
    private LinearLayout btnSearchCustomer;
    private LinearLayout btnCreateAccount;
    private LinearLayout layoutSearchContainer;
    private ImageView btnCloseSearch;
    private EditText etSearchQuery;
    private CardView btnHomeLogout;
    private TextView tvEkycCount;
    private Button btnViewEkyc;

    // Views cho phần Interest Rate (Savings)
    private TextView tvCurrentRate;
    private Button btnUpdateRates;

    // [MỚI] Views cho phần Mortgage Rate
    private TextView tvCurrentMortgageRate;
    private Button btnUpdateMortgageRates;

    // RecyclerView Components
    private RecyclerView rvCustomers;
    private TextView tvEmptyState;
    private CustomerAdapter adapter;

    // Dữ liệu
    private List<CustomerItem> fullList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bank_officer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupSearchLogic();
        setupHomeLogic();
        setupCreateCustomerLogic();

        setupRateLogic(); // Cập nhật logic sửa tỷ giá

        loadPendingKycCount();
        loadCurrentInterestRate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomersFromFirestore();
        loadPendingKycCount();
        loadCurrentInterestRate();
    }

    private void initViews() {
        btnSearchCustomer = findViewById(R.id.btn_search_customer);
        btnCreateAccount = findViewById(R.id.btn_create_account);
        layoutSearchContainer = findViewById(R.id.layout_search_container);
        btnCloseSearch = findViewById(R.id.btn_close_search);
        etSearchQuery = findViewById(R.id.et_search_query);
        btnHomeLogout = findViewById(R.id.btn_home_logout);

        rvCustomers = findViewById(R.id.rv_recent_customers);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        tvEkycCount = findViewById(R.id.tv_ekyc_count);
        btnViewEkyc = findViewById(R.id.btn_view_ekyc);

        // Savings Rate
        tvCurrentRate = findViewById(R.id.tv_current_rate);
        btnUpdateRates = findViewById(R.id.btn_update_rates);

        // [MỚI] Mortgage Rate
        tvCurrentMortgageRate = findViewById(R.id.tv_current_mortgage_rate);
        btnUpdateMortgageRates = findViewById(R.id.btn_update_mortgage_rates);
    }

    private void setupRecyclerView() {
        rvCustomers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CustomerAdapter(this, fullList, customer -> {
            Intent intent = new Intent(BankOfficerActivity.this, EditCustomerActivity.class);
            intent.putExtra("CUSTOMER_ID", customer.getUid());
            startActivity(intent);
        });
        rvCustomers.setAdapter(adapter);
    }

    private void setupCreateCustomerLogic() {
        if (btnCreateAccount != null) {
            btnCreateAccount.setOnClickListener(v -> {
                Intent intent = new Intent(BankOfficerActivity.this, AddCustomerActivity.class);
                startActivity(intent);
            });
        }
    }

    // 1. Tải lãi suất từ Firestore (Cả Savings và Mortgage)
    private void loadCurrentInterestRate() {
        db.collection("bank_config").document("rates").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Savings
                        Double savingsRate = documentSnapshot.getDouble("savings_rate");
                        if (savingsRate != null) {
                            tvCurrentRate.setText("Current: " + savingsRate + "%/year");
                        } else {
                            tvCurrentRate.setText("Current: Not set");
                        }

                        // [MỚI] Mortgage
                        Double loanRate = documentSnapshot.getDouble("loan_rate");
                        if (loanRate != null) {
                            tvCurrentMortgageRate.setText("Current: " + loanRate + "%/year");
                        } else {
                            tvCurrentMortgageRate.setText("Current: Not set");
                        }
                    } else {
                        tvCurrentRate.setText("Current: 5.5% (Default)");
                        tvCurrentMortgageRate.setText("Current: 7.5% (Default)");
                    }
                })
                .addOnFailureListener(e -> {
                    tvCurrentRate.setText("Error");
                    tvCurrentMortgageRate.setText("Error");
                });
    }

    // 2. Setup sự kiện click nút Edit
    private void setupRateLogic() {
        // Edit Savings
        btnUpdateRates.setOnClickListener(v -> showRateDialog("Savings Interest Rate", "savings_rate"));

        // [MỚI] Edit Mortgage
        btnUpdateMortgageRates.setOnClickListener(v -> showRateDialog("Mortgage Loan Rate", "loan_rate"));
    }

    // Hàm chung để hiện Dialog
    private void showRateDialog(String title, String fieldName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update " + title);
        builder.setMessage("Enter new annual rate (%):");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("e.g. 6.5");
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String rateStr = input.getText().toString().trim();
            if (!rateStr.isEmpty()) {
                double newRate = Double.parseDouble(rateStr);
                updateInterestRateInFirestore(fieldName, newRate);
            } else {
                Toast.makeText(this, "Rate cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // 3. Lưu xuống Firestore
    private void updateInterestRateInFirestore(String fieldName, double newRate) {
        Map<String, Object> data = new HashMap<>();
        data.put(fieldName, newRate);

        db.collection("bank_config").document("rates")
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Rate Updated Successfully!", Toast.LENGTH_SHORT).show();
                    loadCurrentInterestRate(); // Reload lại hiển thị
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupSearchLogic() {
        btnSearchCustomer.setOnClickListener(v -> {
            layoutSearchContainer.setVisibility(View.VISIBLE);
            etSearchQuery.requestFocus();
            showKeyboard();
        });

        btnCloseSearch.setOnClickListener(v -> {
            etSearchQuery.setText("");
            layoutSearchContainer.setVisibility(View.GONE);
            hideKeyboard();
            adapter.updateList(fullList);
            updateListVisibility(fullList);
        });

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());
            }
        });
    }

    private void filter(String text) {
        List<CustomerItem> filteredList = new ArrayList<>();
        for (CustomerItem item : fullList) {
            if (item.getFullName().toLowerCase().contains(text.toLowerCase()) ||
                    item.getPhoneNumber().contains(text)) {
                filteredList.add(item);
            }
        }
        adapter.updateList(filteredList);
        updateListVisibility(filteredList);
    }

    private void setupHomeLogic() {
        btnHomeLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(BankOfficerActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnViewEkyc.setOnClickListener(v -> {
            Intent intent = new Intent(BankOfficerActivity.this, ReviewRequestActivity.class);
            startActivity(intent);
        });
    }

    private void loadCustomersFromFirestore() {
        db.collection("users")
                .whereEqualTo("role", "CUSTOMER")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    fullList.clear();
                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            CustomerItem item = doc.toObject(CustomerItem.class);
                            if (item != null) {
                                item.setUid(doc.getId());
                                fullList.add(item);
                            }
                        }
                    }
                    adapter.updateList(fullList);
                    updateListVisibility(fullList);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error", e);
                });
    }

    private void updateListVisibility(List<CustomerItem> list) {
        if (list == null || list.isEmpty()) {
            rvCustomers.setVisibility(View.GONE);
            if (tvEmptyState != null) tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvCustomers.setVisibility(View.VISIBLE);
            if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etSearchQuery, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void loadPendingKycCount() {
        db.collection("users")
                .whereEqualTo("kyc_status", "PENDING")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    if (count > 0) {
                        tvEkycCount.setText(count + " pending");
                        tvEkycCount.setTextColor(getColor(R.color.design_default_color_error));
                    } else {
                        tvEkycCount.setText("No pending requests");
                        tvEkycCount.setTextColor(getColor(android.R.color.darker_gray));
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Count error", e));
    }
}