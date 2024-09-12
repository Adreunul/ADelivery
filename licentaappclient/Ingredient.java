package com.example.licentaappclient;

public class Ingredient
{
    String nume;
    int gramaj;

    public Ingredient(String numeIngredient, int gramajIngredient)
    {
        this.nume = numeIngredient;
        this.gramaj = gramajIngredient;
    }

    public int getGramaj()
    {
        return gramaj;
    }

    public String getNume()
    {
        return nume;
    }
}
