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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    private Context context;
    private List<AccountItem> accountList;
    private boolean isBalanceVisible = true; // Trạng thái ẩn/hiện số dư

    public AccountAdapter(Context context, List<AccountItem> accountList) {
        this.context = context;
        // Khởi tạo list an toàn để tránh null
        this.accountList = (accountList != null) ? accountList : new ArrayList<>();
    }

    // Hàm cập nhật dữ liệu mới
    public void setData(List<AccountItem> newList) {
        this.accountList = (newList != null) ? newList : new ArrayList<>();
        notifyDataSetChanged(); // Báo cho RecyclerView vẽ lại
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

        // 1. [CẬP NHẬT] Định dạng tiền tệ thủ công để hiện chữ "đ"
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        String balanceStr = decimalFormat.format(item.getBalance()) + " VND";

        // 2. Xử lý Logic Ẩn/Hiện Số dư
        if (isBalanceVisible) {
            holder.tvBalance.setText(balanceStr);
            holder.imgVisibility.setImageResource(R.drawable.ic_eye); // Icon mở mắt
        } else {
            // [CẬP NHẬT] Đổi hiển thị khi ẩn
            holder.tvBalance.setText("****** VND");
            holder.imgVisibility.setImageResource(R.drawable.ic_eye_off); // Icon nhắm mắt
        }

        // Sự kiện click vào con mắt
        holder.imgVisibility.setOnClickListener(v -> {
            isBalanceVisible = !isBalanceVisible;
            notifyDataSetChanged(); // Load lại toàn bộ list để áp dụng cho tất cả thẻ
        });

        String rawNum = item.getAccountNumber();
        if (rawNum != null && rawNum.length() > 4) {
            String last4 = rawNum.substring(rawNum.length() - 4);
            holder.tvAccountNumber.setText("**** **** **** " + last4);
        } else {
            holder.tvAccountNumber.setText(rawNum != null ? rawNum : "N/A");
        }

        // 4. Loại thẻ & Thông tin phụ
        String type = item.getAccountType();
        String displayType = "Payment Account";

        if ("SAVING".equals(type)) {
            displayType = "Savings Account (" + item.getProfitRate() + "%)";
        } else if ("MORTGAGE".equals(type)) {
            String monthlyPay = decimalFormat.format(item.getMonthlyPayment()) + " VND";
            displayType = "Mortgage Loan (Pay: " + monthlyPay + ")";
        } else {
            displayType = "Checking / Payment Account";
        }

        holder.tvCardType.setText(displayType);

        holder.tvBankName.setText("Interastral Peace Corp");
    }

    @Override
    public int getItemCount() {
        return accountList != null ? accountList.size() : 0;
    }

    // === VIEWHOLDER ===
    public static class AccountViewHolder extends RecyclerView.ViewHolder {

        TextView tvBankName, tvCardType, tvBalance, tvAccountNumber;
        ImageView imgVisibility, imgChip;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);

            tvBankName = itemView.findViewById(R.id.tv_bank_name);
            tvCardType = itemView.findViewById(R.id.tv_card_type);
            tvBalance = itemView.findViewById(R.id.tv_balance);
            tvAccountNumber = itemView.findViewById(R.id.tv_account_number);
            imgVisibility = itemView.findViewById(R.id.img_visibility);
            imgChip = itemView.findViewById(R.id.img_chip);
        }
    }
}