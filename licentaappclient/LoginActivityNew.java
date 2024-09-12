package com.example.licentaappclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LoginActivityNew extends AppCompatActivity {
    FirebaseFirestore db;
    private String idClient;

    private TextView txtEmail;
    private TextView txtPassword;
    private CheckBox checkRememberMe;
    private SharedPreferences preferences;
    private LinearLayout cardContent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_new);

        txtEmail = findViewById(R.id.txtEmail);
        txtEmail.setText("");
        txtPassword = findViewById(R.id.txtParola);
        checkRememberMe = findViewById(R.id.checkRememberMe);
        cardContent = findViewById(R.id.card_content);

        db = FirebaseFirestore.getInstance();
        setTxtPrimire();
        addListenerOnTxtEmail();

        cardContent.animate().alpha(1).setDuration(1000);

        preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean rememberMeChecked = preferences.getBoolean("rememberMeChecked", false);
        if (rememberMeChecked) {
            // Retrieve stored user ID and proceed to MainMenuActivity
            String storedUserId = preferences.getString("userId", "");
            idClient = storedUserId;
            if (!storedUserId.isEmpty()) {
                startMainMenuActivity(storedUserId);
            }
        }
    }




    private void setTxtPrimire() {
        TextView txtPrimire = findViewById(R.id.txtPrimire);

        //get system time and if its day make the text of txtPrimire be "Buna ziua !" else "Buna seara !"
        //get system time
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        String currentTime = simpleDateFormat.format(calendar.getTime());
        //if its day make the text of txtPrimire be "Buna ziua !" else "Buna seara !"

        if (currentTime.compareTo("06:00:00") >= 0 && currentTime.compareTo("19:00:00") < 0) {
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

        /*db.collection("Clients")
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
                            Toast.makeText(LoginActivityNew.this, "Error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });*/

        if(isEmailFormat(email))
            txtEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.email_check_success_icon, 0);
        else
            txtEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.email_check_failure_icon, 0);
    }

    public void attemptLogin(View v)
    {
        // Retrieve input username or email and password
        String userInput = txtEmail.getText().toString().trim();
        String password = txtPassword.getText().toString().trim();

        // Validate username/email and password fields
        if (TextUtils.isEmpty(userInput) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Te rugăm să completezi datele de necesare autentificării.", Toast.LENGTH_SHORT).show();
            return;
        }

        String hashedPassword = hashPassword(password);

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
                                if (actualPassword != null && actualPassword.equals(hashedPassword)) {
                                    // Get the id of the document
                                    idClient = document.getString("idClient");
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
                            Toast.makeText(LoginActivityNew.this, "Datele introduse sunt invalide.", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("LoginActivity", "Error getting documents: ", task.getException());
                            Toast.makeText(LoginActivityNew.this, "Error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private boolean isEmailFormat(String input) {
        // Simple email format check using regex
        return android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches();
    }


    private void startMainMenuActivity(String userId) {
        // Load animations
        //Animation slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        // Apply animations to the card view
        CardView cardView = findViewById(R.id.card_view);
        //cardView.startAnimation(slideUpAnimation);

        View cardHeight = findViewById(R.id.card_height);

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
                transitionToMainMenuActivity(cardView, cardHeight);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void transitionToMainMenuActivity(CardView cardView, View cardHeight)
    {
        Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_login_to_main_menu);
        cardView.startAnimation(slideDownAnimation);


// Handler to delay the execution of the code
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Finish the current activity without any animation
                setContentView(R.layout.activity_main_menu_new);
                Intent intent = new Intent(LoginActivityNew.this, MainMenuActivityNew.class);
                intent.putExtra("idClient", idClient);
                startActivity(intent);
                // Make this transition be instant
                finish();
                overridePendingTransition(0, 0);
            }
        }, 750); // Delay in milliseconds (750 milliseconds for the animation duration)



        //make it wait for the animation to finish
        slideDownAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                // Finish the current activity without any animation
                /*setContentView(R.layout.activity_main_menu_new);
                Intent intent = new Intent(LoginActivityNew.this, MainMenuActivityNew.class);
                intent.putExtra("idClient", idClient);
                startActivity(intent);
                //make this transition be instant
                finish();
                overridePendingTransition(0,0);*/
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

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

    public void startRegisterActivity(View v)
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
                transitionToRegisterActivity(cardView, cardHeight);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });



    }

    private void transitionToRegisterActivity(CardView cardView, View cardHeight)
    {
        Animation slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        cardView.startAnimation(slideUpAnimation);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
// Finish the current activity without any animation
                setContentView(R.layout.activity_register_new);
                Intent intent = new Intent(LoginActivityNew.this, RegisterActivityNew.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                //make this transition be instant
                finish();
            }
        }, 750);

        //make it wait for the animation to finish
        slideUpAnimation.setAnimationListener(new Animation.AnimationListener() {
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


                /*// Finish the current activity without any animation
                setContentView(R.layout.activity_register_new);
                Intent intent = new Intent(LoginActivityNew.this, RegisterActivityNew.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
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
