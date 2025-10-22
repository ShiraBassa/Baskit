package com.example.baskit.List;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.baskit.AI.AIHandler;
import com.example.baskit.Categories.CategoryActivity;
import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ListActivity extends AppCompatActivity
{
    TextView tvListName;
    ImageButton btnBack, btnFinished;

    List list;
    String listId;
    Map<String, Category> categories;

    LinearLayout categoriesListContainer;
    LayoutInflater categoriesListInflater;
    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    Map<String, View> categoriesViews;
    AddItemAlertDialog addItemAlertDialog;
    Button btnAddItem;
    AIHandler aiHandler = AIHandler.getInstance();

    Map<String, String> allItems = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        allItems.put("1", "חלב 3%");
        allItems.put("2", "יוגורט");
        allItems.put("3", "מלפפון");
        allItems.put("4", "חזה עוף");

        createInit();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        resumeInit();
    }

    private void createInit()
    {
        tvListName = findViewById(R.id.tv_list_name);
        btnBack = findViewById(R.id.btn_back);
        btnFinished = findViewById(R.id.btn_finished);
        btnAddItem = findViewById(R.id.btn_add_item);

        listId = getIntent().getStringExtra("listId");

        categoriesListContainer = findViewById(R.id.categories_container);
        categoriesListInflater = LayoutInflater.from(this);

        setButton();
        addItemAlertDialog = new AddItemAlertDialog(ListActivity.this,
                ListActivity.this,
                new ArrayList<>(allItems.values()),
                ListActivity.this::addItem);
    }

    private void resumeInit()
    {
        dbHandler.getList(listId, new FirebaseDBHandler.GetListCallback()
        {
            @Override
            public void onListFetched(List newList)
            {
                ListActivity.this.list = newList;

                if (newList == null)
                {
                    ListActivity.this.list = new List();
                    categoriesListContainer.removeAllViews();
                    return;
                }

                tvListName.setText(ListActivity.this.list.getName());
                tvListName.setVisibility(View.VISIBLE);

                categories = ListActivity.this.list.getCategories();

                if (categories == null)
                {
                    categories = new HashMap<>();
                }

                setCategoriesInflater();

                dbHandler.listenToList(listId, new FirebaseDBHandler.GetListCallback()
                {
                    @Override
                    public void onListFetched(List newList)
                    {
                        if (!list.getName().equals(newList.getName()))
                        {
                            tvListName.setText(newList.getName());
                        }

                        if (!list.getCategories().equals(newList.getCategories()))
                        {
                            Map<String, Category> newCategories = newList.getCategories();

                            if (categories != null)
                            {
                                updateCategoriesInflater(newCategories);
                            }

                            //If needed categories = newCategories
                        }

                        list = newList;
                    }

                    @Override
                    public void onError(String error) {}
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void setButton()
    {
        btnBack.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                finish();
            }
        });

        btnFinished.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Category category;

                for (int i=0; i<list.getCategories().size(); i++)
                {
                    category = list.getCategories().get(i);

                    if (category.isFinished())
                    {
                        i--;
                    }

                    category.finished(list);
                }

                resumeInit();
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

    private void setCategoriesInflater()
    {
        categoriesListContainer.removeAllViews();
        categoriesViews = new HashMap<>();

        for (Category category : categories.values())
        {
            inflaterAddItem(category);
        }
    }

    private void updateCategoriesInflater(Map<String, Category> newCategories)
    {
        for (String key : categories.keySet())
        {
            if (!newCategories.containsKey(key))
            {
                View toRemove = categoriesViews.get(key);
                categoriesListContainer.removeView(toRemove);
                categoriesViews.remove(key);
            }
        }

        for (Map.Entry<String, Category> entry : newCategories.entrySet())
        {
            String key = entry.getKey();
            Category newCategory = entry.getValue();

            boolean hasOld = categories.containsKey(key);
            Category oldCategory = hasOld ? categories.get(key) : null;

            if (categoriesViews.containsKey(key))
            {
                if (oldCategory != null && oldCategory.isFinished() != newCategory.isFinished())
                {
                    View oldView = categoriesViews.get(key);
                    categoriesListContainer.removeView(oldView);
                    categoriesViews.remove(key);
                    inflaterAddItem(newCategory);
                }
                else
                {
                    View categoryView = categoriesViews.get(key);
                    TextView tv_count = categoryView.findViewById(R.id.tv_count);

                    if (tv_count != null)
                    {
                        tv_count.setText(Integer.toString(newCategory.countUnchecked()));
                    }
                }
            }
            else
            {
                inflaterAddItem(newCategory);
            }
        }

        categories = new HashMap<>(newCategories);
    }

    private void inflaterAddItem(Category category)
    {
        View categoryView;
        TextView tv_name;

        if (!category.isFinished())
        {
            categoryView = categoriesListInflater.inflate(R.layout.category_list_item_default, categoriesListContainer, false);

            tv_name = categoryView.findViewById(R.id.tv_name);
            TextView tv_count = categoryView.findViewById(R.id.tv_count);

            tv_name.setText(category.getName());
            tv_count.setText(Integer.toString(category.countUnchecked()));
        }
        else
        {
            categoryView = categoriesListInflater.inflate(R.layout.category_list_item_finished, categoriesListContainer, false);
            tv_name = categoryView.findViewById(R.id.tv_name);
            tv_name.setText(category.getName());
        }

        tv_name.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(ListActivity.this, CategoryActivity.class);
                intent.putExtra("listId", list.getId());
                intent.putExtra("categoryName", category.getName());

                startActivity(intent);
            }
        });

        categoriesListContainer.addView(categoryView);
        categoriesViews.put(category.getName(), categoryView);
    }

    public void addItem(Item item)
    {
        addItemAlertDialog.startProgressBar();
        item.updateId(getKeyByValue(allItems, item.getName()));

        aiHandler.getCategoryName(item, ListActivity.this, categoryName -> {

            if (!list.hasCategory(categoryName))
            {
                dbHandler.addCategory(list, new Category(categoryName));
            }

            dbHandler.addItem(list, categoryName, item, new FirebaseDBHandler.DBCallback() {
                @Override
                public void onComplete()
                {
                    runOnUiThread(() ->
                    {
                        addItemAlertDialog.endProgressBar();
                        addItemAlertDialog.finish();
                    });
                }

                @Override
                public void onFailure(Exception e)
                {
                    runOnUiThread(() ->
                    {
                        addItemAlertDialog.endProgressBar();
                        addItemAlertDialog.finish();
                        Toast.makeText(ListActivity.this, "Failed to add item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
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