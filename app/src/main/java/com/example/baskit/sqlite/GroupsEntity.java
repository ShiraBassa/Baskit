package com.example.baskit.sqlite;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "item_groups")
public class GroupsEntity
{
    @PrimaryKey
    @NonNull
    public final String baseName;

    public final String structureJson;

    public GroupsEntity(@NonNull String baseName, String structureJson)
    {
        this.baseName = baseName;
        this.structureJson = structureJson;
    }

    @NonNull
    public String getBaseName()
    {
        return baseName;
    }

    public String getStructureJson()
    {
        return structureJson;
    }
}
