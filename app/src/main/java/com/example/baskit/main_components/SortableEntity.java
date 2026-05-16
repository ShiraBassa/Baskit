package com.example.baskit.main_components;

import java.util.ArrayList;
import java.util.Map;
import com.example.baskit.main_components.Item.ItemVariant;

public interface SortableEntity
{
    double getTotal();
    boolean allPricesKnown();

    void setCheapestVariants(Map<String, ArrayList<ItemVariant>> variantsAllItems);
    void setSupermarketsVariants(Supermarket supermarket, Map<String, ArrayList<ItemVariant>> variantsAllItems);

    SortableEntity copy();
}