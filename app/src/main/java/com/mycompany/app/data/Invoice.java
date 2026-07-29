package com.mycompany.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "invoices")
public class Invoice {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long createdAt; // unix millis
    public double total;
    public double paid;
    public String customerName;
    public int userId;

    public Invoice() {}

    public Invoice(long createdAt, double total, double paid, String customerName, int userId) {
        this.createdAt = createdAt;
        this.total = total;
        this.paid = paid;
        this.customerName = customerName;
        this.userId = userId;
    }
}
