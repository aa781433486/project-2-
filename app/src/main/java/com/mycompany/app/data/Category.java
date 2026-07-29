package com.mycompany.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;

    public Category(String name) {
        this.name = name;
    }

    public Category() {}
}
