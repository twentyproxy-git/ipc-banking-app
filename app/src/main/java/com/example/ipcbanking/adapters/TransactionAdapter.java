package com.example.ipcbanking.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipcbanking.R;
import com.example.ipcbanking.models.TransactionItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private Context context;
    private List<TransactionItem> list;
    private String currentAccountNumber;
    private Map<String, String> accountNameMap;

    private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public TransactionAdapter(Context context, List<TransactionItem> list, String currentAccountNumber) {
        this.context = context;
        this.list = list;
        this.currentAccountNumber = currentAccountNumber;
    }

    public void setData(List<TransactionItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public void setAccountNameMap(Map<String, String> map) {
        this.accountNameMap = map;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionItem item = list.get(position);

        String senderName = accountNameMap != null ? accountNameMap.getOrDefault(item.getSenderAccount(), item.getSenderAccount()) : item.getSenderAccount();
        String receiverName = accountNameMap != null ? accountNameMap.getOrDefault(item.getReceiverAccount(), item.getReceiverAccount()) : item.getReceiverAccount();

        holder.tvSender.setText("Sender: " + senderName);
        holder.tvReceiver.setText("Receiver: " + receiverName);

        holder.tvDate.setText(item.getCreatedAt() != null ? sdf.format(item.getCreatedAt()) : "");

        double amount = item.getAmount();
        boolean isSent = currentAccountNumber.equals(item.getSenderAccount());

        holder.tvAmount.setText(
                (isSent ? "- " : "+ ")
                        + String.format(Locale.getDefault(), "%.2f", amount)
                        + " VND"
        );
        holder.tvAmount.setTextColor(
                context.getResources().getColor(isSent ? R.color.red_700 : R.color.green_700, null)
        );
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvReceiver, tvAmount, tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tv_sender);
            tvReceiver = itemView.findViewById(R.id.tv_receiver);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvDate = itemView.findViewById(R.id.tv_date);
        }
    }
}
