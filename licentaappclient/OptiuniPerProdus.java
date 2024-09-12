package com.example.licentaappclient;

import java.io.Serializable;

public class OptiuniPerProdus implements Serializable
{
    String idProdus;
    String textOptiuni;

    public OptiuniPerProdus(String idProdus, String textOptiuni)
    {
        this.idProdus = idProdus;
        this.textOptiuni = textOptiuni;
    }

    public String getIdProdus() {
        return idProdus;
    }

    public String getTextOptiuni() {
        return textOptiuni;
    }
}
