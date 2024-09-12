package com.example.licentaappclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CheckOrderActivity extends AppCompatActivity {

    private LinearLayout cardContent;
    TextView txtRestaurantName, txtPret, txtStareComanda, txtPin;

    String numeRestaurant, pinConfirmare, stareComanda, idClient;
    double pret;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_order);

        numeRestaurant = getIntent().getStringExtra("numeRestaurant");
        pinConfirmare = getIntent().getStringExtra("pinConfirmare");
        stareComanda = getIntent().getStringExtra("stareComanda");
        idClient = getIntent().getStringExtra("idClient");
        pret = getIntent().getDoubleExtra("pret", 0);

        cardContent = findViewById(R.id.card_content);
        txtRestaurantName = findViewById(R.id.lblRestaurantName);
        txtPret = findViewById(R.id.lblPret);
        txtStareComanda = findViewById(R.id.lblStare);
        txtPin = findViewById(R.id.lblPinConfirmare);

        loadDetails();
        cardContent.animate().alpha(1).setDuration(1000);
    }

    private void loadDetails()
    {
        String pretText = "";
        if (pret % 1 == 0)
            pretText = String.format("%.0f", pret);
         else
            pretText = String.valueOf(pret);

         pretText += " ron";
        txtRestaurantName.setText(numeRestaurant);
        txtPin.setText(String.format("PIN: %s", pinConfirmare));
        txtStareComanda.setText(stareComanda);
        txtPret.setText(pretText);
    }

    public void startMainMenuActivity(View v)
    {
        startMainMenuActivity();
    }

    private void startMainMenuActivity()
    {
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
                transitionToMainMenuActivity();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void transitionToMainMenuActivity()
    {
        setContentView(R.layout.activity_main_menu_new);
        Intent intent = new Intent(CheckOrderActivity.this, MainMenuActivityNew.class);
        intent.putExtra("idClient", idClient);

        startActivity(intent);
        overridePendingTransition(0, 0);
        //make this transition be instant
        finish();
    }
}