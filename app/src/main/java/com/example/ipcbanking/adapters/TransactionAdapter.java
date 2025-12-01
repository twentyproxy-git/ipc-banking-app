package com.example.ipcbanking.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipcbanking.R;
import com.example.ipcbanking.models.TransactionItem;
import com.example.ipcbanking.viewholders.TransactionViewHolder;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionViewHolder> {

    private Context context;
    private List<TransactionItem> list;
    private String currentAccountNumber;
    private Map<String, String> accountNameMap;

    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

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
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionItem item = list.get(position);

        String senderName = accountNameMap != null
                ? accountNameMap.getOrDefault(item.getSenderAccount(), item.getSenderAccount())
                : item.getSenderAccount();

        String receiverName = accountNameMap != null
                ? accountNameMap.getOrDefault(item.getReceiverAccount(), item.getReceiverAccount())
                : item.getReceiverAccount();

        holder.tvSender.setText("Sender: " + senderName);
        holder.tvReceiver.setText("Receiver: " + receiverName);

        if (item.getCreatedAt() != null) {
            holder.tvDate.setText(sdf.format(item.getCreatedAt()));
        }

        double amount = item.getAmount();
        boolean isSent = currentAccountNumber.equals(item.getSenderAccount());

        holder.tvAmount.setText(
                (isSent ? "- " : "+ ")
                        + String.format(Locale.getDefault(), "%.2f", amount)
                        + " VND"
        );

        holder.tvAmount.setTextColor(
                context.getResources().getColor(
                        isSent ? R.color.red_700 : R.color.green_700,
                        null
                )
        );
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }
}
