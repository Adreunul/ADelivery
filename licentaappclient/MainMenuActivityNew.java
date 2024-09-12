package com.example.licentaappclient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainMenuActivityNew extends AppCompatActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference activeOrdersRef = db.collection("Active Orders");

    DocumentSnapshot documentFound;

    private Button btnOrder;
    private TextView txtLogOut;
    private LinearLayout cardContent;
    private String idClient, numeRestaurant, linkPoza = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu_new);

        idClient = getIntent().getStringExtra("idClient");

        btnOrder = findViewById(R.id.btn_order);
        txtLogOut = findViewById(R.id.log_out);
        cardContent = findViewById(R.id.card_content);

        checkForActiveOrder();
        checkIfOwesReview();


        cardContent.animate().alpha(1).setDuration(1000);
    }

    private void checkForActiveOrder() {
        db.collection("Active Orders").whereEqualTo("idClient", idClient)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        // Call the function to handle success and pass the documentSnapshot
                        showOrderButton(documentSnapshot);
                        Log.d("debug55", "1");
                    } else {
                        // Call the function to handle the case when no document is found
                        showOrderButton();
                        Log.d("debug55", "2");
                    }
                })
                .addOnFailureListener(e -> {
                    // Call the function to handle failure
                    showOrderButton();
                    Log.d("debug55", "3");
                });

    }

    private void showOrderButton()
    {
        btnOrder.setText("Plasează comanda");
        btnOrder.setOnClickListener(v -> {
            checkIfWithinSchedule();
        });
    }

    private void showOrderButton(DocumentSnapshot document)
    {
        btnOrder.setText("Comanda mea");
        btnOrder.setOnClickListener(v -> {
            checkOrder(document);
        });
    }

    private void checkOrder(DocumentSnapshot document)
    {
        String idRestaurant = document.getString("idRestaurant");
        double pret = document.getDouble("pret");
        String stareComanda = document.getBoolean("comandaPreluata") ? "Comanda este în curs de livrare" :  "Comanda este în curs de facere";
        String pinConfirmare = document.getString("pinConfirmare");

        Query restaurantQuery = db.collection("Restaurants").whereEqualTo("idRestaurant", idRestaurant);
        restaurantQuery.get().addOnSuccessListener(querySnapshot -> {
            // Check if there is any document that matches the query
            if (!querySnapshot.isEmpty()) {
                // Assuming there's only one document matching the query
                DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                numeRestaurant = documentSnapshot.getString("numeRestaurant");
                startCheckOrderActivity(stareComanda, pinConfirmare, numeRestaurant, pret);
            }
        }).addOnFailureListener(e -> {
            Log.e("getDateRestaurant", "Error getting documents: " + e.getMessage());
        });
    }


    private void checkIfWithinSchedule() {
        // Get the current system time
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

        // Reference to the "Reguli" collection
        CollectionReference reguliRef = db.collection("Reguli");

        // Get the document reference for the document "program1" and "program2"
        DocumentReference program1DocRef = reguliRef.document("program1");
        DocumentReference program2DocRef = reguliRef.document("program2");

        // Retrieve the document "program1"
        program1DocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot program1Doc = task.getResult();

                if (program1Doc.exists()) {
                    // Get the value of "oraIncepere" from program1 document
                    String oraIncepere = program1Doc.getString("oraIncepere");

                    // Parse the hour part from oraIncepere string
                    int intOraIncepere = Integer.parseInt(oraIncepere.split(":")[0]);

                    // Retrieve the document "program2"
                    program2DocRef.get().addOnCompleteListener(task2 -> {
                        if (task2.isSuccessful()) {
                            DocumentSnapshot program2Doc = task2.getResult();

                            if (program2Doc.exists()) {
                                // Get the value of "oraTerminare" from program2 document
                                String oraTerminare = program2Doc.getString("oraTerminare");

                                // Parse the hour part from oraTerminare string
                                int intOraTerminare = Integer.parseInt(oraTerminare.split(":")[0]);

                                // Check if the current hour falls within the range [oraIncepere, oraTerminare)
                                if (currentHour >= intOraIncepere && currentHour < intOraTerminare) {
                                    startSelectRestaurantActivity();
                                } else {
                                    // Current time is not within the specified range, show error message
                                    Toast.makeText(MainMenuActivityNew.this, "Ne aflam in afara programului, te rugăm să revii !", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.e("FirestoreError", "Document 'program2' does not exist.");
                            }
                        } else {
                            Log.e("FirestoreError", "Error getting document 'program2': ", task2.getException());
                        }
                    });
                } else {
                    Log.e("FirestoreError", "Document 'program1' does not exist.");
                }
            } else {
                Log.e("FirestoreError", "Error getting document 'program1': ", task.getException());
            }
        });
    }

    public void checkIfOwesReview()
    {
        owesReview();
    }

    private void owesReview() {
        // Reference to the "Clients" collection
        CollectionReference clientsRef = db.collection("Clients");

        // Query to find the document with the specified idClient
        Query query = clientsRef.whereEqualTo("idClient", idClient);

        // Execute the query
        String defaultResult = ""; // Default value if no matching document is found
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Assuming there's at most one matching document, get the first one
                    DocumentSnapshot clientDoc = querySnapshot.getDocuments().get(0);

                    // Get the values of 'datoreazaRating' and 'idRestaurantComandaAnterioara'
                    boolean owesRating = clientDoc.getBoolean("datoreazaRating");
                    String previousRestaurantId = clientDoc.getString("idRestaurantComandaAnterioara");

                    // If 'datoreazaRating' is true, return the previous restaurant ID, otherwise return ""
                    if (owesRating) {
                        showReviewPopup(previousRestaurantId);

                        // Update the 'datoreazaRating' field to false
                        clientsRef.document(clientDoc.getId())
                                .update("datoreazaRating", false)
                                .addOnSuccessListener(aVoid -> {
                                    // Update successful
                                    Log.d("FirestoreDebug", "'datoreazaRating' set to false for idClient: " + idClient);
                                })
                                .addOnFailureListener(e -> {
                                    // Update failed
                                    Log.e("FirestoreError", "Error updating 'datoreazaRating' field", e);
                                });
                    }
                } else {
                    // Handle the case when no document matches the query
                    Log.d("FirestoreDebug", "No document found for idClient: " + idClient);
                }
            } else {
                // Handle errors
                Log.e("FirestoreError", "Error querying Clients collection: ", task.getException());
            }
        });
    }

    private void showReviewPopup(String idRestaurant) {
        // Reference to the "Restaurants" collection
        CollectionReference restaurantsRef = db.collection("Restaurants");

        // Query to find the document with the specified idRestaurant
        Query query = restaurantsRef.whereEqualTo("idRestaurant", idRestaurant);

        // Execute the query
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Assuming there's at most one matching document, get the first one
                    DocumentSnapshot restaurantDoc = querySnapshot.getDocuments().get(0);

                    // Get the restaurant name from the document
                    String restaurantName = restaurantDoc.getString("numeRestaurant");

                    // Build and show the AlertDialog for the review
                    showReviewAlertDialog(restaurantName, idRestaurant);
                } else {
                    // Handle the case when no document matches the query
                    Log.d("FirestoreDebug", "No document found for idRestaurant: " + idRestaurant);
                }
            } else {
                // Handle errors
                Log.e("FirestoreError", "Error querying Restaurants collection: ", task.getException());
            }
        });
    }

    private void showReviewAlertDialog(String restaurantName, String idRestaurant) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.review_dialog, null);

        // Set the title and message for the AlertDialog
        builder.setTitle("Review Experience");
        builder.setMessage("Please share your review about your experience with: " + restaurantName);

        // Set up the ComboBox (Spinner) with options {1,2,3,4,5}
        Spinner spinnerRating = view.findViewById(R.id.spinnerRating);
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new Integer[]{1, 2, 3, 4, 5});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRating.setAdapter(adapter);

        builder.setView(view);

        // Set up the OK button
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle the OK button click action after getting the rating from the Spinner
                int selectedRating = (int) spinnerRating.getSelectedItem();
                registerReview(selectedRating, idRestaurant);

                // Dismiss the dialog
                dialog.dismiss();
            }
        });

        // Set up the Cancel button
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Dismiss the dialog
                dialog.dismiss();
                // Proceed with the activity as normal
                // Add any additional actions you need to perform on cancel
            }
        });

        // Show the AlertDialog
        builder.create().show();
    }

    private void registerReview(int rating, String idRestaurant) {
        // Reference to the "Pending Reviews" collection
        CollectionReference pendingReviewsRef = db.collection("Pending Reviews");

        // Create a new review document with the specified fields
        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("idRestaurant", idRestaurant);
        reviewData.put("rating", rating);

        // Add the review document to the "Pending Reviews" collection
        pendingReviewsRef.add(reviewData)
                .addOnSuccessListener(documentReference -> {
                    // Review added successfully
                    Log.d("ReviewSubmission", "Review added to Pending Reviews: " + documentReference.getId());
                    // You can add any additional actions here if needed
                })
                .addOnFailureListener(e -> {
                    // Error adding review
                    Log.e("FirestoreError", "Error adding review to Pending Reviews", e);
                    // You can handle the failure here, such as showing a Toast or logging an error
                });
    }

    public void startLoginActivity(View v)
    {
        deleteSharedPreferences();
        // Apply animations to the card view
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
                transitionToLoginActivity(cardView);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void transitionToLoginActivity(CardView cardView)
    {
        Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_main_menu_to_login);
        cardView.startAnimation(slideDownAnimation);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Finish the current activity without any animation
                setContentView(R.layout.activity_login_new);
                Intent intent = new Intent(MainMenuActivityNew.this, LoginActivityNew.class);
                startActivity(intent);
                overridePendingTransition(0,0);
                //make this transition be instant
                finish();
            }
        }, 750);


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

    private void startCheckOrderActivity(String stareComanda, String pinConfirmare, String numeRestaurant, double pret)
    {
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
                transitionToCheckOrderActivity(stareComanda, pinConfirmare, numeRestaurant, pret);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void transitionToCheckOrderActivity(String stareComanda, String pinConfirmare, String numeRestaurant, double pret)
    {
        setContentView(R.layout.activity_check_order);
        Intent intent = new Intent(MainMenuActivityNew.this, CheckOrderActivity.class);
        intent.putExtra("numeRestaurant", numeRestaurant);
        intent.putExtra("pinConfirmare", pinConfirmare);
        intent.putExtra("stareComanda", stareComanda);
        intent.putExtra("pret", pret);
        intent.putExtra("idClient", idClient);


        startActivity(intent);
        overridePendingTransition(0, 0);
        //make this transition be instant
        finish();
    }


    private void startSelectRestaurantActivity()
    {
        CardView cardView = findViewById(R.id.card_view);


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
                transitionToSelectRestaurantActivity(cardView);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void transitionToSelectRestaurantActivity(CardView cardView)
    {
        Animation slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_main_menu_to_select_restaurant);
        cardView.startAnimation(slideUpAnimation);



        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Finish the current activity without any animation
                setContentView(R.layout.activity_select_restaurant_new);
                Intent intent = new Intent(MainMenuActivityNew.this, SelectRestaurantActivityNew.class);
                intent.putExtra("idClient", idClient);
                startActivity(intent);
                overridePendingTransition(0,0);
                //make this transition be instant
                finish();
            }
        }, 750);


        //make it wait for the animation to finish
        slideUpAnimation.setAnimationListener(new Animation.AnimationListener() {
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

    public void startManageAccountActivity(View v)
    {
        // Apply animations to the card view
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
                transitionToManageAccountActivity(cardView);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void transitionToManageAccountActivity(CardView cardView) {
        Animation slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_main_menu_to_manage_account);
        cardView.startAnimation(slideUpAnimation);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Finish the current activity without any animation
                setContentView(R.layout.activity_manage_account_new);
                Intent intent = new Intent(MainMenuActivityNew.this, ManageAccountActivityNew.class);
                intent.putExtra("idClient", idClient);
                startActivity(intent);
                overridePendingTransition(0, 0);
                //make this transition be instant
                finish();
            }
        }, 750);



        //make it wait for the animation to finish
        slideUpAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                /*// Finish the current activity without any animation
                setContentView(R.layout.activity_manage_account_new);
                Intent intent = new Intent(MainMenuActivityNew.this, ManageAccountActivityNew.class);
                intent.putExtra("idClient", idClient);
                startActivity(intent);
                overridePendingTransition(0, 0);
                //make this transition be instant
                finish();*/
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void deleteSharedPreferences() {
        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }
}