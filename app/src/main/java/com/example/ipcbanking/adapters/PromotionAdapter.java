package com.example.ipcbanking.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipcbanking.R;
import com.example.ipcbanking.models.PromotionItem;
import com.example.ipcbanking.viewholders.PromotionViewHolder;

import java.util.List;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionViewHolder> {

    private Context context;
    private List<PromotionItem> promotionList;

    public PromotionAdapter(Context context, List<PromotionItem> promotionList) {
        this.context = context;
        this.promotionList = promotionList;
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_promotion_banner, parent, false);
        return new PromotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        PromotionItem item = promotionList.get(position);
        holder.imgBanner.setImageResource(item.getImageResId());
    }

    @Override
    public int getItemCount() {
        return promotionList != null ? promotionList.size() : 0;
    }
}
