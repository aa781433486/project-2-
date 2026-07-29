package com.mycompany.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface InvoiceItemDao {
    @Insert
    long insert(InvoiceItem item);

    @Update
    void update(InvoiceItem item);

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    List<InvoiceItem> findByInvoice(long invoiceId);

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId AND productId = :productId LIMIT 1")
    InvoiceItem findByInvoiceAndProduct(long invoiceId, int productId);
}
