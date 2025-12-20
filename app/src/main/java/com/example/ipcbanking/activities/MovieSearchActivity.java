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
import androidx.viewpager2.widget.ViewPager2;
import com.example.ipcbanking.R;
import com.example.ipcbanking.adapters.MovieAdapter;
import com.example.ipcbanking.adapters.PromotionAdapter;
import com.example.ipcbanking.models.Movie;
import com.example.ipcbanking.models.PromotionItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MovieSearchActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_search);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupPromotions();
        setupFeaturedMovies();

    }

    private void setupPromotions() {
        //ViewPager2 promotionViewPager = findViewById(R.id.view_pager_promotions);
        List<PromotionItem> promotionList = new ArrayList<>();
        promotionList.add(new PromotionItem(R.drawable.ic_promotion_01));
        promotionList.add(new PromotionItem(R.drawable.ic_promotion_02));
        promotionList.add(new PromotionItem(R.drawable.ic_promotion_03));
        PromotionAdapter promotionAdapter = new PromotionAdapter(this, promotionList);
        //promotionViewPager.setAdapter(promotionAdapter);
    }

    private void setupFeaturedMovies() {
        RecyclerView featuredMoviesRecyclerView = findViewById(R.id.rv_featured_movies);
        featuredMoviesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        List<Movie> movieList = new ArrayList<>();
        MovieAdapter movieAdapter = new MovieAdapter(movieList, movie -> {
            Intent intent = new Intent(MovieSearchActivity.this, CinemaSelectionActivity.class);
            intent.putExtra("MOVIE_ID", movie.getMovieId());
            startActivity(intent);
        });
        featuredMoviesRecyclerView.setAdapter(movieAdapter);

        db.collection("movies").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Movie movie = document.toObject(Movie.class);
                movieList.add(movie);
            }
            movieAdapter.notifyDataSetChanged();
        });
    }

    private void searchMovies(String name) {

    }
}
