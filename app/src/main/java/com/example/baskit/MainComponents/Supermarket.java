package com.example.baskit.MainComponents;

import com.example.baskit.Baskit;

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

    public Supermarket(String supermarket, String section) {
        this.supermarket = supermarket;
        this.section = section;
    }

    public String getSupermarket()
    {
        return supermarket;
    }

    public String getDecodedSupermarket()
    {
        return Baskit.decodeKey(supermarket);
    }

    public void setSupermarket(String supermarket)
    {
        this.supermarket = Baskit.encodeKey(supermarket);
    }

    public String getSection()
    {
        return section;
    }

    public String getDecodedSection()
    {
        return Baskit.decodeKey(section);
    }

    public void setSection(String section)
    {
        this.section = Baskit.encodeKey(section);
    }

    @Override
    public String toString()
    {
        if (section.isEmpty())
        {
            return getDecodedSupermarket();
        }

        return getDecodedSupermarket() + ", " + getDecodedSection();
    }

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

    public static ArrayList<Supermarket> getSupermarketsFromStrings(Map<String, ArrayList<String>> supermarketStrings)
    {
        ArrayList<Supermarket> supermarkets = new ArrayList<>();

        for (String supermarketName : supermarketStrings.keySet())
        {
            for (String sectionName : supermarketStrings.get(supermarketName))
            {
                supermarkets.add(new Supermarket(supermarketName, sectionName));
            }
        }

        return supermarkets;
    }

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

            supermarketsStrings.get(supermarketName).add(sectionName);
        }

        return supermarketsStrings;
    }

    public static Map<Supermarket, Double> getSupermarketsPricesFromStrings(Map<String, Map<String, Double>> supermarketStrings)
    {
        Map<Supermarket, Double> supermarketsPrices = new HashMap<>();

        if (supermarketStrings == null) return supermarketsPrices;

        for (Map.Entry<String, Map<String, Double>> supermarketEntry : supermarketStrings.entrySet())
        {
            String supermarketName = supermarketEntry.getKey();
            Map<String, Double> sectionMap = supermarketEntry.getValue();

            if (sectionMap == null) continue;

            for (Map.Entry<String, Double> sectionEntry : sectionMap.entrySet())
            {
                String sectionName = sectionEntry.getKey();
                Double price = sectionEntry.getValue();

                if (price == null) continue;

                Supermarket supermarket = new Supermarket(supermarketName, sectionName);
                supermarketsPrices.put(supermarket, price);
            }
        }

        return supermarketsPrices;
    }

    public static Map<String, Map<String, Double>> getStringsPricesFromSupermarkets(Map<Supermarket, Double> supermarketsPrices)
    {
        Map<String, Map<String, Double>> supermarketStrings = new HashMap<>();

        if (supermarketsPrices == null) return supermarketStrings;

        for (Map.Entry<Supermarket, Double> entry : supermarketsPrices.entrySet())
        {
            Supermarket supermarket = entry.getKey();
            Double price = entry.getValue();

            if (supermarket == null || price == null) continue;

            String supermarketName = supermarket.getSupermarket();
            String sectionName = supermarket.getSection();

            if (!supermarketStrings.containsKey(supermarketName))
            {
                supermarketStrings.put(supermarketName, new HashMap<>());
            }

            supermarketStrings.get(supermarketName).put(sectionName, price);
        }

        return supermarketStrings;
    }
}
