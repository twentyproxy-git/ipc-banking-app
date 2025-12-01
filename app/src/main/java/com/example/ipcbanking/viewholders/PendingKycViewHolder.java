package com.example.ipcbanking.viewholders;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipcbanking.R;

public class PendingKycViewHolder extends RecyclerView.ViewHolder {

    public TextView tvName, tvIdNum;
    public ImageView imgFace, imgIdCard;
    public Button btnAccept, btnReject;

    public PendingKycViewHolder(@NonNull View itemView) {
        super(itemView);

        tvName = itemView.findViewById(R.id.tv_customer_name);
        tvIdNum = itemView.findViewById(R.id.tv_id_number);

        imgFace = itemView.findViewById(R.id.img_face);
        imgIdCard = itemView.findViewById(R.id.img_id_card);

        btnAccept = itemView.findViewById(R.id.btn_accept);
        btnReject = itemView.findViewById(R.id.btn_reject);
    }
}
