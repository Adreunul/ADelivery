package org.example;


import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.cloud.firestore.Query;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class ManageOrders {
    private String idRestaurant;
    private JFrame frame;
    private JTextField searchField;

    public ManageOrders(String idRestaurant) {
        this.idRestaurant = idRestaurant;
        createAndShowGUI();
    }

    private void createAndShowGUI() {
        frame = new JFrame("Manage Orders");
        frame.setSize(500, 600);
        frame.setLayout(new BorderLayout());

        // Create panel for displaying orders (left side)
        JPanel ordersPanel = createOrdersPanel();
        JScrollPane ordersScrollPane = new JScrollPane(ordersPanel);
        ordersScrollPane.setPreferredSize(new Dimension(450, 300));
        frame.add(ordersScrollPane, BorderLayout.WEST);

        // Create search panel with search bar and button
        JPanel searchPanel = createSearchPanel();
        frame.add(searchPanel, BorderLayout.NORTH);

        // Create refresh button and add ActionListener
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshOrders());

        // Make refresh button smaller
        refreshButton.setPreferredSize(new Dimension(80, 30));

        // Add refresh button next to search button
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(searchPanel);
        buttonPanel.add(refreshButton);
        frame.add(buttonPanel, BorderLayout.NORTH);

        // Make the frame visible
        frame.setVisible(true);
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel();
        searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchOrders(searchField.getText()));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        return searchPanel;
    }

    private void searchOrders(String searchString) {
        Container contentPane = frame.getContentPane();
        Component[] components = contentPane.getComponents();
        for (Component component : components) {
            if (component instanceof JScrollPane) {
                contentPane.remove(component);
                break;
            }
        }
        JPanel newOrdersPanel = createOrdersPanel(searchString);
        JScrollPane newOrdersScrollPane = new JScrollPane(newOrdersPanel);
        newOrdersScrollPane.setPreferredSize(new Dimension(450, 300));
        contentPane.add(newOrdersScrollPane, BorderLayout.WEST);
        frame.repaint();
        frame.revalidate();
    }

    private JPanel createOrdersPanel() {
        return createOrdersPanel("");
    }

    private JPanel createOrdersPanel(String searchString) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        List<DocumentSnapshot> activeOrders = fetchActiveOrders();
        for (DocumentSnapshot orderSnapshot : activeOrders) {
            boolean comandaPreluata = orderSnapshot.getBoolean("comandaPreluata");
            if (!comandaPreluata) {
                String idClient = orderSnapshot.getString("idClient");
                if (idClient.contains(searchString)) {
                    Timestamp timestamp = orderSnapshot.getTimestamp("timestamp");
                    boolean cheamaCurier = orderSnapshot.getBoolean("cheamaCurier");
                    boolean curierPeDrum = false;
                    if (cheamaCurier && !Objects.equals(orderSnapshot.getString("idCurier"), "0") && orderSnapshot.getString("idCurier") != null) {
                        curierPeDrum = true;
                    }
                    double price = orderSnapshot.getDouble("pret");
                    List<Map<String, Object>> products = (List<Map<String, Object>>) orderSnapshot.get("Products");

                    JPanel orderRow = new JPanel(new BorderLayout());
                    orderRow.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                    JLabel idClientLabel = new JLabel("ID Comanda: " + idClient);
                    JLabel timestampLabel = new JLabel("Timp: " + formatTimestamp(timestamp));
                    JLabel priceLabel = new JLabel("Pret: " + price);
                    JLabel cheamaCurierLabel = new JLabel("Chemat curier: " + (cheamaCurier ? "Da" : "Nu"));
                    JLabel curierPeDrumLabel = new JLabel("Curier pe drum: " + (curierPeDrum ? "Da" : "Nu"));

                    JPanel infoPanel = new JPanel(new GridLayout(0, 1));
                    infoPanel.add(idClientLabel);
                    infoPanel.add(timestampLabel);
                    infoPanel.add(priceLabel);
                    infoPanel.add(cheamaCurierLabel);
                    infoPanel.add(curierPeDrumLabel);

                    orderRow.add(infoPanel, BorderLayout.WEST);

                    // Add mouse listener to show order details
                    orderRow.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            showOrderDetails(orderSnapshot);
                        }
                    });

                    gbc.gridy++;
                    panel.add(orderRow, gbc);
                }
            }
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(450, 300));

        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.add(scrollPane, BorderLayout.CENTER);
        return wrapperPanel;
    }





    private void showOrderDetails(DocumentSnapshot orderSnapshot) {
        JDialog orderDialog = new JDialog(frame, "Order Details", Dialog.ModalityType.MODELESS);

        // Extract detailed information about the selected order

        String idClient = orderSnapshot.getString("idClient");
        Timestamp timestamp = orderSnapshot.getTimestamp("timestamp");
        double price = orderSnapshot.getDouble("pret");
        boolean isCashPayment = orderSnapshot.getBoolean("plataCash");
        String mentiune = orderSnapshot.getString("mentiune");
        String optiuniText = orderSnapshot.getString("optiuniText");
        List<Map<String, Object>> products = (List<Map<String, Object>>) orderSnapshot.get("Products");
        String clientId = orderSnapshot.getString("idClient");
        String delivererId = orderSnapshot.getString("idCurier"); // Assuming the field name is "idCurier"

        boolean cheamaCurier = orderSnapshot.getBoolean("cheamaCurier");

        // Fetch additional information from Firestore
        String clientName = fetchClientName(clientId);

        String produseText = createProductString(products);
        System.out.println(produseText);
        produseText = makeReadable(produseText);

        // Create a panel to display detailed information
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));

        // Display detailed information
        JLabel idComandaLabel = new JLabel("ID Comanda: " + idClient);
        JLabel timestampLabel = new JLabel("Timestamp: " + formatTimestamp(timestamp));
        JLabel priceLabel = new JLabel("Pret: " + price);
        JLabel paymentLabel = new JLabel("Plata: " + (isCashPayment ? "Cash" : "Card"));
        JLabel clientLabel = new JLabel("Client: " + clientName);
        JLabel mentiuneLabel = new JLabel("Mentiune: " + mentiune);

        String alignedTitle = "                            Detalii Comanda";
        detailsPanel.add(createDetailPanel(alignedTitle, idComandaLabel, timestampLabel, priceLabel, paymentLabel, clientLabel, mentiuneLabel));

        JTextArea produseArea = new JTextArea(produseText);
        produseArea.setEditable(false);
        produseArea.setLineWrap(true);
        produseArea.setWrapStyleWord(true);
        JScrollPane produseScrollPane = new JScrollPane(produseArea);
        produseScrollPane.setPreferredSize(new Dimension(300, 100));

        detailsPanel.add(createDetailPanel("Produse", produseScrollPane));

        System.out.println("Uite: " + optiuniText);
        if (!optiuniText.equals(" ")) {
            optiuniText = makeReadable(optiuniText);
            // Display ordered products
            JTextArea optiuniArea = new JTextArea(optiuniText);
            optiuniArea.setEditable(false); // Ensure it's not editable
            optiuniArea.setLineWrap(true); // Enable line wrapping
            optiuniArea.setWrapStyleWord(true); // Wrap at word boundaries
            JScrollPane optionsScrollPane = new JScrollPane(optiuniArea);
            optionsScrollPane.setPreferredSize(new Dimension(300, 100)); // Set preferred size

            //detailsPanel.add(createDetailPanel("Ordered Products", produseLabel));
            detailsPanel.add(createDetailPanel("Preferinte", optionsScrollPane));
        }


        String textDelivererButton = "";
        if(cheamaCurier) {
            textDelivererButton = "Preda Comanda";
        }
        if(!cheamaCurier) {
            textDelivererButton = "Cheama Curier";
        }

        JButton delivererButton = new JButton(textDelivererButton);
        delivererButton.addActionListener(e -> {
            showDelivererDialog(delivererId, orderSnapshot, cheamaCurier);
            orderDialog.dispose();
        });
        detailsPanel.add(delivererButton);

        // Create a non-modal dialog
        orderDialog.getContentPane().add(detailsPanel);
        orderDialog.pack();
        orderDialog.setLocationRelativeTo(frame);
        orderDialog.setVisible(true);

        // Optionally, disable the main frame while the dialog is open
        // frame.setEnabled(false);
    }




    private String makeReadable(String optiuniText) {
        String[] splits = optiuniText.split("_"); // Escape the pipe character
        StringBuilder output = new StringBuilder();
        for (String split : splits) {
            output.append(split.trim()).append("\n"); // Trim and add new line
        }
        return output.toString();
    }


    private void showDelivererDialog(String expectedDelivererId, DocumentSnapshot orderSnapshot, boolean cheamaCurier) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        if(cheamaCurier) {
            JLabel label = new JLabel("ID-ul curierului care a venit sa ridice comanda:");
            JTextField delivererIdField = new JTextField();

            panel.add(label);
            panel.add(delivererIdField);

            int result = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "Enter Deliverer ID",
                    JOptionPane.OK_CANCEL_OPTION
            );

            if (result == JOptionPane.OK_OPTION) {
                // OK button clicked, check if the entered ID matches
                String enteredDelivererId = delivererIdField.getText();
                if (enteredDelivererId.equals(expectedDelivererId)) {
                    // Perform the handover action here
                    JOptionPane.showMessageDialog(
                            null,
                            "Comanda a fost predata cu succes!",
                            "Handover Successful",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    // Update the Firestore document
                    updateOrderDocument(orderSnapshot);

                    // Refresh orders
                    //refreshOrders();
                    frame.dispose(); // Close the current frame
                    new ManageOrders(idRestaurant);
                } else {
                    // Incorrect ID entered
                    JOptionPane.showMessageDialog(
                            null,
                            "Invalid Deliverer ID!",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }
        if(!cheamaCurier) {
            JLabel label = new JLabel("Sa chemam un curier sa ridice comanda ?");

            panel.add(label);

            int result = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "Call Deliverer",
                    JOptionPane.OK_CANCEL_OPTION
            );

            if (result == JOptionPane.OK_OPTION) {
                cheamaCurier(orderSnapshot);

                    JOptionPane.showMessageDialog(
                            null,
                            "Am chemat un curier !",
                            "Called deliverer Successfuly",
                            JOptionPane.INFORMATION_MESSAGE
                    );


                    // Refresh orders
                    //refreshOrders();
                    frame.dispose(); // Close the current frame
                     new ManageOrders(idRestaurant);
            }
        }
    }

    // Helper method to update the Firestore document
    private void updateOrderDocument(DocumentSnapshot orderSnapshot) {
        try {
            FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            Firestore firestore = firestoreOptions.getService();

            // Get the reference to the "Active Orders" collection
            CollectionReference ordersRef = firestore.collection("Active Orders");

            // Get the ID of the document to update
            String orderId = orderSnapshot.getId();

            // Update the "comandaPreluata" field to true
            ApiFuture<WriteResult> writeResult = ordersRef.document(orderId).update("comandaPreluata", true);
            System.out.println("Update time: " + writeResult.get().getUpdateTime());

            // Close Firestore connection
            firestore.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cheamaCurier(DocumentSnapshot orderSnapshot) {
        try {
            FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            Firestore firestore = firestoreOptions.getService();

            // Get the ID of the order document
            String orderId = orderSnapshot.getId();

            // Retrieve data from the active order document
            Map<String, Object> orderData = orderSnapshot.getData();

            // Set the "cheamaCurier" attribute to true
            orderData.put("cheamaCurier", true);

            // Add the order document to "Pending Orders" collection
            ApiFuture<DocumentReference> pendingDocReference = firestore.collection("Pending Orders").add(orderData);

            // Delete the document from "Active Orders" collection
            ApiFuture<WriteResult> deleteResult = firestore.collection("Active Orders").document(orderId).delete();
            deleteResult.get(); // Wait for deletion to complete

            // Close Firestore connection
            firestore.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void refreshOrders() {
        // Get the frame content pane
        Container contentPane = frame.getContentPane();

        // Remove the existing orders panel
        Component[] components = contentPane.getComponents();
        for (Component component : components) {
            if (component instanceof JScrollPane) {
                contentPane.remove(component);
                break;
            }
        }

        // Create a new orders panel
        JPanel newOrdersPanel = createOrdersPanel();
        JScrollPane newOrdersScrollPane = new JScrollPane(newOrdersPanel);
        newOrdersScrollPane.setPreferredSize(new Dimension(frame.getWidth() * 2 / 3, frame.getHeight()));

        // Add the new orders panel
        contentPane.add(newOrdersScrollPane, BorderLayout.WEST);

        // Repaint and revalidate the frame
        frame.repaint();
        frame.revalidate();
    }

    // Helper method to format timestamp
    private String formatTimestamp(Timestamp timestamp) {
        java.util.Date date = timestamp.toDate();
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    // Helper method to create a panel for displaying details
    private JPanel createDetailPanel(String title, Component... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));

        for (Component component : components) {
            panel.add(component);
        }

        return panel;
    }


    // Helper method to create labels for ordered products
    private String createProductString(List<Map<String, Object>> products) {
        StringBuilder productInfo = new StringBuilder();

        for (Map<String, Object> product : products) {
            String productId = (String) product.get("idProdus");
            int quantity = ((Long) product.get("cantitate")).intValue();
            String productName = fetchProductName(productId);

            productInfo.append(quantity).append(" ").append(productName).append("\n");
        }

        return productInfo.toString();
    }

    private List<DocumentSnapshot> fetchActiveOrders() {
       System.out.println("debug1");
        try {
            FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            Firestore firestore = firestoreOptions.getService();

            CollectionReference ordersRef = firestore.collection("Active Orders");
            System.out.println("debug2");


            // Query to find active orders for the specific restaurant
            Query query = ordersRef.whereEqualTo("idRestaurant", idRestaurant).orderBy("timestamp");

            // Execute the query
            ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
            QuerySnapshot querySnapshot = querySnapshotFuture.get();

            List<DocumentSnapshot> activeOrders = new ArrayList<>();
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                activeOrders.add(document);
                System.out.println(document.getId() + " => " + document.getData());
            }

            // Close Firestore connection
            firestore.close();

            return activeOrders;
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String fetchClientName(String clientId) {
        try {
            FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            Firestore firestore = firestoreOptions.getService();

            CollectionReference clientsRef = firestore.collection("Clients");

            // Query to find the client with the specific ID
            Query query = clientsRef.whereEqualTo("idClient", clientId);

            // Execute the query
            ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
            QuerySnapshot querySnapshot = querySnapshotFuture.get();

            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot clientDocument = querySnapshot.getDocuments().get(0);
                String clientName = clientDocument.getString("numeClient");

                // Close Firestore connection
                firestore.close();

                return clientName;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Unknown Client";
    }

    private String fetchProductName(String productId) {
        try {
            FileInputStream serviceAccount = new FileInputStream("C:\\Users\\NewPC\\Documents\\key_to_firebase\\licenta-c8625-firebase-adminsdk-fukc0-fe7097910c.json");
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            Firestore firestore = firestoreOptions.getService();

            CollectionReference productsRef = firestore.collection("Products");

            // Query to find the product with the specific ID
            Query query = productsRef.whereEqualTo("idProdus", productId);

            // Execute the query
            ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
            QuerySnapshot querySnapshot = querySnapshotFuture.get();

            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot productDocument = querySnapshot.getDocuments().get(0);
                String productName = productDocument.getString("numeProdus");

                // Close Firestore connection
                firestore.close();

                return productName;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Unknown Product";
    }
}
