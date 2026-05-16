package com.example.baskit.sqlite;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "item_infos")
public class ItemInfoEntity
{
    @SuppressWarnings("NotNullFieldNotInitialized")
    @PrimaryKey
    @NonNull
    public String itemCode;
    public String company;
    public double weight;
    public String unit;
    public String baseName;
}