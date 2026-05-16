package com.example.baskit.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.OnlineComponents.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.Categories.CategoryActivity;
import com.example.baskit.OnlineComponents.FirebaseAuthHandler;
import com.example.baskit.OnlineComponents.FirebaseDBHandler;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.MainComponents.Item.ItemVariant;
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
    List list;
    String listId;

    boolean initialized = true;
    boolean listListenerAttached = false;
    boolean uiInitialized = false;

    Map<String, ArrayList<String>> groups;
    Map<String, Category> categories;

    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    APIHandler apiHandler = APIHandler.getInstance();
    FirebaseAuthHandler authHandler = FirebaseAuthHandler.getInstance();

    LayoutInflater categoriesListInflater;
    AddItemFragment addItemFragment;
    ShareListAlertDialog shareAlertDialog;

    TextView tvListName, tvTotal;
    ImageButton btnBack, btnFinished, btnMore;
    View shareListDot;
    RecyclerView categoriesRecycler;
    CategoryAdapter categoryAdapter;
    Button btnSortList;
    ImageButton btnAddItem, btnPlan, btnShare;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        createInit();
        uiInitialized = true;

        groups = apiHandler.getGroups();

        btnAddItem.setEnabled(groups != null && !groups.isEmpty());

        resumeInit();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Only attach if we never attached before
        if (uiInitialized && !listListenerAttached)
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

        categoriesRecycler = findViewById(R.id.categories_container);
        categoriesRecycler.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        categoriesListInflater = LayoutInflater.from(this);
        categoryAdapter = new CategoryAdapter(
                ListActivity.this,
                listId,
                new ArrayList<>()
        );
        categoriesRecycler.setAdapter(categoryAdapter);

        setButtons();
    }

    private void resumeInit()
    {
        if (listId == null) return;

        shareListDot.setVisibility(View.GONE);

        runWhenServerActive(() ->
        {
            if (!listListenerAttached)
            {
                listListenerAttached = true;

                dbHandler.listenToList(listId, new FirebaseDBHandler.GetListCallback()
                {
                    @Override
                    public void onListFetched(List newList)
                    {
                        refreshWithNewList(newList);
                    }

                    @Override
                    public void onError(String error)
                    {
                        initialized = false;
                    }
                });
            }
        });
    }

    private void setButtons()
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
                runWhenServerActive(() ->
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

                addItemFragment.updateData(list.toItemNames());
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
                    catch (JSONException | IOException e)
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
                                    runWhenServerActive(() -> dbHandler.renameList(list, newName));
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
                        }
                        else if (id == R.id.action_duplicate)
                        {
                            runWhenServerActive(() ->
                            {
                                authHandler.duplicateList(list, new FirebaseAuthHandler.CreateListCallback()
                                {
                                    @Override
                                    public void onSuccess(List newList)
                                    {
                                        runOnUiThread(() ->
                                                Toast.makeText(ListActivity.this,
                                                        "הרשימה שוכפלה",
                                                        Toast.LENGTH_SHORT).show()
                                        );
                                    }

                                    @Override
                                    public void onError(String message)
                                    {
                                        runOnUiThread(() ->
                                                Toast.makeText(ListActivity.this,
                                                        message,
                                                        Toast.LENGTH_SHORT).show()
                                        );
                                    }
                                });
                            });
                        }
                        else if (id == R.id.action_delete_items)
                        {
                            list.removeAllItems();

                            runWhenServerActive(() ->
                            {
                                dbHandler.removeItems(list);

                                runOnUiThread(() ->
                                        Toast.makeText(ListActivity.this,
                                                "כל הפריטים נמחקו",
                                                Toast.LENGTH_SHORT).show()
                                );
                            });
                        }
                        else if (id == R.id.action_delete_list)
                        {
                            runWhenServerActive(() -> dbHandler.removeList(listId));
                            authHandler.getUser().removeList(listId);
                            finish();
                        }
                        else if (id == R.id.action_leave_list)
                        {
                            runWhenServerActive(() ->
                            {
                                dbHandler.leaveList(list, authHandler.getUser());

                                runOnUiThread(() ->
                                        Toast.makeText(ListActivity.this,
                                                "עזבת את הרשימה",
                                                Toast.LENGTH_SHORT).show()
                                );
                            });

                            authHandler.getUser().removeList(listId);
                            finish();
                        }

                        return true;
                    }
                });

                popup.show();
            }
        });
    }

    private void refreshWithNewList(List newList)
    {
        ListActivity.this.list = newList;

        // The list is null
        if (newList == null)
        {
            Toast.makeText(this,
                    "הרשימה נמחקה",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // We have it from the add list
        if (ListActivity.this.list.getId() == null)
        {
            ListActivity.this.list.setId(listId);
        }

        tvListName.setText(ListActivity.this.list.getName());
        tvListName.setVisibility(View.VISIBLE);
        tvTotal.setText(Baskit.getTotalDisplayString(list.getTotal(), list.allPricesKnown(), true, true));
        tvTotal.setVisibility(View.VISIBLE);
        btnAddItem.setAlpha(1f);
        shareListDot.setVisibility(View.GONE);
        btnSortList.setEnabled(true);
        btnPlan.setEnabled(true);
        btnFinished.setEnabled(true);
        btnSortList.setAlpha(1f);
        btnPlan.setAlpha(1f);
        btnFinished.setAlpha(1f);

        if (addItemFragment == null)
        {
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
        }

        categories = ListActivity.this.list.getCategories();

        if (categories == null)
        {
            categories = new HashMap<>();
        }

        if (categoryAdapter == null)
        {
            categoryAdapter = new CategoryAdapter(
                    ListActivity.this,
                    listId,
                    new ArrayList<>(categories.values())
            );
            categoriesRecycler.setAdapter(categoryAdapter);
        }
        else
        {
            categoryAdapter.update(new ArrayList<>(categories.values()));
        }

        shareAlertDialog = new ShareListAlertDialog(list, ListActivity.this, ListActivity.this);

        if (!list.getRequests().isEmpty())
        {
            shareListDot.setVisibility(View.VISIBLE);
        }

        if (list.isEmpty())
        {
            btnSortList.setEnabled(false);
            btnPlan.setEnabled(false);
            btnFinished.setEnabled(false);

            btnSortList.setAlpha(0.5f);
            btnPlan.setAlpha(0.5f);
            btnFinished.setAlpha(0.5f);
        }
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

            runWhenServerActive(() ->
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
        Map<String, ArrayList<ItemVariant>> variants = apiHandler.buildVariants(list.getRemainedItems());

        SortListBottomSheetBuilder.show(
                this,
                list,
                variants,
                apiHandler.getSupermarkets(),
                new SortListBottomSheetBuilder.ApplyListener()
                {
                    @Override
                    public void onApplyCheapest()
                    {
                        Map<String, ArrayList<ItemVariant>> variants = apiHandler.buildVariants(list.getRemainedItems());
                        list.setCheapestVariants(variants);
                        tvTotal.setText(
                                Baskit.getTotalDisplayString(
                                        list.getTotal(),
                                        list.allPricesKnown(),
                                        true,
                                        true
                                )
                        );
                        runWhenServerActive(() -> dbHandler.updateList(list));
                    }

                    @Override
                    public void onApplySupermarket(Supermarket sm)
                    {
                        Map<String, ArrayList<ItemVariant>> variants = apiHandler.buildVariants(list.getRemainedItems());
                        list.setSupermarketsVariants(sm, variants);
                        tvTotal.setText(
                                Baskit.getTotalDisplayString(
                                        list.getTotal(),
                                        list.allPricesKnown(),
                                        true,
                                        true
                                )
                        );
                        runWhenServerActive(() -> dbHandler.updateList(list));
                    }
                }
        );
    }


    public class CategoryAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<CategoryAdapter.ViewHolder>
    {
        private final String listId;

        private ArrayList<Category> categories;

        private final Context context;

        CategoryAdapter(Context context, String listId, ArrayList<Category> categories)
        {
            this.context = context;
            this.listId = listId;
            this.categories = categories;
        }

        public class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder
        {
            TextView tvName, tvCount, tvPrice;
            LinearLayout loutInfo;

            ViewHolder(View v)
            {
                super(v);
                tvName = v.findViewById(R.id.tv_supermarket);
                tvCount = v.findViewById(R.id.tv_count);
                tvPrice = v.findViewById(R.id.tv_price);
                loutInfo = v.findViewById(R.id.lout_info);
            }
        }

        @NonNull
        @Override
        public CategoryAdapter.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType)
        {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_list_item, parent, false);
            return new CategoryAdapter.ViewHolder(v);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(CategoryAdapter.ViewHolder holder, int position)
        {
            Category category = categories.get(position);

            String text = "- " + category.getName();
            SpannableString spannable = new SpannableString(text);

            spannable.setSpan(
                    new ForegroundColorSpan(
                            Baskit.getAppColor(holder.tvName.getContext(), com.google.android.material.R.attr.colorSecondary)
                    ),
                    0, 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            spannable.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    0, 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            holder.tvName.setText(spannable);

            if (!category.isFinished())
            {
                holder.tvCount.setText(Integer.toString(category.countUnchecked()));
                holder.tvPrice.setText(Baskit.getTotalDisplayString(category.getTotal(), category.allPricesKnown(), false, false));
                holder.loutInfo.setVisibility(View.VISIBLE);
                holder.tvName.setAlpha(1f);
            }
            else
            {
                holder.loutInfo.setVisibility(View.GONE);
                holder.tvName.setAlpha(0.5f);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, CategoryActivity.class);
                intent.putExtra("listId", listId);
                intent.putExtra("categoryName", category.getName());
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount()
        {
            return categories == null ? 0 : categories.size();
        }

        void update(ArrayList<Category> newData)
        {
            this.categories = newData;
            notifyDataSetChanged();
        }
    }
}