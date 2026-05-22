package com.example.baskit.sqlite;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import android.content.Context;

@SuppressWarnings("deprecation")
@Database(
        entities = {
                ItemPricesEntity.class,
                GroupsEntity.class,
                ItemInfoEntity.class
        },
        version = 5,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase
{
    public abstract ItemPricesDao itemPricesDao();
    public abstract GroupsDao groupDao();
    public abstract ItemInfoDao itemInfoDao();

    private static volatile AppDatabase INSTANCE;

    private static final Migration MIGRATION_4_5 =
            new Migration(4, 5)
            {
                @Override
                public void migrate(SupportSQLiteDatabase database)
                {
                    database.execSQL(
                            "ALTER TABLE item_infos ADD COLUMN category TEXT"
                    );
                }
            };

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
                            .addMigrations(MIGRATION_4_5)
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}
