package com.example.ipcbanking.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipcbanking.R;
import com.example.ipcbanking.models.AccountItem;
import com.example.ipcbanking.viewholders.AccountViewHolder;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountViewHolder> {

    private Context context;
    private List<AccountItem> accountList;
    private boolean isBalanceVisible = true;

    public AccountAdapter(Context context, List<AccountItem> accountList) {
        this.context = context;
        this.accountList = (accountList != null) ? accountList : new ArrayList<>();
    }

    public void setData(List<AccountItem> newList) {
        this.accountList = (newList != null) ? newList : new ArrayList<>();
        notifyDataSetChanged();
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

        DecimalFormat df = new DecimalFormat("#,###");
        String balanceStr = df.format(item.getBalance()) + " VND";

        // ====== ẨN/HIỆN SỐ DƯ ======
        if (isBalanceVisible) {
            holder.tvBalance.setText(balanceStr);
            holder.imgVisibility.setImageResource(R.drawable.ic_eye);
        } else {
            holder.tvBalance.setText("****** VND");
            holder.imgVisibility.setImageResource(R.drawable.ic_eye_off);
        }

        holder.imgVisibility.setOnClickListener(v -> {
            isBalanceVisible = !isBalanceVisible;
            notifyDataSetChanged();
        });

        // ====== FORMAT ACCOUNT NUMBER ======
        String num = item.getAccountNumber();
        if (num != null && num.length() > 4) {
            holder.tvAccountNumber.setText("**** **** **** " + num.substring(num.length() - 4));
        } else {
            holder.tvAccountNumber.setText(num != null ? num : "N/A");
        }

        // ====== LOẠI TÀI KHOẢN ======
        String type = item.getAccountType();
        String displayType;

        switch (type) {
            case "SAVING":
                displayType = "Savings Account (" + item.getProfitRate() + "%)";
                break;
            case "MORTGAGE":
                displayType = "Mortgage Loan (Pay: " + df.format(item.getMonthlyPayment()) + " VND)";
                break;
            default:
                displayType = "Checking / Payment Account";
        }

        holder.tvCardType.setText(displayType);

        holder.tvBankName.setText("Interastral Peace Corp");
    }

    @Override
    public int getItemCount() {
        return (accountList != null) ? accountList.size() : 0;
    }
}
