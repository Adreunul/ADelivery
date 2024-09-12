package com.example.licentaappclient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConfirmCartActivityNew extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    String idClient, idRestaurant;

    CosProduse cosProduse;
    Produs produs; //for removing
    private List<String> idProduseAfisate;
    private List<Produs> produseList;

    private boolean forRemoving;
    private double sumaMinima;

    private LinearLayout productList, cardContent;
    private ScrollView scrollView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_cart);

        cosProduse = (CosProduse) getIntent().getSerializableExtra("cosProduse");
        idClient = getIntent().getStringExtra("idClient");
        idRestaurant = getIntent().getStringExtra("idRestaurant");
        forRemoving = getIntent().getBooleanExtra("forRemoving", false);

        if(forRemoving)
        {
            produs = (Produs) getIntent().getSerializableExtra("produs");
        }

        produseList = cosProduse.getProduseList();

        productList = findViewById(R.id.productList);
        scrollView = findViewById(R.id.scroll_view);
        cardContent = findViewById(R.id.card_content);

        idProduseAfisate = new ArrayList<>();

        loadCardsForProducts();
        getSumaMinima();

        if(forRemoving)
            loadForRemoving();
        cardContent.animate().alpha(1).setDuration(1000);
    }


    private void loadCardsForProducts() {
        idProduseAfisate.clear();
        if(productList.getChildCount() >= 1)
            productList.removeViews(0, productList.getChildCount());

        for (Produs produs : produseList)
        {
            Log.d("ID5555", produs.getIdProdus());
            if(!produs.getArePreferinte() && !checkProdusDejaAfisat(produs.getIdProdus()) && !forRemoving){
                createCard(produs, cosProduse.getCantitateProdus(produs.getIdProdus()));
                idProduseAfisate.add(produs.getIdProdus());
            }
            else if (produs.getArePreferinte())
            {
                if(forRemoving) {
                    if (Objects.equals(produs.getIdProdus(), this.produs.getIdProdus())) {
                        createCard(produs, 1);
                        Log.d("xaxa", "am ajuns aici" + produs.getIdProdus() + " " + produs.getNumeProdus() + " " + produs.getArePreferinte());
                    }
                }
                else {
                    createCard(produs, 1);
                    Log.d("xaxa2", "am ajuns aici" + produs.getIdProdus() + " " + produs.getNumeProdus() + " " + produs.getArePreferinte());

                }
                Log.d("ID5555", "am venit aici" + produs.getIdProdus() + " " + produs.getNumeProdus() + " " +produs.getArePreferinte());
            }
        }
    }

    private void loadForRemoving()
    {
        Button btnMentiune = findViewById(R.id.btnMentiune);
        Button btnConfirma = findViewById(R.id.btnConfirmare);
        TextView txtInapoi = findViewById(R.id.txtInapoi);
        TextView txtHint = findViewById(R.id.lblHint);

        txtHint.setText("Te rugăm să apeși pe produsul pe care să îl eliminăm din coșul tău.");

        btnConfirma.setOnClickListener(
                v -> startSelectProductsActivity());

        txtInapoi.setVisibility(View.INVISIBLE);
        btnMentiune.setVisibility(View.INVISIBLE);
    }

    private boolean checkProdusDejaAfisat(String idProdus)
    {
        for(String idProdusAfisat : idProduseAfisate)
        {
            if(idProdusAfisat.equals(idProdus))
            {
                return true;
            }
        }
        return false;
    }

    private void createCard(Produs produs, int cantitate) {
        View cardView = getLayoutInflater().inflate(R.layout.product_in_cart_card, null);

        TextView txtNumeProdus = cardView.findViewById(R.id.lblNumeProdus);
        TextView txtPret = cardView.findViewById(R.id.lblPret);
        TextView txtCantitate = cardView.findViewById(R.id.lblCantitate);
        TextView txtGramaj = cardView.findViewById(R.id.lblGramaj);
        TextView txtPreferinte = cardView.findViewById(R.id.lblPreferinte);
        Button btnMinus = cardView.findViewById(R.id.btnMinus);
        Button btnPlus = cardView.findViewById(R.id.btnPlus);
        ImageView imgProdus = cardView.findViewById(R.id.pozaProdus);


        Glide.with(this)
                .load(produs.getLinkPoza())
                .placeholder(R.drawable.placeholder) // Placeholder image while loading
                .error(R.drawable.placeholder) // Error image if unable to load
                .into(imgProdus);

        txtNumeProdus.setText(produs.getNumeProdus());
        txtPret.setText(String.valueOf(produs.getPretProdus()));
        txtCantitate.setText(String.valueOf(cantitate));;
        txtGramaj.setText(String.format("(%sg)", produs.getGramaj()));
        if(produs.getArePreferinte())
            txtPreferinte.setText(String.valueOf(produs.getPreferinteString()));
        else
            txtPreferinte.setVisibility(View.GONE);

        //Log.d("raspunsuri", produs.getPreferinteString());
        // Add margin to the card
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 25); // 10dp bottom margin
        cardView.setLayoutParams(params);

        // Add OnClickListener to btnAction
        if(forRemoving) {
            btnPlus.setVisibility(View.GONE);
            btnMinus.setVisibility(View.GONE);
            cardView.setOnClickListener(v -> {
                selectThisProductForRemoving(produs);
            });
        }
        else {
            btnPlus.setOnClickListener(v -> {
                cosProduse.addProdus(produs);
                scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_UP));
                loadCardsForProducts();
            });

            btnMinus.setOnClickListener(v -> {
                cosProduse.removeProdus(produs);
                scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_UP));
                loadCardsForProducts();
            });



            txtPreferinte.setOnClickListener(v -> {
                cosProduse.removeProdus(produs);

                startSelectPreferencesActivity(produs);
            });
        }

        // Add the card to the LinearLayout
        LinearLayout cardContainer = findViewById(R.id.productList);
        cardContainer.addView(cardView);
    }

    private void selectThisProductForRemoving(Produs produs)
    {
        cosProduse.removeProdus(produs);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_UP));
        loadCardsForProducts();
    }


    private void startSelectPreferencesActivity(Produs produs)
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
                transitionToSelectPreferencesActivity(produs);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void transitionToSelectPreferencesActivity(Produs produs)
    {
        setContentView(R.layout.activity_select_preferences_new);
        Intent intent = new Intent(ConfirmCartActivityNew.this, SelectPreferencesActivityNew.class);
        intent.putExtra("produs", produs);
        intent.putExtra("cosProduse", cosProduse);
        intent.putExtra("idClient", idClient);
        intent.putExtra("idRestaurant", idRestaurant);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    public void startConfirmOrderActivity(View v)
    {
        checkOrder();
    }

    private void checkOrder()
    {
        Log.d("xaxa5", String.valueOf(sumaMinima));
        Log.d("xaxa6", String.valueOf(cosProduse.getTotal()));
        if(cosProduse.getProduseList().size() == 0 || cosProduse.getTotal() < sumaMinima) {
            Toast.makeText(this, "Comanda trebuie sa aiba o valoare minima de " + sumaMinima + " lei.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!cosProduse.getPreferinteleSuntSelectate()) {
            Toast.makeText(this, "Selecteaza preferintele pentru produsele din cos.", Toast.LENGTH_SHORT).show();
            return;
        }
        checkExistaCurieriDisponibili();
    }

    private void checkExistaCurieriDisponibili()
    {
        db.collection("Deliverers").whereEqualTo("areComanda", false).whereEqualTo("disponibil", true).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if(task.getResult().size() > 0)
                    startConfirmOrderActivity();
                 else
                    Toast.makeText(this, "Ne pare rau, nu exista curieri disponibili in acest moment.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private double getSumaMinima()
    {
        CollectionReference rulesCollection = db.collection("Reguli");
        String documentId = "Reguli"; // Assuming "Reguli" is the document ID
        double[] sumaMinima = new double[1];

        DocumentReference rulesDocumentRef = rulesCollection.document(documentId);

        rulesDocumentRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                sumaMinima[0] = documentSnapshot.getDouble("SumaMinima");
                if (sumaMinima[0] != 0.0)
                    setSumaMinima(sumaMinima[0]);
                else
                    setSumaMinima(20.0);
            }
        });
        return 0.0;
    }

    private void setSumaMinima(double sumaMinima)
    {
        this.sumaMinima = sumaMinima;
    }


    private void startConfirmOrderActivity()
    {
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
                transitionToConfirmOrderActivity(cardView, cardHeight);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void transitionToConfirmOrderActivity(CardView cardView, View cardHeight)
    {
        Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_confirm_cart_to_confirm_order);
        cardView.startAnimation(slideDownAnimation);


// Handler to delay the execution of the code
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setContentView(R.layout.activity_confirm_order_new);
                Intent intent = new Intent(ConfirmCartActivityNew.this, ConfirmOrderActivityNew.class);
                intent.putExtra("cosProduse", cosProduse);
                intent.putExtra("idClient", idClient);
                intent.putExtra("idRestaurant", idRestaurant);
                startActivity(intent);
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


    public void startSelectProductsActivity(View v)
    {
        startSelectProductsActivity();
    }

    private void startSelectProductsActivity()
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
                transitionToSelectProductsActivity();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }


    private void transitionToSelectProductsActivity()
    {
            setContentView(R.layout.activity_select_products_new);
            Intent intent = new Intent(ConfirmCartActivityNew.this, SelectProductsActivityNew.class);
            intent.putExtra("cosProduse", cosProduse);
            intent.putExtra("idClient", idClient);
            intent.putExtra("idRestaurant", idRestaurant);
            startActivity(intent);
            overridePendingTransition(0, 0);
            //make this transition be instant
            finish();
    }

    public void addMention(View v) {
        // Create an EditText view to be inserted into the dialog
        final EditText input = new EditText(this);

        // Set the initial text from cosProduse.getMentiuni()
        input.setText(cosProduse.getMentiuni());

        // Create an AlertDialog.Builder and set its title
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Adauga mentiune");

        // Set the view of the dialog to the EditText
        builder.setView(input);

        // Set the positive button with its click listener
        builder.setPositiveButton("OK", (dialog, which) -> {
            // Get the text from the EditText and set it to cosProduse
            String mention = input.getText().toString();
            cosProduse.setMentiune(mention);
        });

        // Set the negative button with its click listener
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // Do nothing on Cancel
            dialog.cancel();
        });

        // Show the dialog
        builder.show();
    }
}
