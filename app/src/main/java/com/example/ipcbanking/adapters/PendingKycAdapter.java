package com.example.ipcbanking.adapters;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.ipcbanking.R;
import com.example.ipcbanking.models.PendingKycItem;
import com.example.ipcbanking.views.MagnifierView;

import java.util.List;
import java.util.Map;

public class PendingKycAdapter extends RecyclerView.Adapter<PendingKycAdapter.PendingKycViewHolder> {

    private Context context;
    private List<PendingKycItem> list;
    private OnKycActionListener actionListener;

    public interface OnKycActionListener {
        void onAccept(PendingKycItem item, int position);
        void onReject(PendingKycItem item, int position);
    }

    public PendingKycAdapter(Context context, List<PendingKycItem> list, OnKycActionListener listener) {
        this.context = context;
        this.list = list;
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public PendingKycViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pending_kyc, parent, false);
        return new PendingKycViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingKycViewHolder holder, int position) {
        PendingKycItem item = list.get(position);
        holder.tvName.setText(item.getFullName());

        Map<String, Object> kycData = item.getKycData();
        if (kycData != null) {
            String idNumber = (String) kycData.get("id_card_number");
            String faceUrl = (String) kycData.get("face_image_url");
            String idCardUrl = (String) kycData.get("id_card_url");

            holder.tvIdNum.setText("ID: " + (idNumber != null ? idNumber : "N/A"));

            // Setup Face Image
            if (faceUrl != null && !faceUrl.isEmpty()) {
                Glide.with(context).load(faceUrl).centerCrop().into(holder.imgFace);
                holder.imgFace.setImageTintList(null);
                holder.imgFace.setPadding(0, 0, 0, 0);
                holder.imgFace.setScaleType(ImageView.ScaleType.CENTER_CROP);
                holder.imgFace.setOnClickListener(v -> showImageDialog(faceUrl));
            } else {
                holder.imgFace.setImageResource(R.drawable.ic_launcher_background); // Thay bằng ảnh placeholder của bạn
                holder.imgFace.setOnClickListener(null);
            }

            // Setup ID Card Image
            if (idCardUrl != null && !idCardUrl.isEmpty()) {
                Glide.with(context).load(idCardUrl).centerCrop().into(holder.imgIdCard);
                holder.imgIdCard.setImageTintList(null);
                holder.imgIdCard.setPadding(0, 0, 0, 0);
                holder.imgIdCard.setScaleType(ImageView.ScaleType.CENTER_CROP);
                holder.imgIdCard.setOnClickListener(v -> showImageDialog(idCardUrl));
            } else {
                holder.imgIdCard.setImageResource(R.drawable.ic_launcher_background);
                holder.imgIdCard.setOnClickListener(null);
            }
        }

        holder.btnAccept.setOnClickListener(v -> actionListener.onAccept(item, position));
        holder.btnReject.setOnClickListener(v -> actionListener.onReject(item, position));
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < list.size()) {
            list.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, list.size());
        }
    }

    // === HÀM SHOW DIALOG VỚI KÍNH LÚP (ĐÃ SỬA LỖI ZOOM) ===
    private void showImageDialog(String imageUrl) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_view_image);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView imgPreview = dialog.findViewById(R.id.img_full_preview);
        View btnClose = dialog.findViewById(R.id.btn_close_dialog);
        final MagnifierView magnifierView = dialog.findViewById(R.id.magnifier_view);

        // 1. Load ảnh vào ImageView bình thường (Không cần nạp vào kính lúp ở đây nữa)
        Glide.with(context)
                .load(imageUrl)
                .into(imgPreview);

        // 2. Xử lý logic chạm
        imgPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // [QUAN TRỌNG] Khi bắt đầu chạm, "chụp" lại hình ảnh đang hiển thị
                        // Để nạp vào kính lúp. Điều này đảm bảo toạ độ khớp 100%.
                        Bitmap viewBitmap = getBitmapFromView(imgPreview);
                        magnifierView.setupBitmap(viewBitmap);

                        magnifierView.setPoint(event.getX(), event.getY());
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        magnifierView.setPoint(event.getX(), event.getY());
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.performClick();
                        magnifierView.hide();
                        // Giải phóng bitmap để tránh tốn ram
                        magnifierView.setupBitmap(null);
                        return true;
                }
                return false;
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // === HÀM HỖ TRỢ LẤY ẢNH TỪ VIEW (Copy thêm hàm này vào dưới hàm showImageDialog) ===
    private Bitmap getBitmapFromView(View view) {
        // Tạo một bitmap có kích thước bằng đúng cái ImageView trên màn hình
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        // Vẽ nội dung của View lên bitmap đó
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    public static class PendingKycViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvIdNum;
        ImageView imgFace, imgIdCard;
        Button btnAccept, btnReject;

        public PendingKycViewHolder(@NonNull View itemView) {
            super(itemView);
            // Đảm bảo ID này khớp với file item_pending_kyc.xml của bạn
            tvName = itemView.findViewById(R.id.tv_customer_name);
            tvIdNum = itemView.findViewById(R.id.tv_id_number);
            imgFace = itemView.findViewById(R.id.img_face);
            imgIdCard = itemView.findViewById(R.id.img_id_card);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}