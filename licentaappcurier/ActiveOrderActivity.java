package com.example.licentaappcurier;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class ActiveOrderActivity extends AppCompatActivity {

    String orderId, idConfirmare, idClient, idRestaurant, idCurier;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private TextView txtClientName, txtClientAddress, txtRestaurantName,
                     txtRestaurantAddress, txtPrice, txtIdComanda;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_order_display);

        orderId = getIntent().getStringExtra("orderId");
        idCurier = getIntent().getStringExtra("idCurier");

        txtRestaurantName = findViewById(R.id.txt_restaurant_name);
        txtRestaurantAddress = findViewById(R.id.txt_restaurant_address);
        txtPrice = findViewById(R.id.txt_price);
        txtIdComanda = findViewById(R.id.txt_order_id);

        getOrderInfo();
    }

    public void getOrderInfo()
    {
        DocumentReference orderRef = db.collection("Active Orders").document(orderId);
        orderRef.addSnapshotListener(this, (documentSnapshot, error) -> {
            if(error != null)
                Log.e("FirestoreError", "Error listening for updates: ", error);

            if(documentSnapshot != null && documentSnapshot.exists())
            {
                idConfirmare = documentSnapshot.getString("pinConfirmare");
                idClient = documentSnapshot.getString("idClient");
                idRestaurant = documentSnapshot.getString("idRestaurant");

                txtIdComanda.setText(idClient);

                if(idClient != null)
                    getClientInfo();
                if(idRestaurant !=null)
                    getRestaurantInfo();

                setTotalPrice(documentSnapshot);
            }
        });
    }

    public void getClientInfo()
    {
        txtClientName = findViewById(R.id.txt_client_name);
        txtClientAddress = findViewById(R.id.txt_client_address);

        Query query = db.collection("Clients").whereEqualTo("idClient", idClient);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Since you expect only one document, get the first one
                    DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);

                    String txtClientNameFromFirestore = documentSnapshot.getString("numeClient");
                    String txtClientAddressFromFirestore = documentSnapshot.getString("adresaClient");

                    //txtClientAddressFromFirestore = insertNewlines(txtClientAddressFromFirestore);

                    txtClientName.setText(txtClientNameFromFirestore);
                    txtClientAddress.setText(txtClientAddressFromFirestore);
                } else {
                    // Handle the case when no document matches the query
                    Log.d("FirestoreError", "No document found for idClient: " + idClient);
                }
            } else {
                // Handle errors
                Log.e("FirestoreError", "Error querying Clients collection: ", task.getException());
            }
        });
    }

    public void getRestaurantInfo()
    {
        txtRestaurantName = findViewById(R.id.txt_restaurant_name);
        txtRestaurantAddress = findViewById(R.id.txt_restaurant_address);

        Query query = db.collection("Restaurants").whereEqualTo("idRestaurant", idRestaurant);


        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Since you expect only one document, get the first one
                    DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);

                    String txtRestaurantNameFromFirestore = documentSnapshot.getString("numeRestaurant");
                    String txtRestaurantAddressFromFirestore = documentSnapshot.getString("adresaRestaurant");

                    //txtRestaurantAddressFromFirestore = insertNewlines(txtRestaurantAddressFromFirestore);

                    txtRestaurantName.setText(txtRestaurantNameFromFirestore);
                    txtRestaurantAddress.setText(txtRestaurantAddressFromFirestore);
                } else {
                    // Handle the case when no document matches the query
                    Log.d("FirestoreError", "No document found for idClient: " + idClient);
                }
            } else {
                // Handle errors
                Log.e("FirestoreError", "Error querying Clients collection: ", task.getException());
            }
        });
    }

    //habar n-am
    private void setTotalPrice(DocumentSnapshot documentSnapshot) {
        // Get the total price directly from the "pret" field in the document
        Double totalPrice = documentSnapshot.getDouble("pret");

        if (totalPrice != null) {
            // Set the total price to the TextView
            txtPrice.setText(String.format("%s lei", totalPrice));
        }
    }



    public void checkPin(View v)
    {
        EditText lblPin = findViewById(R.id.txt_confirm_id);

        String pinAttempt = lblPin.getText().toString().trim();

        if(pinAttempt.equals(idConfirmare))
            finishDelivery();
            //Toast.makeText(this, "yessir", Toast.LENGTH_LONG).show();
    }

    private void finishDelivery() {
        // Reference to the "Active Orders" and "Order History" collections
        CollectionReference activeOrdersRef = db.collection("Active Orders");
        CollectionReference orderHistoryRef = db.collection("Order History");

        // Reference to the specific document in the "Active Orders" collection
        DocumentReference activeOrderDocRef = activeOrdersRef.document(orderId);

        // Get the document data from "Active Orders"
        activeOrderDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // Document found in "Active Orders", move it to "Order History"
                    double saleAmount = document.getDouble("pret");
                    addSale(saleAmount);
                    orderHistoryRef.add(document.getData())
                            .addOnSuccessListener(documentReference -> {
                                // Successfully added to "Order History", now delete from "Active Orders"
                                activeOrderDocRef.delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Delivery completed!", Toast.LENGTH_SHORT).show();
                                            setAvailable();
                                            updateClientRatingStatus();
                                            finish();
                                            // Additional actions after successful completion
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("FirestoreError", "Error deleting from Active Orders", e);
                                            // Handle deletion failure
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FirestoreError", "Error adding to Order History", e);
                                // Handle addition to Order History failure
                            });
                } else {
                    // Document not found in "Active Orders"
                    Log.d("FirestoreError", "Document not found in Active Orders");
                    // Handle the case when the document is not found
                }
            } else {
                // Error getting document from "Active Orders"
                Log.e("FirestoreError", "Error getting document from Active Orders", task.getException());
                // Handle the error
            }
        });
    }

    public void addSale(double saleAmount) {
        CollectionReference deliverersRef = db.collection("Deliverers");

        // Query to find the document with the specified idCurier
        Query query = deliverersRef.whereEqualTo("idCurier", idCurier);
        //Toast.makeText(this, idCurier, Toast.LENGTH_LONG).show();

        // Execute the query
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Assuming there's at most one matching document, get the first one
                    DocumentSnapshot delivererDoc = querySnapshot.getDocuments().get(0);

                    // Get the current value of 'vanzari' from the document
                    double currentSales = delivererDoc.getDouble("vanzari");
                    double nrLivrari = delivererDoc.getDouble("nrLivrari");

                    // Calculate the new value after adding the saleAmount
                    double newSales = currentSales + saleAmount;
                    double newNrLivrari = nrLivrari + 1;

                    // Update the 'vanzari' field in the document
                    deliverersRef.document(delivererDoc.getId())
                            .update("vanzari", newSales)
                            .addOnSuccessListener(aVoid -> {
                                // Update successful
                                Toast.makeText(this, "Vanzare inregistrata!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                // Update failed
                                Log.e("FirestoreError", "Error updating 'vanzari' field", e);
                            });

                    deliverersRef.document(delivererDoc.getId())
                            .update("nrLivrari", newNrLivrari);
                }
                else {
                    // Handle the case when no document matches the query
                    Log.d("FirestoreDebug", "No document found for idCurier: " + idCurier);
                }
            } else {
                // Handle errors
                Log.e("FirestoreError", "Error querying Deliverers collection: ", task.getException());
            }
        });
    }

    public void setAvailable() {
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

                    // Update the 'disponibil' field in the document to true
                    deliverersRef.document(delivererDoc.getId())
                            .update("areComanda", false)
                            .addOnSuccessListener(aVoid -> {
                                // Update successful
                                Toast.makeText(this, "Sunteti disponibil de luat comenzi!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                // Update failed
                                Log.e("FirestoreError", "Error updating 'disponibil' field", e);
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

    private void updateClientRatingStatus() {
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
    }


    public void showAddress(View v)
    {
        String viewId = getResources().getResourceEntryName(v.getId());
        //Toast.makeText(this, viewId, Toast.LENGTH_SHORT).show();

        if(viewId.equals("lbl_client_address") || viewId.equals("txt_client_address"))
        {
            showAlertDialog(this, "Adresa Client", txtClientAddress.getText().toString().trim());
        }

        if(viewId.equals("lbl_restaurant_address") || viewId.equals("txt_restaurant_address"))
        {
            showAlertDialog(this, "Adresa Client", txtRestaurantAddress.getText().toString().trim());
        }
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
                })
                .setNegativeButton("Copy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle the Copy button click
                copyToClipboard(context, message);
                dialog.dismiss();
            }
        });

        // Create and show the dialog
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void copyToClipboard(Context context, String textToCopy) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied Text", textToCopy);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show();
    }
}