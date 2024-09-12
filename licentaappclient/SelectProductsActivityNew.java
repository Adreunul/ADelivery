package com.example.licentaappclient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SelectProductsActivityNew extends AppCompatActivity {

    int x;
    private CosProduse cosProduse;
    private MeniuRestaurant meniuRestaurant;
    String idRestaurant, idClient, numeRestaurant, rating, selectedCategory;
    double sumaMinima;

    private LinearLayout productList;
    LinearLayout cardContent;
    private TextView txtPrimire, txtRating;
    private Button btnCart;

    private ArrayList<Produs> produseList;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference productsRef = db.collection("Products");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_products_new);

        produseList = new ArrayList<>();

        idRestaurant = getIntent().getStringExtra("idRestaurant");
        idClient = getIntent().getStringExtra("idClient");
        cosProduse = (CosProduse) getIntent().getSerializableExtra("cosProduse");


        txtPrimire = findViewById(R.id.txtPrimire);
        txtRating = findViewById(R.id.txtRating);
        productList = findViewById(R.id.productList);
        btnCart = findViewById(R.id.btnCart);
        cardContent = findViewById(R.id.card_content);

        if(cosProduse == null)
            cosProduse = new CosProduse();
        meniuRestaurant = new MeniuRestaurant();

        x = 0;
        selectedCategory = "Toate";

        getDateRestaurant();
        setSpinnerForFiltering();
        setCartButton();
        if (produseList == null) {
            loadProductsFromFirestore();
        }

        loadProductsFromFirestore();
        cardContent.animate().alpha(1).setDuration(1000);
    }

    private void getDateRestaurant() {
        // Query the "Restaurants" collection for documents where "idRestaurant" field equals idRestaurant
        Query restaurantQuery = db.collection("Restaurants").whereEqualTo("idRestaurant", idRestaurant);

        restaurantQuery.get().addOnSuccessListener(querySnapshot -> {
            // Check if there is any document that matches the query
            if (!querySnapshot.isEmpty()) {
                // Assuming there's only one document matching the query
                DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);

                // Retrieve data from the document
                numeRestaurant = documentSnapshot.getString("numeRestaurant");
                rating = String.valueOf(documentSnapshot.getDouble("rating"));
                txtPrimire.setText(numeRestaurant);
                txtRating.setText(rating);
                // Do something with the retrieved data
            }
        }).addOnFailureListener(e -> {
            Log.e("getDateRestaurant", "Error getting documents: " + e.getMessage());
        });
    }


    private void setSpinnerForFiltering() {
        Spinner spinnerFilterOptions = findViewById(R.id.spinnerFilterOptions);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.filter_options_for_products, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterOptions.setAdapter(adapter);

        spinnerFilterOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (x > 0) {
                    String selectedOption = parent.getItemAtPosition(position).toString();
                    createCardsForProducts(selectedOption);
                }
                x++;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle nothing selected if needed
            }
        });

    }

    private void loadProductsFromFirestore() {

        Query query = productsRef.whereEqualTo("idRestaurant", idRestaurant);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null) {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String idProdus = document.getString("idProdus");
                        String numeProdus = document.getString("numeProdus");
                        String categorie = document.getString("categorie");
                        String linkPoza = document.getString("linkPoza");
                        int gramaj = document.getLong("gramaj").intValue();
                        int nrComenzi = document.getLong("nrComenzi").intValue();
                        double pretProdus = document.getDouble("pret");
                        boolean arePreferinte = document.getBoolean("areOptiuni");

                        Produs produs;

                        List<Ingredient> ingredienteProdus = new ArrayList<>();
                        List<Map<String, Object>> ingredienteFirestore = (List<Map<String, Object>>) document.get("Ingredients");
                        for (Map<String, Object> ingredient : ingredienteFirestore) {
                            String numeIngredient = (String) ingredient.get("numeIngredient");
                            int gramajIngredient = ((Long) ingredient.get("gramajIngredient")).intValue();

                            ingredienteProdus.add(new Ingredient(numeIngredient, gramajIngredient));
                        }

                        Collections.sort(ingredienteProdus, (i1, i2) -> i2.getGramaj() - i1.getGramaj());

                        List<String> ingrediente = new ArrayList<>();
                        //iterate thorugh ingredienteProdus and add to ingrediente
                        for (Ingredient ingredient : ingredienteProdus) {
                            ingrediente.add(ingredient.getNume());
                        }

                        if (arePreferinte) {
                            List<Preferinta> preferinteList = new ArrayList<>();
                            List<Map<String, Object>> optiuni = (List<Map<String, Object>>) document.get("Optiuni");
                            for (Map<String, Object> optiune : optiuni) {
                                String intrebare = (String) optiune.get("Intrebare");
                                boolean raspunsMultiplu = (boolean) optiune.get("raspunsMultiplu");
                                List<String> raspunsuri = new ArrayList<>();
                                for (int i = 0; i < optiune.size() - 2; i++) {
                                    String optiuneText = (String) optiune.get("optiune" + i);
                                    raspunsuri.add(optiuneText);
                                }
                                Preferinta preferinta = new Preferinta(intrebare, raspunsMultiplu, raspunsuri, new ArrayList<>());
                                preferinteList.add(preferinta);
                            }
                            produs = new Produs(idProdus, numeProdus, categorie, linkPoza, gramaj, nrComenzi, pretProdus, arePreferinte, ingrediente, preferinteList);
                        } else {
                            produs = new Produs(idProdus, numeProdus, categorie, linkPoza, gramaj, nrComenzi, pretProdus, arePreferinte, ingrediente);
                        }

                        produseList.add(produs);
                    }
                    meniuRestaurant.setProduseList(produseList);
                    meniuRestaurant.sortProduseAlphabetically();
                    createCardsForProducts();
                }
            }
        });
    }


    private void createCardsForProducts() {
        if (productList.getChildCount() > 1)
            productList.removeViews(0, productList.getChildCount());

        //iterate produse list an call createCard for each
        for (Produs produs : meniuRestaurant.getProduseList()) {
            createCard(produs);
            Log.d("Produs", produs.getNumeProdus());
        }
    }

    private void createCardsForProducts(String selectedOption) {
        if(Objects.equals(selectedOption, "Băutură"))
            selectedOption = "Bautura";

        if (productList.getChildCount() >= 1)
            productList.removeViews(0, productList.getChildCount());

        if (selectedOption.equals("Toate")) {
            meniuRestaurant.sortProduseAlphabetically();
            for (Produs produs : meniuRestaurant.getProduseList()) {
                createCard(produs);
            }
        } else if (selectedOption.equals("Comenzi")) {
            meniuRestaurant.sortProduseByNrComenzi();
            for (Produs produs : meniuRestaurant.getProduseList()) {
                createCard(produs);
            }
        } else if (!selectedOption.equals("Toate") && !selectedOption.equals("Comenzi")) {
            meniuRestaurant.sortProduseAlphabetically();
            for (Produs produs : meniuRestaurant.getProduseList()) {
                if (produs.getCategorie().equals(selectedOption)) {
                    createCard(produs);
                }
            }
        }
    }

    private void createCardsForProductsByName(String cautare) {
        if (productList.getChildCount() > 1)
            productList.removeViews(0, productList.getChildCount());

        //iterate produse list an call createCard for each
        for (Produs produs : meniuRestaurant.getProduseList()) {
            if(isSuitableForSearch(produs.getNumeProdus(), cautare))
                createCard(produs);
            Log.d("Produs", produs.getNumeProdus());
        }
    }

    private boolean isSuitableForSearch(String numeProdus, String cautare){
        if(cautare.length() == 0)
            return true;

        if(cautare.length() <= 2)
            return numeProdus.toLowerCase().contains(cautare.toLowerCase());

        return numeProdus.toLowerCase().contains(cautare.toLowerCase());
    }

    public void selectProductsByName(View v)
    {
        TextView txtSearch = findViewById(R.id.edtSearch);
        String cautare = txtSearch.getText().toString();
        createCardsForProductsByName(cautare);
    }

    private void createCard(Produs produs) {
        View cardView = getLayoutInflater().inflate(R.layout.product_card, null);

        TextView txtNumeProdus = cardView.findViewById(R.id.lblNumeProdus);
        TextView txtPret = cardView.findViewById(R.id.lblPret);
        TextView txtCantitate = cardView.findViewById(R.id.lblCantitate);
        TextView txtGramaj = cardView.findViewById(R.id.lblGramaj);
        TextView txtIngrediente = cardView.findViewById(R.id.lblIngrediente);
        Button btnMinus = cardView.findViewById(R.id.btnMinus);
        Button btnPlus = cardView.findViewById(R.id.btnPlus);
        ImageView imgProdus = cardView.findViewById(R.id.pozaProdus);


        Glide.with(this)
                .load(produs.getLinkPoza())
                .placeholder(R.drawable.placeholder) // Placeholder image while loading
                .error(R.drawable.placeholder) // Error image if unable to load
                .into(imgProdus);

        txtNumeProdus.setText(produs.getNumeProdus());
        txtPret.setText(String.valueOf(produs.getPretProdus()));
        txtCantitate.setText(String.valueOf(cosProduse.getCantitateProdus(produs.getIdProdus())));
        txtGramaj.setText(String.format("(%sg)", produs.getGramaj()));
        txtIngrediente.setText(String.valueOf(produs.getIngrediente()));

        // Add margin to the card
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 25); // 10dp bottom margin
        cardView.setLayoutParams(params);

        // Add OnClickListener to btnAction
        btnPlus.setOnClickListener(v -> {
            cosProduse.addProdus(produs);
            txtCantitate.setText(String.valueOf(cosProduse.getCantitateProdus(produs.getIdProdus())));

            setCartButton();
        });

        btnMinus.setOnClickListener(v -> {
            int cantitate = cosProduse.getCantitateProdus(produs.getIdProdus());
            if (cantitate > 0) {
                Log.d("plm", "plm1");
                if (produs.getArePreferinte()) {
                    if (cosProduse.getAreProdusCuOptiuniNealese(produs.getIdProdus()))
                    {
                        Log.d("plm", "plm2");
                        cosProduse.stergeProdusCuOptiuniNealese(produs.getIdProdus());
                        txtCantitate.setText(String.valueOf(cosProduse.getCantitateProdus(produs.getIdProdus())));
                    } else {
                        Log.d("plm", "plm3");
                        if(cantitate > 1)
                            startConfirmCartActivity(true, produs);
                        else {
                            cosProduse.removeProdus(produs.getIdProdus());
                            txtCantitate.setText(String.valueOf(cosProduse.getCantitateProdus(produs.getIdProdus())));
                        }
                    }
                } else {
                    Log.d("plm", "plm4");
                    cosProduse.removeProdus(produs.getIdProdus());
                    txtCantitate.setText(String.valueOf(cosProduse.getCantitateProdus(produs.getIdProdus())));
                }

                Log.d("plm", "plm5");
                setCartButton();
            }
        });

        // Add the card to the LinearLayout
        LinearLayout cardContainer = findViewById(R.id.productList);
        cardContainer.addView(cardView);
    }



    public void startConfirmCartActivity(View v)
    {
        startConfirmCartActivity(false, null);
    }

    private void startConfirmCartActivity(boolean forRemoving, Produs produs) {
        if (cosProduse.getTotal() > 0) {
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
                    transitionToConfirmCartActivity(forRemoving, produs);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }
    }

    private void transitionToConfirmCartActivity(boolean forRemoving, Produs produs) {

        setContentView(R.layout.activity_confirm_cart);
        Intent intent = new Intent(SelectProductsActivityNew.this, ConfirmCartActivityNew.class);
        intent.putExtra("idClient", idClient);
        intent.putExtra("idRestaurant", idRestaurant);
        intent.putExtra("cosProduse", cosProduse);
        intent.putExtra("forRemoving", forRemoving);

        if(forRemoving)
            intent.putExtra("produs", produs);

        startActivity(intent);
        overridePendingTransition(0, 0);
        //make this transition be instant
        finish();
    }


    public void startSelectRestaurantActivity(View v) {
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
                transitionToSelectRestaurantActivity(cardView);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void transitionToSelectRestaurantActivity(CardView cardView) {
        Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_select_products_to_select_restaurant);
        cardView.startAnimation(slideDownAnimation);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setContentView(R.layout.activity_select_restaurant_new);
                Intent intent = new Intent(SelectProductsActivityNew.this, SelectRestaurantActivityNew.class);
                intent.putExtra("idClient", idClient);
                startActivity(intent);
                overridePendingTransition(0, 0);
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


    public void setCartButton() {
        btnCart.setText(MessageFormat.format("{0} produse pentru {1} ron", cosProduse.getNrOfProducts(), cosProduse.getTotal()));
    }
}