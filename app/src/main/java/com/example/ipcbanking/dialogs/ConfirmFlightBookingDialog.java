package com.example.ipcbanking.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.ipcbanking.R;
import com.example.ipcbanking.models.Flight;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.WriteBatch;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ConfirmFlightBookingDialog extends DialogFragment {

    private static final String ARG_FLIGHT = "flight";
    private static final String ARG_FLIGHT_CLASS_KEY = "flight_class_key";

    private Flight flight;
    private String flightClassKey;
    private Flight.FlightClass selectedClass;

    private TextView tvFlightRoute, tvFlightDetails, tvPassengerDetails, tvTotalPrice, tvAccountBalance;
    private Button btnCancel, btnConfirm;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public static ConfirmFlightBookingDialog newInstance(Flight flight, String flightClassKey) {
        ConfirmFlightBookingDialog fragment = new ConfirmFlightBookingDialog();
        Bundle args = new Bundle();
        // Note: Passing the whole object might be slow. Consider passing IDs and re-fetching.
        // args.putSerializable(ARG_FLIGHT, flight);
        // For now, let's pass the necessary details directly.
        args.putString("flightId", flight.getFlightId());
        args.putString("departureAirport", flight.getDepartureAirport());
        args.putString("arrivalAirport", flight.getArrivalAirport());
        args.putString("airlineName", flight.getAirlineName());
        args.putString("departureTime", flight.getDepartureTime());
        args.putSerializable(ARG_FLIGHT_CLASS_KEY, flightClassKey);
        args.putSerializable("selectedClass", flight.getClasses().get(flightClassKey));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            flightClassKey = getArguments().getString(ARG_FLIGHT_CLASS_KEY);
            selectedClass = (Flight.FlightClass) getArguments().getSerializable("selectedClass");

            // Reconstruct a partial flight object for display
            flight = new Flight();
            flight.setFlightId(getArguments().getString("flightId"));
            flight.setDepartureAirport(getArguments().getString("departureAirport"));
            flight.setArrivalAirport(getArguments().getString("arrivalAirport"));
            flight.setAirlineName(getArguments().getString("airlineName"));
            flight.setDepartureTime(getArguments().getString("departureTime"));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_confirm_flight_booking, container, false);

        tvFlightRoute = view.findViewById(R.id.tv_flight_route_confirm);
        tvFlightDetails = view.findViewById(R.id.tv_flight_details_confirm);
        tvPassengerDetails = view.findViewById(R.id.tv_passenger_details_confirm);
        tvTotalPrice = view.findViewById(R.id.tv_total_price_confirm);
        tvAccountBalance = view.findViewById(R.id.tv_account_balance_confirm);
        btnCancel = view.findViewById(R.id.btn_cancel_booking_confirm);
        btnConfirm = view.findViewById(R.id.btn_confirm_booking_final);

        setupDialogData();
        fetchUserAccountBalance();

        btnCancel.setOnClickListener(v -> dismiss());
        btnConfirm.setOnClickListener(v -> confirmBooking());

        return view;
    }

    private void setupDialogData() {
        tvFlightRoute.setText(String.format("%s ✈ %s", flight.getDepartureAirport(), flight.getArrivalAirport()));
        tvFlightDetails.setText(String.format("%s • %s", flight.getAirlineName(), flight.getDepartureTime()));
        tvPassengerDetails.setText(String.format("1 hành khách • Hạng %s", selectedClass.getName()));

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvTotalPrice.setText(currencyFormat.format(selectedClass.getPrice()));
    }

    private void fetchUserAccountBalance() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("accounts")
                .whereEqualTo("owner_id", userId)
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

    private void confirmBooking() {
        btnConfirm.setEnabled(false);
        btnConfirm.setText("Đang xử lý...");

        String userId = auth.getCurrentUser().getUid();
        double ticketPrice = selectedClass.getPrice();

        DocumentReference accountRef = db.collection("accounts").document(userId + "_CHECKING");
        DocumentReference flightRef = db.collection("flights").document(flight.getFlightId());

        db.runTransaction(transaction -> {
            // 1. Get current account balance
            Double currentBalance = transaction.get(accountRef).getDouble("balance");
            if (currentBalance == null || currentBalance < ticketPrice) {
                throw new FirebaseFirestoreException("Số dư không đủ.", FirebaseFirestoreException.Code.ABORTED);
            }

            // 2. Get current available seats
            Long availableSeats = transaction.get(flightRef).getLong("classes." + flightClassKey + ".availableSeats");
            if (availableSeats == null || availableSeats < 1) {
                throw new FirebaseFirestoreException("Đã hết vé hạng này.", FirebaseFirestoreException.Code.ABORTED);
            }

            // 3. Perform updates
            transaction.update(accountRef, "balance", currentBalance - ticketPrice);
            transaction.update(flightRef, "classes." + flightClassKey + ".availableSeats", FieldValue.increment(-1));

            // 4. Create a booking document
            DocumentReference bookingRef = db.collection("flight_bookings").document();
            Map<String, Object> bookingData = new HashMap<>();
            bookingData.put("userId", userId);
            bookingData.put("flightId", flight.getFlightId());
            bookingData.put("flightClass", selectedClass.getName());
            bookingData.put("pricePaid", ticketPrice);
            bookingData.put("bookingDate", FieldValue.serverTimestamp());
            transaction.set(bookingRef, bookingData);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Đặt vé thành công!", Toast.LENGTH_LONG).show();
            dismiss();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
            btnConfirm.setEnabled(true);
            btnConfirm.setText("Xác nhận");
        });
    }
}
