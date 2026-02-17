package com.example.baskit.SQLite;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import android.content.Context;

@Database(entities = {ItemEntity.class, ItemCodeName.class, ItemCategory.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase
{
    public abstract ItemDao itemDao();
    public abstract ItemCodeNamesDao itemCodesDao();
    public abstract ItemCategoryDao itemCategoryDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(Context context)
    {
        if (INSTANCE == null)
        {
            synchronized (AppDatabase.class)
            {
                if (INSTANCE == null)
                {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "baskit_db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}
