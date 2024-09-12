package com.example.licentaappclient;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SelectOptionsActivity extends AppCompatActivity {

    private TextView txtTitle;
    private LinearLayout llOptionsContainer;
    private Button btnConfirm;

    private String numeProdus, idProdus;
    private int quantity;
    private int index;
    private List<Map<String, Object>> optionsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_options);

        txtTitle = findViewById(R.id.txtTitle);
        llOptionsContainer = findViewById(R.id.llOptionsContainer);
        btnConfirm = findViewById(R.id.btnConfirm);

        // Get intent extras
        Intent intent = getIntent();
        if (intent != null) {
            idProdus = intent.getStringExtra("idProdus");
            numeProdus = intent.getStringExtra("numeProdus");
            quantity = intent.getIntExtra("quantity", 0);
            index = intent.getIntExtra("index", 0);
        }

        // Set title
        txtTitle.setText("Optiuni pentru " + numeProdus + " " + index);

        // Fetch options from Firestore
        fetchOptionsFromFirestore();

        // Button click listener
        btnConfirm.setOnClickListener(this::confirmSelection);
    }

    private void fetchOptionsFromFirestore() {
        // Reference to the "Products" collection
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference productsCollection = db.collection("Products");

        // Query to get the document with the specified idProdus
        Query query = productsCollection.whereEqualTo("idProdus", idProdus);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Since you expect only one document, get the first one
                    DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);

                    // Retrieve the Options array from the document
                    optionsList = (List<Map<String, Object>>) documentSnapshot.get("Optiuni");

                    // Add options dynamically
                    if (optionsList != null) {
                        for (Map<String, Object> option : optionsList) {
                            String intrebare = (String) option.get("Intrebare");
                            boolean raspunsMultiplu = (boolean) option.get("raspunsMultiplu");
                            List<String> options = getOptions(option);

                            // Add question text
                            TextView txtQuestion = new TextView(this);
                            txtQuestion.setText(intrebare);
                            llOptionsContainer.addView(txtQuestion);

                            // Add options
                            if (raspunsMultiplu) {
                                // For multiple selection, use CheckBoxes
                                for (String optiune : options) {
                                    CheckBox checkBox = new CheckBox(this);
                                    checkBox.setText(optiune);
                                    checkBox.setChecked(true);
                                    llOptionsContainer.addView(checkBox);
                                }
                            } else {
                                // For single selection, use RadioButtons
                                RadioGroup radioGroup = new RadioGroup(this);
                                for (String optiune : options) {
                                    RadioButton radioButton = new RadioButton(this);
                                    radioButton.setText(optiune);
                                    radioGroup.addView(radioButton);
                                }
                                // Set the first radio button as checked
                                if (radioGroup.getChildCount() > 0) {
                                    ((RadioButton) radioGroup.getChildAt(0)).setChecked(true);
                                }
                                llOptionsContainer.addView(radioGroup);
                            }
                        }
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




    private List<String> getOptions(Map<String, Object> option) {
        List<String> options = new ArrayList<>();
        for (Map.Entry<String, Object> entry : option.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("optiune")) {
                String optiune = (String) entry.getValue();
                options.add(optiune);
            }
        }
        return options;
    }

    private void setDefaultSelections() {
        int childCount = llOptionsContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = llOptionsContainer.getChildAt(i);
            if (view instanceof RadioGroup) {
                RadioGroup radioGroup = (RadioGroup) view;
                if (radioGroup.getChildCount() > 0) {
                    RadioButton radioButton = (RadioButton) radioGroup.getChildAt(0);
                    radioButton.setChecked(true);
                }
            }
        }
    }

    private void confirmSelection(View v) {
        StringBuilder selectedOptionsBuilder = new StringBuilder();
        int childCount = llOptionsContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = llOptionsContainer.getChildAt(i);
            if (view instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) view;
                if (checkBox.isChecked()) {
                    String selectedOption = checkBox.getText().toString();
                    selectedOptionsBuilder.append(selectedOption).append(" ");
                }
            } else if (view instanceof RadioGroup) {
                RadioGroup radioGroup = (RadioGroup) view;
                int selectedId = radioGroup.getCheckedRadioButtonId();
                if (selectedId != -1) {
                    RadioButton radioButton = radioGroup.findViewById(selectedId);
                    String selectedOption = radioButton.getText().toString();
                    selectedOptionsBuilder.append(selectedOption).append(" ");
                }
            }
        }

        String selectedOptions = " " + selectedOptionsBuilder.toString().trim();
        selectedOptions = numeProdus + index + selectedOptions;
        Log.d("debug655", selectedOptions);
        index++;

        // Prepare result intent
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selectedOptions", selectedOptions);
        resultIntent.putExtra("numeProdus", numeProdus);
        resultIntent.putExtra("idProdus", idProdus);
        resultIntent.putExtra("quantity", quantity);
        resultIntent.putExtra("index", index);

        Log.d("optiuniString", numeProdus + " " + index + " " + selectedOptions);

        // Set result and finish activity
        setResult(RESULT_OK, resultIntent);
        finish();
    }

}
