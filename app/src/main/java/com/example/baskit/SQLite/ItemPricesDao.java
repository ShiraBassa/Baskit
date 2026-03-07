package com.example.baskit.SQLite;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ItemPricesDao
{

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ItemPricesEntity> items);

    @Query("SELECT * FROM item_prices")
    List<ItemPricesEntity> getAll();

    @Query("DELETE FROM item_prices")
    void clearAll();
}
