package com.example.licentaappclient;
import java.io.Serializable;

import android.util.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CosProduse implements Serializable
{
    private ArrayList<Produs> produseList;
    private static ArrayList<OptiuniPerProdus> optiuniPerProdusList;
    private String mentiune;

    public CosProduse()
    {
        produseList = new ArrayList<>();
        mentiune = "";

        optiuniPerProdusList = new ArrayList<>();
    }

    public ArrayList<Produs> getProduseOfThisId(String idProdus)
    {
        ArrayList<Produs> produseOfThisId = new ArrayList<>();
        for (Produs existingProdus : produseList) {
            if (existingProdus.getIdProdus().equals(idProdus)) {
                produseOfThisId.add(existingProdus);
            }
        }
        return produseOfThisId;
    }



    public void addProdus(Produs produs)
    {
        produseList.add(produs);
    }

    public void removeProdus(Produs produs)
    {
        produseList.remove(produs);
    }

    public void removeProdus(String idProdus)
    {
        Iterator<Produs> iterator = produseList.iterator();
        while(iterator.hasNext())
        {
            Produs existingProdus = iterator.next();
            if(existingProdus.getIdProdus().equals(idProdus))
            {
                iterator.remove();
                return;
            }
        }
    }

    public ArrayList<Produs> getProduseList()
    {
        return produseList;
    }

    public void addProdus(String idProdus, int cantitate, double pretProdus)
    {
        Iterator<Produs> iterator = produseList.iterator();
        while(iterator.hasNext())
        {
            Produs existingProdus = iterator.next();
            if(existingProdus.getIdProdus().equals(idProdus))
            {
                //existingProdus.setQuantity(cantitate);
                return;
            }
        }

        //produstList.add(new Produs(idProdus, cantitate, pretProdus));
    }

    public int getCantitateProdus(String idProdus)
    {
        int cantitate = 0;
        for (Produs existingProdus : produseList) {
            if (existingProdus.getIdProdus().equals(idProdus)) {
                cantitate++;
            }
        }

        return cantitate;
    }

    public boolean getAreProdusCuOptiuniNealese(String idProdus) {
        for (Produs produs : produseList) {
            if (produs.getIdProdus().equals(idProdus) && !produs.getaAlesPreferinte()) {
                return true; // If a Produs with matching idProdus and aAlesPreferinte=false is found, return true
            }
        }
        return false; // If no such Produs is found, return false
    }

    public void stergeProdusCuOptiuniNealese(String idProdus)
    {
        for (Produs produs : produseList) {
            if (produs.getIdProdus().equals(idProdus) && !produs.getaAlesPreferinte()) {
                produseList.remove(produs);
                return;
            }
        }
    }



    public void addOptiuni(String idProdus, String optiuniText)
    {
        optiuniPerProdusList.add(new OptiuniPerProdus(idProdus, optiuniText));
    }

    public void clearOptiuniList(String idProdus)
    {
        optiuniPerProdusList.removeIf(existingOptiuni -> existingOptiuni.getIdProdus().equals(idProdus));
    }

    /*public double getTotal()
    {
        double totalPrice = 0.0;
        for (Produs existingProdus : produseList) {
            if (existingProdus.getCantitate() > 0) {
                totalPrice += existingProdus.getPretProdus() * existingProdus.getCantitate();
            }
        }
        return totalPrice;
    }*/

    public double getTotal()
    {
        double totalPrice = 0.0;
        for (Produs existingProdus : produseList) {
            totalPrice += existingProdus.getPretProdus();
        }
        return totalPrice;
    }


    public String getMentiuni()
    {
        return mentiune;
    }

    public void setMentiune(String mentiune)
    {
        this.mentiune = mentiune;
    }


    public Map<String, Integer> getProductList() {
        Map<String, Integer> productList = new HashMap<>();
        ArrayList<String> idProduseAdded = new ArrayList<>();

        for (Produs produs : produseList) {
            if (idProduseAdded.contains(produs.getIdProdus()))
                continue;

            productList.put(produs.getIdProdus(), getCantitateProdus(produs.getIdProdus()));
            idProduseAdded.add(produs.getIdProdus());
        }

        return productList;
    }


    public String getOptiuniText() {
        /*StringBuilder optiuniText = new StringBuilder();
        optiuniText.append(" ");

        for (OptiuniPerProdus optiuniPerProdus : optiuniPerProdusList) {
            optiuniText.append(optiuniPerProdus.getTextOptiuni()).append("|");
        }

        return optiuniText.toString();*/
        String optiuniText = "";
        for (Produs produs : produseList) {
            if(produs.getArePreferinte())
                optiuniText += produs.getNumeProdus() + " " + produs.getPreferinteString() + "_";
        }

        return optiuniText;
    }

    public int getNrOfProducts()
    {
        return produseList.size();
    }


    public static String getOptiuniForCart()
    {
        StringBuilder optiuniText = new StringBuilder();
        optiuniText.append(" ");

        for (OptiuniPerProdus optiuniPerProdus : optiuniPerProdusList) {
            optiuniText.append(optiuniPerProdus.getTextOptiuni()).append(", ").append("\n");
        }

        return optiuniText.toString();
    }

    public boolean getPreferinteleSuntSelectate()
    {
        for (Produs produs : produseList) {
            if (!produs.getaAlesPreferinte() && produs.getArePreferinte()) {
                return false;
            }
        }
        return true;
    }


    public void expose()
    {

        for (Produs existingProdus : produseList) {
            //Log.d("expose", existingProdus.getIdProdus() + " " + existingProdus.getCantitate());
        }
    }

}
