package com.example.licentaappclient;
import java.io.Serializable;
import java.util.List;

import java.util.ArrayList;

public class Produs implements Serializable
{
    private String idProdus, numeProdus, categorie, linkPoza;
    private int nrComenzi, gramaj;
    private double pretProdus;
    private boolean arePreferinte, aAlesPreferinte;
    private List<Preferinta> preferinteList;
    private List<String> ingrediente;

    public Produs(String idProdus, String numeProdus, String categorie, String linkPoza, int gramaj, int nrComenzi, double pretProdus, boolean arePreferinte, List<String> ingrediente, List<Preferinta> preferinteList)
    {
        this.idProdus = idProdus;
        this.numeProdus = numeProdus;
        this.categorie = categorie;
        this.linkPoza = linkPoza;
        this.gramaj = gramaj;
        this.nrComenzi = nrComenzi;
        this.pretProdus = pretProdus;
        this.arePreferinte = arePreferinte;
        this.ingrediente = ingrediente;
        this.preferinteList = preferinteList;
        aAlesPreferinte = false;
    }

    public Produs(String idProdus, String numeProdus, String categorie, String linkPoza, int gramaj, int nrComenzi, double pretProdus, boolean arePreferinte, List<String> ingrediente)
    {
        this.idProdus = idProdus;
        this.numeProdus = numeProdus;
        this.categorie = categorie;
        this.linkPoza = linkPoza;
        this.gramaj = gramaj;
        this.nrComenzi = nrComenzi;
        this.pretProdus = pretProdus;
        this.arePreferinte = arePreferinte;
        this.ingrediente = ingrediente;
        if(arePreferinte)
        {
            preferinteList = new ArrayList<>();
        }
        aAlesPreferinte = false;
    }


    public List<Preferinta> getPreferinteList()
    {
        return preferinteList;
    }

    public String getPreferinteString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        for(Preferinta preferinta : preferinteList)
        {
            if(preferinta.getRaspunsuriSelectateString() == null)
                return "Selecteaza preferintele";
            stringBuilder.append(preferinta.getRaspunsuriSelectateString()).append(", ");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 2);
        return stringBuilder.toString();
    }

    public void addPreferinta(Preferinta preferinta)
    {
        preferinteList.add(preferinta);
    }

    public int getNrComenzi()
    {
        return nrComenzi;
    }
    public String getLinkPoza()
    {
        return linkPoza;
    }

    public String getIdProdus()
    {
        return idProdus;
    }
    public double getPretProdus() {return pretProdus;}
    public String getNumeProdus()
    {
        return numeProdus;
    }

    public String getCategorie()
    {
        return categorie;
    }

    public int getGramaj()
    {
        return gramaj;
    }



    public boolean getArePreferinte()
    {
        return arePreferinte;
    }

    public String getIngrediente()
    {
        if(ingrediente.isEmpty())
            return "";

        StringBuilder stringBuilder = new StringBuilder();
        for(String ingredient : ingrediente)
        {
            stringBuilder.append(ingredient).append(", ");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 2);
        return stringBuilder.toString();
    }

    public void setaAlesPreferinte(boolean aAlesPreferinte)
    {
        this.aAlesPreferinte = aAlesPreferinte;
    }

    public boolean getaAlesPreferinte()
    {
        return aAlesPreferinte;
    }

    public void removePreferinta(Preferinta preferinta)
    {
        preferinteList.remove(preferinta);
    }

    public String getRaspunsuriSelectateString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        for(Preferinta preferinta : preferinteList)
        {
            stringBuilder.append(preferinta.getRaspunsuriSelectateString()).append(", ");
        }
        return stringBuilder.toString();
    }

    public boolean getARaspunsLaToatePreferintele()
    {
        for(Preferinta preferinta : preferinteList)
        {
            if(preferinta.getRaspunsuriSelectate().isEmpty())
                return false;
        }
        return true;
    }

}
