package com.example.ipcbanking.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipcbanking.R;
import com.example.ipcbanking.adapters.TransactionAdapter;
import com.example.ipcbanking.models.TransactionItem;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class TransactionHistoryActivity extends AppCompatActivity {

    private RecyclerView rvTransactions;
    private TextView tvEmptyState, tvAccountInfo;
    private ProgressBar progressBar;
    private ImageView btnBack;

    private FirebaseFirestore db;
    private TransactionAdapter adapter;
    private List<TransactionItem> transactionList = new ArrayList<>();
    private String currentAccountNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transaction_history);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        currentAccountNumber = getIntent().getStringExtra("ACCOUNT_NUMBER");

        if (currentAccountNumber == null) {
            Toast.makeText(this, "Account Number Missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();

        tvAccountInfo.setText("History for Account: " + currentAccountNumber);

        loadTransactions();
    }

    private void initViews() {
        rvTransactions = findViewById(R.id.rv_transactions);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        tvAccountInfo = findViewById(R.id.tv_account_info);
        progressBar = findViewById(R.id.loading_progress);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this, transactionList, currentAccountNumber);
        rvTransactions.setAdapter(adapter);
    }

    private void loadTransactions() {
        progressBar.setVisibility(View.VISIBLE);
        transactionList.clear();

        // Query 1: Giao dịch gửi đi
        var taskSent = db.collection("transactions")
                .whereEqualTo("sender_account", currentAccountNumber).get();

        // Query 2: Giao dịch nhận
        var taskReceived = db.collection("transactions")
                .whereEqualTo("receiver_account", currentAccountNumber).get();

        Tasks.whenAllSuccess(taskSent, taskReceived).addOnSuccessListener(results -> {
            QuerySnapshot sentSnapshot = (QuerySnapshot) results.get(0);
            for (QueryDocumentSnapshot doc : sentSnapshot) {
                transactionList.add(doc.toObject(TransactionItem.class));
            }

            QuerySnapshot receivedSnapshot = (QuerySnapshot) results.get(1);
            for (QueryDocumentSnapshot doc : receivedSnapshot) {
                transactionList.add(doc.toObject(TransactionItem.class));
            }

            // Sắp xếp giảm dần theo ngày
            Collections.sort(transactionList, (o1, o2) -> {
                if (o1.getCreatedAt() == null || o2.getCreatedAt() == null) return 0;
                return o2.getCreatedAt().compareTo(o1.getCreatedAt());
            });

            // Load tên người dùng từ accounts -> users
            loadAccountNames();

        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to load history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadAccountNames() {
        // Lấy tất cả account numbers liên quan
        Set<String> accountNumbers = new HashSet<>();
        for (TransactionItem t : transactionList) {
            accountNumbers.add(t.getSenderAccount());
            accountNumbers.add(t.getReceiverAccount());
        }

        Map<String, String> accountNameMap = new HashMap<>();
        if (accountNumbers.isEmpty()) {
            adapter.setAccountNameMap(accountNameMap);
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
            tvEmptyState.setVisibility(transactionList.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        // Duyệt từng account để lấy ownerId -> user -> full_name
        for (String accNum : accountNumbers) {
            db.collection("accounts").whereEqualTo("account_number", accNum).get()
                    .addOnSuccessListener(accQuery -> {
                        if (!accQuery.isEmpty()) {
                            String ownerId = accQuery.getDocuments().get(0).getString("owner_id");
                            db.collection("users").document(ownerId).get()
                                    .addOnSuccessListener(userDoc -> {
                                        String fullName = userDoc.getString("full_name");
                                        if (fullName != null && !fullName.isEmpty()) {
                                            accountNameMap.put(accNum, fullName);
                                        } else {
                                            accountNameMap.put(accNum, accNum);
                                        }
                                        adapter.setAccountNameMap(accountNameMap);
                                        adapter.notifyDataSetChanged();
                                        progressBar.setVisibility(View.GONE);
                                        tvEmptyState.setVisibility(transactionList.isEmpty() ? View.VISIBLE : View.GONE);
                                    });
                        } else {
                            accountNameMap.put(accNum, accNum);
                            adapter.setAccountNameMap(accountNameMap);
                            adapter.notifyDataSetChanged();
                            progressBar.setVisibility(View.GONE);
                            tvEmptyState.setVisibility(transactionList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    });
        }
    }
}
