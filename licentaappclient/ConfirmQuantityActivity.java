package com.example.licentaappclient;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConfirmQuantityActivity extends AppCompatActivity {

    ImageView imageView;
    TextView txtProductName, txtQuantity, txtIngrediente, txtPret;
    String idProdus;
    boolean areOptiuni;
    int cantitateInitiala;
    double pretProdus;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    CollectionReference productsRef = db.collection("Products");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_quantity);


        imageView = findViewById(R.id.imageView);

        txtProductName = findViewById(R.id.txtProductName);
        txtQuantity = findViewById(R.id.txtQuantity);
        txtIngrediente = findViewById(R.id.txtIngrediente);
        txtPret = findViewById(R.id.txtPret);

        idProdus = getIntent().getStringExtra("idProdus");
        cantitateInitiala = getIntent().getIntExtra("cantitateInitiala", 0);
        pretProdus = getIntent().getDoubleExtra("pret", 0.0);


        txtQuantity.setText(String.format("%d", cantitateInitiala));
        txtPret.setText(String.format("%s lei", String.format("%s", pretProdus)));

        Query query = productsRef.whereEqualTo("idProdus", idProdus);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Since you expect only one document, get the first one
                    DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);

                    // Retrieve the value of the 'numeProdus' field
                    String productName = documentSnapshot.getString("numeProdus");
                    String photoLink = documentSnapshot.getString("linkPoza");
                    areOptiuni = documentSnapshot.getBoolean("areOptiuni");

                    // Load image from URL into ImageView using Glide
                    Glide.with(this)
                            //.load("https://firebasestorage.googleapis.com/v0/b/licenta-c8625.appspot.com/o/download.jpeg?alt=media&token=8b589a3b-046a-4ee5-bca4-6824e8ea2190")
                            .load(photoLink)
                            .placeholder(R.drawable.placeholder) // Placeholder image while loading
                            .error(R.drawable.placeholder) // Error image if unable to load
                            .into(imageView);

                    displayIngredients();

                    // Set the value in your TextView
                    txtProductName.setText(productName);
                } else {
                    // Handle the case when no document matches the query
                    txtProductName.setText("Product not found");
                }
            } else {
                // Handle errors
                Exception exception = task.getException();
                if (exception != null) {
                    // Log or display the error
                }
            }
        });
    }

    public void displayIngredients() {
        // Reference to the "Products" collection
        CollectionReference productsCollection = db.collection("Products");

        // Query to get the document with the specified idProdus
        Query query = productsCollection.whereEqualTo("idProdus", idProdus);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Since you expect only one document, get the first one
                    DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);

                    // Retrieve the Ingredients array from the document
                    List<Map<String, Object>> ingredientsList = (List<Map<String, Object>>) documentSnapshot.get("Ingredients");

                    // Check if Ingredients array is not null
                    if (ingredientsList != null) {
                        // Build the display string for ingredients
                        StringBuilder ingredientsString = new StringBuilder();

                        for (Map<String, Object> ingredientMap : ingredientsList) {
                            String numeIngredient = (String) ingredientMap.get("numeIngredient");
                            int gramajIngredient = ((Long) ingredientMap.get("gramajIngredient")).intValue();

                            // Append ingredient details to the string
                            ingredientsString.append(gramajIngredient)
                                    .append("g ")
                                    .append(numeIngredient)
                                    .append(", ");
                        }

                        // Remove the trailing comma and space
                        if (ingredientsString.length() > 0) {
                            ingredientsString.setLength(ingredientsString.length() - 2);
                        }

                        txtIngrediente.setText(ingredientsString);
                    }
                } else {
                    // Handle the case when no document matches the query
                    Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Handle errors
                Exception exception = task.getException();
                if (exception != null) {
                    // Log or display the error
                    Toast.makeText(this, "Error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void modifyQuantity(View v)
    {
        String viewId = getResources().getResourceEntryName(v.getId());
        if(Objects.equals(viewId, "btnPlus"))
            operation(true);
        if(Objects.equals(viewId, "btnMinus"))
            operation(false);
    }

    public void operation(boolean add) {

        // Get the current quantity as a string
        String currentQuantityStr = txtQuantity.getText().toString();

        try {
            // Convert the current quantity to an integer
            int currentQuantity = Integer.parseInt(currentQuantityStr);

            int newQuantity;

            if(add)
                 newQuantity = currentQuantity + 1;
            else
                newQuantity = currentQuantity - 1;

            // Set the new quantity in the TextView
            if(newQuantity >= 0)
                    txtQuantity.setText(String.valueOf(newQuantity));
            else
                    txtQuantity.setText(String.valueOf(0));
        } catch (NumberFormatException e) {
            // Handle the case where the current quantity is not a valid number
            e.printStackTrace(); // Log the exception or handle it as needed
        }
    }

    public void confirm(View v)
    {
        String finalQuantity = txtQuantity.getText().toString();

        

        // Create an Intent to send the result back to the calling activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("finalQuantity", finalQuantity);
        resultIntent.putExtra("idProdus", idProdus);
        resultIntent.putExtra("pret", pretProdus);
        resultIntent.putExtra("numeProdus", txtProductName.getText().toString());
        resultIntent.putExtra("areOptiuni", areOptiuni);


        // Set the result code and data
        setResult(Activity.RESULT_OK, resultIntent);

        // Finish the current activity
        finish();
    }
}