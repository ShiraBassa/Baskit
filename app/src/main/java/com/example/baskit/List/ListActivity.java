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
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.Categories.CategoryActivity;
import com.example.baskit.Categories.ItemViewPricesAdapter;
import com.example.baskit.Firebase.FirebaseAuthHandler;
import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.ItemInfo;
import com.example.baskit.MainComponents.List;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.MasterActivity;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ListActivity extends MasterActivity
{
    TextView tvListName, tvTotal;
    ImageButton btnBack, btnFinished, btnMore;
    View shareListDot;

    List list;
    String listId;
    Map<String, Category> categories;

    LinearLayout categoriesListContainer;
    LayoutInflater categoriesListInflater;
    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    Map<String, View> categoriesViews;
    AddItemFragment addItemFragment;
    Button btnSortList;
    ImageButton btnAddItem, btnPlan, btnShare;
    APIHandler apiHandler = APIHandler.getInstance();

    Map<String, Map<String, Map<String, Double>>> allItemPrices;
    FirebaseAuthHandler authHandler = FirebaseAuthHandler.getInstance();
    Map<String, ArrayList<String>> groups;
    Map<String, ItemInfo> infos;
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
                allItemPrices = apiHandler.getItemPrices();
                groups = apiHandler.getGroups();
                infos = apiHandler.getItemInfos();
                itemsLoaded = true;
            }
            catch (Exception e)
            {
                Log.e("ListActivity", "Failed to load items catalogs", e);
                itemsLoaded = false;
            }

            runOnUiThread(() ->
            {
                if (groups != null && !groups.isEmpty())
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
        btnSortList = findViewById(R.id.btn_sort_list);
        btnPlan = findViewById(R.id.btn_plan);
        shareListDot = findViewById(R.id.share_list_dot);
        btnMore = findViewById(R.id.btn_more);

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

        shareListDot.setVisibility(View.GONE);

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
                    btnAddItem.setEnabled(true);
                    btnAddItem.setAlpha(1f);

                    if (groups != null && !groups.isEmpty())
                    {
                        addItemFragment = new AddItemFragment(
                                ListActivity.this,
                                ListActivity.this,
                                groups,
                                list.toItemNames(),
                                ListActivity.this::addItem,
                                list.getItemSuggestions()
                        );
                    }
                    else
                    {
                        btnAddItem.setAlpha(0.5f);
                    }

                    categories = ListActivity.this.list.getCategories();

                    if (categories == null)
                    {
                        categories = new HashMap<>();
                    }

                    shareAlertDialog = new ShareListAlertDialog(list, ListActivity.this, ListActivity.this);

                    if (!list.getRequests().isEmpty())
                    {
                        shareListDot.setVisibility(View.VISIBLE);
                    }

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

                                if (!list.getRequests().isEmpty())
                                {
                                    shareListDot.setVisibility(View.VISIBLE);
                                }
                                else
                                {
                                    shareListDot.setVisibility(View.GONE);
                                }

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
                if (groups == null || groups.isEmpty())
                {
                    Toast.makeText(ListActivity.this, "נא לבחור סופרים בהגדרות", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (addItemFragment == null)
                {
                    Toast.makeText(ListActivity.this, "עדיין טוען מסך זה...", Toast.LENGTH_SHORT).show();
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

        btnSortList.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!list.getRemainedItems().isEmpty())
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
                Intent intent = new Intent(ListActivity.this, PlanListActivity.class);
                intent.putExtra("listId", list.getId());
                startActivity(intent);
            }
        });

        btnMore.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                PopupMenu popup = new PopupMenu(ListActivity.this, v);
                popup.getMenuInflater().inflate(R.menu.list_options_menu, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                {
                    @Override
                    public boolean onMenuItemClick(MenuItem item)
                    {
                        int id = item.getItemId();

                        if (id == R.id.action_rename)
                        {
                            View dialogView = getLayoutInflater().inflate(R.layout.alert_dialog_rename_list, null);

                            androidx.appcompat.app.AlertDialog dialog =
                                    new androidx.appcompat.app.AlertDialog.Builder(ListActivity.this)
                                            .setView(dialogView)
                                            .create();

                            android.widget.EditText etName = dialogView.findViewById(R.id.et_name);
                            android.widget.ImageButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
                            com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_name);

                            etName.setText(list.getName());
                            etName.setSelection(etName.getText().length());

                            btnCancel.setOnClickListener(v1 -> dialog.dismiss());

                            btnSave.setOnClickListener(v12 ->
                            {
                                String newName = etName.getText().toString().trim();

                                if (!newName.isEmpty())
                                {
                                    list.setName(newName);
                                    runIfOnline(() -> dbHandler.renameList(list, newName));
                                    dialog.dismiss();
                                }
                                else
                                {
                                    Toast.makeText(ListActivity.this,
                                            "נא להזין שם",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });

                            dialog.show();
                            return true;
                        }
                        else if (id == R.id.action_duplicate)
                        {
                            runIfOnline(() ->
                            {
                                authHandler.duplicateList(list, new FirebaseAuthHandler.CreateListCallback()
                                {
                                    @Override
                                    public void onSuccess(List newList)
                                    {
                                        Toast.makeText(ListActivity.this,
                                                "הרשימה שוכפלה",
                                                Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onError(String message)
                                    {
                                        Toast.makeText(ListActivity.this,
                                                message,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                            });

                            return true;
                        }
                        else if (id == R.id.action_delete_items)
                        {
                            list.removeAllItems();
                            runIfOnline(() -> dbHandler.removeItems(list));
                            return true;
                        }
                        else if (id == R.id.action_delete_list)
                        {
                            runIfOnline(() -> dbHandler.removeList(listId));
                            authHandler.getUser().removeList(listId);
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

        tvName = categoryView.findViewById(R.id.tv_supermarket);
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
        if (groups == null)
        {
            if (addItemFragment != null) addItemFragment.endProgressBar();
            Toast.makeText(this, "Items are still loading…", Toast.LENGTH_SHORT).show();
            return;
        }

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
                runOnUiThread(() ->
                        Toast.makeText(ListActivity.this, "לא נמצאה קטגוריה לפריט", Toast.LENGTH_SHORT).show()
                );
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

                                addItemFragment = new AddItemFragment(
                                        ListActivity.this,
                                        ListActivity.this,
                                        groups,
                                        list.toItemNames(),
                                        ListActivity.this::addItem,
                                        list.getItemSuggestions()
                                );
                                addItemFragment.show(getSupportFragmentManager(), "AddItemFragment");
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
        }).start();
    }


    private void showSortBottomSheet() throws JSONException, IOException
    {
        Map<String, ArrayList<com.example.baskit.Categories.ItemViewPricesAdapter.PriceRow>> rows = apiHandler.buildRows(list.getRemainedItems());

        SortListBottomSheetBuilder.show(
                this,
                list,
                rows,
                apiHandler.getSupermarkets(),
                new SortListBottomSheetBuilder.ApplyListener()
                {
                    @Override
                    public void onApplyCheapest()
                    {
                        Map<String, ArrayList<com.example.baskit.Categories.ItemViewPricesAdapter.PriceRow>> rows = apiHandler.buildRows(list.getRemainedItems());
                        list.setCheapestRows(rows);
                        tvTotal.setText(
                                Baskit.getTotalDisplayString(
                                        list.getTotal(),
                                        list.allPricesKnown(),
                                        true,
                                        true
                                )
                        );
                        runIfOnline(() -> dbHandler.updateList(list));
                    }

                    @Override
                    public void onApplySupermarket(Supermarket sm)
                    {
                        Map<String, ArrayList<com.example.baskit.Categories.ItemViewPricesAdapter.PriceRow>> rows = apiHandler.buildRows(list.getRemainedItems());
                        list.setSupermarketsRows(sm, rows);
                        tvTotal.setText(
                                Baskit.getTotalDisplayString(
                                        list.getTotal(),
                                        list.allPricesKnown(),
                                        true,
                                        true
                                )
                        );
                        runIfOnline(() -> dbHandler.updateList(list));
                    }
                }
        );
    }
}