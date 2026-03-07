package com.example.baskit.MainComponents;

import com.example.baskit.Categories.ItemViewPricesAdapter;

import java.util.ArrayList;
import java.util.Map;

public interface SortableEntity
{
    double getTotal();
    boolean allPricesKnown();

    void setCheapestRows(Map<String, ArrayList<ItemViewPricesAdapter.PriceRow>> rowsAllItems);
    void setSupermarketsRows(Supermarket supermarket, Map<String, ArrayList<ItemViewPricesAdapter.PriceRow>> rowsAllItems);

    SortableEntity copy();
}