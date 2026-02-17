package com.example.baskit.SQLite;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "item_categories")
public class ItemCategory
{
    @PrimaryKey
    @NonNull
    public String itemCode;

    public String category;

    public ItemCategory(@NonNull String itemCode, String category)
    {
        this.itemCode = itemCode;
        this.category = category;
    }

    @NonNull
    public String getItemCode()
    {
        return itemCode;
    }

    public void setItemCode(@NonNull String itemCode)
    {
        this.itemCode = itemCode;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }
}
