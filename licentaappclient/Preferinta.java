package com.example.licentaappclient;

import android.util.Log;

import java.io.Serializable;
import java.util.List;

public class Preferinta implements Serializable
{
    String intrebare;
    boolean raspunsMultiplu;
    List<String> raspunsuri, raspunsuriSelectate;

    public Preferinta(String intrebare, boolean raspunsMultiplu
            , List<String> raspunsuri, List<String> raspunsuriSelectate)
    {
        this.intrebare = intrebare;
        this.raspunsMultiplu = raspunsMultiplu;
        this.raspunsuri = raspunsuri;
        this.raspunsuriSelectate = raspunsuriSelectate;
    }

    public String getIntrebare()
    {
        return intrebare;
    }

    public boolean getRaspunsMultiplu()
    {
        return raspunsMultiplu;
    }


    public List<String> getRaspunsuri()
    {
        return raspunsuri;
    }

    public List<String> getRaspunsuriSelectate()
    {
        return raspunsuriSelectate;
    }

    public String getRaspunsuriSelectateString()
    {
        if(raspunsuriSelectate.isEmpty())
            return null;

        StringBuilder stringBuilder = new StringBuilder();
        for(String raspuns : raspunsuriSelectate)
        {
            stringBuilder.append(raspuns).append(", ");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 2);
        return stringBuilder.toString();
    }

    public void setIntrebare(String intrebare)
    {
        this.intrebare = intrebare;
    }

    public void setRaspunsMultiplu(boolean raspunsMultiplu)
    {
        this.raspunsMultiplu = raspunsMultiplu;
    }


    public void setRaspunsuri(List<String> raspunsuri)
    {
        this.raspunsuri = raspunsuri;
    }

    public void setRaspunsuriSelectate(List<String> raspunsuriSelectate)
    {
        this.raspunsuriSelectate = raspunsuriSelectate;
    }


    public void addRaspunsSelectat(String raspuns)
    {
        raspunsuriSelectate.add(raspuns);
    }

    public void removeRaspunsSelectat(String raspuns)
    {
        raspunsuriSelectate.remove(raspuns);
    }

    public void clearRaspunsuriSelectate()
    {
        raspunsuriSelectate.clear();
    }

    public boolean getAreRaspunsuriSelectate()
    {
        return !raspunsuriSelectate.isEmpty();
    }

}
