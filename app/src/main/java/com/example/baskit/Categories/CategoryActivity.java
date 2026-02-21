package com.example.baskit.Categories;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.AI.AIHandler;
import com.example.baskit.API.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.List.AddItemFragment;
import com.example.baskit.List.PlanListActivity;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.MasterActivity;
import com.example.baskit.R;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class CategoryActivity extends MasterActivity
{
    List list;
    Map<String, Category> categories;
    Category category;

    TextView tvListName, tvCategoryName, tvTotal;
    ImageButton btnFinished, btnBack;
    Button btnCheapest;
    ImageButton btnAddItem, btnPlan;
    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    AddItemFragment addItemFragment;
    AIHandler aiHandler = AIHandler.getInstance();
    APIHandler apiHandler = APIHandler.getInstance();

    Map<String, Map<String, Map<String, Double>>> allItems;
    Map<String, String> itemsCodeNames;
    boolean initialized = true;
    private RecyclerView recyclerItems;
    private CategoryItemsAdapter itemsAdapter;
    private boolean categoryListenerAttached = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        String listId = getIntent().getStringExtra("listId");
        String categoryName = getIntent().getStringExtra("categoryName");

        if (listId == null || categoryName == null)
        {
            Toast.makeText(this, "Missing list/category", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        runWhenServerActive(() ->
        {
            try
            {
                allItems = apiHandler.getItems();
                itemsCodeNames = apiHandler.getItemsCodeName();
            }
            catch (Exception e)
            {
                Log.e("CategoryActivity", "Failed to load catalogs", e);
                allItems = null;
                itemsCodeNames = null;
            }

            runOnUiThread(this::init);
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (!initialized && tvTotal != null && category != null)
        {
            tvTotal.setText(Baskit.getTotalDisplayString(category.getTotal(), category.allPricesKnown(), true, false));
        }
    }

    private void init()
    {
        btnFinished = findViewById(R.id.btn_finished);
        btnBack = findViewById(R.id.btn_back);
        tvListName = findViewById(R.id.tv_list_name);
        tvCategoryName = findViewById(R.id.tv_category_name);
        btnAddItem = findViewById(R.id.btn_add_item);
        tvTotal = findViewById(R.id.tv_total);
        btnCheapest = findViewById(R.id.btn_arrange_cheapest);
        btnPlan = findViewById(R.id.btn_plan);

        final String listId = getIntent().getStringExtra("listId");
        final String categoryName = getIntent().getStringExtra("categoryName");

        runIfOnline(() ->
        {
            dbHandler.getList(listId, new FirebaseDBHandler.GetListCallback()
            {
                @Override
                public void onListFetched(List newList)
                {
                    CategoryActivity.this.list = newList;

                    if (newList == null)
                    {
                        Toast.makeText(CategoryActivity.this, "List not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    categories = newList.getCategories();
                    category = newList.getCategory(categoryName);

                    if (category == null)
                    {
                        Toast.makeText(CategoryActivity.this, "Category not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    tvListName.setText(newList.getName());
                    tvCategoryName.setText(category.getName());
                    tvListName.setVisibility(View.VISIBLE);
                    tvCategoryName.setVisibility(View.VISIBLE);

                    setButtons();

                    if (itemsCodeNames != null && itemsCodeNames.values() != null)
                    {
                        addItemFragment = new AddItemFragment(CategoryActivity.this,
                                CategoryActivity.this,
                                new ArrayList<>(itemsCodeNames.values()),
                                list.toItemNames(),
                                CategoryActivity.this::addItem,
                                list.getItemSuggestions());
                        btnAddItem.setEnabled(true);
                    }
                    else
                    {
                        addItemFragment = null;
                        btnAddItem.setEnabled(false);
                    }

                    recyclerItems = findViewById(R.id.recycler_supermarket_items);
                    recyclerItems.setLayoutManager(new LinearLayoutManager(CategoryActivity.this));

                    runWhenServerActive(() ->
                    {
                        ArrayList<Supermarket> supermarkets;
                        Map<String, Map<String, Map<String, Double>>> itemsCatalog;

                        try
                        {
                            supermarkets = apiHandler.getSupermarkets();
                        }
                        catch (Exception e)
                        {
                            Log.e("CategoryActivity", "Failed to load supermarkets", e);
                            supermarkets = new ArrayList<>();
                        }

                        itemsCatalog = allItems;

                        ArrayList<Supermarket> finalSupermarkets = supermarkets;
                        Map<String, Map<String, Map<String, Double>>> finalItemsCatalog = itemsCatalog;

                        runOnUiThread(() ->
                        {
                            itemsAdapter = new CategoryItemsAdapter(
                                    category,
                                    CategoryActivity.this,
                                    CategoryActivity.this,
                                    new ItemsAdapter.UpperClassFunctions()
                                    {
                                        @Override
                                        public void updateItemCategory(Item item)
                                        {
                                            if (category == null) return;
                                            runIfOnline(() -> dbHandler.updateItem(list, category, item));
                                        }

                                        @Override
                                        public void removeItemCategory(Item item)
                                        {
                                            if (category == null) return;
                                            runIfOnline(() -> dbHandler.removeItem(list, category, item));
                                        }

                                        @Override
                                        public void updateCategory()
                                        {
                                            if (category == null) return;
                                            runIfOnline(() -> dbHandler.updateItems(list, new ArrayList<>(category.getItems().values())));
                                        }

                                        @Override
                                        public void removeCategory()
                                        {
                                            runIfOnline(() ->
                                            {
                                                dbHandler.removeCategory(list, category);
                                                finish();
                                            });
                                        }
                                    },
                                    finalSupermarkets,
                                    finalItemsCatalog
                            );

                            recyclerItems.setAdapter(itemsAdapter);
                        });
                    });

                    // Attach category listener once
                    if (!categoryListenerAttached)
                    {
                        categoryListenerAttached = true;

                        runIfOnline(() ->
                        {
                            dbHandler.listenToCategory(list, category, new FirebaseDBHandler.GetCategoryCallback()
                            {
                                @Override
                                public void onCategoryFetched(Category newCategory)
                                {
                                    if (newCategory == null) return;

                                    category = newCategory;
                                    list.updateCategory(category);

                                    if (category.getItems() == null || category.getItems().isEmpty())
                                    {
                                        if (!isFinishing()) {
                                            finish();
                                        }
                                        return;
                                    }

                                    if (tvTotal != null)
                                    {
                                        tvTotal.setText(Baskit.getTotalDisplayString(category.getTotal(), category.allPricesKnown(), true, false));
                                        tvTotal.setVisibility(View.VISIBLE);
                                    }

                                    if (initialized)
                                    {
                                        initialized = false;
                                        return;
                                    }

                                    runOnUiThread(() ->
                                    {
                                        if (itemsAdapter != null)
                                        {
                                            itemsAdapter.updateItems(new ArrayList<>(category.getItems().values()));
                                        }
                                    });

                                    if (addItemFragment != null)
                                    {
                                        addItemFragment.updateData(list.toItemNames());
                                    }
                                }

                                @Override
                                public void onError(String error)
                                {
                                    initialized = false;
                                }
                            });
                        });
                    }
                }

                @Override
                public void onError(String error)
                {
                    initialized = false;
                }
            });
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
                runIfOnline(() -> dbHandler.finishCategory(list, category));
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
                if (addItemFragment != null)
                {
                    addItemFragment.show(getSupportFragmentManager(), "AddItemFragment");
                }
                else
                {
                    Toast.makeText(CategoryActivity.this, "Still loading…", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCheapest.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (itemsAdapter != null)
                {
                    itemsAdapter.arrangeByCheapest();
                }
            }
        });

        btnPlan.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(CategoryActivity.this, PlanListActivity.class);
                intent.putExtra("listId", list.getId());
                intent.putExtra("category", category.getName());
                startActivity(intent);
            }
        });
    }

    public void addItem(Item item)
    {
        if (addItemFragment != null)
        {
            addItemFragment.startProgressBar();
        }

        if (itemsCodeNames == null)
        {
            Toast.makeText(this, "Items are still loading…", Toast.LENGTH_SHORT).show();
            if (addItemFragment != null) addItemFragment.endProgressBar();
            return;
        }

        item.updateId(getKeyByValue(itemsCodeNames, item.getName()));

        new Thread(() ->
        {
            String categoryName;

            try
            {
                categoryName = apiHandler.getItemCategory(item);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            catch (JSONException e)
            {
                throw new RuntimeException(e);
            }

            if (categoryName == null || categoryName.isEmpty())
            {
                if (addItemFragment != null) addItemFragment.endProgressBar();
                runOnUiThread(() ->
                        Toast.makeText(CategoryActivity.this, "לא נמצאה קטגוריה לפריט", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            if (!list.hasCategory(categoryName))
            {
                runIfOnline(() -> dbHandler.addCategory(list, new Category(categoryName)));
            }

            runIfOnline(() ->
            {
                dbHandler.addItem(list, categoryName, item, new FirebaseDBHandler.DBCallback()
                {
                    @Override
                    public void onComplete()
                    {
                        runOnUiThread(() ->
                        {
                            if (addItemFragment != null)
                            {
                                addItemFragment.endProgressBar();
                                addItemFragment.dismiss();
                            }

                            if (category != null && !categoryName.equals(category.getName()))
                            {
                                Snackbar snackbar = Snackbar.make(
                                        findViewById(android.R.id.content),
                                        "הפריט נוסף לקטגוריה: " + categoryName,
                                        Snackbar.LENGTH_LONG
                                );

                                snackbar.getView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

                                snackbar.setAction("בטל", v ->
                                {
                                    runIfOnline(() -> dbHandler.removeItem(list, categoryName, item));
                                });

                                snackbar.setAnchorView(btnAddItem);
                                snackbar.show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e)
                    {
                        runOnUiThread(() ->
                        {
                            if (addItemFragment != null)
                            {
                                addItemFragment.endProgressBar();
                                addItemFragment.dismiss();
                            }
                            Toast.makeText(CategoryActivity.this, "שגיאה בניסיון להוסיף את הפריט", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
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