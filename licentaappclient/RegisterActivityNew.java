package com.example.licentaappclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mindrot.jbcrypt.BCrypt;

public class RegisterActivityNew extends AppCompatActivity {
    FirebaseFirestore db;
    private EditText txtFullName, txtEmail, txtParola, txtConfirmParola;
    private LinearLayout cardContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_new);

        addListenerOnTxt();

        txtFullName = findViewById(R.id.txtFullName);
        txtEmail = findViewById(R.id.txtEmail);
        txtParola = findViewById(R.id.txtParola);
        txtConfirmParola = findViewById(R.id.txtConfirmParola);
        cardContent = findViewById(R.id.card_content);

        //cardContent.setAlpha(0);
        //Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        //cardContent.startAnimation(fadeInAnimation);
        cardContent.animate().alpha(1).setDuration(1000);
    }

    public void attemptRegister(View view)
    {
        String fullName = txtFullName.getText().toString();
        String email = txtEmail.getText().toString();
        String parola = txtParola.getText().toString();
        String confirmParola = txtConfirmParola.getText().toString();


        if(fullName.isEmpty())
        {
            checkEnteredPassword(1);
            checkEnteredEmail();
            txtFullName.requestFocus();
            txtFullName.setError("Completează numele");
            return;
        }
        if(email.isEmpty())
        {
            checkEnteredPassword(1);
            checkEnteredEmail();
            txtEmail.requestFocus();
            txtEmail.setError("Completează adresa de email");
            return;
        }

        if(!isEmailFormat(email))
        {
            checkEnteredPassword(1);
            checkEnteredEmail();
            txtEmail.requestFocus();
            txtEmail.setError("Adresa de email nu este validă");
            return;
        }

        if(!checkEnteredPassword(1)) {
            checkEnteredPassword(1);
            checkEnteredEmail();
            return;
        }

        String hashedPassword = hashPassword(parola);

        // Apelare API pentru inregistrare
        // Check if username or email already exist
        db = FirebaseFirestore.getInstance();

        db.collection("Clients").whereEqualTo("email", email).get().addOnCompleteListener(emailTask -> {
            if (emailTask.isSuccessful()) {
                if (!emailTask.getResult().isEmpty()) {
                    // Email already exists
                    txtEmail.setError("Adresa email deja existenta");
                } else {
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
                                                clientData.put("email", email);
                                                clientData.put("password", hashedPassword);
                                                clientData.put("adresaClient", "");
                                                clientData.put("idClient", idClient);
                                                clientData.put("datoreazaRating", false);
                                                clientData.put("numeClient", fullName); // Include full name

                                                db.collection("Clients").add(clientData)
                                                        .addOnSuccessListener(documentReference -> {
                                                            startLoginActivity();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            // Registration failed
                                                            Toast.makeText(RegisterActivityNew.this, "Registration failed", Toast.LENGTH_SHORT).show();
                                                        });
                                            } else {
                                                // Failed to update counter
                                                Toast.makeText(RegisterActivityNew.this, "Failed to update counter", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                // Document for CounterIdClient does not exist
                                Toast.makeText(RegisterActivityNew.this, "Counter document does not exist", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Error getting idClient
                            Toast.makeText(RegisterActivityNew.this, "Error getting idClient", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                // Error getting documents
                Toast.makeText(RegisterActivityNew.this, "Error checking email", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private boolean isEmailFormat(String input) {
        // Simple email format check using regex
        return android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches();
    }

    private void addListenerOnTxt() {
        // Set listener for email EditText to check entered email when focus is lost
        if(txtEmail == null)
            txtEmail = findViewById(R.id.txtEmail);
        txtEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    checkEnteredEmail();
                }
            }
        });

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

    public void checkEnteredEmail() {
        String email = txtEmail.getText().toString().trim();
        if (!TextUtils.isEmpty(email)) {
            checkEnteredEmail(email);
        }
        Log.d("debug", "checkEnteredEmail: " + email);
    }

    private void checkEnteredEmail(String email) {
        Log.d("debug", "checkEnteredEmailPas2: " + email);
        if(!isEmailFormat(email))
        {
            txtEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.email_check_failure_icon, 0);
            return;
        }

        db = FirebaseFirestore.getInstance();
        db.collection("Clients")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
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
                            Toast.makeText(RegisterActivityNew.this, "Error occurred. Please try again.", Toast.LENGTH_SHORT).show();
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

    public void startLoginActivity(View v)
    {
        startLoginActivity();
    }

    private void startLoginActivity()
    {
        // Apply animations to the card view
        CardView cardView = findViewById(R.id.card_view);
        LinearLayout cardContent = findViewById(R.id.card_content);
        View cardHeight = findViewById(R.id.card_height);
        LinearLayout logoPositionLayout = findViewById(R.id.logo_position);

        //logoPositionLayout.setPadding(logoPositionLayout.getPaddingLeft(), 18, logoPositionLayout.getPaddingRight(), 18);



        Animation cardContentFadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        cardContent.startAnimation(cardContentFadeOut);



        //make it wait for the animation to finish
        cardContentFadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                //wait 3 seconds before going to the next line
                /*try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/

                cardContent.setAlpha(0);
                transitionToLoginActivity(cardView);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void transitionToLoginActivity(CardView cardView)
    {
        Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_register_to_login);
        cardView.startAnimation(slideDownAnimation);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Finish the current activity without any animation
                setContentView(R.layout.activity_login_new);
                Intent intent = new Intent(RegisterActivityNew.this, LoginActivityNew.class);
                startActivity(intent);
                overridePendingTransition(0,0);
                //make this transition be instant
                finish();
            }
        }, 750);



        //make it wait for the animation to finish
        slideDownAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                /*// Finish the current activity without any animation
                setContentView(R.layout.activity_login_new);
                Intent intent = new Intent(RegisterActivityNew.this, LoginActivityNew.class);
                startActivity(intent);
                overridePendingTransition(0,0);
                //make this transition be instant
                finish();*/
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

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