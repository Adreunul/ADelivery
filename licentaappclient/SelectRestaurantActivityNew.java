package com.example.licentaappclient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ImageButton;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.List;

public class SelectRestaurantActivityNew extends AppCompatActivity {
    int x;
    private String idClient;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    LinearLayout cardsToAnimate, cardContent;

    private CollectionReference restaurantsRef = db.collection("Restaurants");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_restaurant_new);

        cardsToAnimate = findViewById(R.id.restaurantList);
        cardContent = findViewById(R.id.card_content);

        idClient = getIntent().getStringExtra("idClient");

        x = 0;
        setSpinnerForFiltering();
        createCardsForRestaurants();

        cardContent.animate().alpha(1).setDuration(1000);
    }

    private void setSpinnerForFiltering()
    {
        Spinner spinnerFilterOptions = findViewById(R.id.spinnerFilterOptions);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.filter_options_for_restaurants, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterOptions.setAdapter(adapter);

        spinnerFilterOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(x > 0) {
                    String selectedOption = parent.getItemAtPosition(position).toString();
                    createCardsForRestaurants(selectedOption);
                }
                x++;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle nothing selected if needed
            }
        });

    }


    private void createCardsForRestaurants() {
        restaurantsRef.orderBy("numeRestaurant").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null) {
                    // Iterate through the documents and create cards
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Boolean isDeschis = document.getBoolean("deschis");
                        int nrProduseDisponibile = document.getLong("nrProduseDisponibile").intValue();
                        if (isDeschis != null && isDeschis && nrProduseDisponibile > 0)
                            createCard(document);
                    }
                }
            }
        });
    }


    private void createCardsForRestaurants(String criteriuSortare) {
        //make it fade out
        cardContent.animate().alpha(0).setDuration(1000);

        // Initialize a query for the restaurants collection
        Query restaurantsQuery;

        // Sort restaurants based on the sorting criterion
        switch (criteriuSortare) {
            /*case "Favorite":
                // Query the restaurants collection and order by whether the restaurant is favorited by the client
                restaurantsQuery = restaurantsRef.orderBy("idRestaurant", Query.Direction.DESCENDING);
                break;*/
            case "Rating":
                // Query the restaurants collection and order by rating in descending order
                restaurantsQuery = restaurantsRef.orderBy("rating", Query.Direction.DESCENDING);
                break;
            default:
                // Default sorting: alphabetical order based on restaurant name
                restaurantsQuery = restaurantsRef.orderBy("numeRestaurant");
                break;
        }

        LinearLayout cardContainer = findViewById(R.id.restaurantList);
        cardContainer.removeAllViews();

        // Execute the query
        restaurantsQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null) {
                    // Iterate through the documents and create cards
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Boolean isDeschis = document.getBoolean("deschis");
                        int nrProduseDisponibile = document.getLong("nrProduseDisponibile").intValue();

                        if(criteriuSortare.equals("Favorite"))
                        {
                            String idRestaurant = document.getString("idRestaurant");
                            checkIfRestaurantIsFavorited(document, idRestaurant);
                        }
                        if (isDeschis != null && isDeschis && nrProduseDisponibile > 0 && !criteriuSortare.equals("Favorite"))
                            createCard(document);
                    }
                    cardContent.animate().alpha(1).setDuration(1000);
                }
            }
        });
    }

    private void createCardsForRestaurantsByName(String cautare) {
        //make it fade out
        cardContent.animate().alpha(0).setDuration(1000);

        // Initialize a query for the restaurants collection
        Query restaurantsQuery;

        // Sort restaurants based on the sorting criterion

        // Default sorting: alphabetical order based on restaurant name
        restaurantsQuery = restaurantsRef.orderBy("numeRestaurant");

        LinearLayout cardContainer = findViewById(R.id.restaurantList);
        cardContainer.removeAllViews();

        // Execute the query
        restaurantsQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null) {
                    // Iterate through the documents and create cards
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Boolean isDeschis = document.getBoolean("deschis");
                        int nrProduseDisponibile = document.getLong("nrProduseDisponibile").intValue();

                        if (isDeschis != null && isDeschis && nrProduseDisponibile > 0 && isSuitableForSearch(document.getString("numeRestaurant"), cautare))
                            createCard(document);
                    }
                    cardContent.animate().alpha(1).setDuration(1000);
                }
            }
        });
    }

    private boolean isSuitableForSearch(String numeRestaurant, String cautare) {
        if(cautare.length() == 0)
            return true;

        if(cautare.length() <= 2)
            return numeRestaurant.toLowerCase().startsWith(cautare.toLowerCase());

        return numeRestaurant.toLowerCase().contains(cautare.toLowerCase());
    }

    public void selectRestaurantsByName(View v)
    {
        TextView txtSearchRestaurant = findViewById(R.id.edtSearch);
        String cautare = txtSearchRestaurant.getText().toString();
        createCardsForRestaurantsByName(cautare);
    }


    private void createCard(QueryDocumentSnapshot document) {
        // Inflate the card layout
        View cardView = getLayoutInflater().inflate(R.layout.restaurant_card, null);

        // Find TextViews and ImageButton inside the card layout
        TextView lblRestaurantName = cardView.findViewById(R.id.lblRestaurantName);
        TextView lblRating = cardView.findViewById(R.id.lblRating);
        TextView lblProgram = cardView.findViewById(R.id.lblProgram);
        ImageButton btnAction = cardView.findViewById(R.id.btnAction);

        // Set text based on document data
        String idRestaurant = document.getString("idRestaurant");
        String numeRestaurant = document.getString("numeRestaurant");
        Double rating = document.getDouble("rating");
        String oraIncepere = document.getString("oraIncepere");
        String oraTerminare = document.getString("oraTerminare");

        lblRestaurantName.setText(numeRestaurant);
        lblRating.setText(String.valueOf(rating));
        lblProgram.setText(oraIncepere + " - " + oraTerminare);

        // Check if the restaurant is favorited and set the image source of btnAction
        checkIfRestaurantIsFavorited(idRestaurant, btnAction, "generare");

        // Add margin to the card
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 25); // 10dp bottom margin
        cardView.setLayoutParams(params);

        // Add OnClickListener to btnAction
        btnAction.setOnClickListener(v -> {
            // Check if the restaurant is already favorited
            checkIfRestaurantIsFavorited(idRestaurant, btnAction, "schimbare");
        });

        // Add OnClickListener to the cardView
        cardView.setOnClickListener(v -> {
            selectThisRestaurant(document);
        });

        // Add the card to the LinearLayout
        LinearLayout cardContainer = findViewById(R.id.restaurantList);
        cardContainer.addView(cardView);
    }


    private void selectThisRestaurant(QueryDocumentSnapshot document) {
        String idRestaurant = document.getString("idRestaurant");
        String numeRestaurant = document.getString("numeRestaurant");
        String rating = document.getDouble("rating").toString();


        Intent intent = new Intent(SelectRestaurantActivityNew.this, SelectProductsActivityNew.class);
        intent.putExtra("idClient", idClient);
        intent.putExtra("idRestaurant", idRestaurant);

        startSelectProductsActivity(intent);
        //startActivity(intent);
    }




    private void checkIfRestaurantIsFavorited(String idRestaurant, ImageButton btnAction, String actiune) {
        // Get reference to the Clients collection
        CollectionReference clientsRef = db.collection("Clients");

        // Query the Clients collection to find the document with idClient equal to the global variable idClient
        clientsRef.whereEqualTo("idClient", idClient)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Check if the restaurant is in the client's favorites
                            List<String> favoriteRestaurants = (List<String>) document.get("restauranteFavorite");
                            if (favoriteRestaurants != null && favoriteRestaurants.contains(idRestaurant)) {
                                if(actiune == "schimbare") {
                                    btnAction.setImageResource(R.drawable.restaurant_favorite_unchecked_icon);
                                    removeRestaurantFromFavorites(idRestaurant);
                                }
                                if(actiune == "generare"){
                                    btnAction.setImageResource(R.drawable.restaurant_favorite_checked_icon);
                                }

                            } else {
                                if(actiune == "schimbare") {
                                    btnAction.setImageResource(R.drawable.restaurant_favorite_checked_icon);
                                    addRestaurantToFavorites(idRestaurant);
                                }
                                if(actiune == "generare"){
                                    btnAction.setImageResource(R.drawable.restaurant_favorite_unchecked_icon);
                                }
                            }
                        }
                    }
                });
    }

    private void checkIfRestaurantIsFavorited(QueryDocumentSnapshot cardDocument, String idRestaurant) {
        // Get reference to the Clients collection
        CollectionReference clientsRef = db.collection("Clients");

        // Query the Clients collection to find the document with idClient equal to the global variable idClient
        clientsRef.whereEqualTo("idClient", idClient)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Check if the restaurant is in the client's favorites
                            List<String> favoriteRestaurants = (List<String>) document.get("restauranteFavorite");
                            if (favoriteRestaurants != null && favoriteRestaurants.contains(idRestaurant)) {
                                createCard(cardDocument);
                            }
                        }
                    }
                });
    }

    private void addRestaurantToFavorites(String idRestaurant) {
        // Get reference to the Clients collection
        CollectionReference clientsRef = db.collection("Clients");

        // Query the Clients collection to find the document with idClient equal to the global variable idClient
        clientsRef.whereEqualTo("idClient", idClient)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Add the restaurant to favorites
                            document.getReference().update("restauranteFavorite", FieldValue.arrayUnion(idRestaurant))
                                    .addOnSuccessListener(aVoid -> {
                                        // Successfully added the restaurant to favorites
                                    })
                                    .addOnFailureListener(e -> {
                                        // Failed to add the restaurant to favorites
                                    });
                        }
                    }
                });
    }

    private void removeRestaurantFromFavorites(String idRestaurant) {
        // Get reference to the Clients collection
        CollectionReference clientsRef = db.collection("Clients");

        // Query the Clients collection to find the document with idClient equal to the global variable idClient
        clientsRef.whereEqualTo("idClient", idClient)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Remove the restaurant from favorites
                            document.getReference().update("restauranteFavorite", FieldValue.arrayRemove(idRestaurant))
                                    .addOnSuccessListener(aVoid -> {
                                        // Successfully removed the restaurant from favorites
                                    })
                                    .addOnFailureListener(e -> {
                                        // Failed to remove the restaurant from favorites
                                    });
                        }
                    }
                });
    }

    public void startSelectProductsActivity(Intent intent)
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
                cardContent.setAlpha(0);
                transitionToSelectProductsActivity(intent, cardView);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void transitionToSelectProductsActivity(Intent intent, CardView cardView) {
        Animation slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_select_restaurant_to_select_products);
        cardView.startAnimation(slideUpAnimation);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Finish the current activity without any animation
                setContentView(R.layout.activity_select_products_new);
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



    public void startMainMenuActivity(View v)
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
                cardContent.setAlpha(0);
                transitionToMainMenuActivity(cardView);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void transitionToMainMenuActivity(CardView cardView)
    {
        Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_select_restaurant_to_main_menu);
        cardView.startAnimation(slideDownAnimation);


        // Handler to delay the execution of the code
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Finish the current activity without any animation
                setContentView(R.layout.activity_main_menu_new);
                Intent intent = new Intent(SelectRestaurantActivityNew.this, MainMenuActivityNew.class);
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
