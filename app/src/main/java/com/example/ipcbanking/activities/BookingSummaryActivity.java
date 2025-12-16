package com.example.ipcbanking.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.ipcbanking.R;
import com.example.ipcbanking.models.Movie;
import com.example.ipcbanking.models.Showtime;
import com.example.ipcbanking.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

public class BookingSummaryActivity extends AppCompatActivity {

    private static final String TAG = "BookingSummaryActivity";
    private static final long HIGH_VALUE_TRANSACTION_THRESHOLD = 200000;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    private String showtimeId;
    private ArrayList<String> selectedSeats;
    private double totalPrice;

    private Movie mCurrentMovie;
    private Showtime mCurrentShowtime;

    private TextView movieTitle, movieRating, showtimeSummary, formatSummary, auditoriumSummary, seatsSummary, totalPriceSummary;
    private ImageView moviePoster;

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private NotificationHelper notificationHelper;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // Handle permission grant/denial if needed
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_summary);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        showtimeId = getIntent().getStringExtra("SHOWTIME_ID");
        selectedSeats = getIntent().getStringArrayListExtra("SELECTED_SEATS");

        notificationHelper = new NotificationHelper(this);

        initViews();

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        loadBookingSummary();

        Button continueButton = findViewById(R.id.btn_continue);
        continueButton.setOnClickListener(v -> {
            if (mCurrentMovie != null && mCurrentShowtime != null) {
                processBooking();
            }
        });

        setupBiometricPrompt();
        requestNotificationPermission();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
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

        double pricePerSeat = 110500; // Example price
        totalPrice = pricePerSeat * selectedSeats.size();
        DecimalFormat formatter = new DecimalFormat("#,###đ");
        totalPriceSummary.setText(formatter.format(totalPrice));
    }

    private void processBooking() {
        if (totalPrice >= HIGH_VALUE_TRANSACTION_THRESHOLD) {
            biometricPrompt.authenticate(promptInfo);
        } else {
            showOtpDialog();
        }
    }

    private void setupBiometricPrompt() {
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                showOtpDialog();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Authenticate for your movie ticket booking")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Cancel")
                .build();
    }

    private void showOtpDialog() {
        notificationHelper.sendOtpNotification("123456");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_otp_verification, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        EditText etOtp = dialogView.findViewById(R.id.et_otp);
        Button btnConfirmOtp = dialogView.findViewById(R.id.btn_confirm_otp);
        Button btnCancelOtp = dialogView.findViewById(R.id.btn_cancel_otp);

        btnConfirmOtp.setOnClickListener(v -> {
            String otp = etOtp.getText().toString();
            if (otp.equals("123456")) { // Hardcoded OTP
                dialog.dismiss();
                executeBookingTransaction();
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
            }
        });
        btnCancelOtp.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void executeBookingTransaction() {
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        DocumentReference accountRef = db.collection("accounts").document(userId + "_CHECKING");

        db.runTransaction(transaction -> {
            DocumentSnapshot userSnapshot = transaction.get(db.collection("users").document(userId));
            DocumentSnapshot accountSnapshot = transaction.get(accountRef);

            String senderName = userSnapshot.getString("full_name");
            String senderAccount = accountSnapshot.getString("account_number");

            Double currentBalance = accountSnapshot.getDouble("balance");
            if (currentBalance == null || currentBalance < totalPrice) {
                throw new FirebaseFirestoreException("Insufficient balance.", FirebaseFirestoreException.Code.ABORTED);
            }

            transaction.update(accountRef, "balance", currentBalance - totalPrice);

            DocumentReference bookingRef = db.collection("movie_bookings").document();
            Map<String, Object> bookingData = new HashMap<>();
            bookingData.put("userId", userId);
            bookingData.put("showtimeId", showtimeId);
            bookingData.put("movieId", mCurrentMovie.getMovieId());
            bookingData.put("movieTitle", mCurrentMovie.getTitle());
            bookingData.put("cinemaName", mCurrentShowtime.getCinemaName());
            bookingData.put("selectedSeats", selectedSeats);
            bookingData.put("totalPrice", totalPrice);
            bookingData.put("bookingDate", FieldValue.serverTimestamp());
            transaction.set(bookingRef, bookingData);

            DocumentReference transactionRef = db.collection("transactions").document();
            Map<String, Object> txData = new HashMap<>();
            txData.put("type", "MOVIE_TICKET");
            txData.put("amount", totalPrice);
            txData.put("message", "Thanh toán vé xem phim " + mCurrentMovie.getTitle());
            txData.put("status", "SUCCESS");
            txData.put("created_at", FieldValue.serverTimestamp());
            txData.put("sender_account", senderAccount);
            txData.put("sender_name", senderName);
            txData.put("receiver_name", mCurrentShowtime.getCinemaName());
            txData.put("counterparty_bank", "Cinema");
            transaction.set(transactionRef, txData);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Booking Confirmed!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(BookingSummaryActivity.this, MovieSearchActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Booking transaction failed", e);
            Toast.makeText(this, "Booking failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
