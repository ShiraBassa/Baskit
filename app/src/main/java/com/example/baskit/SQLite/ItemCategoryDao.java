package com.example.baskit.SQLite;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ItemCategoryDao
{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ItemCategory category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ItemCategory> categories);

    @Query("SELECT * FROM item_categories")
    List<ItemCategory> getAll();

    @Query("DELETE FROM item_categories")
    void clearAll();
}
