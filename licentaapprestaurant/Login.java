package org.example;

public class Login
{
    public static void main(String[] args) throws Exception {
        login("123");
    }

    private static void login(String idRestaurant) throws Exception {
        new MainMenu(idRestaurant);
    }
}