package com.example.baskit.MainComponents;

import com.example.baskit.Categories.ItemViewPricesAdapter;

import java.util.ArrayList;
import java.util.Map;

public interface SortableEntity
{
    double getTotal();
    boolean allPricesKnown();

    void setCheapestFromSmMap(Map<String, Map<Supermarket, Double>> pricesAllItems);
    void setCheapestFromStringsMap(Map<String, Map<String, Map<String, Double>>> allItems);
    public void setSupermarketFromSmMap(Supermarket supermarket, Map<String, Map<Supermarket, Double>> pricesAllItems);
    void setSupermarketFromStringsMap(Supermarket supermarket, Map<String, Map<String, Map<String, Double>>> allItems);

    void setCheapestRows(Map<String, ArrayList<ItemViewPricesAdapter.PriceRow>> rowsAllItems);
    void setSupermarketsRows(Supermarket supermarket, Map<String, ArrayList<ItemViewPricesAdapter.PriceRow>> rowsAllItems);

    SortableEntity copy();
}