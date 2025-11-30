package com.example.ipcbanking.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipcbanking.R;
import com.example.ipcbanking.adapters.PendingKycAdapter;
import com.example.ipcbanking.models.PendingKycItem;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewRequestActivity extends AppCompatActivity {

    private RecyclerView rvPendingList;
    private ImageView btnBack;
    private LinearLayout layoutEmptyState;

    private PendingKycAdapter adapter;
    private List<PendingKycItem> pendingList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_request);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPendingRequests();
    }

    private void initViews() {
        rvPendingList = findViewById(R.id.rv_pending_list);
        btnBack = findViewById(R.id.btn_back);
        layoutEmptyState = findViewById(R.id.layout_empty_state);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvPendingList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingKycAdapter(this, pendingList, new PendingKycAdapter.OnKycActionListener() {
            @Override
            public void onAccept(PendingKycItem item, int position) {
                processKycAction(item, position, true);
            }

            @Override
            public void onReject(PendingKycItem item, int position) {
                processKycAction(item, position, false);
            }
        });
        rvPendingList.setAdapter(adapter);
    }

    private void processKycAction(PendingKycItem item, int position, boolean isApproved) {
        Map<String, Object> updates = new HashMap<>();

        if (isApproved) {
            updates.put("kyc_status", "VERIFIED");
            updates.put("is_kyced", true);
            updates.put("kyc_data.verified_at", FieldValue.serverTimestamp());
        } else {
            updates.put("kyc_status", "UNVERIFIED");
            updates.put("is_kyced", false);
            updates.put("kyc_data", FieldValue.delete());
        }

        db.collection("users").document(item.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    String msg = isApproved ? "Đã duyệt: " : "Đã từ chối: ";
                    Toast.makeText(this, msg + item.getFullName(), Toast.LENGTH_SHORT).show();
                    adapter.removeItem(position);
                    checkEmptyState();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadPendingRequests() {
        db.collection("users")
                .whereEqualTo("kyc_status", "PENDING")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    pendingList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        PendingKycItem item = doc.toObject(PendingKycItem.class);
                        if (item != null) {
                            item.setUid(doc.getId());
                            pendingList.add(item);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    checkEmptyState();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Không tải được dữ liệu", Toast.LENGTH_SHORT).show());
    }

    private void checkEmptyState() {
        if (pendingList.isEmpty()) {
            rvPendingList.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvPendingList.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }
}