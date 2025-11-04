package com.example.baskit.MainComponents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class Category
{
    private String name = "";
    private boolean finished = false;
    private Map<String, Item> items = new HashMap<>();

    public Category() {}

    public Category(String name)
    {
        this.name = name;
        this.finished = true;
    }

    public Category(String name, Map<String, Item> items)
    {
        this.name = name;
        this.items = items;
        updateFinished();
    }

    public void updateFinished()
    {
        if (items != null && !items.isEmpty())
        {
            for (Item item : items.values())
            {
                if (!item.isChecked())
                {
                    this.finished = false;
                    return;
                }
            }
        }

        this.finished = true;
    }

    public int countUnchecked()
    {
        int count = 0;

        if (items != null && !items.isEmpty())
        {
            for (Item item : items.values())
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

    public Map<String, Item> getItems() {
        return items;
    }

    public void setItems(Map<String, Item> items) {
        this.items = items;

        updateFinished();
    }

    @Override
    public String toString() {
        return name;
    }

    public void addItem(Item item)
    {
        this.items.put(item.getId(), item);

        if (finished)
        {
            updateFinished();
        }
    }

    public void finished(List list)
    {
        if (finished)
        {
            list.removeCategory(name);
        }
        else
        {
            for (Item item : items.values())
            {
                if (item.isChecked())
                {
                    items.remove(item);
                }
            }

            list.modifyCategory(name, this);
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public java.util.List<Item> getItemsSorted()
    {
        java.util.List<Item> sortedItems = new ArrayList<>(items.values());
        sortedItems.sort(Comparator.comparing(Item::getName, String.CASE_INSENSITIVE_ORDER));
        return sortedItems;
    }
}
