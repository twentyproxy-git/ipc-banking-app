package com.example.ipcbanking.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipcbanking.R;

public class CustomerViewHolder extends RecyclerView.ViewHolder {

    public ImageView imgAvatar;
    public TextView tvName;
    public TextView tvPhone;

    public CustomerViewHolder(@NonNull View itemView) {
        super(itemView);
        // Ánh xạ ID từ file item_customer.xml
        imgAvatar = itemView.findViewById(R.id.img_customer_avatar);
        tvName = itemView.findViewById(R.id.tv_customer_name);
        tvPhone = itemView.findViewById(R.id.tv_customer_phone);
    }
}