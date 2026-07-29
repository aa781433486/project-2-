package com.mycompany.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface InvoiceDao {
    @Insert
    long insert(Invoice invoice);

    @Update
    void update(Invoice invoice);

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC LIMIT 100")
    List<Invoice> getRecent();

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    Invoice findById(long id);
}
