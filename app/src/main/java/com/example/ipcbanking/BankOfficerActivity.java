package com.example.ipcbanking;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class BankOfficerActivity extends AppCompatActivity {

    private LinearLayout btnSearchCustomer;
    private LinearLayout layoutSearchContainer;
    private ImageView btnCloseSearch;
    private EditText etSearchQuery;
    private CardView btnHomeLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bank_officer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // 2. Ánh xạ View (Kết nối Java với XML)
        initViews();
        // 3. Xử lý logic Tìm kiếm (Hiện/Ẩn)
        setupSearchLogic();
        // 4. Xử lý logic Nút Home (Đăng xuất)
        setupHomeLogic();
    }

    private void initViews() {
        // Hãy chắc chắn trong XML bạn đã có các ID này
        btnSearchCustomer = findViewById(R.id.btn_search_customer);
        layoutSearchContainer = findViewById(R.id.layout_search_container);
        btnCloseSearch = findViewById(R.id.btn_close_search);
        etSearchQuery = findViewById(R.id.et_search_query);
        btnHomeLogout = findViewById(R.id.btn_home_logout);
    }

    private void setupSearchLogic() {
        // A. Khi bấm nút "Find Customer"
        btnSearchCustomer.setOnClickListener(v -> {
            // 1. Hiện thanh tìm kiếm ra
            layoutSearchContainer.setVisibility(View.VISIBLE);

            // 2. Focus vào ô nhập liệu để gõ luôn
            etSearchQuery.requestFocus();

            // 3. (Optional) Tự động bật bàn phím ảo lên cho tiện
            showKeyboard();
        });

        // B. Khi bấm nút "X" (Close)
        btnCloseSearch.setOnClickListener(v -> {
            // 1. Xóa chữ đã nhập
            etSearchQuery.setText("");

            // 2. Ẩn thanh tìm kiếm đi
            layoutSearchContainer.setVisibility(View.GONE);

            // 3. Ẩn bàn phím ảo
            hideKeyboard();
        });
    }

    private void setupHomeLogic() {
        // Khi bấm nút tròn Home ở dưới
        btnHomeLogout.setOnClickListener(v -> {
            // 1. Đăng xuất khỏi Firebase (nếu đã tích hợp)
            FirebaseAuth.getInstance().signOut();

            Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show();

            // 2. Tạo Intent quay về màn hình Login
            Intent intent = new Intent(BankOfficerActivity.this, LoginActivity.class);

            // 3. [QUAN TRỌNG] Xóa lịch sử (Back Stack)
            // Để user bấm nút Back trên điện thoại sẽ thoát App chứ không quay lại màn hình Officer được nữa
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish(); // Đóng Activity hiện tại
        });
    }

    // Hàm phụ trợ: Hiện bàn phím
    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etSearchQuery, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // Hàm phụ trợ: Ẩn bàn phím
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