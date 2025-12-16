package com.example.ipcbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.ipcbanking.R;
import com.example.ipcbanking.models.Movie;
import com.google.firebase.firestore.FirebaseFirestore;

public class MovieDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String movieId;
    private String cinemaId;
    private String movieTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        movieId = getIntent().getStringExtra("MOVIE_ID");
        cinemaId = getIntent().getStringExtra("CINEMA_ID");

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        loadMovieDetails();

        Button buyTicketButton = findViewById(R.id.btn_buy_ticket);
        buyTicketButton.setOnClickListener(v -> {
            Intent intent = new Intent(MovieDetailActivity.this, ShowtimeSelectionActivity.class);
            intent.putExtra("MOVIE_ID", movieId);
            intent.putExtra("MOVIE_TITLE", movieTitle);
            intent.putExtra("CINEMA_ID", cinemaId);
            startActivity(intent);
        });
    }

    private void loadMovieDetails() {
        ImageView poster = findViewById(R.id.iv_movie_poster_detail);
        TextView title = findViewById(R.id.tv_movie_title_detail);
        TextView genre = findViewById(R.id.tv_movie_genre);
        // Add other TextViews for details

        db.collection("movies").document(movieId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Movie movie = documentSnapshot.toObject(Movie.class);
                if (movie != null) {
                    movieTitle = movie.getTitle(); // Save the title
                    title.setText(movieTitle);
                    genre.setText(movie.getGenre());
                    Glide.with(this).load(movie.getPosterUrl()).into(poster);
                    // Set other details
                }
            }
        });
    }
}
