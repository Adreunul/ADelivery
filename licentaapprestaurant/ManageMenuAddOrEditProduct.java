package org.example;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

import java.io.FileInputStream;
import java.io.IOException;

import java.util.concurrent.TimeUnit;

public class ManageMenuAddOrEditProduct {
    private String idRestaurant;
    private JTextField denumireProdusField;
    private JComboBox<String> categorieComboBox;
    private JTextField gramajField;
    private JTextField pretField;
    private String photoUrl;
    private int nrComenzi;
    private List<Map<String, Object>> ingredientsList, optionsList;
    private Map<String, Object> productData;
    private Firestore firestore;

    JFrame frame = new JFrame("Manage Menu - Add Product");


    // Create a panel for components
    JPanel mainPanel = new JPanel(new GridLayout(6, 2)); // Updated to accommodate ingredient prompts

    boolean isModification;

    //////ADD PRODUCT//////
    public ManageMenuAddOrEditProduct(String idRestaurant) throws IOException {
        this.idRestaurant = idRestaurant;
        this.ingredientsList = new ArrayList<>();
        this.optionsList = new ArrayList<>();
        isModification = false;
        nrComenzi = 0;
        firestore = initializeFirestore();
        createAndShowGUI();
    }

    //////EDIT PRODUCT//////
    public ManageMenuAddOrEditProduct(String idRestaurant, Map<String, Object> productData) throws IOException {
        this.idRestaurant = idRestaurant;
        this.ingredientsList = new ArrayList<>();
        ingredientsList = (List<Map<String, Object>>) productData.get("Ingredients");
        this.optionsList = new ArrayList<>();
        isModification = true;
        nrComenzi = 0;
        firestore = initializeFirestore();
        this.productData = productData;
        createAndShowGUI(productData);
    }

    //////ADD PRODUCT////////
    private void createAndShowGUI() {
        // Create and set up the frame
        JFrame frame = new JFrame("Manage Menu - Add Product");
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        // Create a panel for components
        JPanel mainPanel = new JPanel(new GridLayout(6, 2)); // Updated to accommodate ingredient prompts

        // Initialize TextFields and ComboBox
        denumireProdusField = new JTextField();
        categorieComboBox = new JComboBox<>(new String[]{"Bautura", "Fel principal", "Aperitiv", "Desert"});
        gramajField = new JTextField();
        pretField = new JTextField();

        // Create "Select Photo" button
        JButton selectPhotoButton = new JButton("Select Photo");
        selectPhotoButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    // Get the selected file
                    java.io.File selectedFile = fileChooser.getSelectedFile();

                    // Upload the file to Firebase Storage and get the download URL
                   photoUrl = uploadFileToFirebase(selectedFile);
                    // Show a message dialog to inform the user that the photo has been uploaded
                    JOptionPane.showMessageDialog(frame, "Photo uploaded successfully!");

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Error uploading photo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Add components to the panel
        mainPanel.add(new JLabel("Denumire Produs:"));
        mainPanel.add(denumireProdusField);
        mainPanel.add(new JLabel("Categorie:"));
        mainPanel.add(categorieComboBox);
        mainPanel.add(new JLabel("Gramaj:"));
        mainPanel.add(gramajField);
        mainPanel.add(new JLabel("Pret:"));
        mainPanel.add(pretField);

        mainPanel.add(new JLabel("Photo:"));
        mainPanel.add(selectPhotoButton);

        mainPanel.add(new JLabel()); // Empty label for spacing


        // Create "OK" button
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            try {
                handleOkButtonClick(frame, true, "");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        // Add button to the panel
        mainPanel.add(okButton);

        // Add panel to the frame
        frame.add(mainPanel, BorderLayout.CENTER);

        // Set the frame to be visible
        frame.setVisible(true);
    }

    //////EDIT PRODUCT//////
    private void createAndShowGUI(Map<String, Object> productDetails) {
        // Create and set up the frame
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        // Initialize TextFields and ComboBox
        denumireProdusField = new JTextField();
        categorieComboBox = new JComboBox<>(new String[]{"Bautura", "Fel principal", "Aperitiv", "Desert"});
        gramajField = new JTextField();
        pretField = new JTextField();

        // Create "Select Photo" button
        JButton selectPhotoButton = new JButton("Select Photo");
        selectPhotoButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    // Get the selected file
                    java.io.File selectedFile = fileChooser.getSelectedFile();

                    // Upload the file to Firebase Storage and get the download URL
                    photoUrl = uploadFileToFirebase(selectedFile);
                    // Show a message dialog to inform the user that the photo has been uploaded
                    JOptionPane.showMessageDialog(frame, "Photo uploaded successfully!");

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Error uploading photo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Add components to the panel
        mainPanel.add(new JLabel("Denumire Produs:"));
        mainPanel.add(denumireProdusField);
        mainPanel.add(new JLabel("Categorie:"));
        mainPanel.add(categorieComboBox);
        mainPanel.add(new JLabel("Gramaj:"));
        mainPanel.add(gramajField);
        mainPanel.add(new JLabel("Pret:"));
        mainPanel.add(pretField);

        mainPanel.add(new JLabel("Photo:"));
        mainPanel.add(selectPhotoButton);

        mainPanel.add(new JLabel()); // Empty label for spacing

        String denumireProdusInitiala = (String) productDetails.get("numeProdus");
        denumireProdusField.setText(denumireProdusInitiala);
        categorieComboBox.setSelectedItem((String) productDetails.get("categorie"));
        gramajField.setText(String.valueOf(productDetails.get("gramaj")));
        pretField.setText(String.valueOf(productDetails.get("pret")));
        photoUrl = (String) productDetails.get("linkPoza");
        nrComenzi = Integer.parseInt((String.valueOf(productDetails.get("nrComenzi"))));

        // Create "OK" button
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e ->
                {
                    try {
                        askForChangingIngredients(productDetails, denumireProdusInitiala);
                        //handleOkButtonClick(frame);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
        );

        // Add button to the panel
        mainPanel.add(okButton);

        // Add panel to the frame
        frame.add(mainPanel, BorderLayout.CENTER);

        // Set the frame to be visible
        frame.setVisible(true);
    }

    private void askForChangingIngredients(Map<String, Object> productDetails, String denumireProdusInitiala) throws Exception {
        // Extract existing ingredients from productDetails
        List<Map<String, Object>> existingIngredients = (List<Map<String, Object>>) productDetails.get("Ingredients");

        // Generate text for existing ingredients
        StringBuilder ingredientsText = new StringBuilder();
        ingredientsText.append("Current Ingredients:\n");
        for (Map<String, Object> ingredient : existingIngredients) {
            String ingredientName = (String) ingredient.get("numeIngredient");
            Long ingredientGramajLong = (Long) ingredient.get("gramajIngredient");
            int ingredientGramaj = ingredientGramajLong.intValue(); // Convert Long to int
            ingredientsText.append("- ").append(ingredientName).append(" ").append(ingredientGramaj).append("g\n");
        }

        // Show confirmation dialog with ingredients text
        int choice = JOptionPane.showConfirmDialog(frame, ingredientsText.toString() + "\nSchimbam ingredientele?", "Change Ingredients", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            ingredientsList.clear();
            handleOkButtonClick(frame, true, denumireProdusInitiala);
        } else {
            handleOkButtonClick(frame, false, denumireProdusInitiala);
        }
    }


    private void handleOkButtonClick(JFrame frame, boolean wantsToChangeIngredients, String denumireProdusInitiala) throws Exception {
        String checkDenumireProdus = denumireProdusField.getText();

        // Check if a product with the same name already exists
        boolean productExists = checkProductExists(checkDenumireProdus);

        if (productExists && !isModification) {
            // Display an error message and return without adding the product
            JOptionPane.showMessageDialog(frame, "Un produs cu acelasi nume deja exista, te rugam sa alegi alt nume.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (denumireProdusField.getText().isEmpty() || gramajField.getText().isEmpty() || pretField.getText().isEmpty() || photoUrl == null) {
            JOptionPane.showMessageDialog(frame, "Completeaza toate campurile de text si selecteaza o poza.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }


        JTextArea ingredientsTextArea = new JTextArea();
        ingredientsTextArea.setEditable(false);
        JScrollPane ingredientsScrollPane = new JScrollPane(ingredientsTextArea);

        JTextArea optionsTextArea = new JTextArea();
        optionsTextArea.setEditable(false);
        JScrollPane optionsScrollPane = new JScrollPane(optionsTextArea);

        ingredientsScrollPane.setPreferredSize(new Dimension(250, 150));
        optionsScrollPane.setPreferredSize(new Dimension(250, 150));

        ArrayList<Optiune> optionsArrayList = new ArrayList<Optiune>();

        boolean confirmedOptions = false;

        if (isModification) {
            // Get the list of maps representing options from productData
            List<Map<String, Object>> productOptions = (List<Map<String, Object>>) productData.get("Optiuni");

            // Iterate over each map in productOptions
            for (Map<String, Object> optionData : productOptions) {
                // Extract relevant data from the map
                String optionQuestion = (String) optionData.get("Intrebare");
                boolean hasMultipleAnswers = (boolean) optionData.get("raspunsMultiplu");
                List<String> optionResponses = new ArrayList<>();
                for (int i = 0; optionData.containsKey("optiune" + i); i++) {
                    optionResponses.add((String) optionData.get("optiune" + i));
                }

                // Create an Optiune object with the extracted data
                Optiune newOption = new Optiune(optionQuestion);
                newOption.setRaspunsMultiplu(hasMultipleAnswers);
                newOption.setRaspunsuri(optionResponses);

                // Add the new Optiune object to the optionsArrayList
                optionsArrayList.add(newOption);
            }

            // Update the optionsTextArea to display the added options
            optionsTextArea.setText("");
            for (Optiune optiune : optionsArrayList) {
                optionsTextArea.append(optiune.getIntrebare() + "\n");
                optionsTextArea.append(optiune.getRaspunsMultiplu() ? "Raspuns multiplu\n" : "Raspuns unic\n");
                for (String raspuns : optiune.getRaspunsuri()) {
                    optionsTextArea.append("- " + raspuns + "\n");
                }
            }
        }

        while (true) {
            int optionOptions;
            JTextField optionQuestionField = new JTextField();


            Object[] messages = {
                    "Intrebare:", optionQuestionField,
                    "Optiuni:", optionsScrollPane
            };

            if(!confirmedOptions) {
                optionOptions = JOptionPane.showOptionDialog(frame, messages,
                        "Add Option", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                        new String[]{"Add Option", "Confirm Options", "Delete Option", "Cancel"}, "Add Ingredient");
            }
            else
                optionOptions = 1;


            System.out.println(optionOptions);

            if(optionOptions == 0)
            {
                String newIntrebare = optionQuestionField.getText();
                if(!Objects.equals(newIntrebare, ""))
                {
                    Optiune newOptiune = new Optiune(optionQuestionField.getText());
                    int answer = JOptionPane.showConfirmDialog(frame, "Intrebarea are raspuns multiplu?", "Multiple Answers", JOptionPane.YES_NO_OPTION);
                    boolean hasMultipleAnswers = (answer == JOptionPane.YES_OPTION);

                    // Create a new option with the question and set if it has multiple answers
                    newOptiune.setRaspunsMultiplu(hasMultipleAnswers);

                    while (true) {
                        JTextField addRaspunsField = new JTextField();
                        Object[] addRaspunsMessage = {
                                "Adauga raspuns:",
                                addRaspunsField
                        };
                        int addRaspunsOption = JOptionPane.showOptionDialog(frame, addRaspunsMessage, "Adauga raspuns", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{"Adauga raspuns", "Confirma raspunsurile", "Cancel"}, "Adauga raspuns");

                        if (addRaspunsOption == JOptionPane.YES_OPTION) {
                            // Add the response to the option
                            newOptiune.adaugaRaspuns(addRaspunsField.getText());
                        } else if (addRaspunsOption == JOptionPane.NO_OPTION) {
                            // Stop adding responses and break the loop
                            optionsArrayList.add(newOptiune);
                            Iterator<Optiune> iterator = optionsArrayList.iterator();
                            optionsTextArea.setText("");
                            while(iterator.hasNext())
                            {
                                Optiune optiune = iterator.next();
                                System.out.println(optiune.getIntrebare());
                                optionsTextArea.append(optiune.getIntrebare() + "\n");
                                optionsTextArea.append(optiune.getRaspunsMultiplu() ? "Raspuns multiplu\n" : "Raspuns unic\n");
                                for(String raspuns : optiune.getRaspunsuri())
                                {
                                    System.out.println(raspuns);
                                    optionsTextArea.append("- " + raspuns + "\n");
                                }
                            }
                            break;
                        } else {
                            // Cancel the option dialog
                            break;
                        }
                    }
                }
                else
                {
                    JOptionPane.showMessageDialog(frame, "Please fill in the question field.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            if(optionOptions == 2)
            {
                optionsArrayList.remove(optionsArrayList.size() - 1);
                Iterator<Optiune> iterator = optionsArrayList.iterator();
                optionsTextArea.setText("");
                while(iterator.hasNext())
                {
                    Optiune optiune = iterator.next();
                    optionsTextArea.append(optiune.getIntrebare() + "\n");
                    optionsTextArea.append(optiune.getRaspunsMultiplu() ? "Raspuns multiplu\n" : "Raspuns unic\n");
                    for(String raspuns : optiune.getRaspunsuri())
                    {
                        optionsTextArea.append("- " + raspuns + "\n");
                    }
                }
            }

            if(optionOptions == 3)
            {
                return;
            }

            if(optionOptions == 1) //a confirmat optiunile, trecem mai departe
            {
                confirmedOptions = true;
                optionsList.clear();

                Iterator<Optiune> iterator = optionsArrayList.iterator();
                while(iterator.hasNext())
                {
                    Optiune optiune = iterator.next();
                    Map<String, Object> optionData = new HashMap<>();
                    optionData.put("Intrebare", optiune.getIntrebare());
                    optionData.put("raspunsMultiplu", optiune.getRaspunsMultiplu());
                    int i = 0;
                    for(String raspuns : optiune.getRaspunsuri())
                    {
                        optionData.put("optiune" + i, raspuns);
                        i++;
                    }
                    optionsList.add(optionData);
                }

                System.out.println("Dimensiune optionsList: " + optionsArrayList.size());
                int optionIngredients;
                JTextField ingredientNameField = new JTextField();
                JTextField ingredientGramajField = new JTextField();

                Object[] message = {
                        "Nume Ingredient:", ingredientNameField,
                        "Gramaj:", ingredientGramajField,
                        "Ingrediente:", ingredientsScrollPane
                };

                if (wantsToChangeIngredients) {
                    optionIngredients = JOptionPane.showOptionDialog(frame, message,
                            "Add Ingredient", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                            new String[]{"Add Ingredient", "Confirm Product", "Clear Ingredients", "Cancel"}, "Add Ingredient");
                } else
                    optionIngredients = 1;

                if (optionIngredients == JOptionPane.YES_OPTION) {
                    // Add ingredient logic
                    String ingredientName = ingredientNameField.getText();
                    int ingredientGramaj = Integer.parseInt(ingredientGramajField.getText());


                    // Add ingredient data to the list
                    Map<String, Object> ingredientData = new HashMap<>();
                    ingredientData.put("numeIngredient", ingredientName);
                    ingredientData.put("gramajIngredient", ingredientGramaj);
                    ingredientsList.add(ingredientData);

                    // Update ingredients text area
                    ingredientsTextArea.append("- " + ingredientName + " " + ingredientGramaj + "g\n");
                } else if (optionIngredients == JOptionPane.NO_OPTION) {
                    // Continue with the rest of your OK button logic
                    String denumireProdus = denumireProdusField.getText();
                    String categorie = (String) categorieComboBox.getSelectedItem();
                    int gramaj = Integer.parseInt(gramajField.getText());
                    double pret = Double.parseDouble(pretField.getText());
                    String idProdus = fetchAndUpdateIdProdus();

                    boolean areOptiuni = false;

                    // Create a map with product data
                    Map<String, Object> productData = new HashMap<>();
                    productData.put("idRestaurant", idRestaurant);
                    productData.put("numeProdus", denumireProdus);
                    productData.put("categorie", categorie);
                    productData.put("gramaj", gramaj);
                    productData.put("pret", pret);
                    productData.put("verificat", false);
                    productData.put("Ingredients", ingredientsList);
                    productData.put("Optiuni", optionsList);
                    productData.put("idProdus", idProdus);
                    productData.put("disponibil", true); //temp
                    productData.put("areOptiuni", optionsArrayList.size() > 0);
                    productData.put("nrComenzi", nrComenzi);

                    productData.put("linkPoza", photoUrl);

                    if (isModification)
                        deleteOldProdus(denumireProdusInitiala);

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

                        // Generate a new document reference and set data for the new document
                        DocumentReference newProductRef = productsRef.document();
                        ApiFuture<WriteResult> result = newProductRef.set(productData);

                        // Wait for the write operation to complete
                        result.get();

                        // Show a message indicating success
                        JOptionPane.showMessageDialog(frame, "Product added successfully!");

                        // Close the frame after product confirmation
                        firestore.close();
                        frame.dispose();
                        break;  // Exit the loop after closing the frame

                    } catch (IOException | InterruptedException | ExecutionException ex) {
                        // Handle exceptions
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(frame, "Error adding product: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        firestore.close();
                    }
                } else if (optionIngredients == 2) {  // "Clear Ingredients" button
                    // Clear the ingredients list and update the text area
                    ingredientsList.clear();
                    ingredientsTextArea.setText("");
                } else {
                    // User pressed Cancel, exit the loop
                    break;
                }
            }
        }
    }

    private boolean checkProductExists(String productName) {
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

            // Query to check if a product with the same name already exists in the specified restaurant
            Query query = productsRef.whereEqualTo("idRestaurant", idRestaurant)
                    .whereEqualTo("numeProdus", productName);

            // Execute the query
            ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
            QuerySnapshot querySnapshot = querySnapshotFuture.get();

            firestore.close();
            // Return true if any documents are returned by the query, indicating that the product already exists
            return !querySnapshot.isEmpty();
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException("Error checking product existence: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void deleteOldProdus(String denumireProdus) {
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

            // Query to find the document with the specified product name
            Query query = productsRef.whereEqualTo("idRestaurant", idRestaurant)
                    .whereEqualTo("numeProdus", denumireProdus);

            // Execute the query
            ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
            QuerySnapshot querySnapshot = querySnapshotFuture.get();

            // Delete the document if it exists
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                document.getReference().delete();
                JOptionPane.showMessageDialog(frame, "Old product deleted successfully!");
            }
            firestore.close();
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error deleting old product: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String fetchAndUpdateIdProdus() {
        String idProdus = "";

        try {
            // Set up Firestore credentials
            FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // Initialize Firestore
            Firestore firestore = firestoreOptions.getService();

            // Reference to the "Reguli" collection
            CollectionReference reguliRef = firestore.collection("Reguli");

            // Reference to the "CounterIdProdus" document
            DocumentReference counterRef = reguliRef.document("CounterIdProdus");

            // Fetch the current idProdus
            ApiFuture<DocumentSnapshot> counterSnapshotFuture = counterRef.get();
            DocumentSnapshot counterSnapshot = counterSnapshotFuture.get();

            if (counterSnapshot.exists()) {
                // Use `getLong` instead of `getString` for Long values
                Long idProdusLong = counterSnapshot.getLong("idProdus");

                if (idProdusLong != null) {
                    idProdus = String.valueOf(idProdusLong);

                    // Increment idProdus by one
                    String newIdProdus = String.valueOf(Long.parseLong(idProdus) + 1);
                    counterRef.update("idProdus", Long.parseLong(newIdProdus));
                }
            }
            firestore.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return idProdus;
    }


    private String uploadFileToFirebase(java.io.File file) throws IOException {
        try {
            // Initialize FirebaseApp
            FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            // Get a reference to the storage service
            StorageClient storageClient = StorageClient.getInstance();

            // Specify the bucket name
            String bucketName = "licenta-c8625.appspot.com"; // Replace with your Firebase Storage bucket name

            // Specify the file name
            String fileName = file.getName();

            // Define BlobInfo
            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, fileName).build();

            // Upload the file to Firebase Storage
            Blob blob = storageClient.bucket(bucketName).create(String.valueOf(blobInfo), Files.readAllBytes(file.toPath()));

            // Generate a signed URL with a token
            String signedUrlWithToken = blob.signUrl(365, TimeUnit.DAYS).toString();

            return signedUrlWithToken;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error uploading file to Firebase Storage: " + e.getMessage());
        }
    }



    private Firestore initializeFirestore() throws IOException {
        // Set up Firestore credentials
        FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");
        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        // Initialize Firestore and return the instance
        return firestoreOptions.getService();
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
