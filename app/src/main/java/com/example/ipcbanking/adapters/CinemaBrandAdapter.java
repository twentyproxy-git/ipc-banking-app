package com.example.ipcbanking.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ipcbanking.R;
import com.example.ipcbanking.models.CinemaBrand;
import java.util.List;

public class CinemaBrandAdapter extends RecyclerView.Adapter<CinemaBrandAdapter.CinemaBrandViewHolder> {

    private final List<CinemaBrand> brandList;

    public CinemaBrandAdapter(List<CinemaBrand> brandList) {
        this.brandList = brandList;
    }

    @NonNull
    @Override
    public CinemaBrandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cinema_brand, parent, false);
        return new CinemaBrandViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CinemaBrandViewHolder holder, int position) {
        CinemaBrand brand = brandList.get(position);
        holder.bind(brand);
    }

    @Override
    public int getItemCount() {
        return brandList.size();
    }

    static class CinemaBrandViewHolder extends RecyclerView.ViewHolder {
        private final ImageView brandLogo;
        private final TextView brandName;

        public CinemaBrandViewHolder(@NonNull View itemView) {
            super(itemView);
            brandLogo = itemView.findViewById(R.id.iv_brand_logo);
            brandName = itemView.findViewById(R.id.tv_brand_name);
        }

        public void bind(final CinemaBrand brand) {
            brandName.setText(brand.getName());
            Glide.with(itemView.getContext())
                    .load(brand.getLogoUrl())
                    .into(brandLogo);
        }
    }
}
