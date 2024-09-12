package org.example;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.DistanceMatrixElementStatus;
import com.google.maps.model.DistanceMatrixRow;


import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class FirestoreListener {

    public static void main(String[] args) throws IOException, InterruptedException {
        FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");

        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setProjectId("licenta-c8625")
                .build();

        Firestore firestore = firestoreOptions.getService();

        CollectionReference pendingOrdersRef = firestore.collection("Pending Orders");
        CollectionReference activeOrdersRef = firestore.collection("Active Orders");
        CollectionReference deliverersRef = firestore.collection("Deliverers");

        // Add a snapshot listener to the "Pending Orders" collection
        pendingOrdersRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                System.err.println("Listen failed: " + error.getMessage());
                return;
            }

            if (snapshot != null && !snapshot.isEmpty()) {
                for (DocumentChange dc : snapshot.getDocumentChanges()) {
                    DocumentSnapshot document = dc.getDocument();

                    // Get the data from the "Pending Orders" document
                    Map<String, Object> orderData = document.getData();

                    // Get adresaRestaurant from Pending Orders
                    String adresaRestaurant = (String) orderData.get("adresaRestaurant");

                    // Generate a random 4-digit PIN
                    String pinConfirmare = generateRandomPIN();
                    orderData.put("pinConfirmare", pinConfirmare);



                    // Initialize variables for closest deliverer
                    // Initialize variables for closest deliverer
                    String closestDelivererId = null;
                    double minDistance = Double.MAX_VALUE;

// Iterate through every document in the "Deliverers" collection
                    QuerySnapshot deliverersSnapshot = null;
                    try {
                        deliverersSnapshot = deliverersRef.get().get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                    for (QueryDocumentSnapshot delivererDocument : deliverersSnapshot) {
                        // Get adresaCurenta from the Deliverers document
                        String adresaCurenta = (String) delivererDocument.get("adresaCurenta");
                        boolean isDisponibil = delivererDocument.getBoolean("disponibil");
                        if (!isDisponibil) {
                            continue;
                        }

                        // Calculate distance using Haversine formula
                        double distance = calculateDistance(adresaRestaurant, adresaCurenta, "AIzaSyCwS8_VN8m97soBmxqh2CgvtlyMtXL2jEE");

                        // Update closest deliverer if this one is closer
                        if (distance < minDistance) {
                            minDistance = distance;
                            closestDelivererId = (String) delivererDocument.get("idCurier");
                        }
                    }

// Check if there is a deliverer within the specified distance
                    if (minDistance < Double.MAX_VALUE) {
                        // Add the data to the "Active Orders" collection with closest deliverer
                        orderData.put("idCurier", closestDelivererId);
                    } else {
                        // No deliverer found within the specified distance, handle accordingly
                        System.out.println("No deliverer found within the specified distance.");
                    }

// Add the data to the "Active Orders" collection
                    // Add the data to the "Active Orders" collection
                    ApiFuture<DocumentReference> addOrderResult = activeOrdersRef.add(orderData);
                    try {
                        DocumentReference addedOrderRef = addOrderResult.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }

// Update the corresponding deliverer's disponibil field to false
                    if (closestDelivererId != null) {
                        try {
                            deliverersSnapshot = deliverersRef.whereEqualTo("idCurier", closestDelivererId).limit(1).get().get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }

                        if (deliverersSnapshot != null && !deliverersSnapshot.isEmpty()) {
                            DocumentSnapshot delivererDocument = deliverersSnapshot.getDocuments().get(0);
                            try {
                                delivererDocument.getReference().update("disponibil", false).get();
                            } catch (InterruptedException | ExecutionException e) {
                                throw new RuntimeException(e);
                            }
                            System.out.println("Deliverer's disponibil field updated to false");
                        } else {
                            System.err.println("No deliverer found with idCurier: " + closestDelivererId);
                        }
                    }

// Delete the document from "Pending Orders"
                    try {
                        pendingOrdersRef.document(document.getId()).delete().get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("Document deleted from Pending Orders");


                }
            }
        });

        CollectionReference pendingReviewsRef = firestore.collection("Pending Reviews");
        CollectionReference restaurantsRef = firestore.collection("Restaurants");

        // Add a snapshot listener to the "Pending Reviews" collection
        pendingReviewsRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                System.err.println("Listen failed: " + error.getMessage());
                return;
            }

            if (snapshot != null && !snapshot.isEmpty()) {
                for (DocumentChange dc : snapshot.getDocumentChanges()) {
                    DocumentSnapshot reviewDocument = dc.getDocument();

                    // Get the data from the "Pending Reviews" document
                    String idRestaurant = reviewDocument.getString("idRestaurant");
                    int rating = reviewDocument.getLong("rating").intValue();

                    // Get the corresponding restaurant document from "Restaurants" collection
                    Query query = restaurantsRef.whereEqualTo("idRestaurant", idRestaurant);

                    // Use ApiFuture to handle asynchronous operation
                    ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
                    try {
                        QuerySnapshot querySnapshot = querySnapshotFuture.get();

                        if (!querySnapshot.isEmpty()) {
                            // Get the current rating and nrRating from the restaurant document
                            DocumentSnapshot restaurantDocument = querySnapshot.getDocuments().get(0);

                            // Create a DocumentReference for the found restaurant document
                            DocumentReference restaurantDocRef = restaurantDocument.getReference();

                            double currentRating = restaurantDocument.getDouble("rating");
                            int nrRating = restaurantDocument.getLong("nrRating").intValue();

                            // Calculate new rating and update nrRating
                            double newRating = ((currentRating * nrRating) + rating) / (nrRating + 1);
                            int newNrRating = nrRating + 1;

                            DecimalFormat df = new DecimalFormat("#.##");
                            newRating = Double.parseDouble(df.format(newRating));

                            // Create a map with updated values
                            Map<String, Object> updatedData = new HashMap<>();
                            updatedData.put("rating", newRating);
                            updatedData.put("nrRating", newNrRating);

                            // Update the restaurant document in "Restaurants" collection
                            ApiFuture<WriteResult> updateFuture = restaurantDocRef.update(updatedData);
                            updateFuture.get(); // Block to wait for the update operation to complete

                            System.out.println("Restaurant document updated with new rating.");

                            // Remove the document from "Pending Reviews" collection
                            ApiFuture<WriteResult> deleteFuture = pendingReviewsRef.document(reviewDocument.getId()).delete();
                            deleteFuture.get(); // Block to wait for the delete operation to complete

                            System.out.println("Review document deleted from Pending Reviews.");
                        } else {
                            System.err.println("Restaurant document not found for idRestaurant: " + idRestaurant);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        System.err.println("Error getting restaurant document: " + e.getMessage());
                    }
                }
            }

        });

        // Keep the program running to listen for changes
        Thread.sleep(Long.MAX_VALUE);
    }

    private static String generateRandomPIN() {
        Random random = new Random();
        int pin = 1000 + random.nextInt(9000); // Generate a random 4-digit number
        return String.valueOf(pin);
    }

    private static double calculateDistance(String origin, String destination, String apiKey) {
        try {
            GeoApiContext context = new GeoApiContext.Builder().apiKey(apiKey).build();
            DistanceMatrixApiRequest request = DistanceMatrixApi.newRequest(context);

            DistanceMatrix matrix = request.origins(origin).destinations(destination).await();
            DistanceMatrixRow[] rows = matrix.rows;

            if (rows != null && rows.length > 0) {
                DistanceMatrixElement[] elements = rows[0].elements;

                if (elements != null && elements.length > 0) {
                    DistanceMatrixElement element = elements[0];

                    if (element.status == DistanceMatrixElementStatus.OK) {
                        return element.distance.inMeters / 1000.0; // Convert to kilometers
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Double.MAX_VALUE; // Return a large distance if calculation fails
    }



}
