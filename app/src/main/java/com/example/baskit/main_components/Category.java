package com.example.baskit.main_components;

import androidx.annotation.NonNull;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.example.baskit.main_components.Item.ItemVariant;

@SuppressWarnings("unused")
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

    public void setFinished(boolean finished)
    {
        this.finished = finished;
    }

    public boolean isFinished() {
        return finished;
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
                newItems.put(item.getBaseName(), item);
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

    @Exclude
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

    @Exclude
    public void addItem(Item item)
    {
        this.items.add(item);

        if (finished)
        {
            updateFinished();
        }
    }

    @Exclude
    public void removeItem(String name)
    {
        if (name != null)
        {
            this.items.removeIf(i -> name.equals(i.getBaseName()));
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
    public void setCheapestVariants(Map<String, ArrayList<ItemVariant>> variantsAllItems)
    {
        if (variantsAllItems == null) return;

        for (Item item : this.items)
        {
            ArrayList<ItemVariant> variants = variantsAllItems.get(item.baseName);
            if (variants == null) continue;

            item.setCheapestVariant(variants);
        }
    }

    @Exclude
    public void setSupermarketsVariants(Supermarket supermarket, Map<String, ArrayList<ItemVariant>> variantsAllItems)
    {
        if (variantsAllItems == null) return;

        for (Item item : this.items)
        {
            ArrayList<ItemVariant> variants = variantsAllItems.get(item.baseName);
            if (variants == null) continue;

            item.setSupermarketVariant(supermarket, variants);
        }
    }

    @Exclude
    @Override
    public SortableEntity copy()
    {
        return new Category(this);
    }

    @NonNull
    @Exclude
    @Override
    public String toString()
    {
        return name;
    }
}
