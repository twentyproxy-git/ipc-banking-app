package com.example.ipcbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

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
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_search);

        editText = findViewById(R.id.et_search);

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

        //Search by Keyword
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchMovies(s.toString().trim());
            }
        });

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
                movie.setMovieId(document.getId());
                movieList.add(movie);
            }
            movieAdapter.notifyDataSetChanged();
        });
    }

    private void searchMovies(String keyword) {
        RecyclerView featuredMoviesRecyclerView = findViewById(R.id.rv_featured_movies);
        MovieAdapter movieAdapter = (MovieAdapter) featuredMoviesRecyclerView.getAdapter();

        if (movieAdapter == null) return;

        List<Movie> resultList = new ArrayList<>();

        if (keyword == null || keyword.trim().isEmpty()) {
            // Nếu không nhập gì → load lại toàn bộ phim
            setupFeaturedMovies();
            return;
        }

        db.collection("movies")
                .orderBy("title")
                .startAt(keyword)
                .endAt(keyword + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Movie movie = document.toObject(Movie.class);
                        movie.setMovieId(document.getId());
                        resultList.add(movie);
                    }
                    movieAdapter.updateData(resultList);
                });
    }

}
