package com.example.baskit.Categories;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.OnlineComponents.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.OnlineComponents.FirebaseDBHandler;
import com.example.baskit.List.AddItemFragment;
import com.example.baskit.List.PlanListActivity;
import com.example.baskit.List.SortListBottomSheetBuilder;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.Item.ItemInfo;
import com.example.baskit.MainComponents.List;
import com.example.baskit.MainComponents.Item.ItemVariant;
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

    Map<String, ArrayList<String>> groups;
    Map<String, ItemInfo> infos;
    ArrayList<Supermarket> supermarkets;

    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    APIHandler apiHandler = APIHandler.getInstance();

    boolean initialized = true;

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

        groups = apiHandler.getGroups();
        infos = apiHandler.getItemInfos();
        supermarkets = apiHandler.getSupermarkets();

        init();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (!initialized && tvTotal != null && category != null)
        {
            tvTotal.setText(Baskit.getTotalDisplayString(category.getTotal(), category.allPricesKnown(), true, false));
            tvTotal.setVisibility(View.VISIBLE);
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

        recyclerItems = findViewById(R.id.recycler_supermarket_items);
        recyclerItems.setLayoutManager(new LinearLayoutManager(CategoryActivity.this));

        setButtons();

        final String listId = getIntent().getStringExtra("listId");
        final String categoryName = getIntent().getStringExtra("categoryName");

        runWhenServerActive(() ->
        {
            dbHandler.getList(listId, new FirebaseDBHandler.GetListCallback()
            {
                @Override
                public void onListFetched(List newList)
                {
                    CategoryActivity.this.list = newList;

                    if (newList == null)
                    {
                        finish();
                        return;
                    }

                    runWhenServerActive(() ->
                    {
                        dbHandler.listenToList(listId, new FirebaseDBHandler.GetListCallback()
                        {
                            @Override
                            public void onListFetched(List newList)
                            {
                                if (newList == null)
                                {
                                    finish();
                                    return;
                                }

                                list = newList;
                                tvListName.setText(list.getName());
                                tvListName.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onError(String error) {}
                        });

                        dbHandler.listenToCategory(list, categoryName, new FirebaseDBHandler.GetCategoryCallback()
                        {
                            @Override
                            public void onCategoryFetched(Category newCategory)
                            {
                                category = newCategory;

                                if (category == null || category.getItems() == null || category.getItems().isEmpty())
                                {
                                    if (!isFinishing())
                                    {
                                        finish();
                                    }
                                    return;
                                }

                                list.updateCategory(category);
                                tvCategoryName.setText(category.getName());
                                tvCategoryName.setVisibility(View.VISIBLE);
                                btnAddItem.setEnabled(true);
                                tvTotal.setText(Baskit.getTotalDisplayString(category.getTotal(), category.allPricesKnown(), true, false));
                                tvTotal.setVisibility(View.VISIBLE);
                                btnSortList.setEnabled(true);

                                if (addItemFragment == null)
                                {
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
                                    }
                                    else
                                    {
                                        btnAddItem.setEnabled(false);
                                    }
                                }

                                runOnUiThread(() ->
                                {
                                    if (itemsAdapter == null)
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
                                                        runWhenServerActive(() ->
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
                                                        runWhenServerActive(() -> dbHandler.removeItem(list, category, item));
                                                    }

                                                    @Override
                                                    public void updateCategory()
                                                    {
                                                        if (category == null) return;
                                                        runWhenServerActive(() -> dbHandler.updateCategory(list, category));
                                                    }

                                                    @Override
                                                    public void removeCategory()
                                                    {
                                                        runWhenServerActive(() ->
                                                        {
                                                            dbHandler.removeCategory(list, category);
                                                            finish();
                                                        });
                                                    }
                                                },
                                                supermarkets
                                        );

                                        recyclerItems.setAdapter(itemsAdapter);
                                    }
                                    else
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
                runWhenServerActive(() -> dbHandler.finishCategory(list, category));
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
                            runWhenServerActive(() -> dbHandler.removeCategory(list, category));
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
                runWhenServerActive(() -> dbHandler.addCategory(list, new Category(categoryName)));
            }

            runWhenServerActive(() ->
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
                                    runWhenServerActive(() -> dbHandler.removeItem(list, categoryName, item));
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
        Map<String, ArrayList<ItemVariant>> rows = apiHandler.buildVariants(category.getRemainedItems());

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
                        Map<String, ArrayList<ItemVariant>> rows = apiHandler.buildVariants(category.getRemainedItems());
                        category.setCheapestVariants(rows);

                        tvTotal.setText(
                                Baskit.getTotalDisplayString(
                                        category.getTotal(),
                                        category.allPricesKnown(),
                                        true,
                                        true
                                )
                        );

                        runWhenServerActive(() -> dbHandler.updateCategory(list, category));
                        itemsAdapter.updateItems(new ArrayList<>(category.getItems()));
                    }

                    @Override
                    public void onApplySupermarket(Supermarket sm)
                    {
                        Map<String, ArrayList<ItemVariant>> rows = apiHandler.buildVariants(category.getRemainedItems());
                        category.setSupermarketsVariants(sm, rows);

                        tvTotal.setText(
                                Baskit.getTotalDisplayString(
                                        category.getTotal(),
                                        category.allPricesKnown(),
                                        true,
                                        true
                                )
                        );

                        runWhenServerActive(() -> dbHandler.updateCategory(list, category));
                        itemsAdapter.updateItems(new ArrayList<>(category.getItems()));
                    }
                }
        );
    }
}