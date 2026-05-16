package com.example.baskit.sqlite;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GroupsDao
{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<GroupsEntity> groups);

    @Query("SELECT * FROM item_groups")
    List<GroupsEntity> getAll();

    @Query("DELETE FROM item_groups")
    void clearAll();
}
