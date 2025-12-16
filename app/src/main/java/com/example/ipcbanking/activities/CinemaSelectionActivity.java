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
import com.example.ipcbanking.adapters.CinemaAdapter;
import com.example.ipcbanking.adapters.CinemaBrandAdapter;
import com.example.ipcbanking.models.Cinema;
import com.example.ipcbanking.models.CinemaBrand;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CinemaSelectionActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String movieId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cinema_selection);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        movieId = getIntent().getStringExtra("MOVIE_ID");

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupCinemaBrands();
        setupCinemas();

    }

    private void setupCinemaBrands() {
        RecyclerView brandRecyclerView = findViewById(R.id.rv_cinema_brands);
        brandRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        List<CinemaBrand> brandList = new ArrayList<>();
        CinemaBrandAdapter brandAdapter = new CinemaBrandAdapter(brandList);
        brandRecyclerView.setAdapter(brandAdapter);

        db.collection("cinema_brands").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                CinemaBrand brand = document.toObject(CinemaBrand.class);
                brandList.add(brand);
            }
            brandAdapter.notifyDataSetChanged();
        });
    }

    private void setupCinemas() {
        RecyclerView cinemaRecyclerView = findViewById(R.id.rv_cinemas);
        cinemaRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Cinema> cinemaList = new ArrayList<>();
        CinemaAdapter cinemaAdapter = new CinemaAdapter(cinemaList, cinema -> {
            Intent intent = new Intent(CinemaSelectionActivity.this, MovieDetailActivity.class);
            intent.putExtra("MOVIE_ID", movieId);
            intent.putExtra("CINEMA_ID", cinema.getCinemaId());
            startActivity(intent);
        });
        cinemaRecyclerView.setAdapter(cinemaAdapter);

        db.collection("cinemas").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Cinema cinema = document.toObject(Cinema.class);
                cinemaList.add(cinema);
            }
            cinemaAdapter.notifyDataSetChanged();
        });
    }
}
