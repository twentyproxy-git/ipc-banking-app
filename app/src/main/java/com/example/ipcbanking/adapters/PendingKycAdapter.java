package com.example.ipcbanking.adapters;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ipcbanking.R;
import com.example.ipcbanking.models.PendingKycItem;
import com.example.ipcbanking.viewholders.PendingKycViewHolder;
import com.example.ipcbanking.views.MagnifierView;

import java.util.List;
import java.util.Map;

public class PendingKycAdapter extends RecyclerView.Adapter<PendingKycViewHolder> {

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

            // Face Image
            if (faceUrl != null && !faceUrl.isEmpty()) {
                Glide.with(context).load(faceUrl).centerCrop().into(holder.imgFace);
                holder.imgFace.setOnClickListener(v -> showImageDialog(faceUrl));
            }

            // ID Card Image
            if (idCardUrl != null && !idCardUrl.isEmpty()) {
                Glide.with(context).load(idCardUrl).centerCrop().into(holder.imgIdCard);
                holder.imgIdCard.setOnClickListener(v -> showImageDialog(idCardUrl));
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

    // ===========================
    //  SHOW DIALOG ZOOM IMAGE
    // ===========================
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

        Glide.with(context).load(imageUrl).into(imgPreview);

        imgPreview.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    Bitmap bmp = getBitmapFromView(imgPreview);
                    magnifierView.setupBitmap(bmp);
                    magnifierView.setPoint(event.getX(), event.getY());
                    return true;

                case MotionEvent.ACTION_MOVE:
                    magnifierView.setPoint(event.getX(), event.getY());
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    magnifierView.hide();
                    magnifierView.setupBitmap(null);
                    return true;
            }
            return false;
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private Bitmap getBitmapFromView(View view) {
        Bitmap b = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(b);
        view.draw(canvas);
        return b;
    }
}
