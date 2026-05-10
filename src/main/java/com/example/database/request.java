package com.example.database;


public class request{
    private String id;
    private String center;
    private String item;
    private int quantity;
    private String date;



    public request(String id, String center, String item, int quantity, String date) {
        this.id = id;
        this.center = center;
        this.item = item;
        this.quantity = quantity;
        this.date = date;
    }


    public String getId() {
        return id;
    }

    public String getCenter() {
        return center;
    }

    public String getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getDate() {
        return date;
    }
}