package com.example.baskit.MainComponents;

import java.util.ArrayList;

public class Category
{
    private String name;
    private boolean finished;
    private ArrayList<Item> items;

    public Category(String name)
    {
        this.name = name;
        this.items = new ArrayList<>();
        this.finished = true;
    }

    public Category(String name, ArrayList<Item> items)
    {
        this.name = name;
        this.items = items;
        updateFinished();
    }

    public void updateFinished()
    {
        if (items != null && !items.isEmpty())
        {
            for (Item item : items)
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Item> getItems() {
        return items;
    }

    public void setItems(ArrayList<Item> items) {
        this.items = items;

        updateFinished();
    }

    @Override
    public String toString() {
        return name;
    }

    public void addItem(Item item)
    {
        this.items.add(item);

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
            for (Item item : items)
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
}
