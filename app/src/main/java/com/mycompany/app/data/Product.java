package com.mycompany.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class Product {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String barcode;
    public double pricePurchase;
    public double priceRetail;
    public double priceWholesale;
    public int stock;
    public long expirationDate; // unix millis, 0 = no expiry
    public int minStock;
    public int categoryId;

    public Product(String name, String barcode, double pricePurchase, double priceRetail, double priceWholesale, int stock, long expirationDate, int minStock, int categoryId) {
        this.name = name;
        this.barcode = barcode;
        this.pricePurchase = pricePurchase;
        this.priceRetail = priceRetail;
        this.priceWholesale = priceWholesale;
        this.stock = stock;
        this.expirationDate = expirationDate;
        this.minStock = minStock;
        this.categoryId = categoryId;
    }

    // Empty constructor for Room
    public Product() {
    }
}
