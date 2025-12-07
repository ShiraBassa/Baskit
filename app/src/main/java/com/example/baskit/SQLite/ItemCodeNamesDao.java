package com.example.baskit.SQLite;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ItemCodeNamesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ItemCodeName> codes);

    @Query("SELECT * FROM item_code_names")
    List<ItemCodeName> getAll();

    @Query("SELECT itemName FROM item_code_names WHERE itemCode = :code LIMIT 1")
    String getNameByCode(String code);

    @Query("DELETE FROM item_code_names")
    void clearAll();
}