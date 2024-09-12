package org.example;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.cloud.firestore.Query;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class MainMenu {
    private String idRestaurant;
    private String restaurantName = "Your Restaurant"; // Default value
    private String schedule = "8:00 - 21:00"; // Default value

    private boolean deschis = false;

    public MainMenu(String idRestaurant) {
        this.idRestaurant = idRestaurant;
        // Fetch restaurant name from Firestore
        fetchRestaurantDetails();
        createAndShowGUI();
    }

    private void fetchRestaurantDetails() {
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
                restaurantName = restaurantSnapshot.getString("numeRestaurant");

                String startHour = restaurantSnapshot.getString("oraIncepere");
                String endHour = restaurantSnapshot.getString("oraTerminare");
                schedule = startHour + " - " + endHour;

                deschis = restaurantSnapshot.getBoolean("deschis");
            }

            // Close Firestore connection
            firestore.close();

        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createAndShowGUI() {
        // Create and set up the frame
        JFrame frame = new JFrame("Main Menu");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        // Create a panel for components
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Create label for restaurant name
        JLabel restaurantLabel = new JLabel("Welcome to " + restaurantName);
        restaurantLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(restaurantLabel);

        JLabel scheduleLabel = new JLabel("Program: " + schedule);
        scheduleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(scheduleLabel);

        JLabel restaurantStateLabel = new JLabel("Restaurantul este " + (deschis ? "deschis" : "închis"));
        restaurantStateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(restaurantStateLabel);

        // Add some vertical space
        mainPanel.add(Box.createVerticalStrut(20));

        Dimension buttonSize = new Dimension(200, 50); // Set preferred size

        // Create "See Active Orders" button
        JButton seeActiveOrdersButton = new JButton("Vezi Comenzi Active");
        seeActiveOrdersButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        seeActiveOrdersButton.setPreferredSize(buttonSize); // Set preferred size
        seeActiveOrdersButton.setMaximumSize(buttonSize); // Set maximum size
        seeActiveOrdersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new ManageOrders(idRestaurant);
                JOptionPane.showMessageDialog(frame, "Button clicked: See Active Orders");
            }
        });

        // Add "See Active Orders" button to the panel
        mainPanel.add(seeActiveOrdersButton);

        // Create "Manage Menu" button
        JButton manageMenuButton = new JButton("Gestioneaza Meniu");
        manageMenuButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        manageMenuButton.setPreferredSize(buttonSize);
        manageMenuButton.setMaximumSize(buttonSize); // Set maximum size
        manageMenuButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new ManageMenuSelectProduct(idRestaurant);
                JOptionPane.showMessageDialog(frame, "Button clicked: Manage Menu");
            }
        });

        // Add button to the panel
        mainPanel.add(manageMenuButton);

        // Create "Manage Account" button
        JButton manageAccountButton = new JButton("Gestioneaza Profil");
        manageAccountButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        manageAccountButton.setPreferredSize(buttonSize); // Set preferred size
        manageAccountButton.setMaximumSize(buttonSize); // Set maximum size
        manageAccountButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new ManageAccount(idRestaurant);
                frame.dispose();
                JOptionPane.showMessageDialog(frame, "Button clicked: Manage Account");
            }
        });

        // Add "Manage Account" button to the panel
        mainPanel.add(manageAccountButton);

        // Add main panel to the frame
        frame.add(mainPanel, BorderLayout.CENTER);

        // Set the frame to be visible
        frame.setVisible(true);
    }
}
