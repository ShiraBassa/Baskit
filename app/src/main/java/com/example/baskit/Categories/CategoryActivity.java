package com.example.baskit.Categories;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.AI.AIHandler;
import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.List.AddItemAlertDialog;
import com.example.baskit.List.ListActivity;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CategoryActivity extends AppCompatActivity
{
    List list;
    Map<String, Category> categories;
    Category category;

    ItemsListHandler itemsListHandler;
    TextView tvListName, tvCategoryName;
    ImageButton btnFinished, btnBack;
    Button btnAddItem;
    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    AddItemAlertDialog addItemAlertDialog;
    AIHandler aiHandler = AIHandler.getInstance();

    Map<String, String> allItems = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        allItems.put("1", "חלב 3%");
        allItems.put("2", "יוגורט");
        allItems.put("3", "מלפפון");
        allItems.put("4", "חזה עוף");

        init();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if (list.hasCategory(category.getName()))
        {
            list.getCategory(category.getName()).setItems(itemsListHandler.getAllItems());
        }
    }

    private void init()
    {
        btnFinished = findViewById(R.id.btn_finished);
        btnBack = findViewById(R.id.btn_back);
        tvListName = findViewById(R.id.tv_list_name);
        tvCategoryName = findViewById(R.id.tv_category_name);
        btnAddItem = findViewById(R.id.btn_add_item);

        dbHandler.getList(getIntent().getStringExtra("listId"), new FirebaseDBHandler.GetListCallback()
        {
            @Override
            public void onListFetched(List newList)
            {
                CategoryActivity.this.list = newList;

                categories = CategoryActivity.this.list.getCategories();
                category = newList.getCategory(getIntent().getStringExtra("categoryName"));

                tvListName.setText(newList.getName());
                tvCategoryName.setText(category.getName());
                tvListName.setVisibility(View.VISIBLE);
                tvCategoryName.setVisibility(View.VISIBLE);

                setButtons();
                addItemAlertDialog = new AddItemAlertDialog(CategoryActivity.this,
                        CategoryActivity.this,
                        new ArrayList<>(allItems.values()),
                        CategoryActivity.this::addItem);

                itemsListHandler = new ItemsListHandler(CategoryActivity.this,
                        findViewById(R.id.recycler_unchecked),
                        findViewById(R.id.recycler_checked),
                        CategoryActivity.this::finishedCategory,
                        newList,
                        category);

                dbHandler.listenToCategory(list, category, new FirebaseDBHandler.GetCategoryCallback()
                {
                    @Override
                    public void onCategoryFetched(Category newCategory)
                    {
                        itemsListHandler.update(newCategory);
                    }

                    @Override
                    public void onError(String error) {}
                });
            }

            @Override
            public void onError(String error) {}
        });
    }



    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setButtons()
    {
        btnFinished.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                itemsListHandler.finished();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                finish();
            }
        });

        btnAddItem.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                addItemAlertDialog.show();
            }
        });
    }

    private void finishedCategory()
    {
        list.removeCategory(category.getName());
        finish();
    }

    public void addItem(Item item)
    {
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        item.updateId(getKeyByValue(allItems, item.getName()));

        new Thread(() ->
        {
            aiHandler.getCategoryName(item, CategoryActivity.this, new AIHandler.OnGeminiResult()
            {
                @Override
                public void onResult(String categoryName)
                {
                    if (!categoryName.equals(category.getName()))
                    {
                        return;
                    }

                    itemsListHandler.addItem(item);
                    runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                }
            });
        }).start();
    }

    private String getKeyByValue(Map<String, String> map, String value)
    {
        for (Map.Entry<String, String> entry : map.entrySet())
        {
            if (entry.getValue().equals(value))
            {
                return entry.getKey();
            }
        }
        return null;
    }
}