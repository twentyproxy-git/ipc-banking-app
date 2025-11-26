package com.example.ipcbanking;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BankOfficerActivity extends AppCompatActivity {

    // Khai báo Views
    private LinearLayout btnSearchCustomer;
    private LinearLayout layoutSearchContainer;
    private ImageView btnCloseSearch;
    private EditText etSearchQuery;
    private CardView btnHomeLogout;

    // RecyclerView Components
    private RecyclerView rvCustomers;
    private TextView tvEmptyState;
    private CustomerAdapter adapter;

    // Dữ liệu
    private List<CustomerItem> fullList = new ArrayList<>();

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

        // 2. Ánh xạ View
        initViews();

        // 3. Setup RecyclerView & Adapter
        setupRecyclerView();

        // 4. Logic Tìm kiếm & Home
        setupSearchLogic();
        setupHomeLogic();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomersFromFirestore();
    }

    private void initViews() {
        btnSearchCustomer = findViewById(R.id.btn_search_customer);
        layoutSearchContainer = findViewById(R.id.layout_search_container);
        btnCloseSearch = findViewById(R.id.btn_close_search);
        etSearchQuery = findViewById(R.id.et_search_query);
        btnHomeLogout = findViewById(R.id.btn_home_logout);

        rvCustomers = findViewById(R.id.rv_recent_customers);
        tvEmptyState = findViewById(R.id.tv_empty_state); // Text hiển thị khi list rỗng
    }

    private void setupRecyclerView() {
        rvCustomers.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo Adapter với danh sách rỗng ban đầu
        adapter = new CustomerAdapter(this, fullList, new CustomerAdapter.OnCustomerClickListener() {
            @Override
            public void onCustomerClick(CustomerItem customer) {
                Intent intent = new Intent(BankOfficerActivity.this, EditCustomerActivity.class);
                intent.putExtra("CUSTOMER_ID", customer.getUid());
                startActivity(intent);
            }
        });
        rvCustomers.setAdapter(adapter);
    }

    private void setupSearchLogic() {
        // A. Nút mở tìm kiếm
        btnSearchCustomer.setOnClickListener(v -> {
            layoutSearchContainer.setVisibility(View.VISIBLE);
            etSearchQuery.requestFocus();
            showKeyboard();
        });

        // B. Nút đóng tìm kiếm
        btnCloseSearch.setOnClickListener(v -> {
            etSearchQuery.setText(""); // Xóa chữ
            layoutSearchContainer.setVisibility(View.GONE);
            hideKeyboard();

            // Reset lại danh sách đầy đủ khi đóng tìm kiếm
            adapter.updateList(fullList);
            updateListVisibility(fullList);
        });

        // C. [MỚI] Lắng nghe sự kiện gõ chữ để lọc danh sách (Real-time Search)
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

    // Hàm lọc dữ liệu (Tìm kiếm)
    private void filter(String text) {
        List<CustomerItem> filteredList = new ArrayList<>();

        for (CustomerItem item : fullList) {
            // Kiểm tra tên hoặc số điện thoại có chứa từ khóa không
            if (item.getFullName().toLowerCase().contains(text.toLowerCase()) ||
                    item.getPhoneNumber().contains(text)) {
                filteredList.add(item);
            }
        }

        // Cập nhật Adapter
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
    }

    private void loadCustomersFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Lấy danh sách những user có role là CUSTOMER
        db.collection("users")
                .whereEqualTo("role", "CUSTOMER")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    fullList.clear();
                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            // Convert Document thành Object CustomerItem
                            // Lưu ý: Class CustomerItem cần có constructor rỗng
                            CustomerItem item = doc.toObject(CustomerItem.class);
                            if (item != null) {
                                fullList.add(item);
                            }
                        }
                    }

                    // Cập nhật lên giao diện
                    adapter.updateList(fullList);
                    updateListVisibility(fullList);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error", e);
                });
    }

    // [MỚI] Hàm ẩn hiện List/Empty Text dựa trên dữ liệu
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
}