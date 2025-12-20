package com.example.ipcbanking.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ipcbanking.R;
import com.example.ipcbanking.models.Airport;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FlightSearchActivity extends AppCompatActivity {

    private static final String TAG = "FlightSearchActivity";

    private AutoCompleteTextView actvFromCountry, actvFromCapital, actvToCountry, actvToCapital;
    private TextView tvDepartureDate, tvReturnDate, tvPassengers;
    private Button btnSearchFlight;
    private Toolbar toolbar;
    private int passengerCount = 1;
    final Calendar myCalendar = Calendar.getInstance();

    private FirebaseFirestore db;
    private List<Airport> airportList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_search);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Views
        toolbar = findViewById(R.id.toolbar);
        actvFromCountry = findViewById(R.id.actv_from_country);
        actvFromCapital = findViewById(R.id.actv_from_capital);
        actvToCountry = findViewById(R.id.actv_to_country);
        actvToCapital = findViewById(R.id.actv_to_capital);
        tvDepartureDate = findViewById(R.id.tv_departure_date);
        tvReturnDate = findViewById(R.id.tv_return_date);
        tvPassengers = findViewById(R.id.tv_passengers);
        btnSearchFlight = findViewById(R.id.btn_search_flight);

        db = FirebaseFirestore.getInstance();

        setupToolbar();
        // Set default date to match the XML and initialize the Calendar object
        myCalendar.set(Calendar.YEAR, 2025);
        myCalendar.set(Calendar.MONTH, Calendar.DECEMBER); // Note: Calendar.DECEMBER is 11
        myCalendar.set(Calendar.DAY_OF_MONTH, 15);
        updateLabel(tvDepartureDate); // Update the TextView to be in sync

        setupDatePickers();
        setupPassengerControls();
        setupRoundTripSwitch();
        fetchAirports();

        btnSearchFlight.setOnClickListener(v -> searchFlights());
        findViewById(R.id.fab_swap).setOnClickListener(v -> swapLocations());
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void fetchAirports() {
        db.collection("airports").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                airportList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Airport airport = document.toObject(Airport.class);
                    airportList.add(airport);
                }
                setupLocationDropdowns();
            } else {
                Log.w(TAG, "Error getting documents: ", task.getException());
                Toast.makeText(this, "Failed to load airport data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLocationDropdowns() {
        // Get unique countries
        List<String> countries = airportList.stream().map(Airport::getCountry).distinct().collect(Collectors.toList());
        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, countries);

        actvFromCountry.setAdapter(countryAdapter);
        actvToCountry.setAdapter(countryAdapter);

        // Listener for From Country
        actvFromCountry.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCountry = (String) parent.getItemAtPosition(position);
            updateCityDropdown(selectedCountry, actvFromCapital);
        });

        // Listener for To Country
        actvToCountry.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCountry = (String) parent.getItemAtPosition(position);
            updateCityDropdown(selectedCountry, actvToCapital);
        });
    }

    private void updateCityDropdown(String country, AutoCompleteTextView cityDropdown) {
        List<String> cities = airportList.stream()
                .filter(airport -> airport.getCountry().equals(country))
                .map(airport -> airport.getCity() + " (" + airport.getAirportCode() + ")")
                .collect(Collectors.toList());

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cities);
        cityDropdown.setAdapter(cityAdapter);
        cityDropdown.setText("", false); // Clear previous selection
    }

    private void setupDatePickers() {
        findViewById(R.id.layout_departure_date).setOnClickListener(v -> showDatePickerDialog(tvDepartureDate));
        findViewById(R.id.layout_return_date).setOnClickListener(v -> showDatePickerDialog(tvReturnDate));
    }

    private void setupPassengerControls() {
        findViewById(R.id.btn_increase_passenger).setOnClickListener(v -> updatePassengerCount(1));
        findViewById(R.id.btn_decrease_passenger).setOnClickListener(v -> updatePassengerCount(-1));
    }

    private void setupRoundTripSwitch() {
        SwitchMaterial roundTripSwitch = findViewById(R.id.switch_round_trip);
        LinearLayout returnDateLayout = findViewById(R.id.layout_return_date);
        roundTripSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            returnDateLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
    }

    private void showDatePickerDialog(TextView dateTextView) {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, month);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel(dateTextView);
        };

        new DatePickerDialog(this, dateSetListener, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateLabel(TextView dateTextView) {
        String myFormat = "EEE, dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, new Locale("vi", "VN"));
        dateTextView.setText(sdf.format(myCalendar.getTime()));
    }

    private void swapLocations() {
        String fromCountry = actvFromCountry.getText().toString();
        String fromCapital = actvFromCapital.getText().toString();
        actvFromCountry.setText(actvToCountry.getText().toString(), false);
        actvFromCapital.setText(actvToCapital.getText().toString(), false);
        actvToCountry.setText(fromCountry, false);
        actvToCapital.setText(fromCapital, false);

        updateCityDropdown(actvFromCountry.getText().toString(), actvFromCapital);
        updateCityDropdown(actvToCountry.getText().toString(), actvToCapital);
    }

    private void updatePassengerCount(int change) {
        if (passengerCount + change == 1) {
            passengerCount += change;
            tvPassengers.setText(passengerCount + " Passenger");
        }

        if (passengerCount + change > 1) {
            passengerCount += change;
            tvPassengers.setText(passengerCount + " Passengers");
        }
    }

    private void searchFlights() {
        String fromStr = actvFromCapital.getText().toString();
        String toStr = actvToCapital.getText().toString();
        String dateStr = tvDepartureDate.getText().toString();

        if (fromStr.isEmpty() || toStr.isEmpty() || dateStr.contains("Select")) {
            Toast.makeText(this, "Please fill full information.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Extract airport codes: "Hồ Chí Minh (SGN)" -> "SGN"
        String fromAirportCode = fromStr.substring(fromStr.indexOf("(") + 1, fromStr.indexOf(")"));
        String toAirportCode = toStr.substring(toStr.indexOf("(") + 1, toStr.indexOf(")"));

        // Extract city names for display
        String fromCityName = fromStr.substring(0, fromStr.indexOf(" ("));
        String toCityName = toStr.substring(0, toStr.indexOf(" ("));

        Intent intent = new Intent(this, FlightResultsActivity.class);
        intent.putExtra("DEPARTURE_AIRPORT_CODE", fromAirportCode);
        intent.putExtra("ARRIVAL_AIRPORT_CODE", toAirportCode);
        intent.putExtra("DEPARTURE_CITY_NAME", fromCityName);
        intent.putExtra("ARRIVAL_CITY_NAME", toCityName);
        intent.putExtra("DEPARTURE_DATE_MILLIS", myCalendar.getTimeInMillis());
        intent.putExtra("PASSENGER_COUNT", passengerCount);

        startActivity(intent);
    }
}
