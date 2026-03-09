package com.example.baskit.MainComponents;

import java.util.ArrayList;
import java.util.Map;

public interface SortableEntity
{
    double getTotal();
    boolean allPricesKnown();

    void setCheapestRows(Map<String, ArrayList<PriceRow>> rowsAllItems);
    void setSupermarketsRows(Supermarket supermarket, Map<String, ArrayList<PriceRow>> rowsAllItems);

    SortableEntity copy();
}