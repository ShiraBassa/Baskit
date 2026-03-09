package com.example.baskit.Categories;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.List.AddItemFragment;
import com.example.baskit.List.PlanListActivity;
import com.example.baskit.List.SortListBottomSheetBuilder;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.ItemInfo;
import com.example.baskit.MainComponents.List;
import com.example.baskit.MainComponents.PriceRow;
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
    Category category;

    Map<String, Map<String, Map<String, Double>>> allItemPrices;
    Map<String, ArrayList<String>> groups;
    Map<String, ItemInfo> infos;

    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    APIHandler apiHandler = APIHandler.getInstance();

    boolean initialized = true;
    private boolean categoryListenerAttached = false;

    AddItemFragment addItemFragment;
    RecyclerView recyclerItems;
    CategoryItemsAdapter itemsAdapter;

    TextView tvListName, tvCategoryName, tvTotal;
    ImageButton btnFinished, btnBack, btnMore, btnAddItem, btnPlan;
    Button btnSortList;

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
                allItemPrices = apiHandler.getItemPrices();
                groups = apiHandler.getGroups();
                infos = apiHandler.getItemInfos();
            }
            catch (Exception e)
            {
                Log.e("CategoryActivity", "Failed to load catalogs", e);
                allItemPrices = null;
                groups = null;
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
        btnSortList = findViewById(R.id.btn_sort_list);
        btnPlan = findViewById(R.id.btn_plan);
        btnMore = findViewById(R.id.btn_more);

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

                    if (groups != null && !groups.isEmpty())
                    {
                        addItemFragment = new AddItemFragment(
                                CategoryActivity.this,
                                CategoryActivity.this,
                                groups,
                                list.toItemNames(),
                                CategoryActivity.this::addItem,
                                list.getItemSuggestions()
                        );
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

                        try
                        {
                            supermarkets = apiHandler.getSupermarkets();
                        }
                        catch (Exception e)
                        {
                            Log.e("CategoryActivity", "Failed to load supermarkets", e);
                            supermarkets = new ArrayList<>();
                        }

                        ArrayList<Supermarket> finalSupermarkets = supermarkets;

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
                                            runIfOnline(() ->
                                            {
                                                category.removeVariants(item.getBaseName());
                                                category.addItem(item);
                                                dbHandler.updateCategory(list, category);
                                            });
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
                                            runIfOnline(() -> dbHandler.updateItemsIndividuals(list, new ArrayList<>(category.getItems())));
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
                                    finalSupermarkets
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
                                            itemsAdapter.updateItems(new ArrayList<>(category.getItems()));
                                        }
                                    });
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
                    addItemFragment.updateData(list.toItemNames());
                    addItemFragment.show(getSupportFragmentManager(), "AddItemFragment");
                }
                else
                {
                    Toast.makeText(CategoryActivity.this, "Still loading…", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSortList.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (!category.getRemainedItems().isEmpty())
                {
                    try
                    {
                        showSortBottomSheet();
                    }
                    catch (JSONException e)
                    {
                        throw new RuntimeException(e);
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
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

        btnMore.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                PopupMenu popup = new PopupMenu(CategoryActivity.this, v);
                popup.getMenuInflater().inflate(R.menu.category_options_menu, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                {
                    @Override
                    public boolean onMenuItemClick(MenuItem item)
                    {
                        int id = item.getItemId();

                        if (id == R.id.action_delete_items)
                        {
                            list.removeCategory(category);
                            runIfOnline(() -> dbHandler.removeCategory(list, category));
                            finish();
                            return true;
                        }

                        return false;
                    }
                });

                popup.show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void addItem(Item item)
    {
        if (addItemFragment != null)
        {
            addItemFragment.startProgressBar();
        }

        if (groups == null)
        {
            Toast.makeText(this, "Items are still loading…", Toast.LENGTH_SHORT).show();
            if (addItemFragment != null) addItemFragment.endProgressBar();
            return;
        }

        new Thread(() ->
        {
            String categoryName;

            try
            {
                categoryName = apiHandler.getItemCategory(item);
            }
            catch (IOException | JSONException e)
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


    private void showSortBottomSheet() throws JSONException, IOException
    {
        Map<String, ArrayList<PriceRow>> rows = apiHandler.buildRows(category.getRemainedItems());

        SortListBottomSheetBuilder.show(
                this,
                category,
                rows,
                apiHandler.getSupermarkets(),
                new SortListBottomSheetBuilder.ApplyListener()
                {
                    @Override
                    public void onApplyCheapest()
                    {
                        Map<String, ArrayList<PriceRow>> rows = apiHandler.buildRows(category.getRemainedItems());
                        category.setCheapestRows(rows);

                        tvTotal.setText(
                                Baskit.getTotalDisplayString(
                                        category.getTotal(),
                                        category.allPricesKnown(),
                                        true,
                                        true
                                )
                        );

                        runIfOnline(() -> dbHandler.updateCategory(list, category));
                        itemsAdapter.updateItems(new ArrayList<>(category.getItems()));
                    }

                    @Override
                    public void onApplySupermarket(Supermarket sm)
                    {
                        Map<String, ArrayList<PriceRow>> rows = apiHandler.buildRows(category.getRemainedItems());
                        category.setSupermarketsRows(sm, rows);

                        tvTotal.setText(
                                Baskit.getTotalDisplayString(
                                        category.getTotal(),
                                        category.allPricesKnown(),
                                        true,
                                        true
                                )
                        );

                        runIfOnline(() -> dbHandler.updateCategory(list, category));
                        itemsAdapter.updateItems(new ArrayList<>(category.getItems()));
                    }
                }
        );
    }
}