package com.example.baskit.MainComponents;

import java.util.Map;

public interface SortableEntity
{
    double getTotal();
    boolean allPricesKnown();

    void setCheapestFromSmMap(Map<String, Map<Supermarket, Double>> pricesAllItems);
    void setCheapestFromStringsMap(Map<String, Map<String, Map<String, Double>>> allItems);
    public void setSupermarketFromSmMap(Supermarket supermarket, Map<String, Map<Supermarket, Double>> pricesAllItems);
    void setSupermarketFromStringsMap(Supermarket supermarket, Map<String, Map<String, Map<String, Double>>> allItems);

    SortableEntity copy();
}