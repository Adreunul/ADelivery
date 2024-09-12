package com.example.licentaappcurier;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    String idCurier = "5555";
    String orderId;
    Button activeOrderButton, accountDetailsButton;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference activeOrdersRef = db.collection("Active Orders");
    private CollectionReference deliverersRef = db.collection("Deliverers");
    private Button setStateButton;
    private TextView delivererState;
    private Handler handler = new Handler();
    private static final long UPDATE_INTERVAL = 1 * 30 * 1000; //5 * 60 * 1000

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_CHECK_SETTINGS = 1001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activeOrderButton = findViewById(R.id.active_order);
        activeOrderButton.setOnClickListener(this);

        accountDetailsButton = findViewById(R.id.account_details);
        accountDetailsButton.setOnClickListener(this);

        delivererState = findViewById(R.id.delivererState);

        setStateButton = findViewById(R.id.btnSetState);
        setStateButton.setOnClickListener(this);
        setState();

        TextView accountIdTextView = findViewById(R.id.account_id);
        accountIdTextView.setText(idCurier);

        TextView programTextView = findViewById(R.id.program);
        fetchAndSetSchedule(programTextView);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permissions already granted, fetch the address
            if(delivererState.getText() == "Active")
                getAddress();
        }

        activeOrdersRef.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot querySnapshot, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    // Log the specific error message
                    Log.e("FirestoreError", "Error: " + error.getMessage(), error);
                    Toast.makeText(MainActivity.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                    return;
                }

                if (querySnapshot != null) {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        // Check if the document contains the idCurier field
                        if (document.contains("idCurier") && Objects.equals(document.getString("idCurier"), idCurier)) {
                            // Document with idCurier found, show the button
                            orderId = document.getId();
                            showButton();
                            return; // Stop checking further documents
                        }
                    }

                    // No document with idCurier found, hide the button
                    hideButton();
                }
            }
        });

        // Call the method to update the address immediately
        //updateAddressPeriodically();

        // Schedule periodic address updates
        handler.postDelayed(addressUpdateRunnable, UPDATE_INTERVAL);
    }

    private Runnable addressUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            // Call the method to update the address

            if(delivererState.getText() == "Active")
                getAddress();

            // Schedule the next address update after the delay
            handler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    // Your existing code...

    @Override
    protected void onStop() {
        super.onStop();

        // Remove pending callbacks to prevent memory leaks
        handler.removeCallbacks(addressUpdateRunnable);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, fetch the address
                if(delivererState.getText() == "Active")
                    getAddress();
            } else {
                // Permission denied, show a message or handle accordingly
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }





    }

    /*private void getAddress() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && addresses.size() > 0) {
                        Address address = addresses.get(0);
                        String fullAddress = address.getAddressLine(0);
                        Log.d("Address", "User's address: " + fullAddress);
                        Toast.makeText(this, "User's address: " + fullAddress, Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("Location", "Last known location is null.");
            }
        }).addOnFailureListener(e -> {
            Log.e("Location", "Error getting last known location: " + e.getMessage());
        });
    }*/

    public void getAddress() {
        // Create a location request
        LocationRequest locationRequest = LocationRequest.create();

        // Create a location settings request builder
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        // Get the location settings client
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);

        // Check if the device's location settings are satisfied
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // All location settings are satisfied, proceed with location request
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Request location permission if not granted
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }

            // Request location updates
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            super.onLocationResult(locationResult);
                            Location location = locationResult.getLastLocation();
                            if (location != null) {
                                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    if (addresses != null && addresses.size() > 0) {
                                        Address address = addresses.get(0);
                                        String fullAddress = address.getAddressLine(0);
                                        Log.d("Address", "User's address: " + fullAddress);
                                        Toast.makeText(MainActivity.this, "User's address: " + fullAddress, Toast.LENGTH_SHORT).show();

                                        saveAddressToFirestore(fullAddress);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Log.e("Location", "Last known location is null.");
                            }
                        }
                    },
                    null /* Looper */
            );
        });

        task.addOnFailureListener(this, e -> {
            // Location settings are not satisfied, prompt the user to change them
            if (e instanceof ResolvableApiException) {
                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult()
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore the error
                }
            }
        });
    }

    public void getAddressDebug(View v) {
        // Create a location request
        LocationRequest locationRequest = LocationRequest.create();

        // Create a location settings request builder
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        // Get the location settings client
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);

        // Check if the device's location settings are satisfied
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // All location settings are satisfied, proceed with location request
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Request location permission if not granted
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }

            // Request location updates
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            super.onLocationResult(locationResult);
                            Location location = locationResult.getLastLocation();
                            if (location != null) {
                                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    if (addresses != null && addresses.size() > 0) {
                                        Address address = addresses.get(0);
                                        String fullAddress = address.getAddressLine(0);
                                        Log.d("Address", "User's address: " + fullAddress);
                                        Toast.makeText(MainActivity.this, "User's address: " + fullAddress, Toast.LENGTH_SHORT).show();

                                        saveAddressToFirestore(fullAddress);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Log.e("Location", "Last known location is null.");
                            }
                        }
                    },
                    null /* Looper */
            );
        });

        task.addOnFailureListener(this, e -> {
            // Location settings are not satisfied, prompt the user to change them
            if (e instanceof ResolvableApiException) {
                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult()
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore the error
                }
            }
        });
    }


    private void saveAddressToFirestore(String address) {
        // Reference to the "Deliverers" collection
        CollectionReference deliverersRef = db.collection("Deliverers");

        // Query to find the document with the specified idCurier
        Query query = deliverersRef.whereEqualTo("idCurier", idCurier);

        // Execute the query
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Assuming there's at most one matching document, get the first one
                    DocumentSnapshot delivererDoc = querySnapshot.getDocuments().get(0);

                    // Update the document with the new address
                    delivererDoc.getReference().update("adresaCurenta", address)
                            .addOnCompleteListener(updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    Log.d("FirestoreDebug", "Current address updated successfully.");
                                } else {
                                    Log.e("FirestoreError", "Error updating current address: ", updateTask.getException());
                                }
                            });
                } else {
                    // Handle the case when no document matches the query
                    Log.d("FirestoreDebug", "No document found for idCurier: " + idCurier);
                }
            } else {
                // Handle errors
                Log.e("FirestoreError", "Error querying Deliverers collection: ", task.getException());
            }
        });
    }



    private void showButton() {
        activeOrderButton.setVisibility(View.VISIBLE);
    }

    private void hideButton() {
        activeOrderButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onClick(View v)
    {
        if(v.getId() == R.id.active_order) {
            Intent intent = new Intent(MainActivity.this, ActiveOrderActivity.class);
            intent.putExtra("orderId", orderId);
            intent.putExtra("idCurier", idCurier);
            startActivity(intent);
        }

        if(v.getId() == R.id.account_details)
        {
            getAccountDetails();
        }

        if (v.getId() == R.id.btnSetState) {
            // Handle button click, toggle the state in Firestore
            //toggleDelivererState();
            getConfirmationForChangingState();
        }
    }

    private void getAccountDetails() {
        // Reference to the "Deliverers" collection
        CollectionReference deliverersRef = db.collection("Deliverers");

        // Query to find the document with the specified idCurier
        Query query = deliverersRef.whereEqualTo("idCurier", idCurier);

        // Execute the query
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Assuming there's at most one matching document, get the first one
                    DocumentSnapshot delivererDoc = querySnapshot.getDocuments().get(0);

                    // Get the 'vanzari' field from the document
                    double vanzari = delivererDoc.getDouble("vanzari");
                    double nrLivrari = delivererDoc.getDouble("nrLivrari");

                    String accountDetails = "Vanzari: " + vanzari + " lei" + "\nNumar livrari realizate: " + (int) nrLivrari;
                    showAlertDialog(this, "Detalii cont",accountDetails);

                } else {
                    // Handle the case when no document matches the query
                    Log.d("FirestoreDebug", "No document found for idCurier: " + idCurier);
                }
            } else {
                // Handle errors
                Log.e("FirestoreError", "Error querying Deliverers collection: ", task.getException());
            }
        });
    }


    private void showAlertDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // Set the dialog title and message
        builder.setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Handle the OK button click
                        dialog.dismiss();
                    }
                });

        // Create and show the dialog
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void getConfirmationForChangingState() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirma schimbarea de stare")
                .setMessage("Esti sigur ca doresti sa schimbi starea?")
                .setPositiveButton("Da", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User confirmed, toggle the state
                        //toggleDelivererState();
                        checkIfInSchedule();
                    }
                })
                .setNegativeButton("Nu", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User cancelled, do nothing
                    }
                })
                .show();
    }

    private void toggleDelivererState() {
        // Query to find the document with the specified idCurier
        Query query = deliverersRef.whereEqualTo("idCurier", idCurier);

        // Execute the query
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Assuming there's at most one matching document, get the first one
                    DocumentSnapshot delivererDoc = querySnapshot.getDocuments().get(0);
                    // Get the current disponibil field from the document
                    boolean isDisponible = delivererDoc.getBoolean("disponibil");
                    boolean alreadyWorked = delivererDoc.getBoolean("ziTerminata");

                    if(!isDisponible)
                        getAddress();

                    // Toggle the state and update in Firestore
                    if (!isDisponible && alreadyWorked) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Nu poti incepe din nou programul")
                                .setMessage("Ai muncit deja astazi.")
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                        return;
                    }
                    delivererDoc.getReference().update("disponibil", !isDisponible)
                            .addOnCompleteListener(updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    Log.d("FirestoreDebug", "Deliverer state updated successfully.");

                                    checkIfLate(!isDisponible, delivererDoc);
                                    if (isDisponible)//adica daca incheie ziua
                                        delivererDoc.getReference().update("ziTerminata", true);

                                    // Update the text of the button and textView based on the new state
                                    updateButtonText(!isDisponible);
                                } else {
                                    Log.e("FirestoreError", "Error updating deliverer state: ", updateTask.getException());
                                }
                            });
                } else {
                    // Handle the case when no document matches the query
                    Log.d("FirestoreDebug", "No document found for idCurier: " + idCurier);
                }
            } else {
                // Handle errors
                Log.e("FirestoreError", "Error querying Deliverers collection: ", task.getException());
            }
        });
    }

    private void updateButtonText(boolean newDisponibilState) {
        // Get references to button and textView
        Button btnSetState = findViewById(R.id.btnSetState);

        // Set the text based on the new disponibil state
        if (newDisponibilState) {
            btnSetState.setText("Termina Programul");
            delivererState.setText("Activ");
        } else {
            btnSetState.setText("Incepe Programul");
            delivererState.setText("Inactiv");
        }
    }

    public void setState()
    {
        Query query = deliverersRef.whereEqualTo("idCurier", idCurier);

        // Execute the query
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Assuming there's at most one matching document, get the first one
                    DocumentSnapshot delivererDoc = querySnapshot.getDocuments().get(0);

                    // Get the current disponibil field from the document
                    boolean currentDisponibil = delivererDoc.getBoolean("disponibil");

                    updateButtonText(currentDisponibil);
                } else {
                    // Handle the case when no document matches the query
                    Log.d("FirestoreDebug", "No document found for idCurier: " + idCurier);
                }
            } else {
                // Handle errors
                Log.e("FirestoreError", "Error querying Deliverers collection: ", task.getException());
            }
        });
    }

    public void checkIfLate(boolean toggledTo, DocumentSnapshot delivererDoc) {
        // Get the program value from the delivererDoc
        String program = delivererDoc.getString("program");
        AtomicInteger x = new AtomicInteger();

        // Get the field name based on toggledTo
        String oraProgram = toggledTo ? "oraIncepere" : "oraTerminare";

        // Reference to the "Reguli" collection
        DocumentReference reguliRef = db.collection("Reguli").document(program);

        // Get the scheduled time from the "Reguli" document
        reguliRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Get the scheduled time from the "Reguli" document
                String scheduledTimeString = documentSnapshot.getString(oraProgram);

                // Parse the scheduled time string to extract hours and minutes
                String[] scheduledTimeParts = scheduledTimeString.split(":");
                int scheduledHours = Integer.parseInt(scheduledTimeParts[0]);
                int scheduledMinutes = Integer.parseInt(scheduledTimeParts[1]);

                // Get the current time
                Calendar currentTime = Calendar.getInstance();

                // Convert current time to minutes
                int currentMinutes = currentTime.get(Calendar.HOUR_OF_DAY) * 60 + currentTime.get(Calendar.MINUTE);

                // Convert scheduled time to minutes
                int scheduledTimeMinutes = scheduledHours * 60 + scheduledMinutes;

                // Compare the current time with the scheduled time
                if ((toggledTo && currentMinutes >= scheduledTimeMinutes) || (!toggledTo && currentMinutes < scheduledTimeMinutes)) {
                    // Calculate the late time in minutes
                    long lateTimeMinutes = toggledTo ? (currentMinutes - scheduledTimeMinutes) : (scheduledTimeMinutes - currentMinutes);


                    long toleratedDelay = documentSnapshot.getLong("intarziereTolerata");

                    // Check if the late time exceeds the tolerated delay
                    Log.d("plm342", "minutare, intarziere: " + lateTimeMinutes + " toleranta " + toleratedDelay);
                    if (lateTimeMinutes > toleratedDelay) {
                        // Store the late time in the "Intarzieri" collection
                        Map<String, Object> lateInfo = new HashMap<>();
                        lateInfo.put("idCurier", idCurier);
                        lateInfo.put("lateTime", lateTimeMinutes);

                        // Add a document to the "Intarzieri" collection
                        db.collection("Intarzieri").add(lateInfo)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d("FirestoreDebug", "Late time recorded successfully.");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FirestoreError", "Error recording late time: ", e);
                                });
                    }
                }
                else
                {
                    x.getAndIncrement();
                }
            }
        }).addOnFailureListener(e -> {
            Log.e("FirestoreError", "Error getting scheduled time: ", e);
        });
    }

    private void checkIfInSchedule() {
        // Query to find the document with the specified idCurier
        Query query = deliverersRef.whereEqualTo("idCurier", idCurier);

        // Execute the query
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Assuming there's at most one matching document, get the first one
                    DocumentSnapshot delivererDoc = querySnapshot.getDocuments().get(0);

                    // Get the program value from the delivererDoc
                    String program = delivererDoc.getString("program");

                    // Get the schedule from the "Reguli" collection
                    DocumentReference reguliRef = db.collection("Reguli").document(program);

                    // Fetch the schedule document
                    reguliRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Get the scheduled start and end times from the document
                            String oraIncepere = documentSnapshot.getString("oraIncepere");
                            String oraTerminare = documentSnapshot.getString("oraTerminare");

                            // Get the current time
                            Calendar currentTime = Calendar.getInstance();
                            int currentHour = currentTime.get(Calendar.HOUR_OF_DAY);
                            int currentMinute = currentTime.get(Calendar.MINUTE);

                            // Parse the scheduled time string to extract hours and minutes
                            int startHour = Integer.parseInt(oraIncepere.split(":")[0]);
                            int startMinute = Integer.parseInt(oraIncepere.split(":")[1]);
                            int endHour = Integer.parseInt(oraTerminare.split(":")[0]);
                            int endMinute = Integer.parseInt(oraTerminare.split(":")[1]);

                            // Check if the current time is within the schedule
                            if ((currentHour > startHour || (currentHour == startHour && currentMinute >= startMinute)) &&
                                    (currentHour < endHour || (currentHour == endHour && currentMinute <= endMinute))) {
                                // Enable the option to finish delivering
                                toggleDelivererState();
                            } else {
                                // The deliverer is outside the schedule
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Nu poti incepe programul")
                                        .setMessage("Trebuie sa te aflii in programul de lucru alocat!")
                                        .setPositiveButton(android.R.string.ok, null)
                                        .show();
                            }
                        } else {
                            Log.d("FirestoreDebug", "No schedule found for program: " + program);
                        }
                    }).addOnFailureListener(e -> {
                        Log.e("FirestoreError", "Error fetching schedule for program: " + program, e);
                    });
                } else {
                    // Handle the case when no document matches the query
                    Log.d("FirestoreDebug", "No document found for idCurier: " + idCurier);
                }
            } else {
                // Handle errors
                Log.e("FirestoreError", "Error querying Deliverers collection: ", task.getException());
            }
        });
    }

    private void fetchAndSetSchedule(TextView programTextView) {
        // Query to find the document with the specified idCurier
        Query query = deliverersRef.whereEqualTo("idCurier", idCurier);

        // Execute the query
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Assuming there's at most one matching document, get the first one
                    DocumentSnapshot delivererDoc = querySnapshot.getDocuments().get(0);

                    // Get the program value from the delivererDoc
                    String program = delivererDoc.getString("program");

                    // Get the schedule from the "Reguli" collection
                    DocumentReference reguliRef = db.collection("Reguli").document(program);

                    // Fetch the schedule document
                    reguliRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Get the scheduled start and end times from the document
                            String oraIncepere = documentSnapshot.getString("oraIncepere");
                            String oraTerminare = documentSnapshot.getString("oraTerminare");

                            // Format the schedule string
                            String scheduleString = oraIncepere + " - " + oraTerminare;

                            // Set the schedule string to the TextView
                            programTextView.setText(scheduleString);
                        } else {
                            Log.d("FirestoreDebug", "No schedule found for program: " + program);
                        }
                    }).addOnFailureListener(e -> {
                        Log.e("FirestoreError", "Error fetching schedule for program: " + program, e);
                    });
                } else {
                    // Handle the case when no document matches the query
                    Log.d("FirestoreDebug", "No document found for idCurier: " + idCurier);
                }
            } else {
                // Handle errors
                Log.e("FirestoreError", "Error querying Deliverers collection: ", task.getException());
            }
        });
    }



}