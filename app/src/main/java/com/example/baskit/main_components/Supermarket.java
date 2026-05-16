package com.example.baskit.main_components;

import androidx.annotation.NonNull;

import com.example.baskit.Baskit;
import com.google.firebase.database.Exclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Supermarket implements Cloneable
{
    private String supermarket;
    private String section;

    public Supermarket() {}

    public Supermarket(String supermarket)
    {
        this.supermarket = supermarket;
    }

    public Supermarket(String supermarket, String section)
    {
        this.supermarket = supermarket;
        this.section = section;
    }

    public String getSupermarket()
    {
        return supermarket;
    }

    public void setSupermarket(String supermarket)
    {
        this.supermarket = Baskit.encodeKey(supermarket);
    }

    public String getSection()
    {
        return section;
    }

    public void setSection(String section)
    {
        this.section = Baskit.encodeKey(section);
    }

    @Exclude
    public String getDecodedSupermarket()
    {
        return Baskit.decodeKey(supermarket);
    }

    @Exclude
    public String getDecodedSection()
    {
        return Baskit.decodeKey(section);
    }

    @Exclude
    public static ArrayList<Supermarket> getSupermarketsFromStrings(Map<String, ArrayList<String>> supermarketStrings)
    {
        ArrayList<Supermarket> supermarkets = new ArrayList<>();

        for (String supermarketName : supermarketStrings.keySet())
        {
            for (String sectionName : Objects.requireNonNull(supermarketStrings.get(supermarketName)))
            {
                supermarkets.add(new Supermarket(supermarketName, sectionName));
            }
        }

        return supermarkets;
    }

    @Exclude
    public static Map<String, ArrayList<String>> getStringsFromSupermarkets(ArrayList<Supermarket> supermarkets)
    {
        Map<String, ArrayList<String>> supermarketsStrings = new HashMap<>();

        for (Supermarket supermarket : supermarkets)
        {
            String supermarketName = supermarket.getSupermarket();
            String sectionName = supermarket.getSection();

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
        if (section.isEmpty())
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
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Supermarket other = (Supermarket) obj;
        return supermarket.equals(other.supermarket) &&
                section.equals(other.section);
    }

    @Override
    public int hashCode() {
        return Objects.hash(supermarket, section);
    }
}
