package com.example.baskit.sqlite;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@SuppressWarnings("ALL")
@Dao
public interface ItemInfoDao
{
    @Query("SELECT * FROM item_infos")
    List<ItemInfoEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ItemInfoEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ItemInfoEntity> entities);

    @Query("DELETE FROM item_infos")
    void clearAll();
}