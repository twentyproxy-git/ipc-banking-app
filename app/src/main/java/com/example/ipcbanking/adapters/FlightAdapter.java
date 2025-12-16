package com.example.ipcbanking.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ipcbanking.R;
import com.example.ipcbanking.models.Flight;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class FlightAdapter extends RecyclerView.Adapter<FlightAdapter.FlightViewHolder> {

    private List<Flight> flightList;
    private Context context;
    private OnFlightListener onFlightListener;

    public FlightAdapter(List<Flight> flightList, Context context, OnFlightListener onFlightListener) {
        this.flightList = flightList;
        this.context = context;
        this.onFlightListener = onFlightListener;
    }

    @NonNull
    @Override
    public FlightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_flight_ticket, parent, false);
        return new FlightViewHolder(view, onFlightListener);
    }

    @Override
    public void onBindViewHolder(@NonNull FlightViewHolder holder, int position) {
        Flight flight = flightList.get(position);
        holder.bind(flight);
    }

    @Override
    public int getItemCount() {
        return flightList.size();
    }

    public interface OnFlightListener {
        void onFlightClick(int position);
    }

    public class FlightViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView imgAirlineLogo;
        TextView tvPromoBadge, tvDepTime, tvDepCode, tvDuration, tvArrTime, tvArrCode, tvPrice, tvAirlineName, tvClassName;
        OnFlightListener onFlightListener;

        public FlightViewHolder(@NonNull View itemView, OnFlightListener onFlightListener) {
            super(itemView);
            imgAirlineLogo = itemView.findViewById(R.id.img_airline_logo);
            tvPromoBadge = itemView.findViewById(R.id.tv_promo_badge);
            tvDepTime = itemView.findViewById(R.id.tv_dep_time);
            tvDepCode = itemView.findViewById(R.id.tv_dep_code);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvArrTime = itemView.findViewById(R.id.tv_arr_time);
            tvArrCode = itemView.findViewById(R.id.tv_arr_code);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvAirlineName = itemView.findViewById(R.id.tv_airline_name);
            tvClassName = itemView.findViewById(R.id.tv_class_name);
            this.onFlightListener = onFlightListener;

            itemView.setOnClickListener(this);
        }

        void bind(Flight flight) {
            Glide.with(context).load(flight.getAirlineLogoUrl()).into(imgAirlineLogo);

            if (flight.getPromoBadge() != null && !flight.getPromoBadge().isEmpty()) {
                tvPromoBadge.setText(flight.getPromoBadge());
                tvPromoBadge.setVisibility(View.VISIBLE);
            } else {
                tvPromoBadge.setVisibility(View.GONE);
            }

            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            try {
                Date depDate = inputFormat.parse(flight.getDepartureTime());
                Date arrDate = inputFormat.parse(flight.getArrivalTime());

                tvDepTime.setText(timeFormat.format(depDate));
                tvArrTime.setText(timeFormat.format(arrDate));

                long diffInMillis = arrDate.getTime() - depDate.getTime();
                long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60;
                tvDuration.setText(String.format(Locale.getDefault(), "%dh %02dm", hours, minutes));

            } catch (ParseException e) {
                e.printStackTrace();
                tvDepTime.setText("N/A");
                tvArrTime.setText("N/A");
                tvDuration.setText("N/A");
            }

            tvDepCode.setText(flight.getDepartureAirport());
            tvArrCode.setText(flight.getArrivalAirport());
            tvAirlineName.setText(flight.getAirlineName());

            if (flight.getClasses() != null && flight.getClasses().containsKey("economy")) {
                Flight.FlightClass economyClass = flight.getClasses().get("economy");
                tvPrice.setText(String.format(Locale.getDefault(), "%,.0fđ", economyClass.getPrice()));
                tvClassName.setText(economyClass.getName());
            } else {
                tvPrice.setText("N/A");
                tvClassName.setText("N/A");
            }
        }

        @Override
        public void onClick(View v) {
            onFlightListener.onFlightClick(getAdapterPosition());
        }
    }
}
