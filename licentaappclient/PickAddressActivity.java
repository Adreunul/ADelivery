package com.example.licentaappclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class PickAddressActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap gMap;
    private Marker draggableMarker;
    private Location currentLocation;
    private static final int REQUEST_CODE = 101;
    private SearchView searchView;
    private Button okButton, myAddressButton;
    private FusedLocationProviderClient fusedClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        searchView = findViewById(R.id.search);
        searchView.clearFocus();
        okButton = findViewById(R.id.ok_button);
        myAddressButton = findViewById(R.id.my_address_button);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        getLocation();

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert supportMapFragment != null;
        supportMapFragment.getMapAsync(this);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String locationFromSearchBar = searchView.getQuery().toString() + ", Constanta, Romania";
                if (locationFromSearchBar == null) {
                    Toast.makeText(PickAddressActivity.this, "Location Not Found", Toast.LENGTH_SHORT).show();
                } else {
                    Geocoder geocoder = new Geocoder(PickAddressActivity.this, Locale.getDefault());
                    try {
                        List<Address> addressList = geocoder.getFromLocationName(locationFromSearchBar, 1);
                        if (!addressList.isEmpty()) {
                            LatLng latLng = new LatLng(addressList.get(0).getLatitude(), addressList.get(0).getLongitude());

                            // Move the camera to the updated position
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
                            gMap.animateCamera(cameraUpdate);

                            // Add a marker at the searched location
                            gMap.clear(); // Clear existing markers
                            draggableMarker = gMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(addressList.get(0).getAddressLine(0))
                                    .draggable(true)); // Set draggable to true

                            // Update search text based on the new marker position
                            searchView.setQuery(addressList.get(0).getAddressLine(0), false);
                        } else {
                            Toast.makeText(PickAddressActivity.this, "Location Not Found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (draggableMarker != null) {
                    // Get the position of the draggable marker
                    LatLng markerPosition = draggableMarker.getPosition();

                    // Convert LatLng to address
                    String address = getAddressFromLatLng(markerPosition);

                    // Pass the address as an extra to the previous activity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("selectedAddress", address);
                    resultIntent.putExtra("Latitude", markerPosition.latitude);
                    resultIntent.putExtra("Longitude", markerPosition.longitude);
                    setResult(Activity.RESULT_OK, resultIntent);

                    // Finish the current activity
                    finish();
                } else {
                    Toast.makeText(PickAddressActivity.this, "Place the pin on the map first!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        myAddressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Request location permission if not granted already
                if (ActivityCompat.checkSelfPermission(
                        PickAddressActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(
                        PickAddressActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(PickAddressActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
                    return;
                }

                // Check if location services are enabled
                if (!isLocationEnabled()) {
                    // Location services are disabled, show dialog to prompt the user to enable them
                    showLocationServiceAlertDialog();
                    return;
                }

                // Get user's current location
                fusedClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            // Move the camera to the user's current location
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
                            gMap.animateCamera(cameraUpdate);
                            // Add a marker at the user's current location
                            gMap.clear();
                            draggableMarker = gMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title("My Location")
                                    .draggable(true));
                            // Update search text based on the new marker position
                            String address = getAddressFromLatLng(latLng);
                            searchView.setQuery(address, false);
                        } else {
                            Toast.makeText(PickAddressActivity.this, "Failed to get location. Please make sure location services are enabled.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }

        Task<Location> task = fusedClient.getLastLocation();

        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        googleMap.getUiSettings().setAllGesturesEnabled(true);
        LatLng initialPosition = new LatLng(44.43552728201183, 26.102526056429287);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 15f));

        // Make the marker draggable by setting draggable(true) in MarkerOptions
        draggableMarker = gMap.addMarker(new MarkerOptions()
                .position(initialPosition)
                .draggable(true)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))); // Customize marker color if needed

        // Add OnMarkerDragListener to draggableMarker
        gMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                // Not needed
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                // Not needed
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                // Update search text based on the new marker position
                LatLng position = marker.getPosition();
                String address = getAddressFromLatLng(position);
                searchView.setQuery(address, false);
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            }
        }
    }

    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getAddressLine(0); // You can customize this based on your needs
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown Address";
    }

    // Method to check if location services are enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // Method to show dialog to prompt the user to enable location services
    private void showLocationServiceAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(PickAddressActivity.this);
        builder.setMessage("Location services are disabled. Do you want to enable them?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Open location settings to enable location services
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
}