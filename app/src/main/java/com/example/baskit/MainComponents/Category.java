package com.example.baskit.MainComponents;

import com.example.baskit.Categories.ItemViewPricesAdapter;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Category implements SortableEntity
{
    private String name = "";
    private boolean finished = false;
    private ArrayList<Item> items = new ArrayList<>();

    public Category() {}

    public Category(String name)
    {
        this.name = name;
        this.finished = true;
    }

    public Category(String name, ArrayList<Item> items)
    {
        this.name = name;
        this.items = items;
        updateFinished();
    }

    public Category(Category other)
    {
        if (other == null) return;

        this.name = other.getName();
        this.finished = other.isFinished();

        this.items = new ArrayList<>();

        if (other.getItems() != null)
        {
            for (Item item : other.getItems())
            {
                this.items.add(new Item(item));
            }
        }
    }

    public void updateFinished()
    {
        if (items == null || items.isEmpty())
        {
            finished = false;
            return;
        }

        for (Item item : items)
        {
            if (!item.isChecked())
            {
                finished = false;
                return;
            }
        }

        finished = true;
    }

    @Exclude
    public int countUnchecked()
    {
        int count = 0;

        if (items != null && !items.isEmpty())
        {
            for (Item item : items)
            {
                if (!item.isChecked())
                {
                    count++;
                }
            }
        }

        return count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Item> getItems()
    {
        return items;
    }

    @Exclude
    public ArrayList<Item> getRemainedItems()
    {
        ArrayList<Item> items = new ArrayList<>();

        for (Item item : this.items)
        {
            if (!item.isChecked())
            {
                items.add(item);
            }
        }

        return items;
    }

    public void setItems(Map<String, Item> items)
    {
        this.items = new ArrayList<>();

        if (items != null)
        {
            this.items.addAll(items.values());
        }

        updateFinished();
    }

    @Exclude
    public void setItems(ArrayList<Item> items)
    {
        Map<String, Item> newItems = new HashMap<>();

        if (items != null)
        {
            for (Item item : items)
            {
                newItems.put(item.getId(), item);
            }
        }

        setItems(newItems);
    }

    @Exclude
    public void setItemsFromFlat(ArrayList<Item> items)
    {
        this.items = new ArrayList<>(items);
        updateFinished();
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Exclude
    public void addItem(Item item)
    {
        this.items.add(item);

        if (finished)
        {
            updateFinished();
        }
    }

    public void setFinished(boolean finished)
    {
        this.finished = finished;
    }

    @Exclude
    public void finished(List list)
    {
        if (finished)
        {
            list.removeCategory(name);
        }
        else
        {
            items.removeIf(Item::isChecked);
            list.updateCategory(this);
        }
    }

    public boolean isFinished() {
        return finished;
    }

    @Exclude
    public java.util.List<Item> getItemsSorted()
    {
        java.util.List<Item> sortedItems = new ArrayList<>(items);
        sortedItems.sort(Comparator.comparing(Item::getBaseName, String.CASE_INSENSITIVE_ORDER));
        return sortedItems;
    }

    @Exclude
    public double getTotal()
    {
        double sum = 0;

        for (Item item : this.items)
        {
            if (!item.isChecked())
            {
                sum += item.getTotal();
            }
        }

        return Math.round(sum * 100.0) / 100.0;
    }

    @Exclude
    public boolean allPricesKnown()
    {
        for (Item item : this.items)
        {
            if (!item.isPriceKnown() && !item.isChecked())
            {
                return false;
            }
        }

        return true;
    }

    @Exclude
    public boolean doesExists(Item item)
    {
        for (Item i : items)
        {
            if (i.getId().equals(item.getId())) return true;
        }
        return false;
    }

    @Exclude
    public ArrayList<String> toItemNames()
    {
        ArrayList<String> itemNames = new ArrayList<>();

        for (Item item : items)
        {
            itemNames.add(item.baseName);
        }

        return itemNames;
    }

    @Exclude
    public boolean isEmpty()
    {
        return items.isEmpty();
    }

    @Exclude
    public void removeItem(Item item)
    {
        if (item != null)
        {
            removeItem(item.getId());
        }
    }

    @Exclude
    public void removeItem(String id)
    {
        if (id != null)
        {
            this.items.removeIf(i -> id.equals(i.getId()));
            updateFinished();
        }
    }

    @Exclude
    public void removeVariants(String baseName)
    {
        if (baseName != null)
        {
            this.items.removeIf(i -> baseName.equals(i.getBaseName()));
            updateFinished();
        }
    }

    @Exclude
    public void removeItems(ArrayList<String> itemIDs)
    {
        if (itemIDs == null) return;

        for (String id : itemIDs)
        {
            this.items.removeIf(i -> Item.getFullId(id).equals(i.getId()));
        }

        updateFinished();
    }

    @Exclude
    public void setCheapestRows(Map<String, ArrayList<ItemViewPricesAdapter.PriceRow>> rowsAllItems)
    {
        if (rowsAllItems == null) return;

        for (Item item : this.items)
        {
            ArrayList<ItemViewPricesAdapter.PriceRow> rows = rowsAllItems.get(item.baseName);
            if (rows == null) continue;

            item.setCheapestRow(rows);
        }
    }

    @Exclude
    public void setSupermarketsRows(Supermarket supermarket, Map<String, ArrayList<ItemViewPricesAdapter.PriceRow>> rowsAllItems)
    {
        if (rowsAllItems == null) return;

        for (Item item : this.items)
        {
            ArrayList<ItemViewPricesAdapter.PriceRow> rows = rowsAllItems.get(item.baseName);
            if (rows == null) continue;

            item.setSupermarketRow(supermarket, rows);
        }
    }

    @Exclude
    public void setCheapestFromSmMap(Map<String, Map<Supermarket, Double>> pricesAllItems)
    {
        if (pricesAllItems == null) return;

        for (Item item : this.items)
        {
            Map<Supermarket, Double> prices = pricesAllItems.get(item.baseName);
            if (prices == null) continue;

            item.setCheapestSupermarketFromSmMap(prices);
        }
    }

    @Exclude
    public void setCheapestFromStringsMap(Map<String, Map<String, Map<String, Double>>> pricesAllItems)
    {
        if (pricesAllItems == null) return;

        for (Item item : this.items)
        {
            Map<String, Map<String, Double>> prices = pricesAllItems.get(item.baseName);
            if (prices == null) continue;

            item.setCheapestSupermarketFromStringsMap(prices);
        }
    }

    @Exclude
    public void setSupermarketFromSmMap(Supermarket supermarket, Map<String, Map<Supermarket, Double>> pricesAllItems)
    {
        if (pricesAllItems == null) return;

        for (Item item : this.items)
        {
            Map<Supermarket, Double> prices = pricesAllItems.get(item.baseName);
            if (prices == null) continue;

            item.setSupermarketFromSmMap(supermarket, prices);
        }
    }

    @Exclude
    public void setSupermarketFromStringsMap(Supermarket supermarket, Map<String, Map<String, Map<String, Double>>> pricesAllItems)
    {
        if (pricesAllItems == null) return;

        for (Item item : this.items)
        {
            Map<String, Map<String, Double>> prices = pricesAllItems.get(item.baseName);
            if (prices == null) continue;

            item.setSupermarketFromStringsMap(supermarket, prices);
        }
    }

    @Exclude
    @Override
    public SortableEntity copy()
    {
        return new Category(this);
    }
}
