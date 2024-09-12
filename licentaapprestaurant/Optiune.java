package org.example;

import java.util.ArrayList;
import java.util.List;

public class Optiune
{
    boolean raspunsMultiplu;
    String intrebare;
    ArrayList<String> raspunsuri;

    public Optiune(String intrebare)
    {
        this.intrebare = intrebare;
        raspunsuri = new ArrayList<String>();
        raspunsMultiplu = false;
    }

    public void adaugaRaspuns(String raspuns)
    {
        raspunsuri.add(raspuns);
    }

    public boolean getRaspunsMultiplu()
    {
        return raspunsMultiplu;
    }

    public void setRaspunsMultiplu(boolean raspunsMultiplu)
    {
        this.raspunsMultiplu = raspunsMultiplu;
    }

    public String getIntrebare()
    {
        return intrebare;
    }

    public ArrayList<String> getRaspunsuri()
    {
        return raspunsuri;
    }
    public void setRaspunsuri(List<String> raspunsuri)
    {
        this.raspunsuri = new ArrayList<>(raspunsuri);
    }
}
