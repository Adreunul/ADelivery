package org.example;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class ManageMenuSelectProduct {
    private String idRestaurant;
    private String selectedCategory = "Toate";
    private JFrame frame;
    private JPanel mainPanel;
    JLabel productCounterLabel;

    public ManageMenuSelectProduct(String idRestaurant) {
        productCounterLabel = new JLabel();
        this.idRestaurant = idRestaurant;
        createAndShowGUI();
    }

    private void createAndShowGUI() {
        // Create and set up the frame
        frame = new JFrame("Manage Menu - Select Product");
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 600);
        frame.setLayout(new BorderLayout());

        // Create a panel for components
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Create a scroll pane
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Create a panel for additional components (filter and "Add Product" button)
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        // Create a combo box for category filtering
        String[] categories = {"Toate", "Bautura", "Fel principal", "Aperitiv", "Desert"};
        JComboBox<String> categoryComboBox = new JComboBox<>(categories);

        // Create a label for displaying the number of available products
        productCounterLabel.setText("Numar produse disponibile: " + countAvailableProducts());

        // Create "Add Product" button
        JButton addProductButton = new JButton("Add Product");
        addProductButton.addActionListener(e -> {
            // Handle "Add Product" button click
            // You can add your logic here for adding a new product
            try {
                new ManageMenuAddOrEditProduct(idRestaurant);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            JOptionPane.showMessageDialog(frame, "Add Product button clicked!");
        });


        // Add components to the control panel
        controlPanel.add(addProductButton);
        controlPanel.add(categoryComboBox);
        controlPanel.add(productCounterLabel);

        // Add control panel and scroll pane to the frame
        frame.add(controlPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Set the frame to be visible
        frame.setVisible(true);

        // Add an action listener to the category combo box
        categoryComboBox.addActionListener(e -> {
            // Handle category selection change
            selectedCategory = (String) categoryComboBox.getSelectedItem();
            // Regenerate buttons based on the selected category
            if(selectedCategory.equals("Toate"))
                countAvailableProducts();

            regenerateButtons(selectedCategory);
        });

        // Initial button generation (all products)
        regenerateButtons(selectedCategory);
    }

    private void regenerateButtons(String selectedCategory) {
        // Clear existing buttons
        mainPanel.removeAll();

        // Retrieve products based on the selected category
        List<String> productNames = retrieveProducts(selectedCategory);

        if (!productNames.isEmpty()) {
            // Create buttons for each product
            List<JButton> buttons = new ArrayList<>();
            int maxWidth = 0;

            for (String productName : productNames) {
                boolean disponibil = getProductDisponibil(productName);

                JButton productButton = new JButton(productName);
                productButton.setAlignmentX(Component.CENTER_ALIGNMENT);

                // Set background color based on disponibil
                Color backgroundColor = disponibil ? Color.GREEN : Color.RED;
                productButton.setBackground(backgroundColor);

                buttons.add(productButton);

                // Find the maximum width
                int width = productButton.getPreferredSize().width;
                maxWidth = Math.max(maxWidth, width);

                productButton.addActionListener(e -> {
                    // Handle button click
                    String selectedProduct = productButton.getText();
                    promptActionForProduct(selectedProduct);
                });

                // Add button to the panel
                mainPanel.add(productButton);
            }

            // Set the preferred size for all buttons
            Dimension buttonSize = new Dimension(maxWidth, buttons.get(0).getPreferredSize().height);

            for (JButton button : buttons) {
                button.setPreferredSize(buttonSize);
                button.setMaximumSize(buttonSize);
                button.setMinimumSize(buttonSize);
            }
        }

        // Refresh the main panel to reflect the changes
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void promptActionForProduct(String productName) {
        int choice = JOptionPane.showOptionDialog(frame,
                "Ati selectat produsul : " + productName,
                "Product Action", JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE, null,
                new String[]{"Modifica", "Sterge", "Schimba disponibilitate", "Cancel"}, "Modify");

        if (choice == JOptionPane.YES_OPTION) {
            modifyProduct(productName);
        } else if (choice == JOptionPane.NO_OPTION) {
            deleteProduct(productName);
        } else if (choice == 2) {
            toggleProductVisibility(productName);
        }
    }

    private void toggleProductVisibility(String productName) {
        try {
            // Fetch product details from the database
            Map<String, Object> productData = fetchProductDetails(productName);

            // Toggle the visibility status
            boolean isVisible = (boolean) productData.getOrDefault("disponibil", true); // Assuming there's a field named "visible" in Firestore
            productData.put("disponibil", !isVisible);

            // Update the product in the Firestore database
            updateProduct(productName, productData);

            // Show confirmation message
            String visibilityStatus = (!isVisible) ? "disponibil" : "hidden";
            JOptionPane.showMessageDialog(frame, "Product is now " + visibilityStatus + ".", "Product Visibility", JOptionPane.INFORMATION_MESSAGE);
            regenerateButtons(selectedCategory);
            countAvailableProducts();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error toggling product visibility: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateProduct(String productName, Map<String, Object> updatedData) {
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

            // Query to get the selected product
            Query query = productsRef.whereEqualTo("idRestaurant", idRestaurant)
                    .whereEqualTo("numeProdus", productName);

            // Execute the query
            ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
            QuerySnapshot querySnapshot = querySnapshotFuture.get();

            // Update the document with the new data
            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                document.getReference().update(updatedData);
            }

            firestore.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    //ia ID-ul si da-i si id-ul ca parametru ca sa-l copieze
    private void modifyProduct(String productName) {
        try {
            // Fetch product details from the database
            Map<String, Object> productData = fetchProductDetails(productName);

            // Open the ManageMenuAddProduct dialog with pre-filled details
            new ManageMenuAddOrEditProduct(idRestaurant, productData);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error fetching product details: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<String> retrieveProducts(String selectedCategory) {
        List<String> productNames = new ArrayList<>();

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

            // Query to get products for the specific restaurant
            Query query = productsRef.whereEqualTo("idRestaurant", idRestaurant);

            // Modify the query based on the selected category
            if (!selectedCategory.equals("Toate")) {
                query = query.whereEqualTo("categorie", selectedCategory);
            }

            // Execute the query
            ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
            QuerySnapshot querySnapshot = querySnapshotFuture.get();

            // Extract product names from the documents
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                String productName = document.getString("numeProdus");
                if (productName != null) {
                    productNames.add(productName);
                }
            }
            firestore.close();
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return productNames;
    }

    private boolean getProductDisponibil(String productName)
    {
        boolean disponibil = false;
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

            // Query to get the selected product
            Query query = productsRef.whereEqualTo("idRestaurant", idRestaurant)
                    .whereEqualTo("numeProdus", productName);

            // Execute the query
            ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
            QuerySnapshot querySnapshot = querySnapshotFuture.get();

            // Extract product details from the document
            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                disponibil = document.getBoolean("disponibil");
            }

            firestore.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return disponibil;
    }


    private void deleteProduct(String productName) {
        int confirmDelete = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete the product '" + productName + "'?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (confirmDelete == JOptionPane.YES_OPTION) {
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

                // Query to get the selected product
                Query query = productsRef.whereEqualTo("idRestaurant", idRestaurant)
                        .whereEqualTo("numeProdus", productName);

                // Execute the query
                ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
                QuerySnapshot querySnapshot = querySnapshotFuture.get();

                // Check if the product exists
                if (!querySnapshot.isEmpty()) {
                    // Get the document reference for the first matching document
                    DocumentReference documentRef = querySnapshot.getDocuments().get(0).getReference();

                    // Delete the document
                    ApiFuture<WriteResult> deleteResult = documentRef.delete();
                    deleteResult.get(); // Wait for the delete operation to complete

                    // Show confirmation message
                    JOptionPane.showMessageDialog(frame, "Product '" + productName + "' deleted successfully.");

                    countAvailableProducts();

                    // Regenerate buttons after deletion
                    regenerateButtons("Toate");
                } else {
                    JOptionPane.showMessageDialog(frame, "Product '" + productName + "' not found.");
                }

                firestore.close();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error deleting product: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }



    private Map<String, Object> fetchProductDetails(String productName) throws Exception {
        Map<String, Object> productData = new HashMap<>();

        // Set up Firestore credentials
        FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");
        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        // Initialize Firestore
        Firestore firestore = firestoreOptions.getService();

        // Reference to the "Products" collection
        CollectionReference productsRef = firestore.collection("Products");

        // Query to get the selected product
        Query query = productsRef.whereEqualTo("idRestaurant", idRestaurant)
                .whereEqualTo("numeProdus", productName);

        // Execute the query
        ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
        QuerySnapshot querySnapshot = querySnapshotFuture.get();

        // Extract product details from the document
        if (!querySnapshot.isEmpty()) {
            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
            productData = document.getData();
        }

        firestore.close();

        return productData;
    }

    private int countAvailableProducts() {
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

            productCounterLabel.setText("Numar produse disponibile: " + numberOfProducts);

            firestore.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return numberOfProducts;
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