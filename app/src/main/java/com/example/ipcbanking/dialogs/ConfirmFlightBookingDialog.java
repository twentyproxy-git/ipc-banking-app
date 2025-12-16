package com.example.ipcbanking.dialogs;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.ipcbanking.R;
import com.example.ipcbanking.models.Flight;
import com.example.ipcbanking.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

public class ConfirmFlightBookingDialog extends DialogFragment {

    private static final String TAG = "ConfirmFlightBooking";
    private static final String ARG_FLIGHT_CLASS_KEY = "flight_class_key";
    private static final long HIGH_VALUE_TRANSACTION_THRESHOLD = 1000000;

    private Flight flight;
    private String flightClassKey;
    private Flight.FlightClass selectedClass;

    private TextView tvFlightRoute, tvFlightDetails, tvPassengerDetails, tvTotalPrice, tvAccountBalance;
    private Button btnCancel, btnConfirm;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private NotificationHelper notificationHelper;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // Handle permission grant/denial if needed
            });

    public static ConfirmFlightBookingDialog newInstance(Flight flight, String flightClassKey) {
        ConfirmFlightBookingDialog fragment = new ConfirmFlightBookingDialog();
        Bundle args = new Bundle();
        args.putSerializable("flight", flight);
        args.putString(ARG_FLIGHT_CLASS_KEY, flightClassKey);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        notificationHelper = new NotificationHelper(requireContext());

        if (getArguments() != null) {
            flight = (Flight) getArguments().getSerializable("flight");
            flightClassKey = getArguments().getString(ARG_FLIGHT_CLASS_KEY);
            if (flight != null && flight.getClasses() != null) {
                selectedClass = flight.getClasses().get(flightClassKey);
            }
        }

        setupBiometricPrompt();
        requestNotificationPermission();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_confirm_flight_booking, container, false);
        initViews(view);
        setupDialogData();
        fetchUserAccountBalance();
        btnCancel.setOnClickListener(v -> dismiss());
        btnConfirm.setOnClickListener(v -> processBooking());
        return view;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void initViews(View view) {
        tvFlightRoute = view.findViewById(R.id.tv_flight_route_confirm);
        tvFlightDetails = view.findViewById(R.id.tv_flight_details_confirm);
        tvPassengerDetails = view.findViewById(R.id.tv_passenger_details_confirm);
        tvTotalPrice = view.findViewById(R.id.tv_total_price_confirm);
        tvAccountBalance = view.findViewById(R.id.tv_account_balance_confirm);
        btnCancel = view.findViewById(R.id.btn_cancel_booking_confirm);
        btnConfirm = view.findViewById(R.id.btn_confirm_booking_final);
    }

    private void setupDialogData() {
        if (flight == null || selectedClass == null) return;
        tvFlightRoute.setText(String.format("%s ✈ %s", flight.getDepartureAirport(), flight.getArrivalAirport()));
        tvFlightDetails.setText(String.format("%s • %s", flight.getAirlineName(), flight.getDepartureTime()));
        tvPassengerDetails.setText(String.format("1 hành khách • Hạng %s", selectedClass.getName()));
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvTotalPrice.setText(currencyFormat.format(selectedClass.getPrice()));
    }

    private void fetchUserAccountBalance() {
        if (currentUser == null) return;
        db.collection("accounts")
                .whereEqualTo("owner_id", currentUser.getUid())
                .whereEqualTo("account_type", "CHECKING")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Double balance = queryDocumentSnapshots.getDocuments().get(0).getDouble("balance");
                        if (balance != null) {
                            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                            tvAccountBalance.setText(String.format("Số dư tài khoản: %s", currencyFormat.format(balance)));
                        }
                    }
                });
    }

    private void processBooking() {
        if (selectedClass.getPrice() >= HIGH_VALUE_TRANSACTION_THRESHOLD) {
            biometricPrompt.authenticate(promptInfo);
        } else {
            showOtpDialog();
        }
    }

    private void setupBiometricPrompt() {
        executor = ContextCompat.getMainExecutor(requireContext());
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
                    Toast.makeText(getContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Authenticate for your flight booking")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Cancel")
                .build();
    }

    private void showOtpDialog() {
        notificationHelper.sendOtpNotification("123456");
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_otp_verification, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();

        EditText etOtp = view.findViewById(R.id.et_otp);
        Button btnConfirmOtp = view.findViewById(R.id.btn_confirm_otp);
        Button btnCancelOtp = view.findViewById(R.id.btn_cancel_otp);

        btnConfirmOtp.setOnClickListener(v -> {
            String otp = etOtp.getText().toString();
            if (otp.equals("123456")) { // Hardcoded OTP
                dialog.dismiss();
                executeBookingTransaction();
            } else {
                Toast.makeText(getContext(), "Invalid OTP", Toast.LENGTH_SHORT).show();
            }
        });
        btnCancelOtp.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void executeBookingTransaction() {
        btnConfirm.setEnabled(false);
        btnConfirm.setText("Đang xử lý...");

        String userId = currentUser.getUid();
        double ticketPrice = selectedClass.getPrice();

        DocumentReference accountRef = db.collection("accounts").document(userId + "_CHECKING");
        DocumentReference flightRef = db.collection("flights").document(flight.getFlightId());

        db.runTransaction(transaction -> {
            DocumentSnapshot userSnapshot = transaction.get(db.collection("users").document(userId));
            DocumentSnapshot accountSnapshot = transaction.get(accountRef);

            String senderName = userSnapshot.getString("full_name");
            String senderAccount = accountSnapshot.getString("account_number");

            Double currentBalance = accountSnapshot.getDouble("balance");
            if (currentBalance == null || currentBalance < ticketPrice) {
                throw new FirebaseFirestoreException("Số dư không đủ.", FirebaseFirestoreException.Code.ABORTED);
            }

            Long availableSeats = transaction.get(flightRef).getLong("classes." + flightClassKey + ".availableSeats");
            if (availableSeats == null || availableSeats < 1) {
                throw new FirebaseFirestoreException("Đã hết vé hạng này.", FirebaseFirestoreException.Code.ABORTED);
            }

            transaction.update(accountRef, "balance", currentBalance - ticketPrice);
            transaction.update(flightRef, "classes." + flightClassKey + ".availableSeats", FieldValue.increment(-1));

            DocumentReference bookingRef = db.collection("flight_bookings").document();
            Map<String, Object> bookingData = new HashMap<>();
            bookingData.put("userId", userId);
            bookingData.put("flightId", flight.getFlightId());
            bookingData.put("flightNumber", flight.getFlightNumber());
            bookingData.put("airlineName", flight.getAirlineName());
            bookingData.put("flightClass", selectedClass.getName());
            bookingData.put("pricePaid", ticketPrice);
            bookingData.put("bookingDate", FieldValue.serverTimestamp());
            transaction.set(bookingRef, bookingData);

            DocumentReference transactionRef = db.collection("transactions").document();
            Map<String, Object> txData = new HashMap<>();
            txData.put("type", "FLIGHT_BOOKING");
            txData.put("amount", ticketPrice);
            txData.put("message", "Thanh toán vé máy bay " + flight.getAirlineName() + " " + flight.getFlightNumber());
            txData.put("status", "SUCCESS");
            txData.put("created_at", FieldValue.serverTimestamp());
            txData.put("sender_account", senderAccount);
            txData.put("sender_name", senderName);
            txData.put("receiver_name", flight.getAirlineName());
            txData.put("counterparty_bank", flight.getAirlineName());
            transaction.set(transactionRef, txData);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Đặt vé thành công!", Toast.LENGTH_LONG).show();
            dismiss();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Lỗi đặt vé: " + e.getMessage(), Toast.LENGTH_LONG).show();
            btnConfirm.setEnabled(true);
            btnConfirm.setText("Xác nhận");
        });
    }
}
