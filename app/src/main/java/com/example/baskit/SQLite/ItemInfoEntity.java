package com.example.baskit.SQLite;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "item_infos")
public class ItemInfoEntity
{
    @PrimaryKey
    @NonNull
    public String itemCode;

    public String company;
    public double weight;
    public String unit;
    public String baseName;
}