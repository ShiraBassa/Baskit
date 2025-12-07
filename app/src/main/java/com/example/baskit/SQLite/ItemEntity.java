package com.example.baskit.SQLite;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "items")
public class ItemEntity
{
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String itemCode;
    public String store;
    public String branch;
    public double price;
}