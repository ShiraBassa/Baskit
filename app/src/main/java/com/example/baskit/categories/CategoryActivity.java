package com.example.baskit.categories;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.online_components.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.online_components.FirebaseAuthHandler;
import com.example.baskit.online_components.FirebaseDBHandler;
import com.example.baskit.list.AddItemFragment;
import com.example.baskit.list.PlanListActivity;
import com.example.baskit.list.SortListBottomSheetBuilder;
import com.example.baskit.main_components.Category;
import com.example.baskit.main_components.Item;
import com.example.baskit.main_components.List;
import com.example.baskit.main_components.Item.ItemVariant;
import com.example.baskit.main_components.Supermarket;
import com.example.baskit.MasterActivity;
import com.example.baskit.R;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class CategoryActivity extends MasterActivity
{
    List list;
    Category category;

    Map<String, ArrayList<String>> groups;
    ArrayList<Supermarket> supermarkets;

    final FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    final APIHandler apiHandler = APIHandler.getInstance();
    final FirebaseAuthHandler authHandler = FirebaseAuthHandler.getInstance();

    boolean initialized = true;

    ValueEventListener listListener;
    ValueEventListener categoryListener;

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
            Toast.makeText(this, Baskit.getAppStr(R.string.msg_general_error), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        groups = apiHandler.getGroups();
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
                listListener = dbHandler.listenToList(listId, new FirebaseDBHandler.GetListCallback()
                {
                    @Override
                    public void onListFetched(List newList)
                    {
                        if (isFinishing() || isDestroyed())
                        {
                            return;
                        }
                        if (newList == null)
                        {
                            if (!isFinishing())
                            {
                                finish();
                            }
                            return;
                        }

                        // Current user no longer belongs to this list
                        if (authHandler.getUser() != null &&
                                authHandler.getUser().getId() != null &&
                                !newList.hasUser(authHandler.getUser().getId()))
                        {
                            finish();
                            return;
                        }

                        list = newList;

                        runOnUiThread(() ->
                        {
                            tvListName.setText(list.getName());
                            tvListName.setVisibility(View.VISIBLE);
                        });

                        if (categoryListener != null)
                        {
                            dbHandler.removeCategoryListener(listId, categoryName, categoryListener);
                        }

                        categoryListener = dbHandler.listenToCategory(list, categoryName, newCategory ->
                        {
                            if (isFinishing() || isDestroyed())
                            {
                                return;
                            }
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

                            runOnUiThread(() ->
                            {
                                if (isFinishing() || isDestroyed())
                                {
                                    return;
                                }
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
                                                    runProtectedRequest(
                                                            "update_item_category_" + item.getAbsoluteId(),
                                                            null,
                                                            () ->
                                                            {
                                                                category.removeVariants(item.getBaseName());
                                                                category.addItem(item);
                                                                dbHandler.updateCategory(list, category);
                                                            }
                                                    );
                                                }

                                                @Override
                                                public void removeItemCategory(Item item)
                                                {
                                                    if (category == null) return;

                                                    runProtectedRequest(
                                                            "remove_item_" + item.getAbsoluteId(),
                                                            null,
                                                            () -> dbHandler.removeItem(list, category, item)
                                                    );
                                                }

                                                @Override
                                                public void updateCategory()
                                                {
                                                    if (category == null) return;

                                                    runProtectedRequest(
                                                            "update_category_" + category.getName(),
                                                            null,
                                                            () -> dbHandler.updateCategory(list, category)
                                                    );
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
                        });
                    }

                    @Override
                    public void onError()
                    {
                        initialized = false;
                    }
                }));
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        String listId = getIntent().getStringExtra("listId");
        String categoryName = getIntent().getStringExtra("categoryName");

        if (listListener != null && listId != null)
        {
            dbHandler.removeListListener(listId, listListener);
            listListener = null;
        }

        if (categoryListener != null &&
                listId != null &&
                categoryName != null)
        {
            dbHandler.removeCategoryListener(listId, categoryName, categoryListener);
            categoryListener = null;
        }
    }

    private void setButtons()
    {
        btnFinished.setOnClickListener(view ->
                runProtectedRequest(
                        "finish_category_" + (category != null ? category.getName() : "null"),
                        btnFinished,
                        () -> dbHandler.finishCategory(list, category)
                ));

        btnBack.setOnClickListener(view -> finish());

        btnAddItem.setOnClickListener(view ->
        {
            if (addItemFragment != null)
            {
                addItemFragment.updateData(list.toItemNames());
                addItemFragment.show(getSupportFragmentManager(), "AddItemFragment");
            }
            else
            {
                Toast.makeText(CategoryActivity.this, Baskit.getAppStr(R.string.msg_loading), Toast.LENGTH_SHORT).show();
            }
        });

        btnSortList.setOnClickListener(view -> {
            if (!category.getRemainedItems().isEmpty())
            {
                try
                {
                    showSortBottomSheet();
                }
                catch (JSONException | IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        });

        btnPlan.setOnClickListener(v -> {
            Intent intent = new Intent(CategoryActivity.this, PlanListActivity.class);
            intent.putExtra("listId", list.getId());
            intent.putExtra("category", category.getName());
            startActivity(intent);
        });

        btnMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(CategoryActivity.this, v);
            popup.getMenuInflater().inflate(R.menu.category_options_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if (id == R.id.action_delete_items)
                {
                    list.removeCategory(category);
                    runProtectedRequest(
                            "remove_category_" + (category != null ? category.getName() : "null"),
                            btnMore,
                            () -> dbHandler.removeCategory(list, category)
                    );

                    finish();
                    return true;
                }

                return false;
            });

            popup.show();
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
            Toast.makeText(this, Baskit.getAppStr(R.string.msg_loading), Toast.LENGTH_SHORT).show();
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

            if (!list.hasCategory(categoryName))
            {
                runWhenServerActive(() -> dbHandler.addCategory(list, new Category(categoryName)));
            }

            runProtectedRequest(
                    "add_item_" + item.getAbsoluteId(),
                    btnAddItem,
                    () -> dbHandler.addItem(list, categoryName, item, new FirebaseDBHandler.DBCallback()
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
                                            Baskit.getAppStr(R.string.msg_added_to_category) + categoryName,
                                            Snackbar.LENGTH_LONG
                                    );

                                    snackbar.getView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

                                    snackbar.setAction(Baskit.getAppStr(R.string.action_cancel), v ->
                                                    runProtectedRequest(
                                                            "undo_add_item_" + item.getAbsoluteId(),
                                                            null,
                                                            () -> dbHandler.removeItem(list, categoryName, item)
                                                    ));

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
                                Toast.makeText(CategoryActivity.this, Baskit.getAppStr(R.string.msg_general_error), Toast.LENGTH_SHORT).show();
                            });
                        }
                    })
            );
        }).start();
    }


    private void showSortBottomSheet() throws JSONException, IOException
    {
        Map<String, ArrayList<ItemVariant>> variants = apiHandler.buildVariants(category.getRemainedItems());

        SortListBottomSheetBuilder.show(
                this,
                category,
                variants,
                apiHandler.getSupermarkets(),
                new SortListBottomSheetBuilder.ApplyListener()
                {
                    @Override
                    public void onApplyCheapest()
                    {
                        Map<String, ArrayList<ItemVariant>> variants = apiHandler.buildVariants(category.getRemainedItems());
                        category.setCheapestVariants(variants);

                        tvTotal.setText(
                                Baskit.getTotalDisplayString(
                                        category.getTotal(),
                                        category.allPricesKnown(),
                                        true,
                                        true
                                )
                        );

                        runProtectedRequest(
                                "update_category_cheapest_" + category.getName(),
                                btnSortList,
                                () -> dbHandler.updateCategory(list, category)
                        );
                        itemsAdapter.updateItems(new ArrayList<>(category.getItems()));
                    }

                    @Override
                    public void onApplySupermarket(Supermarket sm)
                    {
                        Map<String, ArrayList<ItemVariant>> variants = apiHandler.buildVariants(category.getRemainedItems());
                        category.setSupermarketsVariants(sm, variants);

                        tvTotal.setText(
                                Baskit.getTotalDisplayString(
                                        category.getTotal(),
                                        category.allPricesKnown(),
                                        true,
                                        true
                                )
                        );

                        runProtectedRequest(
                                "update_category_supermarket_" + category.getName(),
                                btnSortList,
                                () -> dbHandler.updateCategory(list, category)
                        );
                        itemsAdapter.updateItems(new ArrayList<>(category.getItems()));
                    }
                }
        );
    }
}