package com.mycompany.app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProductDao {
    @Insert
    long insert(Product product);

    @Update
    void update(Product product);

    @Delete
    void delete(Product product);

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    Product findByBarcode(String barcode);

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' ORDER BY name LIMIT 100")
    List<Product> searchByName(String query);

    @Query("SELECT * FROM products ORDER BY name LIMIT 100")
    List<Product> getAll();

    @Query("SELECT * FROM products WHERE stock <= minStock ORDER BY stock ASC")
    List<Product> getLowStock();
}
