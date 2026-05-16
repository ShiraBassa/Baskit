package com.example.baskit.sqlite;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "item_groups")
public class GroupsEntity
{
    @PrimaryKey
    @NonNull
    public String baseName;

    public String structureJson;

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

    public void setBaseName(@NonNull String baseName)
    {
        this.baseName = baseName;
    }

    public String getStructureJson()
    {
        return structureJson;
    }

    public void setStructureJson(String structureJson)
    {
        this.structureJson = structureJson;
    }
}
