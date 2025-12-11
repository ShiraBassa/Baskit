package com.example.baskit.Categories;

import android.app.Activity;
import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ItemsListHandler implements ItemsAdapter.UpperClassFunctions
{
    public void notifyCheckBox(Item item)
    {
        boolean checked = item.isChecked();
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

    @Override
    public void updateCategory()
    {
        dbHandler.updateItems(list, new ArrayList<>(category.getItems().values()));
    }

    @Override
    public void removeCategory()
    {
        dbHandler.removeCategory(list, category);
    }

    @FunctionalInterface
    public interface EmptyCategoryCase {
        void onFinishedCategory();
    }

    EmptyCategoryCase emptyCategory;
    private RecyclerView recyclerItems;
    private ItemsAdapter itemsAdapter;
    private Context context;
    private List list;
    private Category category;
    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    Activity activity;

    public ItemsListHandler(Activity activity,
                            Context context,
                            RecyclerView recyclerItems,
                            EmptyCategoryCase emptyCategory,
                            List list,
                            Category category)
    {
        this.activity = activity;
        this.list = list;
        this.category = category;
        this.emptyCategory = emptyCategory;
        this.context = context;
        this.recyclerItems = recyclerItems;

        itemsAdapter = new ItemsAdapter(new ArrayList<>(category.getItems().values()), this::notifyCheckBox, this,
               activity, context);

        this.recyclerItems.setLayoutManager(new LinearLayoutManager(this.context));
        this.recyclerItems.setAdapter(itemsAdapter);
    }

    public void addItem(Item item)
    {
        itemsAdapter.addItem(item);
        dbHandler.addItem(list, category, item);
    }

    public void removeItem(int pos, boolean checked)
    {
        itemsAdapter.removeItem(pos);
    }

    public void removeItem(Item item)
    {
        itemsAdapter.removeItem(item);
    }

    public void finished()
    {
        itemsAdapter.clearAll();
        ifFinishedCategory();
    }

    public void ifFinishedCategory()
    {
        for (Item item : category.getItems().values())
        {
            if (!item.isChecked())
            {
                return;
            }
        }

        emptyCategory.onFinishedCategory();
        dbHandler.removeCategory(list, category);
    }

    public void update(Category newCategory)
    {
        this.category = newCategory;
        itemsAdapter.updateItems((ArrayList<Item>) newCategory.getItems().values());

        ifFinishedCategory();
    }
}