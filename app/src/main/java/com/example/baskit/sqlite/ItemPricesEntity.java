package com.example.baskit.sqlite;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "item_prices")
public class ItemPricesEntity
{
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String itemCode;
    public String store;
    public String branch;
    public double price;
}