package com.example.licentaappclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LoginActivityNewClone extends AppCompatActivity {
    FirebaseFirestore db;

    private TextView txtEmail;
    private TextView txtPassword;
    private CheckBox checkRememberMe;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_new_clone);


        txtEmail = findViewById(R.id.txtEmail);
        txtPassword = findViewById(R.id.txtParola);
        checkRememberMe = findViewById(R.id.checkRememberMe);

        db = FirebaseFirestore.getInstance();
        setTxtPrimire();
        addListenerOnTxtEmail();

        preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean rememberMeChecked = preferences.getBoolean("rememberMeChecked", false);
        if (rememberMeChecked) {
            // Retrieve stored user ID and proceed to MainMenuActivity
            String storedUserId = preferences.getString("userId", "");
            if (!storedUserId.isEmpty()) {
                startMainMenuActivity(storedUserId);
            }
        }
    }

    private void setTxtPrimire() {
        TextView txtPrimire = findViewById(R.id.txtNumeClient);

        //get system time and if its day make the text of txtPrimire be "Buna ziua !" else "Buna seara !"
        //get system time
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        String currentTime = simpleDateFormat.format(calendar.getTime());
        //if its day make the text of txtPrimire be "Buna ziua !" else "Buna seara !"

        if (currentTime.compareTo("06:00:00") >= 0 && currentTime.compareTo("23:00:00") < 0) {
            txtPrimire.setText("Bună ziua !");
        } else {
            txtPrimire.setText("Bună seara !");
        }
    }

    public void checkEnteredEmail() {
        String email = txtEmail.getText().toString().trim();
        if (!TextUtils.isEmpty(email)) {
            checkEnteredEmail(email);
        }
        Log.d("debug", "checkEnteredEmail: " + email);
    }

    private void checkEnteredEmail(String email) {

        db.collection("Clients")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                // Email found, set drawableEnd to a success icon
                                txtEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.email_check_success_icon, 0);
                                Log.d("debug", "Email found: " + email);
                            } else {
                                // Email not found, set drawableEnd to a failure icon
                                txtEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.email_check_failure_icon, 0);
                                Log.d("debug", "Email not found: " + email);
                            }
                        } else {
                            Log.e("debug", "Error getting documents: ", task.getException());
                            // Handle error
                            Toast.makeText(LoginActivityNewClone.this, "Error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void attemptLogin(View v)
    {
        // Retrieve input username or email and password
        String userInput = txtEmail.getText().toString().trim();
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
                                    Toast.makeText(LoginActivityNewClone.this, "Login successful", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(LoginActivityNewClone.this, "Invalid username/email or password", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("LoginActivity", "Error getting documents: ", task.getException());
                            Toast.makeText(LoginActivityNewClone.this, "Error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private boolean isEmailFormat(String input) {
        // Simple email format check using regex
        return android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches();
    }


    private void startMainMenuActivity(String userId) {
        Intent intent = new Intent(LoginActivityNewClone.this, MainMenuActivity.class);
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

    private void addListenerOnTxtEmail() {
        // Set listener for email EditText to check entered email when focus is lost
        txtEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    checkEnteredEmail();
                }
            }
        });
    }
}
