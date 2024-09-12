package com.example.licentaappclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import android.content.SharedPreferences;
import android.widget.CheckBox;

public class LoginActivity extends AppCompatActivity {

    private EditText txtUsername, txtPassword;
    private CheckBox checkRememberMe;
    private SharedPreferences preferences;
    FirebaseFirestore db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize EditText fields
        txtUsername = findViewById(R.id.txtUsername);
        txtPassword = findViewById(R.id.txtPassword);
        checkRememberMe = findViewById(R.id.checkRememberMe);

        txtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // Retrieve SharedPreferences instance
        preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // Check if "Remember me" option was checked previously
        boolean rememberMeChecked = preferences.getBoolean("rememberMeChecked", false);
        if (rememberMeChecked) {
            // Retrieve stored user ID and proceed to MainMenuActivity
            String storedUserId = preferences.getString("userId", "");
            if (!storedUserId.isEmpty()) {
                startMainMenuActivity(storedUserId);
            }
        }

        // Handle Register button click
        Button registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start RegisterActivity
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    public void attemptLogin(View v) {
        // Retrieve input username or email and password
        String userInput = txtUsername.getText().toString().trim();
        String password = txtPassword.getText().toString().trim();

        // Validate username/email and password fields
        if (TextUtils.isEmpty(userInput) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter username/email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Query the "Clients" collection in Firestore based on username or email
        db.collection("Clients")
                .whereEqualTo(isEmailFormat(userInput) ? "email" : "username", userInput)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                // Check if the document's "password" field matches the input password
                                String actualPassword = document.getString("password");
                                if (actualPassword != null && actualPassword.equals(password)) {
                                    // Password matches, login successful
                                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                    // Get the id of the document
                                    String idClient = document.getString("idClient");
                                    // Start MainMenuActivity and pass idClient as an extra
                                    startMainMenuActivity(idClient);

                                    // Save user ID in SharedPreferences if "Remember me" is checked
                                    if (checkRememberMe.isChecked()) {
                                        saveUserIdInPreferences(idClient);
                                    }

                                    return;
                                }
                            }
                            // No matching document found or password doesn't match
                            Toast.makeText(LoginActivity.this, "Invalid username/email or password", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("LoginActivity", "Error getting documents: ", task.getException());
                            Toast.makeText(LoginActivity.this, "Error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private boolean isEmailFormat(String input) {
        // Simple email format check using regex
        return android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches();
    }


    private void startMainMenuActivity(String userId) {
        Intent intent = new Intent(LoginActivity.this, MainMenuActivity.class);
        intent.putExtra("idClient", userId);
        startActivity(intent);
        finish(); // Finish the current activity
    }

    private void saveUserIdInPreferences(String userId) {
        // Save user ID and set "Remember me" flag in SharedPreferences
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("userId", userId);
        editor.putBoolean("rememberMeChecked", true);
        editor.apply();
    }
}