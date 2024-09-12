package com.example.licentaappclient;

import java.util.ArrayList;
import java.util.Comparator;

public class MeniuRestaurant
{
    private ArrayList<Produs> produseList;

    public MeniuRestaurant()
    {
        produseList = new ArrayList<>();
    }

    public ArrayList<Produs> getProduseList() {
        return produseList;
    }

    public void setProduseList(ArrayList<Produs> produseList) {
        this.produseList = produseList;
    }

    public void sortProduseAlphabetically()
    {
        produseList.sort(Comparator.comparing(Produs::getNumeProdus));
    }

    public void sortProduseByNrComenzi()
    {
        produseList.sort(Comparator.comparing(Produs::getNrComenzi).reversed());
    }
}
