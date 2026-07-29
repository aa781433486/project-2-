package com.mycompany.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "invoice_items")
public class InvoiceItem {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long invoiceId;
    public int productId;
    public int quantity;
    public double unitPrice;
    public double lineTotal;
    public double profit;

    public InvoiceItem() {}

    public InvoiceItem(long invoiceId, int productId, int quantity, double unitPrice, double lineTotal, double profit) {
        this.invoiceId = invoiceId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
        this.profit = profit;
    }
}
