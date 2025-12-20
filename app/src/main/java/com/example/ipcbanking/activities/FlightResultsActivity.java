package com.example.ipcbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipcbanking.R;
import com.example.ipcbanking.adapters.FlightAdapter;
import com.example.ipcbanking.models.Flight;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FlightResultsActivity extends AppCompatActivity implements FlightAdapter.OnFlightListener {

    private static final String TAG = "FlightResultsActivity";
    private RecyclerView rvFlightList;
    private FlightAdapter flightAdapter;
    private List<Flight> flightList;
    private FirebaseFirestore db;

    private TextView tvFlightRoute, tvFlightDatePassenger, tvFlightCount;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_results);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Init Views
        rvFlightList = findViewById(R.id.rv_flight_list);
        tvFlightRoute = findViewById(R.id.tv_flight_route);
        tvFlightDatePassenger = findViewById(R.id.tv_flight_date_passenger);
        tvFlightCount = findViewById(R.id.tv_flight_count);
        btnBack = findViewById(R.id.btn_back);

        rvFlightList.setLayoutManager(new LinearLayoutManager(this));

        flightList = new ArrayList<>();
        flightAdapter = new FlightAdapter(flightList, this, this);
        rvFlightList.setAdapter(flightAdapter);

        db = FirebaseFirestore.getInstance();

        // Get data from Intent
        Intent intent = getIntent();
        String depAirportCode = intent.getStringExtra("DEPARTURE_AIRPORT_CODE");
        String arrAirportCode = intent.getStringExtra("ARRIVAL_AIRPORT_CODE");
        String depCityName = intent.getStringExtra("DEPARTURE_CITY_NAME");
        String arrCityName = intent.getStringExtra("ARRIVAL_CITY_NAME");
        long depDateMillis = intent.getLongExtra("DEPARTURE_DATE_MILLIS", 0);
        int passengerCount = intent.getIntExtra("PASSENGER_COUNT", 1);

        updateHeaderUI(depCityName, arrCityName, depDateMillis, passengerCount);
        fetchAndFilterFlights(depAirportCode, arrAirportCode, depDateMillis);

        btnBack.setOnClickListener(v -> finish());
    }

    private void updateHeaderUI(String fromCity, String toCity, long dateMillis, int passengers) {
        tvFlightRoute.setText(String.format("%s ✈ %s", fromCity, toCity));

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd/MM/yyyy", new Locale("vi", "VN"));
        String dateStr = sdf.format(new Date(dateMillis));
        tvFlightDatePassenger.setText(String.format("%s • %d passenger", dateStr, passengers));
    }

    private void fetchAndFilterFlights(String depCode, String arrCode, long searchDateMillis) {
        Query query = db.collection("flights")
                .whereEqualTo("departureAirport", depCode)
                .whereEqualTo("arrivalAirport", arrCode);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                flightList.clear();
                List<Flight> allMatchingFlights = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    allMatchingFlights.add(document.toObject(Flight.class));
                }

                // Filter by date in Java
                Calendar searchCal = Calendar.getInstance();
                searchCal.setTimeInMillis(searchDateMillis);

                flightList.addAll(allMatchingFlights.stream().filter(flight -> {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                        Date flightDate = sdf.parse(flight.getDepartureTime());
                        Calendar flightCal = Calendar.getInstance();
                        flightCal.setTime(flightDate);

                        return searchCal.get(Calendar.YEAR) == flightCal.get(Calendar.YEAR) &&
                               searchCal.get(Calendar.MONTH) == flightCal.get(Calendar.MONTH) &&
                               searchCal.get(Calendar.DAY_OF_MONTH) == flightCal.get(Calendar.DAY_OF_MONTH);
                    } catch (Exception e) {
                        Log.e(TAG, "Date parsing error: ", e);
                        return false;
                    }
                }).collect(Collectors.toList()));

                flightAdapter.notifyDataSetChanged();
                tvFlightCount.setText(String.format("✓ Found %d flights", flightList.size()));

            } else {
                Log.w(TAG, "Error getting documents.", task.getException());
                Toast.makeText(this, "Error while searching for flights: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onFlightClick(int position) {
        Flight flight = flightList.get(position);
        Intent intent = new Intent(this, SelectFlightClassActivity.class);
        intent.putExtra("flightId", flight.getFlightId());
        startActivity(intent);
    }
}
