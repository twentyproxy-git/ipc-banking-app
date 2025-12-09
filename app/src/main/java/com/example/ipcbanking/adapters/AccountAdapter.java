package com.example.ipcbanking.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipcbanking.R;
import com.example.ipcbanking.models.AccountItem;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    private Context context;
    private List<AccountItem> accountList;
    private boolean isBalanceVisible = false; // Mặc định ẩn số dư
    private OnSecurityToggleListener securityListener;

    public interface OnSecurityToggleListener {
        void onRequestToggle(boolean currentStatus);
    }

    public AccountAdapter(Context context, List<AccountItem> accountList, OnSecurityToggleListener listener) {
        this.context = context;
        this.accountList = (accountList != null) ? accountList : new ArrayList<>();
        this.securityListener = listener;
    }

    public void setData(List<AccountItem> newList) {
        this.accountList = (newList != null) ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setVisibilityState(boolean visible) {
        this.isBalanceVisible = visible;
        notifyDataSetChanged();
    }

    public boolean isVisible() {
        return isBalanceVisible;
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_account_card, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        AccountItem item = accountList.get(position);
        if (item == null) return;

        DecimalFormat decimalFormat = new DecimalFormat("#,###");

        // 1. CHỈ ẨN/HIỆN SỐ DƯ (BALANCE)
        if (isBalanceVisible) {
            holder.tvBalance.setText(decimalFormat.format(item.getBalance()) + " VND");
            holder.imgVisibility.setImageResource(R.drawable.ic_eye);
        } else {
            holder.tvBalance.setText("****** VND");
            holder.imgVisibility.setImageResource(R.drawable.ic_eye_off);
        }

        // 2. SỐ TÀI KHOẢN (LUÔN HIỂN THỊ DẠNG MASKED - KHÔNG BỊ ẢNH HƯỞNG BỞI MẮT)
        String rawNum = item.getAccountNumber();
        if (rawNum != null && rawNum.length() > 4) {
            String last4 = rawNum.substring(rawNum.length() - 4);
            holder.tvAccountNumber.setText("**** **** **** " + last4);
        } else {
            holder.tvAccountNumber.setText("**** **** ****");
        }

        // Sự kiện click mắt
        holder.imgVisibility.setOnClickListener(v -> {
            if (securityListener != null) {
                securityListener.onRequestToggle(isBalanceVisible);
            }
        });

        // 3. Loại thẻ
        String type = item.getAccountType();
        String displayType = "Payment Account";

        if ("SAVING".equals(type)) {
            displayType = "Savings Account (" + item.getProfitRate() + "%)";
        } else if ("MORTGAGE".equals(type)) {
            String monthlyPay = decimalFormat.format(item.getMonthlyPayment()) + " VND";
            displayType = "Mortgage Loan (Pay: " + monthlyPay + ")";
        }

        holder.tvCardType.setText(displayType);
        holder.tvBankName.setText("Interastral Peace Corp");
    }

    @Override
    public int getItemCount() {
        return accountList != null ? accountList.size() : 0;
    }

    public static class AccountViewHolder extends RecyclerView.ViewHolder {
        TextView tvBankName, tvCardType, tvBalance, tvAccountNumber;
        ImageView imgVisibility, imgChip;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBankName = itemView.findViewById(R.id.tv_bank_name);
            tvCardType = itemView.findViewById(R.id.tv_card_type);
            tvBalance = itemView.findViewById(R.id.tv_balance);
            tvAccountNumber = itemView.findViewById(R.id.tv_card_number);
            imgVisibility = itemView.findViewById(R.id.img_visibility);
            imgChip = itemView.findViewById(R.id.img_chip);
        }
    }
}