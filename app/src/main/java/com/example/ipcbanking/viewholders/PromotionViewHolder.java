package com.example.ipcbanking.viewholders;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipcbanking.R;

public class PromotionViewHolder extends RecyclerView.ViewHolder {

    public ImageView imgBanner;

    public PromotionViewHolder(@NonNull View itemView) {
        super(itemView);
        imgBanner = itemView.findViewById(R.id.img_promo_banner);
    }
}
