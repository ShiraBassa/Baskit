package com.example.baskit.main_components;

import androidx.annotation.NonNull;

import com.example.baskit.Baskit;
import com.google.firebase.database.Exclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public class Supermarket implements Cloneable
{
    private String supermarket;
    private String section;

    public Supermarket() {}

    public Supermarket(String supermarket)
    {
        this.supermarket = supermarket != null ? supermarket : "";
    }

    public Supermarket(String supermarket, String section)
    {
        this.supermarket = supermarket != null ? supermarket : "";
        this.section = section != null ? section : "";
    }

    public String getSupermarket()
    {
        return supermarket;
    }

    public void setSupermarket(String supermarket)
    {
        this.supermarket = supermarket != null
                ? Baskit.encodeKey(supermarket)
                : "";
    }

    public String getSection()
    {
        return section;
    }

    public void setSection(String section)
    {
        this.section = section != null
                ? Baskit.encodeKey(section)
                : "";
    }

    @Exclude
    public String getDecodedSupermarket()
    {
        return supermarket != null
                ? Baskit.decodeKey(supermarket)
                : "";
    }

    @Exclude
    public String getDecodedSection()
    {
        return section != null
                ? Baskit.decodeKey(section)
                : "";
    }

    @Exclude
    public static ArrayList<Supermarket> getSupermarketsFromStrings(Map<String, ArrayList<String>> supermarketStrings)
    {
        ArrayList<Supermarket> supermarkets = new ArrayList<>();

        if (supermarketStrings == null)
        {
            return supermarkets;
        }

        for (String supermarketName : supermarketStrings.keySet())
        {
            if (supermarketName == null)
            {
                continue;
            }

            for (String sectionName : Objects.requireNonNull(supermarketStrings.get(supermarketName)))
            {
                if (sectionName == null)
                {
                    continue;
                }
                supermarkets.add(new Supermarket(supermarketName, sectionName));
            }
        }

        return supermarkets;
    }

    @Exclude
    public static Map<String, ArrayList<String>> getStringsFromSupermarkets(ArrayList<Supermarket> supermarkets)
    {
        Map<String, ArrayList<String>> supermarketsStrings = new HashMap<>();

        if (supermarkets == null)
        {
            return supermarketsStrings;
        }

        for (Supermarket supermarket : supermarkets)
        {
            if (supermarket == null)
            {
                continue;
            }
            String supermarketName = supermarket.getSupermarket();
            String sectionName = supermarket.getSection();

            if (supermarketName == null || sectionName == null)
            {
                continue;
            }

            if (!supermarketsStrings.containsKey(supermarketName))
            {
                supermarketsStrings.put(supermarketName, new ArrayList<>());
            }

            Objects.requireNonNull(supermarketsStrings.get(supermarketName)).add(sectionName);
        }

        return supermarketsStrings;
    }

    @NonNull
    @Override
    public String toString()
    {
        if (section == null || section.isEmpty())
        {
            return getDecodedSupermarket();
        }

        return getDecodedSupermarket() + ", " + getDecodedSection();
    }

    @NonNull
    @Override
    public Supermarket clone()
    {
        try
        {
            return (Supermarket) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Supermarket other = (Supermarket) obj;
        return Objects.equals(supermarket, other.supermarket) &&
                Objects.equals(section, other.section);
    }

    @Override
    public int hashCode() {
        return Objects.hash(supermarket, section);
    }
}
