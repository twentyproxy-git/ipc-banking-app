package com.example.ipcbanking.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Thư viện load ảnh
import com.example.ipcbanking.R;
import com.example.ipcbanking.models.CustomerItem;

import java.util.ArrayList;
import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerViewHolder> {

    private Context context;
    private List<CustomerItem> customerList;
    private OnCustomerClickListener listener;

    // Interface để xử lý sự kiện click ra bên ngoài
    public interface OnCustomerClickListener {
        void onCustomerClick(CustomerItem customer);
    }

    public CustomerAdapter(Context context, List<CustomerItem> customerList, OnCustomerClickListener listener) {
        this.context = context;
        this.customerList = customerList;
        this.listener = listener;
    }

    // Hàm cập nhật list (Dùng cho tính năng Tìm kiếm)
    public void updateList(List<CustomerItem> newList) {
        this.customerList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_customer, parent, false);
        return new CustomerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        CustomerItem customer = customerList.get(position);

        // 1. Gán Text
        holder.tvName.setText(customer.getFullName());
        holder.tvPhone.setText(customer.getPhoneNumber());

        // 2. Load ảnh từ Cloudinary bằng Glide
        if (customer.getAvatarUrl() != null && !customer.getAvatarUrl().isEmpty()) {
            Glide.with(context)
                    .load(customer.getAvatarUrl())
                    .placeholder(R.drawable.ic_topaz) // Ảnh chờ
                    .error(R.drawable.ic_unknown)       // Ảnh lỗi
                    .circleCrop()                     // Cắt tròn
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_topaz);
        }

        // 3. Xử lý click vào item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCustomerClick(customer);
            }
        });
    }

    @Override
    public int getItemCount() {
        return customerList != null ? customerList.size() : 0;
    }
}