package com.example.licentaappclient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ConfirmOrderActivityNew extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final int REQUEST_CODE_MAPS = 1;

    private TextView txtAdresa, txtDetalii, lblMetodaPlata;

    private CosProduse cosProduse;
    LinearLayout cardContent;
    private String idClient, idRestaurant, adresa, detalii, adresaSalvata;
    private boolean plataCash;
    double latitude, longitude;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_order_new);

        idClient = getIntent().getStringExtra("idClient");
        idRestaurant = getIntent().getStringExtra("idRestaurant");
        cosProduse = (CosProduse) getIntent().getSerializableExtra("cosProduse");

        txtAdresa = findViewById(R.id.txtAdresa);
        txtDetalii = findViewById(R.id.txtDetalii);
        cardContent = findViewById(R.id.card_content);

        cardContent.animate().alpha(1).setDuration(1000);
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
                adresa = address;
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

                            if (!Objects.equals(adresa, adresaClient)) {
                                // Address is different, show AlertDialog
                                showSaveAddressDialog();
                            }
                            // Break out of the loop since we only need one document
                            break;
                        }
                    }
                });
    }


    private void showSaveAddressDialog()
    {
        new AlertDialog.Builder(this)
                .setTitle("Save Address")
                .setMessage("Salvăm adresa introdusă ?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    // User clicked Yes, update the adresaClient field
                    changeSavedAddress();
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
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

                            clientDocRef.update("adresaClient", adresa)
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

    public void takeSavedAddress(View v)
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
                            if(!Objects.equals(adresaClient, "")) {
                                //txtAdresa.setText(adresaClient);
                                adresa = adresaClient;
                                txtAdresa.setText(adresa);
                            }

                            // Break out of the loop since we only need one document
                            break;
                        }
                    }
                });
    }

    public void pickAddress(View v)
    {
        pickAddress();
    }

    private void pickAddress()
    {
        Intent intent = new Intent(ConfirmOrderActivityNew.this, PickAddressActivity.class);
        startActivityForResult(intent, REQUEST_CODE_MAPS);
    }

    public void payByCash(View v)
    {
        plataCash = true;
    }

    public void payByCard(View v)
    {
        plataCash = false;
    }

    public void attemptPlaceOrder(View v)
    {
        checkForActiveOrder();
        //attemptPlaceOrder();
    }

    private void checkForActiveOrder() {
        db.collection("Active Orders").whereEqualTo("idClient", idClient)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Ai deja o comandă plasată ! ", Toast.LENGTH_SHORT).show();
                        goToMenuActivity();
                    } else {
                        attemptPlaceOrder();
                    }
                })
                .addOnFailureListener(e -> {
                    // Call the function to handle failure
                    attemptPlaceOrder();
                });

    }

    private void attemptPlaceOrder()
    {
        if (adresa != null && !adresa.isEmpty()) {
            // Check if a radio button is selected
            RadioGroup radioGroupPlata = findViewById(R.id.radioGroupPlata);
            int selectedRadioButtonId = radioGroupPlata.getCheckedRadioButtonId();

            if (selectedRadioButtonId != -1) {
                // Both address, deliverers available, and radio button are selected, proceed with the order
                RadioButton selectedRadioButton = findViewById(selectedRadioButtonId);
                String paymentMethod = selectedRadioButton.getText().toString();

                //checkAndSaveAddress();
                placeOrder(paymentMethod);

                Toast.makeText(this, "Comanda a fost plasată !" + adresa + " and Payment Method: " + paymentMethod, Toast.LENGTH_SHORT).show();

                /*Intent intent = new Intent(this, MainMenuActivityNew.class);
                intent.putExtra("idClient", idClient);
                startActivity(intent);
                finish();*/

            } else {
                lblMetodaPlata.setError("Te rugăm să selectezi o metodă de plată");
            }
        } else {
            txtAdresa.setError("Te rugăm să selectezi o adresă de livrare");
        }
    }

    private void placeOrder(String paymentMethod) {
        // Get a reference to the "Pending Orders" collection
        CollectionReference pendingOrdersRef = db.collection("Pending Orders");

        plataCash = Objects.equals(paymentMethod, "Cash");
        detalii = txtDetalii.getText().toString();

        // Create a map to represent the order
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("idClient", idClient);
        orderData.put("address", adresa);
        orderData.put("detaliiAdresa", detalii);
        orderData.put("idRestaurant", idRestaurant);
        orderData.put("pret", cosProduse.getTotal());
        orderData.put("comandaPreluata", false);
        orderData.put("mentiune", cosProduse.getMentiuni());
        orderData.put("optiuniText", cosProduse.getOptiuniText());
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
                                Toast.makeText(this, "Comanda a fost plasata cu succes!", Toast.LENGTH_SHORT).show();
                                goToMenuActivity();
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

    public void startConfirmCartActivity(View v)
    {
        startConfirmCartActivity();
    }

    private void startConfirmCartActivity()
    {
        CardView cardView = findViewById(R.id.card_view);
        //cardView.startAnimation(slideUpAnimation);

        View cardHeight = findViewById(R.id.card_height);

        Animation cardContentFadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        cardContent.startAnimation(cardContentFadeOut);



        //make it wait for the animation to finish
        cardContentFadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                //wait 3 seconds before going to the next line
                /*try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/

                cardContent.setAlpha(0);
                transitionToConfirmCartActivity(cardView, cardHeight);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void transitionToConfirmCartActivity(CardView cardView, View cardHeight)
    {
        Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_confirm_order_to_confirm_cart);
        cardView.startAnimation(slideDownAnimation);


// Handler to delay the execution of the code
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Finish the current activity without any animation
                setContentView(R.layout.activity_confirm_cart);
                Intent intent = new Intent(ConfirmOrderActivityNew.this, ConfirmCartActivityNew.class);
                intent.putExtra("idClient", idClient);
                intent.putExtra("idRestaurant", idRestaurant);
                intent.putExtra("cosProduse", cosProduse);
                startActivity(intent);
                // Make this transition be instant
                finish();
                overridePendingTransition(0, 0);
            }
        }, 750); // Delay in milliseconds (750 milliseconds for the animation duration)



        //make it wait for the animation to finish
        slideDownAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                // Finish the current activity without any animation
                /*setContentView(R.layout.activity_main_menu_new);
                Intent intent = new Intent(LoginActivityNew.this, MainMenuActivityNew.class);
                intent.putExtra("idClient", idClient);
                startActivity(intent);
                //make this transition be instant
                finish();
                overridePendingTransition(0,0);*/
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void goToMenuActivity()
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startMainMenuActivity();
            }
        }, 500); // Delay in milliseconds (750 milliseconds for the animation duration)
    }

    private void startMainMenuActivity()
    {
        CardView cardView = findViewById(R.id.card_view);
        //cardView.startAnimation(slideUpAnimation);

        View cardHeight = findViewById(R.id.card_height);

        Animation cardContentFadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        cardContent.startAnimation(cardContentFadeOut);



        //make it wait for the animation to finish
        cardContentFadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                cardContent.setAlpha(0);
                transitionToMainMenuActivity(cardView, cardHeight);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void transitionToMainMenuActivity(CardView cardView, View cardHeight) {
        Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_confirm_order_to_main_menu);
        cardView.startAnimation(slideDownAnimation);


// Handler to delay the execution of the code
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Finish the current activity without any animation
                setContentView(R.layout.activity_main_menu_new);
                Intent intent = new Intent(ConfirmOrderActivityNew.this, MainMenuActivityNew.class);
                intent.putExtra("idClient", idClient);
                startActivity(intent);
                // Make this transition be instant
                finish();
                overridePendingTransition(0, 0);
            }
        }, 750); // Delay in milliseconds (750 milliseconds for the animation duration)



        //make it wait for the animation to finish
        slideDownAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
}