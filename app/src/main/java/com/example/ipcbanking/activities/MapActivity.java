package com.example.ipcbanking.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.ipcbanking.R;
import com.example.ipcbanking.models.BankBranch;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    // Marker cho vị trí tìm kiếm thủ công
    private Marker manualLocationMarker;

    // Views
    private ImageView btnBack, btnSearchSubmit;
    private EditText etSearchLocation;
    private CardView cardBranchInfo;
    private TextView tvBranchName, tvBranchAddress, tvBranchDistance;
    private Button btnGetDirections;

    // Khai báo các nút Zoom thủ công
    private CardView btnZoomIn, btnZoomOut;

    private ExtendedFloatingActionButton btnFindNearest, btnSelectBranch;

    // Data
    private FirebaseFirestore db;
    private List<BankBranch> branches = new ArrayList<>();
    private BankBranch selectedBranch = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        loadBranchesFromFirestore();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back_map);
        btnSearchSubmit = findViewById(R.id.btn_search_submit);
        etSearchLocation = findViewById(R.id.et_search_location);

        cardBranchInfo = findViewById(R.id.card_branch_info);
        tvBranchName = findViewById(R.id.tv_branch_name);
        tvBranchAddress = findViewById(R.id.tv_branch_address);
        tvBranchDistance = findViewById(R.id.tv_branch_distance);
        btnGetDirections = findViewById(R.id.btn_get_directions);

        btnFindNearest = findViewById(R.id.btn_find_nearest);
        btnSelectBranch = findViewById(R.id.btn_select_branch);

        // [MỚI] Ánh xạ nút Zoom
        btnZoomIn = findViewById(R.id.btn_zoom_in);
        btnZoomOut = findViewById(R.id.btn_zoom_out);

        btnBack.setOnClickListener(v -> finish());
        btnFindNearest.setOnClickListener(v -> findAndShowNearestBranch());
        btnSelectBranch.setOnClickListener(v -> showBranchSelectionDialog());

        // [MỚI] Xử lý sự kiện Zoom In
        btnZoomIn.setOnClickListener(v -> {
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });

        // [MỚI] Xử lý sự kiện Zoom Out
        btnZoomOut.setOnClickListener(v -> {
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });

        btnSearchSubmit.setOnClickListener(v -> {
            searchLocation(etSearchLocation.getText().toString());
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etSearchLocation.getWindowToken(), 0);
            }
            etSearchLocation.clearFocus();
        });

        etSearchLocation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation(etSearchLocation.getText().toString());
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etSearchLocation.getWindowToken(), 0);
                }
                etSearchLocation.clearFocus();
                return true;
            }
            return false;
        });

        btnGetDirections.setOnClickListener(v -> {
            if (selectedBranch != null) {
                openGoogleMapsDirections(selectedBranch.getLatitude(), selectedBranch.getLongitude());
            }
        });
    }

    private void loadBranchesFromFirestore() {
        db.collection("bank_branches").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    branches.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        BankBranch branch = doc.toObject(BankBranch.class);
                        if (branch != null) {
                            branch.setId(doc.getId());
                            branches.add(branch);
                        }
                    }
                    if (mMap != null) {
                        addBranchMarkers();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load branches", Toast.LENGTH_SHORT).show());
    }

    private void searchLocation(String locationName) {
        if (locationName == null || locationName.isEmpty()) {
            Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show();
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                currentLocation = new Location("ManualProvider");
                currentLocation.setLatitude(latLng.latitude);
                currentLocation.setLongitude(latLng.longitude);

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

                if (manualLocationMarker != null) manualLocationMarker.remove();
                manualLocationMarker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("My Location: " + locationName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                manualLocationMarker.showInfoWindow();

                Toast.makeText(this, "Location set: " + address.getAddressLine(0), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Geocoding error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showBranchSelectionDialog() {
        if (branches.isEmpty()) {
            Toast.makeText(this, "No branches data loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] branchNames = new String[branches.size()];
        for (int i = 0; i < branches.size(); i++) {
            branchNames[i] = branches.get(i).getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Bank Branch");
        builder.setItems(branchNames, (dialog, which) -> {
            BankBranch selected = branches.get(which);

            LatLng branchLoc = new LatLng(selected.getLatitude(), selected.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(branchLoc, 16));

            showBranchInfo(selected);
        });
        builder.show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // [CẬP NHẬT] Tắt nút Zoom mặc định để dùng nút tùy chỉnh của mình
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        if (!branches.isEmpty()) {
            addBranchMarkers();
        }

        LatLng hcm = new LatLng(10.7769, 106.7009);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hcm, 12));

        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof BankBranch) {
                showBranchInfo((BankBranch) tag);
            }
            return false;
        });

        mMap.setOnMapClickListener(latLng -> cardBranchInfo.setVisibility(View.GONE));
    }

    private void addBranchMarkers() {
        if (mMap == null) return;
        mMap.clear();

        for (BankBranch branch : branches) {
            LatLng position = new LatLng(branch.getLatitude(), branch.getLongitude());
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(branch.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            if (marker != null) marker.setTag(branch);
        }

        if (currentLocation != null && manualLocationMarker != null) {
            manualLocationMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))
                    .title("My Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            }
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    if (manualLocationMarker == null) {
                        currentLocation = location;
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(location.getLatitude(), location.getLongitude()), 15));
                    }
                }
            });
        }
    }

    private void showBranchInfo(BankBranch branch) {
        selectedBranch = branch;
        tvBranchName.setText(branch.getName());
        tvBranchAddress.setText(branch.getAddress());

        if (currentLocation != null) {
            float[] results = new float[1];
            Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                    branch.getLatitude(), branch.getLongitude(), results);
            float distanceInMeters = results[0];
            tvBranchDistance.setText(String.format("Distance: %.2f km", distanceInMeters / 1000));
        } else {
            tvBranchDistance.setText("Distance: Calculating...");
        }
        cardBranchInfo.setVisibility(View.VISIBLE);
    }

    private void findAndShowNearestBranch() {
        if (currentLocation == null) {
            Toast.makeText(this, "Updating location...", Toast.LENGTH_SHORT).show();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if(location != null) {
                        currentLocation = location;
                        findAndShowNearestBranch();
                    } else {
                        Toast.makeText(this, "Cannot find your location. Please enter manually.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Please grant location permission or enter manually.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        BankBranch nearestBranch = null;
        float minDistance = Float.MAX_VALUE;

        for (BankBranch branch : branches) {
            float[] results = new float[1];
            Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                    branch.getLatitude(), branch.getLongitude(), results);
            float distance = results[0];

            if (distance < minDistance) {
                minDistance = distance;
                nearestBranch = branch;
            }
        }

        if (nearestBranch != null) {
            LatLng branchLoc = new LatLng(nearestBranch.getLatitude(), nearestBranch.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(branchLoc, 16));
            showBranchInfo(nearestBranch);
            Toast.makeText(this, "Nearest: " + nearestBranch.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openGoogleMapsDirections(double destLat, double destLng) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destLat + "," + destLng + "&mode=w");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Uri browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + destLat + "," + destLng + "&travelmode=walking");
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
            startActivity(browserIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}