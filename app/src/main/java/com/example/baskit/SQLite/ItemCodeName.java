package com.example.baskit.SQLite;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "item_code_names")
public class ItemCodeName
{
    @PrimaryKey
    @NonNull
    public String itemCode;
    public String itemName;

    public ItemCodeName(String itemCode, String itemName)
    {
        this.itemCode = itemCode;
        this.itemName = itemName;
    }

    public String getItemCode()
    {
        return itemCode;
    }

    public void setItemCode(String itemCode)
    {
        this.itemCode = itemCode;
    }

    public String getItemName()
    {
        return itemName;
    }

    public void setItemName(String itemName)
    {
        this.itemName = itemName;
    }
}
