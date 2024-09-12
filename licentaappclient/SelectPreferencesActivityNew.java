package com.example.licentaappclient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class SelectPreferencesActivityNew extends AppCompatActivity
{
    private CosProduse cosProduse;
    private Produs produs;
    private String idClient, idRestaurant;

    private boolean isGenerate;

    private LinearLayout preferencesLayout, cardContent;
    private TextView txtPrimire;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_preferences_new);

        cosProduse = (CosProduse) getIntent().getSerializableExtra("cosProduse");
        produs = (Produs) getIntent().getSerializableExtra("produs");
        idClient = getIntent().getStringExtra("idClient");
        idRestaurant = getIntent().getStringExtra("idRestaurant");

        cardContent = findViewById(R.id.card_content);
        preferencesLayout = findViewById(R.id.preferenceList);
        txtPrimire = findViewById(R.id.txtPrimire);
        txtPrimire.setText(produs.getNumeProdus());

        isGenerate = true;
        generatePreferences();

        cardContent.animate().alpha(1).setDuration(1000);
    }

    private void generatePreferences() {
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Preferinta preferinta : produs.getPreferinteList()) {
            View preferenceCard = inflater.inflate(R.layout.preference_card, preferencesLayout, false);

            // Set the question text
            TextView questionTextView = preferenceCard.findViewById(R.id.lblIntrebare);
            questionTextView.setText(preferinta.getIntrebare());

            // Determine whether to use checkboxes or radio buttons
            LinearLayout optionsLayout = preferenceCard.findViewById(R.id.preferencesLayout);
            if (preferinta.getRaspunsMultiplu()) {

                for (String raspuns : preferinta.getRaspunsuri()) {
                    CheckBox checkBox = new CheckBox(this);
                    checkBox.setText(raspuns);
                    optionsLayout.addView(checkBox);

                    checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if(!isGenerate) {
                                if (isChecked) {
                                    Log.d("plm668", "hopa1");
                                    Preferinta newPreferinta = preferinta;
                                    newPreferinta.addRaspunsSelectat(raspuns);
                                    produs.removePreferinta(preferinta);
                                    produs.addPreferinta(newPreferinta);
                                }
                                if (!isChecked) {
                                    Log.d("plm668", "hopa2");
                                    Preferinta newPreferinta = preferinta;
                                    newPreferinta.removeRaspunsSelectat(raspuns);
                                    produs.removePreferinta(preferinta);
                                    produs.addPreferinta(newPreferinta);
                                }
                            }
                            Log.d("plm667" , produs.getRaspunsuriSelectateString());

                        }
                    });
                    if(preferinta.getAreRaspunsuriSelectate())
                    {
                        if(preferinta.getRaspunsuriSelectate().contains(raspuns))
                        {
                            checkBox.setChecked(true);
                        }
                        else {
                            checkBox.setChecked(false);
                        }
                    }
                }
            } else {
                // Single choice - generate radio buttons
                RadioGroup radioGroup = new RadioGroup(this);
                radioGroup.setLayoutParams(new RadioGroup.LayoutParams(
                        RadioGroup.LayoutParams.MATCH_PARENT,
                        RadioGroup.LayoutParams.WRAP_CONTENT));
                optionsLayout.addView(radioGroup);

                for (String raspuns : preferinta.getRaspunsuri()) {
                    RadioButton radioButton = new RadioButton(this);
                    radioButton.setText(raspuns);
                    radioGroup.addView(radioButton);

                    // Set an OnCheckedChangeListener for each radio button
                    radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if(!isGenerate) {
                                if (isChecked) {
                                    Log.d("plm668", "hopa");
                                    Preferinta newPreferinta = preferinta;
                                    newPreferinta.clearRaspunsuriSelectate();
                                    newPreferinta.addRaspunsSelectat(raspuns);
                                    produs.removePreferinta(preferinta);
                                    produs.addPreferinta(newPreferinta);
                                }
                                if (!isChecked) {

                                }
                            }
                            Log.d("plm667" , produs.getRaspunsuriSelectateString());
                        }
                    });


                    if(preferinta.getAreRaspunsuriSelectate())
                    {
                        if(preferinta.getRaspunsuriSelectate().contains(raspuns))
                        {
                            radioButton.setChecked(true);
                        }
                        else {
                            radioButton.setChecked(false);
                        }
                    }
                }
            }

            // Add the card to the layout
            preferencesLayout.addView(preferenceCard);
        }
        isGenerate = false;
    }



    public void confirmSelection(View v)
    {
        if(produs.getARaspunsLaToatePreferintele())
            produs.setaAlesPreferinte(true);

        cosProduse.addProdus(produs);

        startConfirmCartActivity();

    }

    private void startConfirmCartActivity()
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
                transitionToConfirmCartActivity();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void transitionToConfirmCartActivity()
    {
        setContentView(R.layout.activity_confirm_cart);
        Intent intent = new Intent(SelectPreferencesActivityNew.this, ConfirmCartActivityNew.class);
        intent.putExtra("cosProduse", cosProduse);
        intent.putExtra("idClient", idClient);
        intent.putExtra("idRestaurant", idRestaurant);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

}