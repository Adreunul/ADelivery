package com.example.licentaappclient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class SelectRestaurantActivity extends AppCompatActivity {

    private String idClient;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference restaurantsRef = db.collection("Restaurants");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_restaurant_new);

        idClient = getIntent().getStringExtra("idClient");

        NestedScrollView scrollView = findViewById(R.id.scrollView);
        LinearLayout linearLayout = findViewById(R.id.linearLayout);

        scrollView.setBackgroundColor(getResources().getColor(R.color.light_lavender));

        restaurantsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null) {
                    // Iterate through the documents and create buttons
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Boolean isDeschis = document.getBoolean("deschis");
                        int nrProduseDisponibile = document.getLong("nrProduseDisponibile").intValue();
                        if(isDeschis != null && isDeschis && nrProduseDisponibile > 0)
                            createButton(linearLayout, document);
                    }
                }
            }
        });
    }

    private void createButton(LinearLayout linearLayout, QueryDocumentSnapshot document) {


        Button button = new Button(this);
        String numeRestaurant =  document.getString("numeRestaurant");
        button.setText(String.format("%s - %s*", numeRestaurant, document.getDouble("rating")));

        button.setOnClickListener(view -> {
            String idRestaurant = document.getString("idRestaurant");
            Boolean servesteAperitiv = document.getBoolean("servesteAperitiv");
            Boolean servesteFelPrincipal = document.getBoolean("servesteFelPrincipal");
            Boolean servesteDesert = document.getBoolean("servesteDesert");
            Boolean servesteBautura = document.getBoolean("servesteBautura");

            String oraDeschidere = document.getString("oraIncepere");
            String oraInchidere = document.getString("oraTerminare");

            selectThisRestaurant(idRestaurant, servesteAperitiv, servesteFelPrincipal, servesteDesert, servesteBautura, numeRestaurant, oraDeschidere, oraInchidere);
        });

         linearLayout.addView(button);
    }

    private void selectThisRestaurant(String idRestaurant, boolean servesteAperitiv, boolean servesteFelPrincipal, boolean servesteDesert, boolean servesteBautura, String numeRestaurant, String oraDeschidere, String oraInchidere)
    {
        Intent intent = new Intent(SelectRestaurantActivity.this, SelectProductsActivity.class);
        intent.putExtra("idRestaurant", idRestaurant);
        intent.putExtra("idClient", idClient);
        intent.putExtra("servesteAperitiv", servesteAperitiv);
        intent.putExtra("servesteFelPrincipal", servesteFelPrincipal);
        intent.putExtra("servesteDesert", servesteDesert);
        intent.putExtra("servesteBautura", servesteBautura);
        intent.putExtra("numeRestaurant", numeRestaurant);
        intent.putExtra("oraDeschidere", oraDeschidere);
        intent.putExtra("oraInchidere", oraInchidere);

        startActivity(intent);
    }
}