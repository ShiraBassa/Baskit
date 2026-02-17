package com.example.baskit.List;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.baskit.AI.AIHandler;
import com.example.baskit.API.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.Categories.CategoryActivity;
import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.MasterActivity;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ListActivity extends MasterActivity
{
    TextView tvListName, tvTotal;
    ImageButton btnBack, btnFinished;

    List list;
    String listId;
    Map<String, Category> categories;

    LinearLayout categoriesListContainer;
    LayoutInflater categoriesListInflater;
    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    Map<String, View> categoriesViews;
    AddItemFragment addItemFragment;
    Button btnAddItem, btnCheapest;;
    ImageButton btnShare;
    AIHandler aiHandler = AIHandler.getInstance();
    APIHandler apiHandler = APIHandler.getInstance();

    Map<String, Map<String, Map<String, Double>>> allItems;
    Map<String, String> itemsCodeNames;
    private boolean itemsLoaded = false;
    private boolean initialized = true;
    private boolean listListenerAttached = false;
    private boolean uiInitialized = false;
    ShareListAlertDialog shareAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list);

        createInit();
        uiInitialized = true;

        runWhenServerActive(() ->
        {
            try
            {
                allItems = apiHandler.getItems();
                itemsCodeNames = apiHandler.getItemsCodeName();
                itemsLoaded = true;
            }
            catch (Exception e)
            {
                Log.e("ListActivity", "Failed to load items catalogs", e);
                itemsLoaded = false;
            }

            runOnUiThread(() ->
            {
                if (itemsCodeNames != null && itemsCodeNames.values() != null)
                {
                    btnAddItem.setEnabled(true);
                }
                else
                {
                    btnAddItem.setEnabled(false);
                }

                resumeInit();
            });
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Only attach if we never attached before.
        if (itemsLoaded && uiInitialized && !listListenerAttached)
        {
            resumeInit();
        }

        if (!initialized && tvTotal != null && list != null)
        {
            tvTotal.setText(Baskit.getTotalDisplayString(list.getTotal(), list.allPricesKnown(), true, true));
            tvTotal.setVisibility(View.VISIBLE);
        }
    }

    private void createInit()
    {
        tvListName = findViewById(R.id.tv_list_name);
        btnBack = findViewById(R.id.btn_back);
        btnFinished = findViewById(R.id.btn_finished);
        btnAddItem = findViewById(R.id.btn_add_item);
        tvTotal = findViewById(R.id.tv_total);
        btnShare = findViewById(R.id.btn_share);
        btnCheapest = findViewById(R.id.btn_arrange_cheapest);

        btnAddItem.setEnabled(false);

        listId = getIntent().getStringExtra("listId");

        if (listId == null)
        {
            Toast.makeText(this, "Missing list id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        categoriesListContainer = findViewById(R.id.categories_container);
        categoriesListInflater = LayoutInflater.from(this);

        setButton();
    }

    private void resumeInit()
    {
        if (listId == null) return;

        runIfOnline(() ->
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

                    if (ListActivity.this.list.getId() == null)
                    {
                        ListActivity.this.list.setId(listId);
                    }

                    tvListName.setText(ListActivity.this.list.getName());
                    tvListName.setVisibility(View.VISIBLE);
                    tvTotal.setText(Baskit.getTotalDisplayString(list.getTotal(), list.allPricesKnown(), true, true));
                    tvTotal.setVisibility(View.VISIBLE);

                    if (itemsCodeNames != null && itemsCodeNames.values() != null)
                    {
                        addItemFragment = new AddItemFragment(ListActivity.this,
                                ListActivity.this,
                                new ArrayList<>(itemsCodeNames.values()),
                                list.toItemNames(),
                                ListActivity.this::addItem);
                        btnAddItem.setEnabled(true);
                    }
                    else
                    {
                        btnAddItem.setEnabled(false);
                    }

                    categories = ListActivity.this.list.getCategories();

                    if (categories == null)
                    {
                        categories = new HashMap<>();
                    }

                    shareAlertDialog = new ShareListAlertDialog(list, ListActivity.this, ListActivity.this);

                    setCategoriesInflater();

                    if (!listListenerAttached)
                    {
                        listListenerAttached = true;

                        dbHandler.listenToList(listId, new FirebaseDBHandler.GetListCallback()
                        {
                            @Override
                            public void onListFetched(List newList)
                            {
                                if (newList == null) return;

                                ListActivity.this.list = newList;
                                shareAlertDialog = new ShareListAlertDialog(newList, ListActivity.this, ListActivity.this);

                                if (tvListName != null)
                                {
                                    tvListName.setText(newList.getName());
                                    tvListName.setVisibility(View.VISIBLE);
                                }

                                if (tvTotal != null)
                                {
                                    tvTotal.setText(Baskit.getTotalDisplayString(newList.getTotal(), newList.allPricesKnown(), true, true));
                                    tvTotal.setVisibility(View.VISIBLE);
                                }

                                Map<String, Category> newCategories = newList.getCategories();
                                if (newCategories == null) newCategories = new HashMap<>();

                                if (categoriesViews == null)
                                {
                                    categories = new HashMap<>(newCategories);
                                    setCategoriesInflater();
                                }
                                else
                                {
                                    updateCategoriesInflater(newCategories);
                                }

                                if (addItemFragment != null)
                                {
                                    addItemFragment.updateData(newList.toItemNames());
                                }
                            }

                            @Override
                            public void onError(String error)
                            {
                                initialized = false;
                            }
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
                runIfOnline(() ->
                {
                    dbHandler.finishList(list);
                });
            }
        });

        btnAddItem.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (addItemFragment == null)
                {
                    Toast.makeText(ListActivity.this, "Items are still loading…", Toast.LENGTH_SHORT).show();
                    return;
                }

                addItemFragment.show(getSupportFragmentManager(), "AddItemFragment");
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (shareAlertDialog != null)
                {
                    shareAlertDialog.show();
                }
                else
                {
                    Toast.makeText(ListActivity.this, "Still loading…", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCheapest.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                setCheapest();
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
        for (String key : new ArrayList<>(categories.keySet()))
        {
            if (!newCategories.containsKey(key))
            {
                View toRemove = categoriesViews.get(key);
                if (toRemove != null) categoriesListContainer.removeView(toRemove);
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
                    TextView tvCount = categoryView.findViewById(R.id.tv_count);
                    TextView tvPrice = categoryView.findViewById(R.id.tv_price);

                    tvCount.setText(Integer.toString(newCategory.countUnchecked()));
                    tvPrice.setText(Baskit.getTotalDisplayString(newCategory.getTotal(), newCategory.allPricesKnown(), false, false));
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
        TextView tvName, tvCount, tvPrice;
        LinearLayout loutInfo;

        categoryView = categoriesListInflater.inflate(R.layout.category_list_item, categoriesListContainer, false);

        tvName = categoryView.findViewById(R.id.tv_section_name);
        tvCount  = categoryView.findViewById(R.id.tv_count);
        tvPrice = categoryView.findViewById(R.id.tv_price);
        loutInfo = categoryView.findViewById(R.id.lout_info);

        String text = "- " + category.getName();
        SpannableString spannable = new SpannableString(text);

        spannable.setSpan(
                new ForegroundColorSpan(
                        Baskit.getAppColor(tvName.getContext(), com.google.android.material.R.attr.colorSecondary)
                ),
                0, 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        spannable.setSpan(
                new StyleSpan(Typeface.BOLD),
                0, 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        tvName.setText(spannable);

        if (!category.isFinished())
        {
            tvCount.setText(Integer.toString(category.countUnchecked()));
            tvPrice.setText(Baskit.getTotalDisplayString(category.getTotal(), category.allPricesKnown(), false, false));
        }
        else
        {
            loutInfo.setVisibility(View.GONE);
            tvName.setAlpha(0.5f);
        }

        categoryView.setOnClickListener(new View.OnClickListener()
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
        if (itemsCodeNames == null)
        {
            Toast.makeText(this, "Items are still loading…", Toast.LENGTH_SHORT).show();
            return;
        }

        item.updateId(getKeyByValue(itemsCodeNames, item.getName()));

        String categoryName = apiHandler.getItemCategoryDB(item.getAbsoluteId());

        if (categoryName == null || categoryName.isEmpty())
        {
            runIfOnline(() ->
            {
                aiHandler.getItemCategoryAI(item, ListActivity.this, aiCategoryName ->
                {
                    if (aiCategoryName == null || aiCategoryName.isEmpty())
                    {
                        runOnUiThread(() ->
                                Toast.makeText(ListActivity.this, "לא נמצאה קטגוריה לפריט", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    runIfOnline(() ->
                    {
                        if (!list.hasCategory(aiCategoryName))
                        {
                            dbHandler.addCategory(list, new Category(aiCategoryName));
                        }

                        dbHandler.addItem(list, aiCategoryName, item, new FirebaseDBHandler.DBCallback()
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
                                    Toast.makeText(ListActivity.this, "Failed to add item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    });
                });
            });

            return;
        }

        runIfOnline(() ->
        {
            if (!list.hasCategory(categoryName))
            {
                dbHandler.addCategory(list, new Category(categoryName));
            }

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

    private void setCheapest()
    {
        if (allItems == null)
        {
            Toast.makeText(this, "Prices are still loading…", Toast.LENGTH_SHORT).show();
            return;
        }

        if (list == null || list.getCategories() == null)
        {
            Toast.makeText(this, "List is not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        double lowest;
        Supermarket lowestSupermarket;

        if (list == null || list.getCategories() == null) {
            Toast.makeText(this, "List is not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Category category : list.getCategories().values())
        {
            if (category == null || category.getItems() == null) continue;

            for (Item item : category.getItems().values())
            {
                if (item == null) continue;
                if (item.isChecked())
                {
                    continue;
                }

                String absId = item.getAbsoluteId();
                Map<String, Map<String, Double>> currItemPrices = (absId == null) ? null : allItems.get(absId);

                if (currItemPrices == null || currItemPrices.isEmpty())
                {
                    lowest = 0.0;
                    lowestSupermarket = Baskit.unassigned_supermarket;
                }
                else
                {
                    lowest = Double.MAX_VALUE;
                    lowestSupermarket = null;

                    for (Map.Entry<String, Map<String, Double>> SupermarketEntry : currItemPrices.entrySet())
                    {
                        String supermarket = SupermarketEntry.getKey();

                        Map<String, Double> sectionMap = SupermarketEntry.getValue();
                        if (sectionMap == null) continue;

                        for (Map.Entry<String, Double> sectionEntry : sectionMap.entrySet())
                        {
                            String section = sectionEntry.getKey();
                            Double price = sectionEntry.getValue();

                            if (price < lowest)
                            {
                                lowest = price;
                                lowestSupermarket = new Supermarket(supermarket, section);
                            }
                        }
                    }
                }

                item.setPrice(lowest);
                item.setSupermarket(lowestSupermarket);
            }
        }

        runIfOnline(() ->
        {
            dbHandler.updateList(list);
        });
    }
}