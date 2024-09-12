package com.example.licentaappclient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ConfirmOrderActivity extends AppCompatActivity
{
    private static final int REQUEST_CODE_MAPS = 1;
    String address, idClient, idRestaurant, mentiune, optiuniText;
    double latitude, longitude, pret;
    CosProduse cosProduse;

    private TextView txtAdresa;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_order);

        txtAdresa = findViewById(R.id.txtPassword);

        idClient = getIntent().getStringExtra("idClient");
        idRestaurant = getIntent().getStringExtra("idRestaurant");
        mentiune = getIntent().getStringExtra("mentiuni");
        optiuniText = getIntent().getStringExtra("optiuniText");
        cosProduse = (CosProduse) getIntent().getSerializableExtra("cosProduse");
        pret = getIntent().getDoubleExtra("pret", 0.0);

        CollectionReference clientRef = db.collection("Clients");

        Log.d("idClient", idClient);
        clientRef.whereEqualTo("idClient", idClient)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Document found, retrieve the value of numeClient
                            String numeClient = document.getString("numeClient");

                            // Update the txtNume EditText
                            EditText txtNume = findViewById(R.id.txtUsername);
                            txtNume.setText(numeClient);

                            // Break out of the loop since we only need one document
                            break;
                        }
                    }
                });
    }

    public void showMaps(View v)
    {
        Intent intent = new Intent(ConfirmOrderActivity.this, PickAddressActivity.class);
        startActivityForResult(intent, REQUEST_CODE_MAPS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_MAPS && resultCode == RESULT_OK) {
            String address = data.getStringExtra("selectedAddress");
            double latitude = data.getDoubleExtra("Latitude", 0.0);
            double longitude = data.getDoubleExtra("Longitude", 0.0);

            if (isConstantaAddress(this, address, latitude, longitude)) {
                this.address = address;
                this.latitude = latitude;
                this.longitude = longitude;

                checkAndSaveAddress();

                txtAdresa.setText(address);
            } else {
                Toast.makeText(this, "Please select an address from Constanta, Romania", Toast.LENGTH_SHORT).show();
            }

            String toastMessage = "Address: " + address + "\nLatitude: " + latitude + "\nLongitude: " + longitude;
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();

            // Do something with the address and coordinates
        }
    }

    public void confirmTheOrder() {
        // Check if an address is selected and deliverers are available
        if (address != null && !address.isEmpty()) {
            // Check if a radio button is selected
            RadioGroup radioGroupPlata = findViewById(R.id.radioGroupPlata);
            int selectedRadioButtonId = radioGroupPlata.getCheckedRadioButtonId();

            if (selectedRadioButtonId != -1) {
                // Both address, deliverers available, and radio button are selected, proceed with the order
                RadioButton selectedRadioButton = findViewById(selectedRadioButtonId);
                String paymentMethod = selectedRadioButton.getText().toString();

                //checkAndSaveAddress();
                placeOrder(paymentMethod);

                Toast.makeText(this, "Order confirmed with Address: " + address + " and Payment Method: " + paymentMethod, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, MainMenuActivity.class);
                startActivity(intent);
                finish();

            } else {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please select a valid address ", Toast.LENGTH_SHORT).show();
        }
    }

    public void onConfirmButtonClick(View view) {
        //checkAndSaveAddress();
        deliverersAvailable();
    }

    private void deliverersAvailable() {
        CollectionReference deliverersRef = db.collection("Deliverers");

        // Query to find at least one available deliverer
        Query query = deliverersRef.whereEqualTo("disponibil", true)
                                    .whereEqualTo("areComanda", false);


        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Found at least one available deliverer
                    confirmTheOrder();
                    //checkAndSaveAddress();
                } else {
                    Toast.makeText(this, "No available deliverers at the moment", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Handle errors
                Log.e("FirestoreError", "Error querying Deliverers collection: ", task.getException());
                Toast.makeText(this, "Something went wrong while checking deliverer availability", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static boolean isConstantaAddress(Context context, String address, double latitude, double longitude)
    {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        try {
            // Try to get the Address object from the provided latitude and longitude
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && addresses.size() > 0) {
                Address fetchedAddress = addresses.get(0);

                // Log details of the fetched address
                logFetchedAddressDetails(fetchedAddress);

                // Check if the fetched address is in Constanta, Romania
                String locality = fetchedAddress.getLocality();
                String country = fetchedAddress.getCountryName();

                // Use a case-insensitive comparison method that takes locale into account
                return "Constanța".equalsIgnoreCase(locality) && "Romania".equalsIgnoreCase(country);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // In case of any error or if the address is not in Constanta, return false
        return false;
    }

    public void fetchSavedAddressAndUpdateTxtAdresa(View v)
    {
        CollectionReference clientsRef = db.collection("Clients");

        clientsRef.whereEqualTo("idClient", idClient)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Document found, retrieve the value of adresaClient
                            String adresaClient = document.getString("adresaClient");

                            // Update the txtAdresa TextView
                            if(adresaClient != "") {
                                txtAdresa.setText(adresaClient);
                                address = adresaClient;
                            }

                            // Break out of the loop since we only need one document
                            break;
                        }
                    }
                });
    }


    private static void logFetchedAddressDetails(Address address)
    {
        if (address != null) {
            Log.d("AddressValidator", "Locality: " + address.getLocality());
            Log.d("AddressValidator", "Country: " + address.getCountryName());
            // Add more details if needed
        }
    }

    private void checkAndSaveAddress()
    {
        CollectionReference clientsRef = db.collection("Clients");

        clientsRef.whereEqualTo("idClient", idClient)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Document found, retrieve the value of adresaClient
                            String adresaClient = document.getString("adresaClient");

                            if (!Objects.equals(address, adresaClient)) {
                                // Address is different, show AlertDialog
                                Log.d("plm555", "te pup tanca-miu");
                                showSaveAddressDialog();
                            }
                            // Break out of the loop since we only need one document
                            break;
                        }
                    }
                });
    }

    private boolean isSavedAddressDifferent(String savedAddress)
    {
        // Check if the entered address is different from the saved one
        return savedAddress == null || !savedAddress.equals(address);
    }

    private void showSaveAddressDialog()
    {
        new AlertDialog.Builder(this)
                .setTitle("Save Address")
                .setMessage("Do you want to save the entered address?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    // User clicked Yes, update the adresaClient field
                    changeSavedAddress();
                    Log.d("plm555", "hai sarut");
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void placeOrder(String paymentMethod) {
        // Get a reference to the "Pending Orders" collection
        CollectionReference pendingOrdersRef = db.collection("Pending Orders");

        boolean plataCash = false;

        plataCash = Objects.equals(paymentMethod, "Cash");

        // Create a map to represent the order
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("idClient", idClient);
        orderData.put("address", address);
        orderData.put("idRestaurant", idRestaurant);
        orderData.put("pret", pret);
        orderData.put("comandaPreluata", false);
        orderData.put("mentiune", mentiune);
        orderData.put("optiuniText", optiuniText);
        orderData.put("plataCash", plataCash);
        orderData.put("cheamaCurier", false);

        // Query to find the restaurant document
        Query restaurantQuery = db.collection("Restaurants").whereEqualTo("idRestaurant", idRestaurant);

        // Fetch the adresaRestaurant from the "Restaurants" collection
        restaurantQuery.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                String adresaRestaurant = documentSnapshot.getString("adresaRestaurant");
                if (adresaRestaurant != null) {
                    // Add the adresaRestaurant field to the order data
                    orderData.put("adresaRestaurant", adresaRestaurant);

                    // Convert the CosProduse object to a list of maps
                    List<Map<String, Object>> productList = new ArrayList<>();
                    for (Map.Entry<String, Integer> entry : cosProduse.getProductList().entrySet()) {
                        Map<String, Object> productMap = new HashMap<>();
                        productMap.put("idProdus", entry.getKey());
                        productMap.put("cantitate", entry.getValue());
                        productList.add(productMap);
                    }

                    orderData.put("Products", productList);

                    // Add the order data to the "Pending Orders" collection
                    pendingOrdersRef.add(orderData)
                            .addOnSuccessListener(documentReference -> {
                                // Handle success, for example, show a toast
                                Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
                                //updateClientRatingStatus();
                            })
                            .addOnFailureListener(e -> {
                                // Handle failure, for example, show a toast
                                Toast.makeText(this, "Failed to place the order. Please try again.", Toast.LENGTH_SHORT).show();
                            });

                } else {
                    // Handle the case where adresaRestaurant is null
                    Toast.makeText(this, "Failed to retrieve restaurant address. Please try again.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Handle the case where the document does not exist
                Toast.makeText(this, "Restaurant document not found. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            // Handle failure in fetching restaurant data
            Toast.makeText(this, "Failed to retrieve restaurant data. Please try again.", Toast.LENGTH_SHORT).show();
        });
    }

    private void changeSavedAddress()
    {
        CollectionReference clientsRef = db.collection("Clients");

        clientsRef.whereEqualTo("idClient", idClient)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Document found, update the adresaClient field
                            DocumentReference clientDocRef = db.collection("Clients").document(document.getId());

                            clientDocRef.update("adresaClient", address)
                                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Address saved successfully", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save address", Toast.LENGTH_SHORT).show());

                            // Break out of the loop since we only need one document
                            break;
                        }
                    } else {
                        Toast.makeText(this, "Failed to fetch document", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /*private void updateClientRatingStatus() {
        // Reference to the "Clients" collection
        CollectionReference clientsRef = db.collection("Clients");

        // Query to find the document with the specified idClient
        Query query = clientsRef.whereEqualTo("idClient", idClient);

        // Execute the query
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Assuming there's at most one matching document, get the first one
                    DocumentSnapshot clientDoc = querySnapshot.getDocuments().get(0);

                    // Update the boolean field "datoreazaRating" to true
                    clientsRef.document(clientDoc.getId())
                            .update("datoreazaRating", true)
                            .addOnSuccessListener(aVoid -> {
                                // Update successful
                                Log.d("FirestoreDebug", "datoreazaRating set to true");

                                // Update the String field "idRestaurantComandaAnterioara" to idRestaurant
                                clientsRef.document(clientDoc.getId())
                                        .update("idRestaurantComandaAnterioara", idRestaurant)
                                        .addOnSuccessListener(aVoid1 -> {
                                            // Update successful
                                            Log.d("FirestoreDebug", "idRestaurantComandaAnterioara updated");
                                        })
                                        .addOnFailureListener(e -> {
                                            // Update failed
                                            Log.e("FirestoreError", "Error updating 'idRestaurantComandaAnterioara' field", e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                // Update failed
                                Log.e("FirestoreError", "Error updating 'datoreazaRating' field", e);
                            });
                } else {
                    // Handle the case when no document matches the query
                    Log.d("FirestoreDebug", "No document found for idClient: " + idClient);
                }
            } else {
                // Handle errors
                Log.e("FirestoreError", "Error querying Clients collection: ", task.getException());
            }
        });
    }*/

}
