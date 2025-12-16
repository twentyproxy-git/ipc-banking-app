package com.example.ipcbanking.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.ipcbanking.R;
import com.example.ipcbanking.models.Movie;
import com.example.ipcbanking.models.Showtime;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class BookingSummaryActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String showtimeId;
    private ArrayList<String> selectedSeats;

    // Member variables to hold data
    private Movie mCurrentMovie;
    private Showtime mCurrentShowtime;

    // Views
    private TextView movieTitle, movieRating, showtimeSummary, formatSummary, auditoriumSummary, seatsSummary, totalPriceSummary;
    private ImageView moviePoster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_summary);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        showtimeId = getIntent().getStringExtra("SHOWTIME_ID");
        selectedSeats = getIntent().getStringArrayListExtra("SELECTED_SEATS");

        initViews();

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        loadBookingSummary();

        Button continueButton = findViewById(R.id.btn_continue);
        continueButton.setOnClickListener(v -> {
            if (mCurrentMovie != null && mCurrentShowtime != null) {
                showConfirmationDialog();
            }
        });
    }

    private void initViews() {
        movieTitle = findViewById(R.id.tv_movie_title_summary);
        movieRating = findViewById(R.id.tv_movie_rating_summary);
        showtimeSummary = findViewById(R.id.tv_showtime_summary);
        formatSummary = findViewById(R.id.tv_format_summary);
        auditoriumSummary = findViewById(R.id.tv_auditorium_summary);
        seatsSummary = findViewById(R.id.tv_seats);
        totalPriceSummary = findViewById(R.id.tv_total_price_summary);
        moviePoster = findViewById(R.id.iv_movie_poster_summary);
    }

    private void loadBookingSummary() {
        if (showtimeId == null) return;

        db.collection("showtimes").document(showtimeId).get().addOnSuccessListener(showtimeDoc -> {
            if (showtimeDoc.exists()) {
                mCurrentShowtime = showtimeDoc.toObject(Showtime.class);
                if (mCurrentShowtime != null) {
                    db.collection("movies").document(mCurrentShowtime.getMovieId()).get().addOnSuccessListener(movieDoc -> {
                        if (movieDoc.exists()) {
                            mCurrentMovie = movieDoc.toObject(Movie.class);
                            if (mCurrentMovie != null) {
                                populateSummaryViews();
                            }
                        }
                    });
                }
            }
        });
    }

    private void populateSummaryViews() {
        movieTitle.setText(mCurrentMovie.getTitle());
        movieRating.setText(mCurrentMovie.getRating() + " Phim được phổ biến đến người xem từ đủ " + mCurrentMovie.getRating().replaceAll("[^0-9]", "") + " tuổi trở lên");
        
        // Format date and time
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputDateFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
            Date date = inputFormat.parse(mCurrentShowtime.getDateTime());
            if (date != null) {
                showtimeSummary.setText(mCurrentShowtime.getTime() + " | " + outputDateFormat.format(date));
            }
        } catch (ParseException e) {
            showtimeSummary.setText(mCurrentShowtime.getDateTime());
        }

        formatSummary.setText(mCurrentShowtime.getFormat());
        auditoriumSummary.setText(mCurrentShowtime.getAuditorium());
        seatsSummary.setText(String.join(", ", selectedSeats));

        Glide.with(this).load(mCurrentMovie.getPosterUrl()).into(moviePoster);

        // Calculate and display price
        double pricePerSeat = 110500; // Example price
        double totalPrice = pricePerSeat * selectedSeats.size();
        DecimalFormat formatter = new DecimalFormat("#,###đ");
        totalPriceSummary.setText(formatter.format(totalPrice));
    }


    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_confirm_booking, null);
        builder.setView(dialogView);

        // --- Populate dialog views ---
        TextView dialogMovieTitle = dialogView.findViewById(R.id.tv_dialog_movie_title);
        TextView dialogCinemaName = dialogView.findViewById(R.id.tv_cinema_name_confirm);
        TextView dialogShowtime = dialogView.findViewById(R.id.tv_showtime_confirm);
        TextView dialogDate = dialogView.findViewById(R.id.tv_date_confirm);

        dialogMovieTitle.setText("Bạn đang đặt vé xem phim " + mCurrentMovie.getTitle() + ":");
        dialogCinemaName.setText(mCurrentShowtime.getCinemaName());
        dialogShowtime.setText(mCurrentShowtime.getTime());
        
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputDateFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
            Date date = inputFormat.parse(mCurrentShowtime.getDateTime());
            if (date != null) {
                dialogDate.setText(outputDateFormat.format(date));
            }
        } catch (ParseException e) {
            dialogDate.setText(mCurrentShowtime.getDateTime());
        }
        // --- End of population ---

        final AlertDialog dialog = builder.create();

        Button confirmButton = dialogView.findViewById(R.id.btn_confirm_purchase);
        confirmButton.setOnClickListener(v -> {
            Toast.makeText(this, "Booking Confirmed!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();

            Intent intent = new Intent(BookingSummaryActivity.this, MovieSearchActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        dialog.show();
    }
}
