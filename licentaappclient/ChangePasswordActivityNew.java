package com.example.licentaappclient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangePasswordActivityNew extends AppCompatActivity {
    FirebaseFirestore db;
    String idClient;
    private LinearLayout cardContent;
    private EditText txtParola, txtConfirmParola;
    private TextView lblParola, lblConfirmParola;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password_new);

        idClient = getIntent().getStringExtra("idClient");
        cardContent = findViewById(R.id.card_content);
        txtParola = findViewById(R.id.txtParola);
        txtConfirmParola = findViewById(R.id.txtConfirmParola);
        lblParola = findViewById(R.id.lblParola);
        lblConfirmParola = findViewById(R.id.lblConfirmParola);

        db = FirebaseFirestore.getInstance();
        addListenerOnTxt();

        cardContent.animate().alpha(1).setDuration(1000);
    }

    public void startMainMenuActivity(View v)
    {
        startMainMenuActivity();
    }

    private void startMainMenuActivity() {
        Animation cardContentFadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        cardContent.startAnimation(cardContentFadeOut);



        //make it wait for the animation to finish
        cardContentFadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                cardContent.setAlpha(0);
                setContentView(R.layout.activity_manage_account_new);
                Intent intent = new Intent(ChangePasswordActivityNew.this, ManageAccountActivityNew.class);
                intent.putExtra("idClient", idClient);
                startActivity(intent);
                //make this transition be instant
                finish();
                overridePendingTransition(0,0);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


    }

    public void confirmNewPassword(View v)
    {
        String newPassword = txtParola.getText().toString().trim();

        if (checkEnteredPassword(1)) {
            updatePasswordInFirestore(newPassword);
        }
    }

    private void addListenerOnTxt() {

        if(txtParola == null)
            txtParola = findViewById(R.id.txtParola);
        txtParola.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    checkEnteredPassword(2);
                }
            }
        });

        if(txtConfirmParola == null)
            txtConfirmParola = findViewById(R.id.txtConfirmParola);
        txtConfirmParola.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    checkEnteredPassword(2);
                }
            }
        });
    }

    public boolean checkEnteredPassword(int tipVerificare) {
        String parola = txtParola.getText().toString().trim();
        String confirmParola = txtConfirmParola.getText().toString().trim();

        if(parola.isEmpty()){
            txtParola.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.email_check_failure_icon, 0);
            if(tipVerificare == 1) {
                txtParola.requestFocus();
                txtParola.setError("Introduceți o parolă.");
            }
            return false;
        }


        if(parola.length() < 8) {
            txtParola.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.email_check_failure_icon, 0);
            if(tipVerificare == 1) {
                txtParola.requestFocus();
                txtParola.setError("Parola trebuie să conțină cel puțin 8 caractere.");
            }
            return false;
        }

        if(checkPasswordForSpaces(parola)) {
            txtParola.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.email_check_failure_icon, 0);
            if(tipVerificare == 1) {
                txtParola.requestFocus();
                txtParola.setError("Parola nu trebuie să conțină spații.");
            }
            return false;
        }

        if(!checkEnteredPassword(parola)){
            txtParola.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.email_check_failure_icon, 0);
            if(tipVerificare == 1) {
                txtParola.requestFocus();
                txtParola.setError("Parola trebuie să conțină cel puțin 8 caractere, să nu conțină spații, să aibă cel puțin o majusculă și să conțină cel puțin un simbol: !, @, #, $, %, ^, &, *, ?");
            }
            return false;
        }

        if(confirmParola.isEmpty()){
            txtConfirmParola.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.email_check_failure_icon, 0);
            if(tipVerificare == 1) {
                txtConfirmParola.requestFocus();
                txtConfirmParola.setError("Introduceți o parolă.");
            }
            return false;
        }


        if(!parola.equals(confirmParola)) {
            txtParola.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.email_check_failure_icon, 0);
            txtConfirmParola.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.email_check_failure_icon, 0);
            if(tipVerificare == 1) {
                txtParola.requestFocus();
                txtParola.setError("Parolele nu coincid.");
                txtConfirmParola.requestFocus();
                txtConfirmParola.setError("Parolele nu coincid.");
            }
            return false;
        }

        txtParola.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.email_check_success_icon, 0);
        txtConfirmParola.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.email_check_success_icon, 0);
        return true;
    }

    public boolean checkEnteredPassword(String parola) {
        String sablon="((?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*?])(?=.*[A-Z]).{8,16})";
        Pattern p = Pattern.compile(sablon);
        Matcher m = p.matcher(parola);
        if(!m.matches())
            return false;

        return true;
    }

    private boolean checkPasswordForSpaces(String password) {
        for (int i = 0; i < password.length(); i++) {
            if (Character.isWhitespace(password.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private void updatePasswordInFirestore(String newPassword) {
        // Reference to the "Clients" collection
        CollectionReference clientsRef = db.collection("Clients");

        // Query to find the document with the specified idClient and password
        Query query = clientsRef.whereEqualTo("idClient", idClient);

        String hashedPassword = hashPassword(newPassword);

        // Execute the query
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Assuming there's at most one matching document, get the first one
                    DocumentSnapshot clientDoc = querySnapshot.getDocuments().get(0);

                    // Update the password field
                    clientsRef.document(clientDoc.getId())
                            .update("password", hashedPassword)
                            .addOnSuccessListener(aVoid -> {
                                // Password updated successfully
                                Toast.makeText(ChangePasswordActivityNew.this, "Parola schimbata cu succes!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                // Error updating password
                                Toast.makeText(ChangePasswordActivityNew.this, "A aparut o eroare. Parola nu a fost schimbata.", Toast.LENGTH_SHORT).show();
                                Log.e("FirestoreError", "Error updating password", e);
                            });
                } else {
                    // Handle the case when no document matches the query or current password is incorrect
                    Toast.makeText(ChangePasswordActivityNew.this, "Parola curenta incorecta.", Toast.LENGTH_SHORT).show();
                    Log.d("FirestoreDebug", "No document found for idClient: " + idClient);
                }
            } else {
                // Handle errors
                Log.e("FirestoreError", "Error querying Clients collection: ", task.getException());
            }
        });

    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}