package com.example.baskit.sqlite;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import android.content.Context;

@SuppressWarnings("deprecation")
@Database(entities = {ItemPricesEntity.class, ItemCategory.class, GroupsEntity.class, ItemInfoEntity.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase
{
    public abstract ItemPricesDao itemPricesDao();
    public abstract ItemCategoryDao itemCategoryDao();
    public abstract GroupsDao groupDao();
    public abstract ItemInfoDao itemInfoDao();

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
