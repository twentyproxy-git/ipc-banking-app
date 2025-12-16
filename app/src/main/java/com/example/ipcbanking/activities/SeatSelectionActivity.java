package com.example.ipcbanking.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ipcbanking.R;
import com.example.ipcbanking.models.Showtime;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class SeatSelectionActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String showtimeId;
    private ArrayList<String> selectedSeats = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_selection);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        showtimeId = getIntent().getStringExtra("SHOWTIME_ID");

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        loadSeats();

        Button continueButton = findViewById(R.id.btn_continue);
        continueButton.setOnClickListener(v -> {
            Intent intent = new Intent(SeatSelectionActivity.this, BookingSummaryActivity.class);
            intent.putExtra("SHOWTIME_ID", showtimeId);
            intent.putStringArrayListExtra("SELECTED_SEATS", selectedSeats);
            startActivity(intent);
        });
    }

    private void loadSeats() {
        GridLayout gridLayout = findViewById(R.id.grid_seats);
        // In a real app, you would load seat status from your database
        for (int i = 0; i < 144; i++) {
            TextView seat = (TextView) getLayoutInflater().inflate(R.layout.item_seat, gridLayout, false);
            String seatNumber = "S" + (i + 1);
            seat.setText(seatNumber);
            seat.setOnClickListener(v -> {
                if (selectedSeats.contains(seatNumber)) {
                    selectedSeats.remove(seatNumber);
                    seat.setBackgroundColor(Color.LTGRAY);
                } else {
                    selectedSeats.add(seatNumber);
                    seat.setBackgroundColor(Color.BLUE);
                }
            });
            gridLayout.addView(seat);
        }
    }
}
