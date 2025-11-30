package com.example.ipcbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper; // [MỚI] Dùng LinearSnapHelper thay vì PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.bumptech.glide.Glide;
import com.example.ipcbanking.R;
import com.example.ipcbanking.adapters.AccountAdapter;
import com.example.ipcbanking.models.AccountItem;
import com.example.ipcbanking.utils.SpaceItemDecoration;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CustomerHomeActivity extends AppCompatActivity {

    // --- 1. KHAI BÁO BIẾN ---
    private TextView tvWelcomeName, tvEmptyAccount;
    private TextView btnViewDetailAction;
    private ImageView imgAvatarHeader;

    private CardView cardAvatarHeader, btnSupportChat;
    private View badgeKycAlert;

    private View btnTransfer, btnPayBill, btnVerifyKyc, btnDeposit, btnWithdraw;

    private RecyclerView rvAccountsCarousel;
    private BottomNavigationView bottomNavBar;

    // Firebase & Data
    private FirebaseFirestore db;
    private String customerId;
    private List<AccountItem> accountList = new ArrayList<>();
    private AccountAdapter accountAdapter;

    private AccountItem currentSelectedAccount = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Init Firebase
        db = FirebaseFirestore.getInstance();
        initViews();

        // Get ID
        Intent intent = getIntent();
        customerId = intent.getStringExtra("CUSTOMER_ID");

        if (customerId == null) {
            Toast.makeText(this, "Error: User ID not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup UI
        setupRecyclerViewWithSnap();
        setupBottomNavigation();
        setupClickListeners();

        // Load Data
        loadUserProfile(customerId);
        loadUserAccounts(customerId);
    }

    private void initViews() {
        tvWelcomeName = findViewById(R.id.tv_welcome_name);
        tvEmptyAccount = findViewById(R.id.tv_empty_account);
        btnViewDetailAction = findViewById(R.id.btn_view_detail_action);

        imgAvatarHeader = findViewById(R.id.img_avatar_header);
        cardAvatarHeader = findViewById(R.id.card_avatar_header);
        btnSupportChat = findViewById(R.id.btn_support_chat);

        badgeKycAlert = findViewById(R.id.badge_kyc_alert);

        rvAccountsCarousel = findViewById(R.id.rv_accounts_carousel);
        bottomNavBar = findViewById(R.id.bottom_nav_bar);

        btnDeposit = findViewById(R.id.btn_deposit);
        btnWithdraw = findViewById(R.id.btn_withdraw);

        btnTransfer = findViewById(R.id.btn_transfer);
        btnPayBill = findViewById(R.id.btn_pay_bill);
        btnVerifyKyc = findViewById(R.id.btn_verify_kyc);
    }

    private void setupRecyclerViewWithSnap() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvAccountsCarousel.setLayoutManager(layoutManager);

        int spaceInPixels = (int) (50 * getResources().getDisplayMetrics().density);
        rvAccountsCarousel.addItemDecoration(new SpaceItemDecoration(spaceInPixels));

        accountAdapter = new AccountAdapter(this, accountList);
        rvAccountsCarousel.setAdapter(accountAdapter);

        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(rvAccountsCarousel);

        rvAccountsCarousel.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View centerView = snapHelper.findSnapView(layoutManager);
                    if (centerView != null) {
                        int pos = layoutManager.getPosition(centerView);
                        if (pos != RecyclerView.NO_POSITION && pos < accountList.size()) {
                            currentSelectedAccount = accountList.get(pos);
                        }
                    }
                }
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNavBar.setItemIconTintList(null);
        bottomNavBar.setSelectedItemId(R.id.nav_home);

        bottomNavBar.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            }
            else if (id == R.id.nav_history) {
                if (currentSelectedAccount != null) {
                    Intent intent = new Intent(CustomerHomeActivity.this, TransactionHistoryActivity.class);
                    intent.putExtra("ACCOUNT_NUMBER", currentSelectedAccount.getAccountNumber());
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "No account selected!", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            else if (id == R.id.nav_profile) {
                openProfileActivity();
                return true;
            }
            return false;
        });
    }

    private void setupClickListeners() {
        // 1. View Detail
        btnViewDetailAction.setOnClickListener(v -> {
            if (currentSelectedAccount != null) {
                Intent intent = new Intent(CustomerHomeActivity.this, AccountDetailActivity.class);
                intent.putExtra("ACCOUNT_DATA", currentSelectedAccount);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No account selected!", Toast.LENGTH_SHORT).show();
            }
        });

        // 2. Các nút mở Profile
        cardAvatarHeader.setOnClickListener(v -> openProfileActivity());
        btnVerifyKyc.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerHomeActivity.this, VerifyKYCActivity.class);
            intent.putExtra("CUSTOMER_ID", customerId);
            startActivity(intent);
        });

        btnDeposit.setOnClickListener(v -> {
            if (currentSelectedAccount != null) {
                Intent intent = new Intent(CustomerHomeActivity.this, DepositActivity.class);
                intent.putExtra("CUSTOMER_ID", customerId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Please select an account first", Toast.LENGTH_SHORT).show();
            }
        });

        btnWithdraw.setOnClickListener(v -> {
            if (currentSelectedAccount != null) {
                Intent intent = new Intent(CustomerHomeActivity.this, WithdrawActivity.class);
                intent.putExtra("ACCOUNT_DATA", currentSelectedAccount);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Please select an account first", Toast.LENGTH_SHORT).show();
            }
        });

        // 3. Các nút khác (Demo)
        btnTransfer.setOnClickListener(v -> Toast.makeText(this, "Transfer Clicked", Toast.LENGTH_SHORT).show());
        btnPayBill.setOnClickListener(v -> Toast.makeText(this, "Pay Bill Clicked", Toast.LENGTH_SHORT).show());
        btnSupportChat.setOnClickListener(v -> Toast.makeText(this, "Support Agent Connecting...", Toast.LENGTH_SHORT).show());
    }

    private void openProfileActivity() {
        Intent intent = new Intent(CustomerHomeActivity.this, EditCustomerActivity.class);
        intent.putExtra("CUSTOMER_ID", customerId);
        startActivity(intent);
    }

    private void loadUserProfile(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("full_name");
                        tvWelcomeName.setText(name != null ? name : "Customer");

                        String avatarUrl = documentSnapshot.getString("avatar_url");
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this).load(avatarUrl).into(imgAvatarHeader);
                        }

                        // Logic hiện Badge đỏ
                        Boolean isKyced = documentSnapshot.getBoolean("is_kyced");
                        if (isKyced != null && isKyced) {
                            badgeKycAlert.setVisibility(View.GONE);
                        } else {
                            badgeKycAlert.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed load profile", Toast.LENGTH_SHORT).show());
    }

    private void loadUserAccounts(String uid) {
        db.collection("accounts")
                .whereEqualTo("owner_id", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    accountList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AccountItem account = doc.toObject(AccountItem.class);
                        accountList.add(account);
                    }

                    if (accountList.isEmpty()) {
                        rvAccountsCarousel.setVisibility(View.GONE);
                        tvEmptyAccount.setVisibility(View.VISIBLE);
                        btnViewDetailAction.setVisibility(View.GONE);
                        currentSelectedAccount = null;
                    } else {
                        rvAccountsCarousel.setVisibility(View.VISIBLE);
                        tvEmptyAccount.setVisibility(View.GONE);
                        btnViewDetailAction.setVisibility(View.VISIBLE);

                        accountAdapter.setData(accountList);

                        if (!accountList.isEmpty()) {
                            currentSelectedAccount = accountList.get(0);
                        }
                    }
                });
    }



    @Override
    protected void onResume() {
        super.onResume();
        if (customerId != null) {
            loadUserProfile(customerId);
            loadUserAccounts(customerId);
        }
    }
}