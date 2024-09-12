package com.example.licentaappcurier;

public class ActiveOrder
{
    private int idClient;

    private int idRestaurant;

    private int price;

    private String address;

    public ActiveOrder()
    {

    }

    public ActiveOrder(int idClient, int idRestaurant, int price, String address) {
        this.idClient = idClient;
        this.idRestaurant = idRestaurant;
        this.price = price;
        this.address = address;
    }

    public int getIdClient() {
        return idClient;
    }

    public void setIdClient(int idClient) {
        this.idClient = idClient;
    }

    public int getIdRestaurant() {
        return idRestaurant;
    }

    public void setIdRestaurant(int idRestaurant) {
        this.idRestaurant = idRestaurant;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
