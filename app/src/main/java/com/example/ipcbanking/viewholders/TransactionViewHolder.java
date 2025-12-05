package com.example.ipcbanking.viewholders;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipcbanking.R;

public class TransactionViewHolder extends RecyclerView.ViewHolder {

    public TextView tvType, tvSender, tvReceiver, tvAmount, tvDate;

    public TransactionViewHolder(@NonNull View itemView) {
        super(itemView);
        tvType = itemView.findViewById(R.id.tv_type);
        tvSender = itemView.findViewById(R.id.tv_sender);
        tvReceiver = itemView.findViewById(R.id.tv_receiver);
        tvAmount = itemView.findViewById(R.id.tv_amount);
        tvDate = itemView.findViewById(R.id.tv_date);
    }
}
