package com.example.baskit.Categories;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.MainComponents.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ItemsListHandler implements ItemsAdapter.UpperClassFunctions
{
    public void notifyCheckBox(Item item)
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

    @Override
    public void updateItemCategory(Item item)
    {
        dbHandler.updateItem(list, category, item);
    }

    @Override
    public void removeItemCategory(Item item)
    {
        dbHandler.removeItem(list, category, item);
    }

    @FunctionalInterface
    public interface EmptyCategoryCase {
        void onFinishedCategory();
    }

    EmptyCategoryCase emptyCategory;
    private RecyclerView recyclerUnchecked, recyclerChecked;
    private ItemsAdapter uncheckedAdapter, checkedAdapter;
    private Context context;
    private List list;
    private Category category;
    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();

    public ItemsListHandler(Context context,
                            RecyclerView recyclerUnchecked,
                            RecyclerView recyclerChecked,
                            EmptyCategoryCase emptyCategory,
                            List list,
                            Category category)
    {
        this.list = list;
        this.category = category;
        this.emptyCategory = emptyCategory;
        this.context = context;
        this.recyclerUnchecked = recyclerUnchecked;
        this.recyclerChecked = recyclerChecked;

        Map<String, Item> uncheckedItems = new HashMap<>();
        Map<String, Item> checkedItems = new HashMap();

        for (Item item : category.getItems().values())
        {
            if (item.isChecked())
            {
                checkedItems.put(item.getId(), item);
            }
            else
            {
                uncheckedItems.put(item.getId(), item);
            }
        }

        uncheckedAdapter = new ItemsAdapter(new ArrayList<>(uncheckedItems.values()), this::notifyCheckBox, this, this::ifFinishedCategory);
        checkedAdapter = new ItemsAdapterChecked(new ArrayList<>(checkedItems.values()), this::notifyCheckBox, this, this::ifFinishedCategory);

        this.recyclerUnchecked.setLayoutManager(new LinearLayoutManager(this.context));
        this.recyclerUnchecked.setAdapter(uncheckedAdapter);

        this.recyclerChecked.setLayoutManager(new LinearLayoutManager(this.context));
        this.recyclerChecked.setAdapter(checkedAdapter);
    }

    public void addItem(String item_category_name, Item item)
    {
        if (item.isChecked())
        {
            checkedAdapter.addItem(item);
        }
        else
        {
            uncheckedAdapter.addItem(item);
        }

        dbHandler.addItem(list, category, item);
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

    public void finished()
    {
        checkedAdapter.clearAll();

        ifFinishedCategory();
    }

    public Map<String, Item> getAllItems()
    {
        Map<String, Item> all_items = new HashMap<>();

        // Add unchecked items
        for (Item item : uncheckedAdapter.items) {
            all_items.put(item.getId(), item);
        }

        // Add checked items
        for (Item item : checkedAdapter.items) {
            all_items.put(item.getId(), item);
        }

        return all_items;
    }

    public void ifFinishedCategory()
    {
        if (uncheckedAdapter.items.isEmpty() && checkedAdapter.items.isEmpty())
        {
            emptyCategory.onFinishedCategory();
            dbHandler.removeCategory(list, category);
        }
    }

    public void update(Category newCategory)
    {
        this.category = newCategory;

        ArrayList<Item> uncheckedItems = new ArrayList<>();
        ArrayList<Item> checkedItems = new ArrayList<>();

        for (Item item : newCategory.getItems().values()) {
            if (item.isChecked()) {
                checkedItems.add(item);
            } else {
                uncheckedItems.add(item);
            }
        }

        uncheckedAdapter.updateItems(uncheckedItems);
        checkedAdapter.updateItems(checkedItems);

        ifFinishedCategory();
    }
}