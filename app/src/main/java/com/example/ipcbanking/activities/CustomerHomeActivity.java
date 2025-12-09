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
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.bumptech.glide.Glide;
import com.example.ipcbanking.R;
import com.example.ipcbanking.adapters.AccountAdapter;
import com.example.ipcbanking.adapters.PromotionAdapter;
import com.example.ipcbanking.models.AccountItem;
import com.example.ipcbanking.models.PromotionItem;
import com.example.ipcbanking.utils.SpaceItemDecoration;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CustomerHomeActivity extends AppCompatActivity {

    private TextView tvWelcomeName, tvEmptyAccount;
    private TextView btnViewDetailAction;
    private ImageView imgAvatarHeader;

    private CardView cardAvatarHeader, btnLogout;
    private View badgeKycAlert;

    private View btnTransfer, btnPayBill, btnFlight, btnTicket, btnDeposit, btnWithdraw, btnVerifyKyc;

    // Accounts
    private RecyclerView rvAccountsCarousel;
    private SnapHelper snapHelper;
    private List<AccountItem> accountList = new ArrayList<>();
    private AccountAdapter accountAdapter;
    private AccountItem currentSelectedAccount = null;

    // Promotions
    private RecyclerView rvPromotions;
    private PromotionAdapter promotionAdapter;
    private List<PromotionItem> promotionList = new ArrayList<>();

    private BottomNavigationView bottomNavBar;
    private FirebaseFirestore db;
    private String customerId;

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

        db = FirebaseFirestore.getInstance();
        initViews();

        Intent intent = getIntent();
        customerId = intent.getStringExtra("CUSTOMER_ID");

        if (customerId == null) {
            Toast.makeText(this, "Error: User ID not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupRecyclerViewWithSnap(); // Accounts
        setupPromotionsRecyclerView(); // Promotions
        setupBottomNavigation();
        setupClickListeners();

        loadUserProfile(customerId);
        loadUserAccounts(customerId);
    }

    private void initViews() {
        tvWelcomeName = findViewById(R.id.tv_welcome_name);
        tvEmptyAccount = findViewById(R.id.tv_empty_account);
        btnViewDetailAction = findViewById(R.id.btn_view_detail_action);

        imgAvatarHeader = findViewById(R.id.img_avatar_header);
        cardAvatarHeader = findViewById(R.id.card_avatar_header);
        btnLogout = findViewById(R.id.btn_logout);

        badgeKycAlert = findViewById(R.id.badge_kyc_alert);

        rvAccountsCarousel = findViewById(R.id.rv_accounts_carousel);
        rvPromotions = findViewById(R.id.rv_promotions);

        bottomNavBar = findViewById(R.id.bottom_nav_bar);

        btnTransfer = findViewById(R.id.btn_transfer);
        btnPayBill = findViewById(R.id.btn_pay_bill);
        btnFlight = findViewById(R.id.btn_flight);
        btnTicket = findViewById(R.id.btn_ticket);
        btnDeposit = findViewById(R.id.btn_deposit);
        btnWithdraw = findViewById(R.id.btn_withdraw);
        btnVerifyKyc = findViewById(R.id.btn_verify_kyc);
    }

    private void setupRecyclerViewWithSnap() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvAccountsCarousel.setLayoutManager(layoutManager);

        int spaceInPixels = (int) (50 * getResources().getDisplayMetrics().density);
        rvAccountsCarousel.addItemDecoration(new SpaceItemDecoration(spaceInPixels));

        accountAdapter = new AccountAdapter(this, accountList);
        rvAccountsCarousel.setAdapter(accountAdapter);

        snapHelper = new LinearSnapHelper();
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

    private void setupPromotionsRecyclerView() {
        rvPromotions.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        promotionList.clear();
        promotionList.add(new PromotionItem(R.drawable.ic_promotion_05));
        promotionList.add(new PromotionItem(R.drawable.ic_promotion_04));
        promotionList.add(new PromotionItem(R.drawable.ic_promotion_03));

        promotionAdapter = new PromotionAdapter(this, promotionList);
        rvPromotions.setAdapter(promotionAdapter);

        SnapHelper promoSnapHelper = new LinearSnapHelper();
        promoSnapHelper.attachToRecyclerView(rvPromotions);
    }

    private void setupBottomNavigation() {
        bottomNavBar.setItemIconTintList(null);

        bottomNavBar.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_history) {
                if (currentSelectedAccount != null) {
                    Intent intent = new Intent(CustomerHomeActivity.this, TransactionHistoryActivity.class);
                    intent.putExtra("ACCOUNT_NUMBER", currentSelectedAccount.getAccountNumber());
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Please select an account first!", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
            else if (id == R.id.nav_map) {
                startActivity(new Intent(CustomerHomeActivity.this, MapActivity.class));
                return false;
            }
            else if (id == R.id.nav_profile) {
                openProfileActivity();
                return false;
            }
            else if (id == R.id.nav_support) {
                Toast.makeText(this, "Connecting to Support Agent...", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private void setupClickListeners() {
        // Detail & Profile
        btnViewDetailAction.setOnClickListener(v -> {
            if (currentSelectedAccount != null) {
                Intent intent = new Intent(CustomerHomeActivity.this, AccountDetailActivity.class);
                intent.putExtra("ACCOUNT_DATA", currentSelectedAccount);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No account selected!", Toast.LENGTH_SHORT).show();
            }
        });
        cardAvatarHeader.setOnClickListener(v -> openProfileActivity());

        // Quick Actions
        btnTransfer.setOnClickListener(v -> {
//            if (currentSelectedAccount != null) {
//                Intent intent = new Intent(CustomerHomeActivity.this, TransferActivity.class);
//                intent.putExtra("SOURCE_ACCOUNT", currentSelectedAccount);
//                startActivity(intent);
//            } else {
//                Toast.makeText(this, "Please select an account first", Toast.LENGTH_SHORT).show();
//            }
        });

        btnPayBill.setOnClickListener(v ->
                Toast.makeText(this, "Pay Bill feature coming soon!", Toast.LENGTH_SHORT).show()
        );

        btnFlight.setOnClickListener(v ->
                Toast.makeText(this, "Flight Booking feature coming soon!", Toast.LENGTH_SHORT).show()
        );

        btnTicket.setOnClickListener(v ->
                Toast.makeText(this, "Ticket Booking feature coming soon!", Toast.LENGTH_SHORT).show()
        );

        btnDeposit.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerHomeActivity.this, DepositActivity.class);
            intent.putExtra("CUSTOMER_ID", customerId);
            startActivity(intent);
        });

        btnWithdraw.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerHomeActivity.this, WithdrawalActivity.class);
            intent.putExtra("CUSTOMER_ID", customerId);
            startActivity(intent);
        });

        btnVerifyKyc.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerHomeActivity.this, VerifyKYCActivity.class);
            intent.putExtra("CUSTOMER_ID", customerId);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Do you really want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(CustomerHomeActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });
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
                        account.setId(doc.getId());
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