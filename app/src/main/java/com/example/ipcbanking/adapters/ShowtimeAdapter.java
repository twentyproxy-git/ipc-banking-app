package com.example.ipcbanking.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ipcbanking.R;
import com.example.ipcbanking.models.Showtime;
import java.util.List;

public class ShowtimeAdapter extends RecyclerView.Adapter<ShowtimeAdapter.ShowtimeViewHolder> {

    private final List<Showtime> showtimeList;
    private final OnShowtimeClickListener listener;

    public interface OnShowtimeClickListener {
        void onShowtimeClick(Showtime showtime);
    }

    public ShowtimeAdapter(List<Showtime> showtimeList, OnShowtimeClickListener listener) {
        this.showtimeList = showtimeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ShowtimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_showtime, parent, false);
        return new ShowtimeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShowtimeViewHolder holder, int position) {
        Showtime showtime = showtimeList.get(position);
        holder.bind(showtime, listener);
    }

    @Override
    public int getItemCount() {
        return showtimeList.size();
    }

    static class ShowtimeViewHolder extends RecyclerView.ViewHolder {
        private final TextView cinemaName;
        private final TextView format;
        private final TextView time;
        private final TextView availability;

        public ShowtimeViewHolder(@NonNull View itemView) {
            super(itemView);
            cinemaName = itemView.findViewById(R.id.tv_cinema_name_showtime);
            format = itemView.findViewById(R.id.tv_format_showtime);
            time = itemView.findViewById(R.id.tv_time_showtime);
            availability = itemView.findViewById(R.id.tv_availability_showtime);
        }

        public void bind(final Showtime showtime, final OnShowtimeClickListener listener) {
            cinemaName.setText(showtime.getCinemaName());
            format.setText(showtime.getFormat());
            time.setText(showtime.getTime());
            availability.setText(showtime.getAvailability());
            itemView.setOnClickListener(v -> listener.onShowtimeClick(showtime));
        }
    }
}
