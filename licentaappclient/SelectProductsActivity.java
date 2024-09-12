package com.example.licentaappclient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.bitmap_recycle.IntegerArrayAdapter;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Map;

public class SelectProductsActivity extends AppCompatActivity {

    private static final int REQUEST_CONFIRM_QUANTITY = 1, REQUEST_OPTIONS = 2;
    private CosProduse cosProduse;
    private String selectedCategory = "";
    String idRestaurant, idClient, numeRestaurant, oraDeschidere, oraInchidere;
    double sumaMinima;
    private TextView restaurantNameTextView, restaurantScheduleTextView;

    private Button checkCartButton, addMentionButton;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference productsRef = db.collection("Products");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_products);

        cosProduse = new CosProduse();

        NestedScrollView scrollView = findViewById(R.id.scrollView);
        LinearLayout linearLayout = findViewById(R.id.linearLayout);

        scrollView.setBackgroundColor(getResources().getColor(R.color.light_lavender));

        idRestaurant = getIntent().getStringExtra("idRestaurant");
        idClient = getIntent().getStringExtra("idClient");
        Log.d("idClientSelectProducts", idClient);

        getDateRestaurant();

        setRestaurantDetails(numeRestaurant, oraDeschidere, oraInchidere);

        sumaMinima = getRegulaSumaMinima();

        boolean servesteBautura = getIntent().getBooleanExtra("servesteBautura", false);
        boolean servesteFelPrincipal = getIntent().getBooleanExtra("servesteFelPrincipal", false);
        boolean servesteDesert = getIntent().getBooleanExtra("servesteDesert", false);
        boolean servesteAperitiv = getIntent().getBooleanExtra("servesteAperitiv", false);

        LinearLayout categoryLinearLayout = new LinearLayout(this);
        categoryLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        categoryLinearLayout.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        // Add buttons for each category based on the restaurant's attributes
        if (servesteBautura) {
            createCategoryButton(categoryLinearLayout, linearLayout, "Bautura");
        }
        if (servesteFelPrincipal) {
            createCategoryButton(categoryLinearLayout, linearLayout,"Fel principal");
        }
        if (servesteDesert) {
            createCategoryButton(categoryLinearLayout, linearLayout, "Desert");
        }
        if (servesteAperitiv) {
            createCategoryButton(categoryLinearLayout, linearLayout, "Aperitiv");
        }

        linearLayout.addView(categoryLinearLayout);

        /*Query query = productsRef.whereEqualTo("idRestaurant", idRestaurant);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null) {
                    // Iterate through the documents and create buttons
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        createButton(linearLayout, document);
                    }

                    addInvisibleButtons(linearLayout, 2);
                    addBottomButton(linearLayout);
                }
            } else {
                // Handle errors
            }
        });*/
    }

    private void getDateRestaurant()
    {
        DocumentReference restaurantRef = db.collection("Restaurants").document(idRestaurant);

        restaurantRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                numeRestaurant = documentSnapshot.getString("numeRestaurant");
                oraDeschidere = documentSnapshot.getString("oraDeschidere");
                oraInchidere = documentSnapshot.getString("oraInchidere");
            }
        });
    }

    private void createCategoryButton(LinearLayout linearLayoutForCategory, LinearLayout linearLayoutForProducts, String categoryName) {
        Button categoryButton = new Button(this);
        categoryButton.setText(categoryName);
        categoryButton.setTextSize(12); // Adjust text size as needed


        categoryButton.setBackgroundColor(getResources().getColor(R.color.mid_lavender));

        // Add background and touch feedback to categoryLinearLayout
        Drawable categoryBackground = new ColorDrawable(getResources().getColor(R.color.semi_transparent_gray)); // define color in colors.xml
        RippleDrawable rippleDrawable = new RippleDrawable(
                ColorStateList.valueOf(getResources().getColor(R.color.category_press_color)), // define color in colors.xml
                categoryBackground,
                null
        );
        linearLayoutForCategory.setBackground(rippleDrawable);

        categoryButton.setOnClickListener(view -> {
            // Update the selected category
            selectedCategory = categoryName;

            // Load products of the selected category
            loadProductsByCategory(linearLayoutForProducts);
            updateCategoryButtons(linearLayoutForCategory);
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(10, 0, 10, 0); // Add margins between buttons
        categoryButton.setLayoutParams(params);

        linearLayoutForCategory.addView(categoryButton);
    }

    private void updateCategoryButtons(LinearLayout linearLayoutForCategory) {
        // Iterate through child views of linearLayoutForCategory
        for (int i = 0; i < linearLayoutForCategory.getChildCount(); i++) {
            View child = linearLayoutForCategory.getChildAt(i);

            // Check if the child is a Button
            if (child instanceof Button) {
                Button button = (Button) child;

                // Set background color based on the selectedCategory
                if (button.getText().toString().equals(selectedCategory)) {
                    button.setBackgroundColor(getResources().getColor(R.color.lavender)); // define color in colors.xml
                } else {
                    button.setBackgroundColor(getResources().getColor(R.color.mid_lavender)); // define color in colors.xml
                }

                // Set selected state based on the selectedCategory
                button.setSelected(button.getText().toString().equals(selectedCategory));
            }
        }
    }

    private void loadProductsByCategory(LinearLayout linearLayout) {
        // Check if a category is selected
        if (!selectedCategory.isEmpty()) {
            Query query = productsRef.whereEqualTo("idRestaurant", idRestaurant)
                    .whereEqualTo("categorie", selectedCategory);

            query.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot querySnapshot = task.getResult();
                    if (querySnapshot != null) {
                        // Clear previous product buttons
                        linearLayout.removeViews(1, linearLayout.getChildCount() - 1);

                        // Iterate through the documents and create buttons
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            createButton(linearLayout, document);
                        }

                        addInvisibleButtons(linearLayout, 5);
                        addConfirmButton(linearLayout);
                        addCheckCartButton(linearLayout);
                        addMentionButton(linearLayout);
                    }
                } else {
                    // Handle errors
                }
            });
        }
    }

    private void createButton(LinearLayout linearLayout, QueryDocumentSnapshot document) {

        boolean esteDisponibil = false;
        esteDisponibil = document.getBoolean("disponibil");

        if(esteDisponibil)
        {
            Button button = new Button(this);
            button.setText(document.getString("numeProdus"));

            button.setOnClickListener(view -> {
                String idProdus = document.getString("idProdus");
                double pret = document.getDouble("pret");

                selectThisProduct(idProdus, pret);
            });

            linearLayout.addView(button);
        }
    }

    private void addMentionButton(LinearLayout linearLayout)
    {
        addMentionButton = new Button(this);
        addMentionButton.setText("Add Mention");

        addMentionButton.setOnClickListener(view -> {
            addMention();
        });

        linearLayout.addView(addMentionButton);
    }


    private void addConfirmButton(LinearLayout linearLayout)
    {
        Button confirmButton = new Button(this);
        confirmButton.setText("Confirm");

        confirmButton.setOnClickListener(view -> {
            confirmCart();
        });

        linearLayout.addView(confirmButton);
    }

    private void addCheckCartButton(LinearLayout linearLayout)
    {
        checkCartButton = new Button(this);
        double cartPrice = cosProduse.getTotal();

        checkCartButton.setText(String.format("%s - Check Cart", cartPrice));

        checkCartButton.setOnClickListener(view -> {
            showCart();
        });

        linearLayout.addView(checkCartButton);
    }

    private void updateCartValue()
    {
        double cartPrice = cosProduse.getTotal();

        checkCartButton.setText(String.format("%s - Check Cart", cartPrice));
    }

    //for UI
    private void addInvisibleButtons(LinearLayout linearLayout, int count) {
        for (int i = 0; i < count; i++) {
            Button invisibleButton = new Button(this);
            invisibleButton.setVisibility(View.INVISIBLE);
            linearLayout.addView(invisibleButton);
        }
    }

    private void selectThisProduct(String idProdus, double pret)
    {
        Intent intent = new Intent(SelectProductsActivity.this, ConfirmQuantityActivity.class);
        intent.putExtra("idProdus", idProdus);
        //intent.putExtra("cantitateInitiala", cosProduse.getQuantityOfProduct(idProdus));
        intent.putExtra("pret", pret);
        startActivityForResult(intent, REQUEST_CONFIRM_QUANTITY);
    }


    private void collectOptions(String numeProdus, int quantity, String idProdus, int index)
    {
        Intent intent = new Intent(SelectProductsActivity.this, SelectOptionsActivity.class);
        intent.putExtra("idProdus", idProdus);
        intent.putExtra("numeProdus", numeProdus);
        intent.putExtra("quantity", quantity);
        intent.putExtra("index", index);
        startActivityForResult(intent, REQUEST_OPTIONS);
    }

    private void confirmCart()
    {
        Toast.makeText(this, "" + cosProduse.getTotal(), Toast.LENGTH_SHORT).show();
        if(cosProduse.getTotal() > sumaMinima) {
            Toast.makeText(this, "Esti tanc", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SelectProductsActivity.this, ConfirmOrderActivity.class);
            intent.putExtra("idClient", idClient);
            intent.putExtra("pret", cosProduse.getTotal());
            intent.putExtra("cosProduse", cosProduse);
            intent.putExtra("idRestaurant", idRestaurant);
            intent.putExtra("mentiuni", cosProduse.getMentiuni());
            intent.putExtra("optiuniText", cosProduse.getOptiuniText());
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CONFIRM_QUANTITY && resultCode == Activity.RESULT_OK) {
            // Retrieve the result from ConfirmQuantityActivity
            String finalQuantity = data.getStringExtra("finalQuantity");
            String idProdus = data.getStringExtra("idProdus");
            double pretProdus = data.getDoubleExtra("pret", 0.0);
            String numeProdus = data.getStringExtra("numeProdus");
            boolean areOptiuni = data.getBooleanExtra("areOptiuni", false);

            if(areOptiuni && Integer.parseInt(finalQuantity) > 0) {
                cosProduse.clearOptiuniList(idProdus);
                collectOptions(numeProdus, Integer.parseInt(finalQuantity), idProdus, 1);
            }
            if(areOptiuni && Integer.parseInt(finalQuantity) == 0)
                cosProduse.clearOptiuniList(idProdus);


            cosProduse.addProdus(idProdus, Integer.parseInt(finalQuantity), pretProdus);
            cosProduse.expose();

            updateCartValue();
        }

        if (requestCode == REQUEST_OPTIONS && resultCode == Activity.RESULT_OK)
        {
            String selectedOptions = data.getStringExtra("selectedOptions");
            String idProdus = data.getStringExtra("idProdus");
            String numeProdus = data.getStringExtra("numeProdus");
            int quantity = data.getIntExtra("quantity", 0);
            int index = data.getIntExtra("index", 0);

            //selectedOptionsForOrder.add(selectedOptions);
            //cosProduse.clearOptiuniList(idProdus);
            cosProduse.addOptiuni(idProdus, selectedOptions);

            if(index <= quantity)
                collectOptions(numeProdus, quantity, idProdus, index);
        }
    }

    private void addMention()
    {
        // Create an EditText view for the user to input the mention
        final EditText input = new EditText(this);
        input.setText(cosProduse.getMentiuni()); // Set the initial text to the current mention

        // Create the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Mention")
                .setView(input)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Retrieve the text entered by the user
                        String mention = input.getText().toString();

                        // Set the mention in the CosProduse object
                        cosProduse.setMentiune(mention);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Cancel button clicked, do nothing
                    }
                });

        // Show the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private double getRegulaSumaMinima()
    {
        CollectionReference rulesCollection = db.collection("Reguli");
        String documentId = "Reguli"; // Assuming "Reguli" is the document ID
        double[] sumaMinima = new double[1];

        DocumentReference rulesDocumentRef = rulesCollection.document(documentId);

        rulesDocumentRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                sumaMinima[0] = documentSnapshot.getDouble("SumaMinima");
                if (sumaMinima[0] != 0.0)
                    setSumaMinima(sumaMinima[0]);
                else
                    setSumaMinima(50.0);
            }
        });
        return 0.0;
    }

    private void showCart()
    {
        Map<String, Integer> productList = cosProduse.getProductList();

        StringBuilder message = new StringBuilder();
        int totalProducts = productList.size();
        int[] count = {0}; // Counter to track the number of retrieved product names

        for (Map.Entry<String, Integer> entry : productList.entrySet()) {
            String productId = entry.getKey();
            int quantity = entry.getValue();

            if(quantity > 0) {
                // Retrieve product name from Firestore using productId
                retrieveProductNameFromFirestore(productId, productName -> {
                    // Append product name and quantity to the message
                    message.append(productName).append(": ").append(quantity).append("\n");


                    // Increment the counter
                    count[0]++;

                    // If all product names are retrieved, show the message box
                    if (count[0] == totalProducts) {
                        // Show message box with cart information
                        message.append("\n").append(CosProduse.getOptiuniForCart());

                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Cart Information")
                                .setMessage(message.toString())
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // Close the dialog
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });
            }
        }
    }




    private void retrieveProductNameFromFirestore(String productId, final OnProductNameRetrievedListener listener)
    {
        Query query = productsRef.whereEqualTo("idProdus", productId);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Assuming there's at most one matching document, get the first one
                    DocumentSnapshot document = querySnapshot.getDocuments().get(0);

                    String productName = document.getString("numeProdus");
                    listener.onProductNameRetrieved(productName);
                } else {
                    listener.onProductNameRetrieved("Product Name Not Found");
                }
            } else {
                listener.onProductNameRetrieved("Error Retrieving Product Name");
            }
        });
    }

    private void setRestaurantDetails(String numeRestaurant, String oraDeschidere, String oraInchidere)
    {
        restaurantNameTextView = findViewById(R.id.restaurantNameTextView);
        restaurantNameTextView.setText(numeRestaurant);

        Log.d("schedule", oraDeschidere + " " + oraInchidere);
        restaurantScheduleTextView = findViewById(R.id.restaurantScheduleTextView);
        restaurantScheduleTextView.setText(String.format("%s - %s", oraDeschidere, oraInchidere));
    }

    private void setSumaMinima(double sumaMinima)
    {
        this.sumaMinima = sumaMinima;
    }


    // Define a listener interface
    interface OnProductNameRetrievedListener {
        void onProductNameRetrieved(String productName);
    }

}