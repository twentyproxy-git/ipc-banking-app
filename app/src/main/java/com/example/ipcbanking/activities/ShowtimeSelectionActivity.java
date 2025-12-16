package com.example.ipcbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ipcbanking.R;
import com.example.ipcbanking.adapters.ShowtimeAdapter;
import com.example.ipcbanking.models.Showtime;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ShowtimeSelectionActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String movieId;
    private String cinemaId;
    private String movieTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_showtime_selection);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        movieId = getIntent().getStringExtra("MOVIE_ID");
        cinemaId = getIntent().getStringExtra("CINEMA_ID");
        movieTitle = getIntent().getStringExtra("MOVIE_TITLE");

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(movieTitle);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupShowtimes();
    }

    private void setupShowtimes() {
        RecyclerView recyclerView = findViewById(R.id.rv_cinemas_with_showtimes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Showtime> showtimeList = new ArrayList<>();
        ShowtimeAdapter adapter = new ShowtimeAdapter(showtimeList, showtime -> {
            Intent intent = new Intent(ShowtimeSelectionActivity.this, SeatSelectionActivity.class);
            intent.putExtra("SHOWTIME_ID", showtime.getShowtimeId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        db.collection("showtimes")
                .whereEqualTo("movieId", movieId)
                .whereEqualTo("cinemaId", cinemaId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Showtime showtime = document.toObject(Showtime.class);
                        showtimeList.add(showtime);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
