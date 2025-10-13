package com.example.baskit.Categories;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.MainComponents.Item;

import java.util.ArrayList;

public class ItemsListHandler
{
    @FunctionalInterface
    public interface EmptyCategoryCase {
        void onFinishedCategory();
    }

    EmptyCategoryCase emptyCategory;
    private RecyclerView recyclerUnchecked, recyclerChecked;
    private ItemsAdapter uncheckedAdapter, checkedAdapter;
    private Context context;

    public ItemsListHandler(Context context,
                            RecyclerView recyclerUnchecked, RecyclerView recyclerChecked,
                            ArrayList<Item> items,
                            EmptyCategoryCase emptyCategory)
    {
        this.emptyCategory = emptyCategory;
        this.context = context;
        this.recyclerUnchecked = recyclerUnchecked;
        this.recyclerChecked = recyclerChecked;

        ArrayList<Item> uncheckedItems = new ArrayList<>();
        ArrayList<Item> checkedItems = new ArrayList<>();

        for (Item item : items)
        {
            if (item.isChecked())
            {
                checkedItems.add(item);
            }
            else
            {
                uncheckedItems.add(item);
            }
        }

        uncheckedAdapter = new ItemsAdapter(uncheckedItems, this::checkBoxClicked, this::ifFinishedCategory);
        checkedAdapter = new ItemsAdapterChecked(checkedItems, this::checkBoxClicked, this::ifFinishedCategory);

        this.recyclerUnchecked.setLayoutManager(new LinearLayoutManager(this.context));
        this.recyclerUnchecked.setAdapter(uncheckedAdapter);

        this.recyclerChecked.setLayoutManager(new LinearLayoutManager(this.context));
        this.recyclerChecked.setAdapter(checkedAdapter);
    }

    public void addItem(Item item)
    {
        if (item.isChecked())
        {
            checkedAdapter.addItem(item);
        }
        else
        {
            uncheckedAdapter.addItem(item);
        }
    }

    public void removeItem(int pos, boolean checked)
    {
        if (checked)
        {
            checkedAdapter.removeItem(pos);
        }
        else
        {
            uncheckedAdapter.removeItem(pos);
        }
    }

    public void removeItem(Item item)
    {
        if (item.isChecked())
        {
            checkedAdapter.removeItem(item);
        }
        else
        {
            uncheckedAdapter.removeItem(item);
        }
    }

    public void checkBoxClicked(Item item)
    {
        boolean checked = item.isChecked();

        ItemsAdapter fromAdapter = checked ? checkedAdapter : uncheckedAdapter;
        ItemsAdapter toAdapter = checked ? uncheckedAdapter : checkedAdapter;

        item.setChecked(!checked);
        toAdapter.addItem(item);
        item.setChecked(checked);
        fromAdapter.removeItem(item);
        item.setChecked(!checked);
    }

    public void finished()
    {
        checkedAdapter.clearAll();

        ifFinishedCategory();
    }

    public ArrayList<Item> getAllItems()
    {
        ArrayList<Item> all_items = new ArrayList<>();
        all_items.addAll(uncheckedAdapter.items);
        all_items.addAll(checkedAdapter.items);

        return all_items;
    }

    public void ifFinishedCategory()
    {
        if (uncheckedAdapter.items.isEmpty() && checkedAdapter.items.isEmpty())
        {
            emptyCategory.onFinishedCategory();
        }
    }
}