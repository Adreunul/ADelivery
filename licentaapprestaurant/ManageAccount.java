package org.example;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class ManageAccount {
    String idRestaurant;
    boolean deschis = false;

    public ManageAccount(String idRestaurant) {
        this.idRestaurant = idRestaurant;
        fetchRestaurantState();
        createAndShowGUI();
    }

    private void fetchRestaurantState()
    {
        try {
            // Set up Firestore credentials
            FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // Initialize Firestore
            Firestore firestore = firestoreOptions.getService();

            // Reference to the "Restaurants" collection
            CollectionReference restaurantsRef = firestore.collection("Restaurants");

            // Query to find the document with the specified idRestaurant
            Query query = restaurantsRef.whereEqualTo("idRestaurant", idRestaurant);

            // Execute the query
            ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
            QuerySnapshot querySnapshot = querySnapshotFuture.get();

            // Check if there is a matching document
            if (!querySnapshot.isEmpty()) {
                // Assuming there's only one matching document
                DocumentSnapshot restaurantSnapshot = querySnapshot.getDocuments().get(0);
                // Retrieve the restaurantName field from the document
                deschis = restaurantSnapshot.getBoolean("deschis");
            }

            // Close Firestore connection
            firestore.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void createAndShowGUI() {
        // Create and set up the frame
        JFrame frame = new JFrame("Manage Account");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLayout(new BorderLayout());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                new MainMenu(idRestaurant); // Create a new MainMenu when the frame is closing
            }
        });

        // Create a panel for components
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        Dimension buttonSize = new Dimension(250, 50);

        // Create "Change State" button
        JButton changeStateButton = new JButton("Schimba starea");
        changeStateButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        changeStateButton.setPreferredSize(buttonSize);
        changeStateButton.setMaximumSize(buttonSize);
        changeStateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Show confirmation dialog
                int result = JOptionPane.showConfirmDialog(frame, "Esti sigur ca doresti sa schimbi starea restaurantului in " + (!deschis ? "deschis" : "inchis") + "?", "Change State", JOptionPane.OK_CANCEL_OPTION);

                // Check if the user clicked OK
                if (result == JOptionPane.OK_OPTION) {
                    try {
                        // Set up Firestore credentials
                        FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");
                        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                .build();

                        // Initialize Firestore
                        Firestore firestore = firestoreOptions.getService();

                        // Reference to the "Restaurants" collection
                        CollectionReference restaurantsRef = firestore.collection("Restaurants");

                        // Query to find the document with the specified idRestaurant
                        Query query = restaurantsRef.whereEqualTo("idRestaurant", idRestaurant);

                        // Execute the query
                        ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
                        QuerySnapshot querySnapshot = querySnapshotFuture.get();

                        // Check if there is a matching document
                        if (!querySnapshot.isEmpty()) {
                            // Assuming there's only one matching document
                            DocumentSnapshot restaurantSnapshot = querySnapshot.getDocuments().get(0);

                            // Create an updates map
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("deschis", !deschis);
                            deschis = !deschis;

                            // Update the document
                            ApiFuture<WriteResult> writeResult = restaurantSnapshot.getReference().update(updates);

                            // Show a message dialog to inform the user that the state has been updated
                            JOptionPane.showMessageDialog(frame, "State updated successfully!");

                            // Close Firestore connection
                            firestore.close();

                            new MainMenu(idRestaurant);
                            frame.dispose();
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        mainPanel.add(changeStateButton);

        // Create "Change Schedule" button
        // Create "Change Schedule" button
        JButton changeScheduleButton = new JButton("Schimba program");
        changeScheduleButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        changeScheduleButton.setPreferredSize(buttonSize);
        changeScheduleButton.setMaximumSize(buttonSize);
        changeScheduleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String timeRegex = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$";
                String startingTime, closingTime = null;

                // Show input dialog for starting time
                startingTime = JOptionPane.showInputDialog(frame, "Ora de deschidere (HH:mm):");

                // Check if the starting time matches the time format
                if (startingTime != null && !startingTime.matches(timeRegex)) {
                    JOptionPane.showMessageDialog(frame, "Format de date invalid.(HH:mm)");
                    return;
                }

                // Show input dialog for closing time
                if(startingTime != null)
                    closingTime = JOptionPane.showInputDialog(frame, "Ora de inchidere (HH:mm):");

                // Check if the closing time matches the time format
                if (closingTime != null && !closingTime.matches(timeRegex)) {
                    JOptionPane.showMessageDialog(frame, "Format de date invalid.(HH:mm)");
                    return;
                }
                // Check if the user has entered both times
                if (startingTime != null && closingTime != null) {
                    try {
                        // Set up Firestore credentials
                        FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");
                        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                .build();

                        // Initialize Firestore
                        Firestore firestore = firestoreOptions.getService();

                        // Reference to the "Restaurants" collection
                        CollectionReference restaurantsRef = firestore.collection("Restaurants");

                        // Query to find the document with the specified idRestaurant
                        Query query = restaurantsRef.whereEqualTo("idRestaurant", idRestaurant);

                        // Execute the query
                        ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
                        QuerySnapshot querySnapshot = querySnapshotFuture.get();

                        // Check if there is a matching document
                        if (!querySnapshot.isEmpty()) {
                            // Assuming there's only one matching document
                            DocumentSnapshot restaurantSnapshot = querySnapshot.getDocuments().get(0);

                            // Create an updates map
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("oraIncepere", startingTime);
                            updates.put("oraTerminare", closingTime);

                            // Update the document
                            ApiFuture<WriteResult> writeResult = restaurantSnapshot.getReference().update(updates);

                            // Show a message dialog to inform the user that the schedule has been updated
                            JOptionPane.showMessageDialog(frame, "Program schimbat cu succes!");

                            // Close Firestore connection
                            firestore.close();

                            new MainMenu(idRestaurant);
                            frame.dispose();
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        mainPanel.add(changeScheduleButton);

        // Create "Change Product Types" button
        JButton changeProductTypesButton = new JButton("Schimba tipuri de produse servite");
        changeProductTypesButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        changeProductTypesButton.setPreferredSize(buttonSize);
        changeProductTypesButton.setMaximumSize(buttonSize);
        changeProductTypesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Create checkboxes for each product type
                JCheckBox mainCourseCheckBox = new JCheckBox("Fel Principal");
                JCheckBox appetizerCheckBox = new JCheckBox("Apetit");
                JCheckBox dessertCheckBox = new JCheckBox("Desert");
                JCheckBox drinksCheckBox = new JCheckBox("Bautura");

                // Create a panel and add the checkboxes to it
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                panel.add(mainCourseCheckBox);
                panel.add(appetizerCheckBox);
                panel.add(dessertCheckBox);
                panel.add(drinksCheckBox);

                // Show the custom dialog
                int result = JOptionPane.showConfirmDialog(frame, panel, "Selecteaza tipul de produse", JOptionPane.OK_CANCEL_OPTION);

                // Check if the user clicked OK
                if (result == JOptionPane.OK_OPTION) {
                    try {
                        // Set up Firestore credentials
                        FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");
                        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                .build();

                        // Initialize Firestore
                        Firestore firestore = firestoreOptions.getService();

                        // Reference to the "Restaurants" collection
                        CollectionReference restaurantsRef = firestore.collection("Restaurants");

                        // Query to find the document with the specified idRestaurant
                        Query query = restaurantsRef.whereEqualTo("idRestaurant", idRestaurant);

                        // Execute the query
                        ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
                        QuerySnapshot querySnapshot = querySnapshotFuture.get();

                        // Check if there is a matching document
                        if (!querySnapshot.isEmpty()) {
                            // Assuming there's only one matching document
                            DocumentSnapshot restaurantSnapshot = querySnapshot.getDocuments().get(0);

                            // Create an updates map
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("servesteFelPrincipal", mainCourseCheckBox.isSelected());
                            changeStateOfProducts(mainCourseCheckBox.isSelected(), "Fel principal");
                            updates.put("servesteAperitiv", appetizerCheckBox.isSelected());
                            changeStateOfProducts(appetizerCheckBox.isSelected(), "Aperitiv");
                            updates.put("servesteDesert", dessertCheckBox.isSelected());
                            changeStateOfProducts(dessertCheckBox.isSelected(), "Desert");
                            updates.put("servesteBautura", drinksCheckBox.isSelected());
                            changeStateOfProducts(drinksCheckBox.isSelected(), "Bautura");

                            // Update the document
                            restaurantSnapshot.getReference().update(updates);

                            // Show a message dialog to inform the user that the product types have been updated
                            JOptionPane.showMessageDialog(frame, "Tipurile de produse servite au fost schimbate cu succes!");

                            // Close Firestore connection
                            firestore.close();

                            new MainMenu(idRestaurant);
                            frame.dispose();
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        mainPanel.add(changeProductTypesButton);

        // Add main panel to the frame
        frame.add(mainPanel, BorderLayout.CENTER);

        // Set the frame to be visible
        frame.setVisible(true);
    }

    private void changeStateOfProducts(boolean changeTo, String category) {
        try {
            // Set up Firestore credentials
            FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // Initialize Firestore
            Firestore firestore = firestoreOptions.getService();

            // Reference to the "Products" collection
            CollectionReference productsRef = firestore.collection("Products");

            // Query to find the documents with the specified idRestaurant and category
            Query query = productsRef.whereEqualTo("idRestaurant", idRestaurant).whereEqualTo("categorie", category);

            // Execute the query
            ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
            QuerySnapshot querySnapshot = querySnapshotFuture.get();

            // Check if there are matching documents
            if (!querySnapshot.isEmpty()) {
                // For each matching document
                for (DocumentSnapshot productSnapshot : querySnapshot.getDocuments()) {
                    // Create an updates map
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("disponibil", changeTo);

                    // Update the document
                    productSnapshot.getReference().update(updates);
                }
                countAvailableProducts();
            }

            // Close Firestore connection
            firestore.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void countAvailableProducts() {
        int numberOfProducts = 0;

        try {
            // Set up Firestore credentials
            FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // Initialize Firestore
            Firestore firestore = firestoreOptions.getService();

            // Reference to the "Products" collection
            CollectionReference productsRef = firestore.collection("Products");

            // Query to count available products
            Query query = productsRef.whereEqualTo("idRestaurant", idRestaurant)
                    .whereEqualTo("disponibil", true);


            // Execute the query
            ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
            QuerySnapshot querySnapshot = querySnapshotFuture.get();

            // Count the number of documents in the query result
            numberOfProducts = querySnapshot.size();

            updateRestaurantDocument(numberOfProducts);

            firestore.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateRestaurantDocument(int numberOfProducts) {
        try {
            // Set up Firestore credentials
            FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // Initialize Firestore
            Firestore firestore = firestoreOptions.getService();

            // Reference to the "Restaurants" collection
            CollectionReference restaurantsRef = firestore.collection("Restaurants");

            // Query to get the document for the current restaurant
            Query query = restaurantsRef.whereEqualTo("idRestaurant", idRestaurant);

            // Execute the query
            ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
            QuerySnapshot querySnapshot = querySnapshotFuture.get();

            // Check if the document exists
            if (!querySnapshot.isEmpty()) {
                // Get the document reference
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                DocumentReference docRef = document.getReference();

                // Update the "nrProduseDisponibile" field
                docRef.update("nrProduseDisponibile", numberOfProducts);
            }

            firestore.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}