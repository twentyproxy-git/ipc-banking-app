package com.example.ipcbanking.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.ipcbanking.R;

public class AccountViewHolder extends RecyclerView.ViewHolder {

    public TextView tvBankName, tvCardType, tvBalance, tvAccountNumber;
    public ImageView imgVisibility, imgChip;

    public AccountViewHolder(View itemView) {
        super(itemView);

        tvBankName = itemView.findViewById(R.id.tv_bank_name);
        tvCardType = itemView.findViewById(R.id.tv_card_type);
        tvBalance = itemView.findViewById(R.id.tv_balance);
        tvAccountNumber = itemView.findViewById(R.id.tv_card_number);
        imgVisibility = itemView.findViewById(R.id.img_visibility);
        imgChip = itemView.findViewById(R.id.img_chip);
    }
}
