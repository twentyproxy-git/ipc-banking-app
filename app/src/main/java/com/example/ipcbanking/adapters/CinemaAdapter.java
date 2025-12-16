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
import com.example.ipcbanking.models.Cinema;
import java.util.List;

public class CinemaAdapter extends RecyclerView.Adapter<CinemaAdapter.CinemaViewHolder> {

    private final List<Cinema> cinemaList;
    private final OnCinemaClickListener listener;

    public interface OnCinemaClickListener {
        void onCinemaClick(Cinema cinema);
    }

    public CinemaAdapter(List<Cinema> cinemaList, OnCinemaClickListener listener) {
        this.cinemaList = cinemaList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CinemaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cinema, parent, false);
        return new CinemaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CinemaViewHolder holder, int position) {
        Cinema cinema = cinemaList.get(position);
        holder.bind(cinema, listener);
    }

    @Override
    public int getItemCount() {
        return cinemaList.size();
    }

    static class CinemaViewHolder extends RecyclerView.ViewHolder {
        private final ImageView cinemaLogo;
        private final TextView cinemaName;
        private final TextView cinemaLocation;
        private final TextView cinemaAddress;

        public CinemaViewHolder(@NonNull View itemView) {
            super(itemView);
            cinemaLogo = itemView.findViewById(R.id.iv_cinema_logo);
            cinemaName = itemView.findViewById(R.id.tv_cinema_name);
            cinemaLocation = itemView.findViewById(R.id.tv_cinema_location);
            cinemaAddress = itemView.findViewById(R.id.tv_cinema_address);
        }

        public void bind(final Cinema cinema, final OnCinemaClickListener listener) {
            cinemaName.setText(cinema.getName());
            cinemaLocation.setText(String.format("%s • %.1fkm", cinema.getLocation(), cinema.getDistance()));
            cinemaAddress.setText(cinema.getAddress());
            Glide.with(itemView.getContext())
                    .load(cinema.getBrandLogoUrl())
                    .into(cinemaLogo);
            itemView.setOnClickListener(v -> listener.onCinemaClick(cinema));
        }
    }
}
