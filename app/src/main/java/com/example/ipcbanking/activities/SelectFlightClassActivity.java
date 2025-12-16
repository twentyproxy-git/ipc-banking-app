package com.example.ipcbanking.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.ipcbanking.R;
import com.example.ipcbanking.dialogs.ConfirmFlightBookingDialog;
import com.example.ipcbanking.models.Flight;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SelectFlightClassActivity extends AppCompatActivity {

    private static final String TAG = "SelectFlightClass";
    private FirebaseFirestore db;
    private Flight currentFlight;
    private String selectedClassKey = "economy"; // Default selection

    private ImageView imgLogo, btnBack;
    private TextView tvTimeDep, tvTimeArr, tvMid, tvTotalPrice, tvDepCode, tvArrCode;
    private LinearLayout ticketClassesContainer;
    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_flight_class);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        imgLogo = findViewById(R.id.img_logo);
        btnBack = findViewById(R.id.btn_back);
        tvTimeDep = findViewById(R.id.tv_time_dep);
        tvDepCode = findViewById(R.id.tv_dep_code);
        tvTimeArr = findViewById(R.id.tv_time_arr);
        tvArrCode = findViewById(R.id.tv_arr_code);
        tvMid = findViewById(R.id.tv_mid);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        ticketClassesContainer = findViewById(R.id.ticket_classes_container);
        btnContinue = findViewById(R.id.btn_continue);

        db = FirebaseFirestore.getInstance();

        String flightId = getIntent().getStringExtra("flightId");
        if (flightId != null) {
            fetchFlightDetails(flightId);
        }

        btnBack.setOnClickListener(v -> finish());
        btnContinue.setOnClickListener(v -> {
            if (currentFlight != null && currentFlight.getClasses().containsKey(selectedClassKey)) {
                ConfirmFlightBookingDialog dialog = ConfirmFlightBookingDialog.newInstance(currentFlight, selectedClassKey);
                dialog.show(getSupportFragmentManager(), "ConfirmFlightBookingDialog");
            }
        });
    }

    private void fetchFlightDetails(String flightId) {
        db.collection("flights").document(flightId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            currentFlight = document.toObject(Flight.class);
                            if (currentFlight != null) {
                                currentFlight.setFlightId(document.getId());
                                fetchAirlineDetails(currentFlight.getAirlineId());
                            }
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                });
    }

    private void fetchAirlineDetails(String airlineId) {
        db.collection("airlines").document(airlineId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            currentFlight.setAirlineName(document.getString("name"));
                            currentFlight.setAirlineLogoUrl(document.getString("logoUrl"));
                            updateUI();
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                });
    }

    private void updateUI() {
        if (currentFlight == null) return;

        Glide.with(this).load(currentFlight.getAirlineLogoUrl()).into(imgLogo);

        tvDepCode.setText(currentFlight.getDepartureAirport());
        tvArrCode.setText(currentFlight.getArrivalAirport());

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        try {
            Date depDate = inputFormat.parse(currentFlight.getDepartureTime());
            Date arrDate = inputFormat.parse(currentFlight.getArrivalTime());

            tvTimeDep.setText(timeFormat.format(depDate));
            tvTimeArr.setText(timeFormat.format(arrDate));

            long diffInMillis = arrDate.getTime() - depDate.getTime();
            long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60;
            String durationStr = String.format(Locale.getDefault(), "%dh %02dm", hours, minutes);

            long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);
            String nightStr = "";
            if (diffInDays > 0) {
                nightStr = String.format(Locale.getDefault(), " (%d đêm)", diffInDays);
            }

            String directFlightStr = currentFlight.isDirectFlight() ? "Bay thẳng" : "";
            tvMid.setText(String.format("%s%s\n──────>\n%s", durationStr, nightStr, directFlightStr));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        updateTicketClassesView();
        updateTotalPrice();
    }

    private void updateTicketClassesView() {
        ticketClassesContainer.removeAllViews();
        if (currentFlight.getClasses() == null) return;

        currentFlight.getClasses().forEach((key, flightClass) -> {
            View view = getLayoutInflater().inflate(R.layout.item_ticket_class, ticketClassesContainer, false);

            TextView tvClassTitle = view.findViewById(R.id.tv_class_title);
            ImageView imgRadio = view.findViewById(R.id.img_radio);
            TextView tvStatus = view.findViewById(R.id.tv_status);

            tvClassTitle.setText(flightClass.getName());

            if (key.equals(selectedClassKey)) {
                view.setBackgroundResource(R.drawable.bg_card_selected);
                imgRadio.setVisibility(View.VISIBLE);
                tvStatus.setVisibility(View.VISIBLE);
            } else {
                view.setBackgroundResource(android.R.color.white);
                imgRadio.setVisibility(View.INVISIBLE);
                tvStatus.setVisibility(View.INVISIBLE);
            }

            view.setOnClickListener(v -> {
                selectedClassKey = key;
                updateTicketClassesView();
                updateTotalPrice();
            });

            ticketClassesContainer.addView(view);
        });
    }

    private void updateTotalPrice() {
        if (currentFlight.getClasses() != null && currentFlight.getClasses().containsKey(selectedClassKey)) {
            Flight.FlightClass flightClass = currentFlight.getClasses().get(selectedClassKey);
            tvTotalPrice.setText(String.format(Locale.getDefault(), "%,.0fđ", flightClass.getPrice()));
        }
    }
}
