package com.example.baskit.List;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

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

        listId = getIntent().getStringExtra("listId");

        categoriesListContainer = findViewById(R.id.categories_container);
        categoriesListInflater = LayoutInflater.from(this);

        setButton();
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

                //Category c = new Category("מוצרי חלב");
                //c.addItem(new Item("2", "name"));
                //dbHandler.addCategory(list, c);

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

    private void updateCategoriesInflater(Map<String, Category> newCategories) {
        // Remove categories that were deleted
        for (String key : categories.keySet())
        {
            if (!newCategories.containsKey(key))
            {
                View toRemove = categoriesViews.get(key); // keep a map of key -> View
                categoriesListContainer.removeView(toRemove);
                categoriesViews.remove(key);
            }
        }

        // Add or update categories
        for (Map.Entry<String, Category> entry : newCategories.entrySet())
        {
            String key = entry.getKey();
            Category category = entry.getValue();

            if (categoriesViews.containsKey(key))
            {
                // Update existing view
                View categoryView = categoriesViews.get(key);
                TextView tv_count = categoryView.findViewById(R.id.tv_count);
                tv_count.setText(Integer.toString(category.getSize()));
            }
            else
            {
                // Add new view
                inflaterAddItem(category);
            }
        }
    }

    private void inflaterAddItem(Category category)
    {
        View categoryView = categoriesListInflater.inflate(R.layout.category_list_item_default, categoriesListContainer, false);

        TextView tv_name = categoryView.findViewById(R.id.tv_name);
        TextView tv_count = categoryView.findViewById(R.id.tv_count);

        tv_name.setText(category.getName());
        tv_count.setText(Integer.toString(category.getSize()));

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
}