package com.example.licentaappclient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final int PICK_ADDRESS_REQUEST_CODE = 1;

    private EditText txtUsername, txtEmail, txtPassword, txtConfirmPassword, txtAddress, txtDateOfBirth, txtFullName;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        txtUsername = findViewById(R.id.txtUsername);
        txtEmail = findViewById(R.id.txtEmail);
        txtPassword = findViewById(R.id.txtPassword);
        txtConfirmPassword = findViewById(R.id.txtConfirmPassword);
        txtAddress = findViewById(R.id.txtAddress);
        txtDateOfBirth = findViewById(R.id.txtDateOfBirth);
        txtFullName = findViewById(R.id.txtFullName);
    }

    public void attemptRegister(View view) {
        String username = txtUsername.getText().toString().trim();
        String email = txtEmail.getText().toString().trim();
        String password = txtPassword.getText().toString().trim();
        String confirmPassword = txtConfirmPassword.getText().toString().trim();
        String address = txtAddress.getText().toString().trim();
        String dateOfBirth = txtDateOfBirth.getText().toString().trim();
        String fullName = txtFullName.getText().toString().trim();

        // Validate username
        if (username.isEmpty()) {
            txtUsername.setError("Username is required");
            return;
        }

        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            txtEmail.setError("Invalid email address");
            return;
        }

        // Check if username or email already exist
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Check if username or email already exist
        db.collection("Clients").whereEqualTo("username", username).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().isEmpty()) {
                    // Username already exists
                    txtUsername.setError("Username already exists");
                } else {
                    // Username is unique, now check email
                    db.collection("Clients").whereEqualTo("email", email).get().addOnCompleteListener(emailTask -> {
                        if (emailTask.isSuccessful()) {
                            if (!emailTask.getResult().isEmpty()) {
                                // Email already exists
                                txtEmail.setError("Email already registered");
                            } else {
                                // Email is also unique, proceed with registration
                                if (!password.equals(confirmPassword) || password.isEmpty() || checkPasswordForSpaces(password)) {
                                    txtConfirmPassword.setError("Passwords not valid");
                                } else {
                                    // Parse dateOfBirth into year, month, and dayOfMonth
                                    String[] dobParts = dateOfBirth.split("/");
                                    int year = Integer.parseInt(dobParts[2]);
                                    int month = Integer.parseInt(dobParts[1]) - 1; // Subtract 1 as month index is zero-based
                                    int dayOfMonth = Integer.parseInt(dobParts[0]);

                                    // Check if the user is at least 18 years old
                                    if (!isUserAbove18(year, month, dayOfMonth)) {
                                        // If user is not at least 18 years old, show error message
                                        txtDateOfBirth.setError("You must be at least 18 years old");
                                        Toast.makeText(RegisterActivity.this, "You must be at least 18 years old to register", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    // Get idClient from Firestore and increment the counter
                                    db.collection("Reguli").document("CounterIdClient").get().addOnCompleteListener(idClientTask -> {
                                        if (idClientTask.isSuccessful()) {
                                            DocumentSnapshot idClientDoc = idClientTask.getResult();
                                            if (idClientDoc.exists()) {
                                                long idClientValue = idClientDoc.getLong("idClient");
                                                String idClient = String.valueOf(idClientValue);
                                                // Increment the counter
                                                idClientValue++;
                                                db.collection("Reguli").document("CounterIdClient").update("idClient", idClientValue)
                                                        .addOnCompleteListener(updateTask -> {
                                                            if (updateTask.isSuccessful()) {
                                                                // Create a new document with idClient and other details
                                                                Map<String, Object> clientData = new HashMap<>();
                                                                clientData.put("username", username);
                                                                clientData.put("email", email);
                                                                clientData.put("password", password);
                                                                clientData.put("adresaClient", address);
                                                                clientData.put("idClient", idClient);
                                                                clientData.put("datoreazaRating", false);
                                                                clientData.put("numeClient", fullName); // Include full name

                                                                db.collection("Clients").add(clientData)
                                                                        .addOnSuccessListener(documentReference -> {
                                                                            // Registration successful
                                                                            String documentId = documentReference.getId();
                                                                            Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                                                            Intent intent = new Intent(RegisterActivity.this, MainMenuActivity.class);
                                                                            intent.putExtra("idClient", idClient);
                                                                            startActivity(intent);
                                                                            finish();
                                                                        })
                                                                        .addOnFailureListener(e -> {
                                                                            // Registration failed
                                                                            Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                                                                        });
                                                            } else {
                                                                // Failed to update counter
                                                                Toast.makeText(RegisterActivity.this, "Failed to update counter", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                            } else {
                                                // Document for CounterIdClient does not exist
                                                Toast.makeText(RegisterActivity.this, "Counter document does not exist", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            // Error getting idClient
                                            Toast.makeText(RegisterActivity.this, "Error getting idClient", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }
                        } else {
                            // Error getting documents
                            Toast.makeText(RegisterActivity.this, "Error checking email", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                // Error getting documents
                Toast.makeText(RegisterActivity.this, "Error checking username", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void pickAddress(View view) {
        Intent intent = new Intent(this, PickAddressActivity.class);
        startActivityForResult(intent, PICK_ADDRESS_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_ADDRESS_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String selectedAddress = data.getStringExtra("selectedAddress");
            txtAddress.setText(selectedAddress);
        }
    }

    private boolean checkPasswordForSpaces(String password) {
        for (int i = 0; i < password.length(); i++) {
            if (Character.isWhitespace(password.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public void pickDateOfBirth(View view) {
        // Get current date
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        // Create DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        // Set selected date to EditText
                        String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                        txtDateOfBirth.setText(selectedDate);

                        // Check if the user is at least 18 years old
                        if (!isUserAbove18(year, month, dayOfMonth)) {
                            // If user is not at least 18 years old, show error message
                            txtDateOfBirth.setError("You must be at least 18 years old to register");
                            Toast.makeText(RegisterActivity.this, "You must be at least 18 years old to register", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, year, month, dayOfMonth);

        // Set maximum date to ensure the user is not selecting a future date
        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());

        // Show DatePickerDialog
        datePickerDialog.show();
    }

    private boolean isUserAbove18(int year, int month, int dayOfMonth) {
        // Get current date
        Calendar currentDate = Calendar.getInstance();

        // Set the date 18 years ago
        currentDate.add(Calendar.YEAR, -18);

        // Set the maximum allowed birth date for the user (18 years ago)
        Calendar userBirthDate = Calendar.getInstance();
        userBirthDate.set(year, month, dayOfMonth);

        // Check if the user's birth date is before the maximum allowed birth date
        return userBirthDate.before(currentDate);
    }
}
