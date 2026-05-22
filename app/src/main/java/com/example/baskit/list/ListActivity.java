package com.example.baskit.list;

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
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.online_components.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.categories.CategoryActivity;
import com.example.baskit.online_components.FirebaseAuthHandler;
import com.example.baskit.online_components.FirebaseDBHandler;
import com.example.baskit.main_components.Category;
import com.example.baskit.main_components.Item;
import com.example.baskit.main_components.List;
import com.example.baskit.main_components.Item.ItemVariant;
import com.example.baskit.main_components.Supermarket;
import com.google.firebase.database.ValueEventListener;
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

    ValueEventListener listListener;

    Map<String, ArrayList<String>> groups;
    Map<String, Category> categories;

    final FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    final APIHandler apiHandler = APIHandler.getInstance();
    final FirebaseAuthHandler authHandler = FirebaseAuthHandler.getInstance();

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

        if (!initialized && !isFinishing() && !isDestroyed() && tvTotal != null && list != null)
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
            Toast.makeText(this, Baskit.getAppStr(R.string.msg_general_error), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        categoriesRecycler = findViewById(R.id.categories_container);
        categoriesRecycler.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
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
            if (isFinishing() || isDestroyed())
            {
                return;
            }
            if (!listListenerAttached)
            {
                listListenerAttached = true;
                listListener = dbHandler.listenToList(listId, new FirebaseDBHandler.GetListCallback()
                {
                    @Override
                    public void onListFetched(List newList)
                    {
                        if (isFinishing() || isDestroyed())
                        {
                            return;
                        }
                        refreshWithNewList(newList);
                    }

                    @Override
                    public void onError()
                    {
                        initialized = false;
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (listListener != null && listId != null)
        {
            dbHandler.removeListListener(listId, listListener);
            listListener = null;
        }

        listListenerAttached = false;
    }

    private void setButtons()
    {
        btnBack.setOnClickListener(view ->
                runProtectedRequest(
                        "close_list",
                        btnBack,
                        () -> runOnUiThread(this::finish)
                ));

        btnFinished.setOnClickListener(view ->
        {
            if (list == null)
            {
                return;
            }

            runProtectedRequest(
                    "finish_list_" + list.getId(),
                    btnFinished,
                    () -> dbHandler.finishList(list)
            );
        });

        btnAddItem.setOnClickListener(view ->
        {
            if (addItemFragment == null)
            {
                Toast.makeText(ListActivity.this, Baskit.getAppStr(R.string.msg_loading), Toast.LENGTH_SHORT).show();
                return;
            }

            if (list == null)
            {
                Toast.makeText(ListActivity.this, Baskit.getAppStr(R.string.msg_loading), Toast.LENGTH_SHORT).show();
                return;
            }

            addItemFragment.updateData(list.toItemNames());
            addItemFragment.show(getSupportFragmentManager(), "AddItemFragment");
        });

        btnShare.setOnClickListener(v ->
        {
            if (shareAlertDialog == null)
            {
                Toast.makeText(ListActivity.this, Baskit.getAppStr(R.string.msg_loading), Toast.LENGTH_SHORT).show();
                return;
            }

            if (isFinishing() || isDestroyed())
            {
                return;
            }

            shareAlertDialog.show();
        });

        btnSortList.setOnClickListener(v ->
        {
            if (list == null || list.getRemainedItems().isEmpty())
            {
                return;
            }

            runProtectedRequest(
                    "sort_list_preview_" + list.getId(),
                    btnSortList,
                    () ->
                    {
                        try
                        {
                            runOnUiThread(() ->
                            {
                                if (isFinishing() || isDestroyed())
                                {
                                    return;
                                }

                                try
                                {
                                    showSortBottomSheet();
                                }
                                catch (JSONException | IOException e)
                                {
                                    android.util.Log.e("ListActivity", "Failed showing sort sheet", e);

                                    Toast.makeText(
                                            ListActivity.this,
                                            Baskit.getAppStr(R.string.msg_general_error),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            });
                        }
                        catch (Exception e)
                        {
                            android.util.Log.e("ListActivity", "Sort request failed", e);
                        }
                    }
            );
        });

        btnPlan.setOnClickListener(v ->
        {
            if (list == null || list.getId() == null)
            {
                return;
            }

            runProtectedRequest(
                    "open_plan_" + list.getId(),
                    btnPlan,
                    () -> runOnUiThread(() ->
                    {
                        if (isFinishing() || isDestroyed())
                        {
                            return;
                        }

                        Intent intent = new Intent(ListActivity.this, PlanListActivity.class);
                        intent.putExtra("listId", list.getId());
                        startActivity(intent);
                    })
            );
        });

        btnMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(ListActivity.this, v);
            popup.getMenuInflater().inflate(R.menu.list_options_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if (id == R.id.action_rename)
                {
                    View dialogView = getLayoutInflater().inflate(R.layout.alert_dialog_rename_list, null);

                    androidx.appcompat.app.AlertDialog dialog =
                            new androidx.appcompat.app.AlertDialog.Builder(ListActivity.this)
                                    .setView(dialogView)
                                    .create();

                    android.widget.EditText etName = dialogView.findViewById(R.id.et_name);
                    ImageButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
                    com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_name);

                    etName.setText(list.getName());
                    etName.setSelection(etName.getText().length());

                    btnCancel.setOnClickListener(v1 -> dialog.dismiss());

                    btnSave.setOnClickListener(v12 ->
                    {
                        String newName = etName.getText().toString().trim();

                        if (!newName.isEmpty())
                        {
                            runProtectedRequest(
                                    "rename_list_" + list.getId(),
                                    btnSave,
                                    () ->
                                    {
                                        list.setName(newName);
                                        dbHandler.renameList(list, newName);

                                        runOnUiThread(dialog::dismiss);
                                    }
                            );
                        }
                        else
                        {
                            Toast.makeText(ListActivity.this,
                                    Baskit.getAppStr(R.string.msg_enter_name),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                    dialog.show();
                }
                else if (id == R.id.action_duplicate)
                {
                    runProtectedRequest(
                            "duplicate_list_" + list.getId(),
                            null,
                            () -> authHandler.duplicateList(list, newList ->
                                    runOnUiThread(() ->
                                    {
                                        if (isFinishing() || isDestroyed())
                                        {
                                            return;
                                        }

                                        Toast.makeText(
                                                ListActivity.this,
                                                Baskit.getAppStr(R.string.msg_list_duplicated),
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }))
                    );
                }
                else if (id == R.id.action_delete_items)
                {
                    runProtectedRequest(
                            "remove_items_" + list.getId(),
                            null,
                            () ->
                            {
                                list.removeAllItems();
                                dbHandler.removeItems(list);
                            }
                    );
                }
                else if (id == R.id.action_delete_list)
                {
                    runProtectedRequest(
                            "delete_list_" + listId,
                            null,
                            () ->
                            {
                                dbHandler.removeList(listId);

                                if (authHandler.getUser() != null)
                                {
                                    authHandler.getUser().removeList(listId);
                                }

                                runOnUiThread(() ->
                                {
                                    if (!isFinishing() && !isDestroyed())
                                    {
                                        finish();
                                    }
                                });
                            }
                    );
                }
                else if (id == R.id.action_leave_list)
                {
                    runProtectedRequest(
                            "leave_list_" + listId,
                            null,
                            () ->
                            {
                                dbHandler.leaveList(list, authHandler.getUser());

                                if (authHandler.getUser() != null)
                                {
                                    authHandler.getUser().removeList(listId);
                                }

                                runOnUiThread(() ->
                                {
                                    if (!isFinishing() && !isDestroyed())
                                    {
                                        finish();
                                    }
                                });
                            }
                    );
                }

                return true;
            });

            popup.show();
        });
    }

    private void refreshWithNewList(List newList)
    {
        if (isFinishing() || isDestroyed())
        {
            return;
        }

        ListActivity.this.list = newList;

        // The list is null
        if (newList == null)
        {
            finish();
            return;
        }

        // We have it from the add list
        if (ListActivity.this.list.getId() == null)
        {
            ListActivity.this.list.setId(listId);
        }

        if (tvListName == null || tvTotal == null)
        {
            return;
        }
        tvListName.setText(
                ListActivity.this.list.getName() != null && !ListActivity.this.list.getName().isBlank()
                        ? ListActivity.this.list.getName()
                        : Baskit.getAppStr(R.string.unnamed_list)
        );
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
        btnFinished.setImageAlpha(255);
        btnPlan.setImageAlpha(255);

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
            btnFinished.setImageAlpha(120);
            btnPlan.setImageAlpha(120);
        }
    }

    public void addItem(Item item)
    {
        if (item == null || list == null)
        {
            return;
        }

        if (groups == null)
        {
            if (addItemFragment != null) addItemFragment.endProgressBar();
            Toast.makeText(this, Baskit.getAppStr(R.string.msg_loading), Toast.LENGTH_SHORT).show();
            return;
        }

        Thread addItemThread = new Thread(() ->
        {
            String categoryName;

            try
            {
                categoryName = apiHandler.getItemCategory(item);

                if (categoryName != null)
                {
                    categoryName = categoryName.trim();
                }
            }
            catch (IOException | JSONException e)
            {
                android.util.Log.e("ListActivity", "Failed resolving category", e);

                runOnUiThread(() ->
                {
                    if (addItemFragment != null)
                    {
                        addItemFragment.endProgressBar();
                    }

                    Toast.makeText(
                            ListActivity.this,
                            Baskit.getAppStr(R.string.msg_general_error),
                            Toast.LENGTH_SHORT
                    ).show();
                });

                return;
            }

            if (categoryName == null || categoryName.isEmpty())
            {
                runOnUiThread(() ->
                        Toast.makeText(ListActivity.this, Baskit.getAppStr(R.string.msg_general_error), Toast.LENGTH_SHORT).show()
                );
                return;
            }

            final String finalCategoryName = categoryName;
            runProtectedRequest(
                    "add_item_" + item.getAbsoluteId(),
                    btnAddItem,
                    () ->
                    {
                        if (!list.hasCategory(finalCategoryName))
                        {
                            dbHandler.addCategory(list, new Category(finalCategoryName));
                        }

                        dbHandler.addItem(list, finalCategoryName, item, new FirebaseDBHandler.DBCallback()
                        {
                            @Override
                            public void onComplete()
                            {
                                if (isFinishing() || isDestroyed())
                                {
                                    return;
                                }
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
                                android.util.Log.e("ListActivity", "Failed adding item", e);

                                if (isFinishing() || isDestroyed())
                                {
                                    return;
                                }
                                runOnUiThread(() ->
                                {
                                    if (addItemFragment != null)
                                    {
                                        addItemFragment.endProgressBar();
                                        addItemFragment.dismiss();
                                    }
                                    Toast.makeText(ListActivity.this, Baskit.getAppStr(R.string.msg_general_error), Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    }
            );
        });

        addItemThread.setName("ListAddItem");
        addItemThread.start();
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
                        runProtectedRequest(
                                "sort_cheapest_" + list.getId(),
                                btnSortList,
                                () -> dbHandler.updateList(list)
                        );
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
                        runProtectedRequest(
                                "sort_supermarket_" + list.getId() + "_" + sm,
                                btnSortList,
                                () -> dbHandler.updateList(list)
                        );
                    }
                }
        );
    }


    public static class CategoryAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<CategoryAdapter.ViewHolder>
    {
        private final String listId;

        private ArrayList<Category> categories;

        private final Context context;

        private String lastDataSignature = "";

        CategoryAdapter(Context context, String listId, ArrayList<Category> categories)
        {
            this.context = context;
            this.listId = listId;
            this.categories = categories;
        }

        public static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder
        {
            final TextView tvName;
            final TextView tvCount;
            final TextView tvPrice;
            final LinearLayout loutInfo;

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
            return new ViewHolder(v);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(CategoryAdapter.ViewHolder holder, int position)
        {
            if (categories == null ||
                    position < 0 ||
                    position >= categories.size())
            {
                return;
            }

            Category category = categories.get(position);

            String categoryName = category.getName();

            if (categoryName == null || categoryName.isBlank())
            {
                categoryName = Baskit.getAppStr(R.string.unnamed_category);
            }

            String text = "- " + categoryName;
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

            if (category == null)
            {
                return;
            }
            holder.itemView.setOnClickListener(v -> {
                if (category.getName() == null || category.getName().isBlank())
                {
                    return;
                }

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

        @SuppressLint("NotifyDataSetChanged")
        void update(ArrayList<Category> newData)
        {
            ArrayList<Category> safeData =
                    newData != null ? newData : new ArrayList<>();

            String newSignature = buildDataSignature(safeData);

            if (newSignature.equals(lastDataSignature))
            {
                return;
            }

            lastDataSignature = newSignature;
            this.categories = safeData;

            notifyDataSetChanged();
        }

        private String buildDataSignature(ArrayList<Category> data)
        {
            if (data == null)
            {
                return "";
            }

            StringBuilder builder = new StringBuilder();

            for (Category category : data)
            {
                if (category == null)
                {
                    continue;
                }

                builder.append(category.getName())
                        .append('|')
                        .append(category.isFinished())
                        .append('|')
                        .append(category.countUnchecked())
                        .append('|')
                        .append(category.getTotal())
                        .append(';');
            }

            return builder.toString();
        }
    }
}