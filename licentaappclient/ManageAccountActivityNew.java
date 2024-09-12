package com.example.licentaappclient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;

public class ManageAccountActivityNew extends AppCompatActivity {

    FirebaseFirestore db;
    TextView txtNumeClient, txtEmailClient, txtAdresaClient;
    String idClient;
    private LinearLayout cardContent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_account_new);

        idClient = getIntent().getStringExtra("idClient");
        cardContent = findViewById(R.id.card_content);
        txtNumeClient = findViewById(R.id.txtNumeClient);
        txtEmailClient = findViewById(R.id.txtEmailClient);
        txtAdresaClient = findViewById(R.id.txtAdresaClient);

        db = FirebaseFirestore.getInstance();

        cardContent.animate().alpha(1).setDuration(1000);

        setNumeAndEmailClient();
    }


    private void setNumeAndEmailClient() {
        db.collection("Clients").whereEqualTo("idClient", idClient).get().addOnSuccessListener(documentSnapshot -> {
            txtNumeClient.setText(documentSnapshot.getDocuments().get(0).getString("numeClient"));
            txtEmailClient.setText(documentSnapshot.getDocuments().get(0).getString("email"));
            txtAdresaClient.setText(documentSnapshot.getDocuments().get(0).getString("adresaClient"));
        });
    }

    public void startChangePasswordActivity(View v)
    {
        LinearLayout cardContent = findViewById(R.id.card_content);

        Animation cardContentFadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        cardContent.startAnimation(cardContentFadeOut);


        //make it wait for the animation to finish
        cardContentFadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                cardContent.setAlpha(0); //marimea cardurilor e identica deci nu mai are rost si slide
                // Finish the current activity without any animation
                setContentView(R.layout.activity_change_password_new);
                Intent intent = new Intent(ManageAccountActivityNew.this, ChangePasswordActivityNew.class);
                intent.putExtra("idClient", idClient);
                startActivity(intent);
                overridePendingTransition(0, 0);
                //make this transition be instant
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

    }



    public void startMainMenuActivity(View v)
    {
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
                cardContent.setAlpha(0);
                transitionToMainMenuActivity(cardView);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void transitionToMainMenuActivity(CardView cardView)
    {
        Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_manage_account_to_main_menu);
        cardView.startAnimation(slideDownAnimation);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Finish the current activity without any animation
                setContentView(R.layout.activity_main_menu_new);
                Intent intent = new Intent(ManageAccountActivityNew.this, MainMenuActivityNew.class);
                intent.putExtra("idClient", idClient);
                startActivity(intent);
                overridePendingTransition(0, 0);
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

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
}