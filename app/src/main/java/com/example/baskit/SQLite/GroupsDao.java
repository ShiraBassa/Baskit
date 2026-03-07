package com.example.baskit.SQLite;

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

    @Query("SELECT * FROM groups")
    List<GroupsEntity> getAll();

    @Query("DELETE FROM groups")
    void clearAll();
}
