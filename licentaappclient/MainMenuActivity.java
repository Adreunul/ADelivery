package com.example.licentaappclient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
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

public class MainMenuActivity extends AppCompatActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference activeOrdersRef = db.collection("Active Orders");

    DocumentSnapshot documentFound;

    private Button btnPlaceOrder, btnCheckOrder, btnLogOut;
    private String idClient, numeRestaurant, linkPoza = "";

    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        idClient = getIntent().getStringExtra("idClient");

        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        btnCheckOrder = findViewById(R.id.btnCheckOrder);
        btnLogOut = findViewById(R.id.btnLogOut);

        imageView = findViewById(R.id.imageView);

        // Check if there is an active order for the current idClient
        checkForActiveOrder();
        checkIfOwesReview();

        // Set click listeners for buttons
        btnPlaceOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Intent intent = new Intent(MainMenuActivity.this, SelectRestaurantActivity.class);
                intent.putExtra("idClient", idClient);
                startActivity(intent);*/
                checkOrderAvailability();
            }
        });

        btnCheckOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check your order and show an AlertDialog
                getRestaurantNameForAskingReview(documentFound.getString("idRestaurant"));
            }
        });
        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Delete SharedPreferences
                deleteSharedPreferences();

                // Finish current activity and start LoginActivity
                startActivity(new Intent(MainMenuActivity.this, LoginActivity.class));
                finish();
            }
        });
      }


        private void checkForActiveOrder() {
        activeOrdersRef.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@androidx.annotation.Nullable QuerySnapshot querySnapshot, @androidx.annotation.Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    // Log the specific error message
                    Log.e("FirestoreError", "Error: " + error.getMessage(), error);
                    Toast.makeText(MainMenuActivity.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                    return;
                }

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        // Check if the document contains the idCurier field
                        if (document.contains("idClient") && Objects.equals(document.getString("idClient"), idClient)) {
                            // Document with idClient found, show the Check Order button
                            showCheckOrderButton();
                            documentFound = document;
                            return; // Stop checking further documents
                        }
                    }

                    // No document with idClient found, show the Place Order button
                    showPlaceOrderButton();
                } else {
                    // No documents in Active Orders, show the Place Order button
                    showPlaceOrderButton();
                }
            }
        });
    }

    private void showPlaceOrderButton() {
        btnPlaceOrder.setVisibility(View.VISIBLE);
        btnCheckOrder.setVisibility(View.GONE);
    }

    private void showCheckOrderButton() {
        btnPlaceOrder.setVisibility(View.GONE);
        btnCheckOrder.setVisibility(View.VISIBLE);
    }

    private void showActiveOrderDetails(String restaurantName) {
        if (documentFound != null) {
            // Retrieve fields from documentFound
            double pret = documentFound.getDouble("pret");
            boolean comandaPreluata = documentFound.getBoolean("comandaPreluata");
            String pinConfirmare = documentFound.getString("pinConfirmare");

            // Get additional information from other collections
            String stareComanda = comandaPreluata ? "Pe drum" : "In curs de facere";

            // Build the message for the AlertDialog
            String message = "Restaurant: " + restaurantName + "\n"
                    + "Pret: " + pret + "\n"
                    + "Stare comanda: " + stareComanda + "\n"
                    + "PIN Confirmare: " + pinConfirmare;

            // Show the AlertDialog
            showAlertDialog("Order Details", message);
        }
    }


    private void showAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        AlertDialog alert = builder.create();
        alert.show();
    }

    private void checkOrderAvailability() {
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
                                    // Current time is within the specified range, proceed to place order
                                    Intent intent = new Intent(MainMenuActivity.this, SelectRestaurantActivity.class);
                                    intent.putExtra("idClient", idClient);
                                    startActivity(intent);
                                } else {
                                    // Current time is not within the specified range, show error message
                                    Toast.makeText(MainMenuActivity.this, "Orders are not accepted at the moment.", Toast.LENGTH_SHORT).show();
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



    private void getRestaurantNameForAskingReview(String idRestaurant) {
        // Reference to the "Restaurants" collection
        CollectionReference restaurantsRef = db.collection("Restaurants");

        // Query to find the document with the specified idRestaurant
        Query query = restaurantsRef.whereEqualTo("idRestaurant", idRestaurant);

        // Execute the query
        String placeholderValue = "Restaurant_" + idRestaurant; // Default value if not found

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Assuming there's at most one matching document, get the first one
                    DocumentSnapshot restaurantDoc = querySnapshot.getDocuments().get(0);

                    // Get the restaurant name from the document
                    String restaurantName = restaurantDoc.getString("numeRestaurant");

                    // If found, update the placeholder value
                    if (restaurantName != null) {
                        //numeRestaurant = restaurantName;
                        showActiveOrderDetails(restaurantName);
                        Log.d("pelemeu", restaurantName + " " + numeRestaurant);
                    }
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

    private void deleteSharedPreferences() {
        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    public void showAccountDetailsDialog(View v) {
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

                    // Retrieve user details from Firestore
                    String numeClient = clientDoc.getString("numeClient");
                    String username = clientDoc.getString("username");
                    String email = clientDoc.getString("email");

                    // Show dialog with user details
                    showAccountDetailsDialog(numeClient, username, email);
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

    private void showAccountDetailsDialog(String numeClient, String username, String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Account Details");

        // Customize the message based on user details
        String message = "Nume: " + numeClient + "\nUsername: " + username + "\nEmail: " + email;

        builder.setMessage(message);

        // Add buttons to the dialog
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Close the dialog
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Schimba Parola", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Call method to show change password dialog
                showChangePasswordDialog();
            }
        });

        builder.setNeutralButton("Schimba Email", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Call method to show change email dialog
                showChangeEmailDialog();
            }
        });

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showChangeEmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Schimba Email");

        View view = getLayoutInflater().inflate(R.layout.change_email_dialog, null);
        builder.setView(view);

        EditText editTextNewEmail = view.findViewById(R.id.editTextNewEmail);

        builder.setPositiveButton("Schimba", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newEmail = editTextNewEmail.getText().toString().trim();

                // Check if new email is not empty
                if (!TextUtils.isEmpty(newEmail)) {
                    // Check if the email is in the correct format
                    if (!isValidEmail(newEmail)) {
                        Toast.makeText(MainMenuActivity.this, "Introduceti un email valid.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Update the email in Firestore
                    updateEmailInFirestore(newEmail);
                } else {
                    Toast.makeText(MainMenuActivity.this, "Introduceti un email valid.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Anuleaza", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Cancel the dialog
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");

        View view = getLayoutInflater().inflate(R.layout.change_password_dialog, null);
        builder.setView(view);

        EditText editTextNewPassword = view.findViewById(R.id.editTextNewPassword);
        EditText editTextConfirmPassword = view.findViewById(R.id.editTextConfirmPassword);

        builder.setPositiveButton("Change", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newPassword = editTextNewPassword.getText().toString().trim();
                String confirmPassword = editTextConfirmPassword.getText().toString().trim();

                if (!TextUtils.isEmpty(newPassword) && !TextUtils.isEmpty(confirmPassword)) {
                    if (newPassword.equals(confirmPassword)) {
                        // Passwords match, update the password
                        updatePasswordInFirestore(newPassword);
                    } else {
                        // Passwords don't match, show error message
                        Toast.makeText(MainMenuActivity.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Either of the passwords is empty, show error message
                    Toast.makeText(MainMenuActivity.this, "Please enter both passwords.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Cancel the dialog
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void updateEmailInFirestore(String newEmail) {
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

                    // Update the email field
                    clientsRef.document(clientDoc.getId())
                            .update("email", newEmail)
                            .addOnSuccessListener(aVoid -> {
                                // Email updated successfully
                                Toast.makeText(MainMenuActivity.this, "Email schimbat cu succes!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                // Error updating email
                                Toast.makeText(MainMenuActivity.this, "A aparut o eroare. Emailul nu a fost schimbat.", Toast.LENGTH_SHORT).show();
                                Log.e("FirestoreError", "Error updating email", e);
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
    }

    private void updatePasswordInFirestore(String newPassword) {
        // Reference to the "Clients" collection
        CollectionReference clientsRef = db.collection("Clients");

        // Query to find the document with the specified idClient and password
        Query query = clientsRef.whereEqualTo("idClient", idClient);

        // Execute the query
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Assuming there's at most one matching document, get the first one
                    DocumentSnapshot clientDoc = querySnapshot.getDocuments().get(0);

                    // Update the password field
                    clientsRef.document(clientDoc.getId())
                            .update("password", newPassword)
                            .addOnSuccessListener(aVoid -> {
                                // Password updated successfully
                                Toast.makeText(MainMenuActivity.this, "Parola schimbata cu succes!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                // Error updating password
                                Toast.makeText(MainMenuActivity.this, "A aparut o eroare. Parola nu a fost schimbata.", Toast.LENGTH_SHORT).show();
                                Log.e("FirestoreError", "Error updating password", e);
                            });
                } else {
                    // Handle the case when no document matches the query or current password is incorrect
                    Toast.makeText(MainMenuActivity.this, "Parola curenta incorecta.", Toast.LENGTH_SHORT).show();
                    Log.d("FirestoreDebug", "No document found for idClient: " + idClient);
                }
            } else {
                // Handle errors
                Log.e("FirestoreError", "Error querying Clients collection: ", task.getException());
            }
        });

    }

    private boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }


}