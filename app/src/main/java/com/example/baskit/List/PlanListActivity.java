package com.example.baskit.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.OnlineComponents.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.Categories.ItemsAdapter;
import com.example.baskit.OnlineComponents.FirebaseDBHandler;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.MainComponents.Item.ItemVariant;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.MasterActivity;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlanListActivity extends MasterActivity
{
    List originalList;
    List list;
    String listId;
    double oldTotal;
    String categoryName;

    boolean initialized = true;
    boolean listListenerAttached = false;
    boolean uiInitialized = false;

    int colorChosen;

    Map<String, Map<String, Map<String, Double>>> allItems;
    Map<String, ArrayList<String>> groups;

    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    APIHandler apiHandler = APIHandler.getInstance();

    PlanListItemsAdapter itemsAdapter;
    LayoutInflater categoriesListInflater;

    ImageButton btnCancel;
    Button btnSave;
    TextView tvListName, tvTotal;
    RecyclerView recyclerItems;
    LinearLayout categoriesListContainer;

    public interface OnItemMovedListener
    {
        void onItemMoved(Item item, Supermarket from, Supermarket to);
    }

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_list);

        colorChosen = Baskit.getAppColor(this, com.google.android.material.R.attr.colorPrimaryVariant);

        listId = getIntent().getStringExtra("listId");
        categoryName = getIntent().getStringExtra("category");

        if (listId == null)
        {
            Toast.makeText(this, "Missing list id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        createInit();
        uiInitialized = true;

        groups = apiHandler.getGroups();

        resumeInit();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Only attach if we never attached before.
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

    private void createInit()
    {
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        tvListName = findViewById(R.id.tv_list_name);
        tvTotal = findViewById(R.id.tv_total);

        categoriesListContainer = findViewById(R.id.categories_container);
        categoriesListInflater = LayoutInflater.from(this);

        setButtons();
    }

    private void resumeInit()
    {
        if (listId == null) return;

        runWhenServerActive(() ->
        {
            dbHandler.getList(listId, new FirebaseDBHandler.GetListCallback()
            {
                @Override
                public void onListFetched(List newList)
                {
                    originalList = newList;
                    PlanListActivity.this.list = new List(newList);

                    oldTotal = list.getTotal();
                    tvTotal.setText(Baskit.getTotalDisplayString(list.getTotal(), true, false, false));
                    tvTotal.setVisibility(View.VISIBLE);

                    if (newList == null)
                    {
                        Toast.makeText(PlanListActivity.this, "List not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String title = newList.getName();

                    if (categoryName != null)
                    {
                        title += " (" + categoryName + ")";
                    }

                    tvListName.setText(title);
                    tvListName.setVisibility(View.VISIBLE);

                    recyclerItems = findViewById(R.id.recycler_supermarket_items);
                    recyclerItems.setLayoutManager(new LinearLayoutManager(PlanListActivity.this));

                    ArrayList<Supermarket> supermarkets = apiHandler.getSupermarkets();

                    itemsAdapter = new PlanListItemsAdapter(
                            list,
                            originalList,
                            PlanListActivity.this,
                            PlanListActivity.this,
                            new ItemsAdapter.UpperClassFunctions()
                            {
                                @Override
                                public void updateItemCategory(Item item) {}

                                @Override
                                public void removeItemCategory(Item item) {}

                                @Override
                                public void updateCategory()
                                {
                                    displayTotalDif();
                                }

                                @Override
                                public void removeCategory() {}
                            },
                            supermarkets,
                            groups,
                            categoryName
                    );

                    recyclerItems.setAdapter(itemsAdapter);
                }

                @Override
                public void onError(String error)
                {
                    initialized = false;
                }
            });
        });
    }

    private void displayTotalDif()
    {
        double priceDif = list.getTotal() - oldTotal;
        String priceStr = Baskit.getTotalDisplayString(list.getTotal(), true, false, false);
        String difStr = Baskit.getTotalDisplayString(priceDif, true, false, false);

        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(priceStr);

        if (priceDif != 0)
        {
            String formattedDiff;

            if (priceDif > 0)
            {
                formattedDiff = " (+" + difStr + ")";
            }
            else
            {
                formattedDiff = " (" + difStr + ")";
            }

            int start = builder.length();
            builder.append(formattedDiff);
            int end = builder.length();

            builder.setSpan(
                    new ForegroundColorSpan(colorChosen),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        tvTotal.setText(builder);
    }

    private void finish(boolean save)
    {
        if (save && list != null)
        {
            dbHandler.updateList(list);
        }

        finish();
    }

    private void setButtons()
    {
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> finish(true));
    }


    public class PlanListItemsAdapter extends RecyclerView.Adapter<PlanListItemsAdapter.ViewHolder>
    {
        private final List originalList;
        private final List list;
        private final String categoryName;
        private Supermarket selectedSupermarket = null;

        private final Map<String, ArrayList<String>> groups;
        private final ArrayList<Supermarket> baseSupermarkets;
        private ArrayList<Supermarket> supermarkets;
        private Map<Supermarket, ArrayList<Item>> itemsBySupermarket;
        private Map<Supermarket, Boolean> expandedStates;
        private Map<String, Integer> supermarketSectionCounts;

        private final int colorBase;
        private final int colorChosen;
        private final Supermarket unassigned_supermarket = Baskit.UNASSIGNED_SUPERMARKET;

        private final Activity activity;
        private final Context context;
        private ItemsAdapter.UpperClassFunctions upperClassFns;

        private final PlanListActivity.OnItemMovedListener listener = (draggedItem, from, to) ->
        {
            if (itemsBySupermarket.get(from) != null)
            {
                itemsBySupermarket.get(from).remove(draggedItem);
            }

            if (to == unassigned_supermarket)
            {
                draggedItem.setUnchosen();
            }
            else
            {
                updateItemRow(draggedItem, to);
            }

            if (itemsBySupermarket.get(to) == null)
            {
                itemsBySupermarket.put(to, new ArrayList<>());
                expandedStates.put(to, true);
            }

            itemsBySupermarket.get(to).add(draggedItem);
            rebuildDisplaySupermarkets();
            notifyDataSetChanged();
            upperClassFns.updateCategory();
        };

        @SuppressLint("PrivateResource")
        public PlanListItemsAdapter(com.example.baskit.MainComponents.List list,
                                    com.example.baskit.MainComponents.List originalList,
                                    Activity activity, Context context,
                                    ItemsAdapter.UpperClassFunctions upperClassFns, ArrayList<Supermarket> supermarkets,
                                    Map<String, ArrayList<String>> groups,
                                    String categoryName)
        {
            this.list = list;
            this.originalList = originalList;
            this.activity = activity;
            this.context = context;
            this.categoryName = categoryName;
            this.upperClassFns = new ItemsAdapter.UpperClassFunctions()
            {
                @Override
                public void updateItemCategory(Item item)
                {
                    upperClassFns.updateItemCategory(item);
                }

                @Override
                public void removeItemCategory(Item item)
                {
                    upperClassFns.removeItemCategory(item);
                }

                @Override
                public void updateCategory()
                {
                    upperClassFns.updateCategory();
                }

                @Override
                public void removeCategory()
                {
                    if (list == null || list.getCategories() == null || list.getCategories().isEmpty())
                    {
                        upperClassFns.removeCategory();
                    }
                }

                @Override
                public void collapseAllSupermarkets()
                {
                    for (Supermarket sm : expandedStates.keySet())
                    {
                        expandedStates.put(sm, false);
                    }
                    notifyDataSetChanged();
                }

                @Override
                public void expandAllSupermarkets()
                {
                    for (Supermarket sm : expandedStates.keySet())
                    {
                        expandedStates.put(sm, true);
                    }
                    notifyDataSetChanged();
                }
            };

            this.baseSupermarkets = supermarkets != null ? new ArrayList<>(supermarkets) : new ArrayList<>();
            this.supermarkets = new ArrayList<>(this.baseSupermarkets);
            this.groups = groups;

            colorBase = Baskit.getAppColor(context, com.google.android.material.R.attr.colorOnBackground);
            colorChosen = Baskit.getAppColor(context, com.google.android.material.R.attr.colorPrimaryVariant);

            if (list == null && list.isEmpty()) return;

            restart();
            sortByExisting();
        }

        private void restart()
        {
            itemsBySupermarket = new HashMap<>();
            expandedStates = new HashMap<>();
            supermarketSectionCounts = new HashMap<>();

            for (Supermarket supermarket : baseSupermarkets)
            {
                itemsBySupermarket.put(supermarket, new ArrayList<>());
                expandedStates.put(supermarket, true);
            }

            itemsBySupermarket.put(unassigned_supermarket, new ArrayList<>());
            expandedStates.put(unassigned_supermarket, true);

            rebuildDisplaySupermarkets();
        }

        private void rebuildDisplaySupermarkets()
        {
            ArrayList<Supermarket> result = new ArrayList<>();
            supermarketSectionCounts.clear();

            for (Supermarket sm : itemsBySupermarket.keySet())
            {
                ArrayList<Item> list = itemsBySupermarket.get(sm);

                if (sm == unassigned_supermarket && (list == null || list.isEmpty()))
                {
                    continue;
                }

                String name = sm.getSupermarket();
                supermarketSectionCounts.put(
                        name,
                        supermarketSectionCounts.getOrDefault(name, 0) + 1
                );

                result.add(sm);
            }

            this.supermarkets = result;
        }

        private void sortByExisting()
        {
            ArrayList<Item> sourceItems = new ArrayList<>();

            if (categoryName != null)
            {
                if (list.getCategories().containsKey(categoryName))
                {
                    sourceItems.addAll(
                            list.getCategories()
                                    .get(categoryName)
                                    .getItems()
                    );
                }
            }
            else
            {
                sourceItems.addAll(list.getRemainedItems());
            }

            for (Item item : sourceItems)
            {
                Supermarket supermarket = item.getSupermarket();
                ArrayList<Item> targetList;

                if (supermarket == null)
                {
                    targetList = itemsBySupermarket.get(unassigned_supermarket);
                }
                else if (!itemsBySupermarket.containsKey(supermarket))
                {
                    itemsBySupermarket.put(supermarket, new ArrayList<>());
                    expandedStates.put(supermarket, true);
                    targetList = itemsBySupermarket.get(supermarket);
                }
                else
                {
                    targetList = itemsBySupermarket.get(supermarket);
                }

                if (targetList != null)
                {
                    targetList.add(item);
                }
            }

            rebuildDisplaySupermarkets();
            notifyDataSetChanged();
        }

        private void updateItemRow(Item item, Supermarket supermarket)
        {
            if (item == null || supermarket == null) return;

            ArrayList<Item> single = new ArrayList<>();
            single.add(item);

            Map<String, ArrayList<ItemVariant>> rowsMap =
                    APIHandler.getInstance().buildVariants(single);

            ArrayList<ItemVariant> rows =
                    rowsMap.get(item.getBaseName());

            if (rows != null)
            {
                item.setSupermarketVariant(supermarket, rows);
            }
            else
            {
                item.setPrice(0.0);
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder
        {
            protected TextView tvSupermarket;
            protected ImageButton btnExpand;
            protected RecyclerView recyclerItems;

            public ViewHolder(View itemView)
            {
                super(itemView);

                tvSupermarket = itemView.findViewById(R.id.tv_supermarket_name);
                btnExpand = itemView.findViewById(R.id.btn_expand);
                recyclerItems = itemView.findViewById(R.id.recycler_items);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_supermarket, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position)
        {
            Supermarket supermarket = supermarkets.get(position);

            if (supermarket == unassigned_supermarket)
            {
                holder.tvSupermarket.setText(supermarket.toString());
                holder.tvSupermarket.setTextColor(colorBase);
                holder.tvSupermarket.setAlpha(0.5f);
                holder.btnExpand.setAlpha(0.5f);
            }
            else
            {
                boolean isChosenSupermarket = baseSupermarkets.contains(supermarket);

                if (!isChosenSupermarket)
                {
                    holder.tvSupermarket.setText(supermarket.toString());
                    holder.tvSupermarket.setAlpha(0.5f);
                    holder.btnExpand.setAlpha(0.5f);
                }
                else if (isSingleSectionCurrently(supermarket))
                {
                    holder.tvSupermarket.setText(supermarket.getDecodedSupermarket());
                    holder.tvSupermarket.setAlpha(1f);
                    holder.btnExpand.setAlpha(1f);
                }
                else
                {
                    holder.tvSupermarket.setText(supermarket.toString());
                    holder.tvSupermarket.setAlpha(1f);
                    holder.btnExpand.setAlpha(1f);
                }
            }

            if (selectedSupermarket != null && selectedSupermarket.equals(supermarket))
            {
                holder.tvSupermarket.setTextColor(colorChosen);
                holder.btnExpand.setColorFilter(colorChosen);
            }
            else
            {
                holder.tvSupermarket.setTextColor(colorBase);
                holder.btnExpand.setColorFilter(colorBase);
            }

            ArrayList<Item> items = itemsBySupermarket.get(supermarket);

            SupermarketItemsAdapterPlan supermarketsAdapter =
                    new SupermarketItemsAdapterPlan(items, activity, context, upperClassFns, supermarket, selectedSupermarket, listener);

            holder.recyclerItems.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
            holder.recyclerItems.setAdapter(supermarketsAdapter);

            Boolean expanded = expandedStates.get(supermarket);
            boolean isExpanded = expanded == null || expanded;
            holder.recyclerItems.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            holder.btnExpand.setRotation(isExpanded ? 180 : 0);

            holder.btnExpand.setOnClickListener(v ->
            {
                expandedStates.put(supermarket, !isExpanded);
                notifyItemChanged(position);
            });

            holder.tvSupermarket.setOnClickListener(v ->
            {
                if (supermarket != unassigned_supermarket && baseSupermarkets.contains(supermarket))
                {
                    if (selectedSupermarket == supermarket)
                    {
                        selectedSupermarket = null;
                    }
                    else
                    {
                        selectedSupermarket = supermarket;
                    }

                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount()
        {
            return supermarkets != null ? supermarkets.size() : 0;
        }

        private boolean isSingleSectionCurrently(Supermarket current)
        {
            if (supermarketSectionCounts == null) return true;

            String name = current.getSupermarket();
            Integer count = supermarketSectionCounts.get(name);

            return count == null || count <= 1;
        }


        public class SupermarketItemsAdapterPlan extends ItemsAdapter
        {
            private final Supermarket supermarket, selectedSupermarketParent;

            private final int colorBase;
            @SuppressLint("PrivateResource")
            private final int colorUnavailable;
            private final int colorChosen;

            private final PlanListActivity.OnItemMovedListener onItemMovedListener;

            @SuppressLint("PrivateResource")
            public SupermarketItemsAdapterPlan(ArrayList<Item> items,
                                               Activity activity, Context context,
                                               UpperClassFunctions upperClassFns,
                                               Supermarket supermarket,
                                               Supermarket selectedSupermarketParent,
                                               PlanListActivity.OnItemMovedListener onItemMovedListener)
            {
                super(items, item -> {}, upperClassFns, activity, context);

                this.supermarket = supermarket;
                this.selectedSupermarketParent = selectedSupermarketParent;
                this.onItemMovedListener = onItemMovedListener;

                colorBase = Baskit.getAppColor(context, com.google.android.material.R.attr.colorOnBackground);
                colorUnavailable = Baskit.getAppColor(context, com.google.android.material.R.attr.colorOnContainerUnchecked);
                colorChosen = Baskit.getAppColor(context, com.google.android.material.R.attr.colorPrimaryVariant);
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position)
            {
                super.onBindViewHolder(holder, position);

                Item item = items.get(position);

                holder.dragHandle.setImageResource(R.drawable.ic_add_simple);

                holder.btnDown.setVisibility(View.GONE);
                holder.btnUp.setVisibility(View.GONE);
                holder.btnCheckBox.setVisibility(View.GONE);
                holder.dragHandle.setVisibility(View.GONE);
                holder.spacer.setVisibility(View.VISIBLE);

                holder.tvName.setTextColor(colorBase);
                holder.tvQuantity.setTextColor(colorBase);
                holder.tvPrice.setTextColor(colorBase);
                holder.dragHandle.setColorFilter(colorBase);
                holder.spacer.setBackgroundColor(colorBase);

                if (selectedSupermarketParent == supermarket)
                {
                    holder.tvName.setTextColor(colorChosen);
                    holder.tvPrice.setTextColor(colorChosen);
                    holder.tvQuantity.setTextColor(colorChosen);
                    holder.spacer.setBackgroundColor(colorChosen);
                }
                else if (selectedSupermarketParent != null)
                {
                    double newPrice = getPrice(item, selectedSupermarketParent);

                    if (newPrice > 0)
                    {
                        holder.dragHandle.setColorFilter(colorChosen);
                        holder.dragHandle.setVisibility(View.VISIBLE);

                        showItemPriceDif(holder, item, newPrice);
                    }
                    else
                    {
                        holder.tvName.setTextColor(colorUnavailable);
                        holder.tvQuantity.setTextColor(colorUnavailable);
                        holder.tvPrice.setTextColor(colorUnavailable);
                        holder.spacer.setBackgroundColor(colorUnavailable);
                    }
                }
                else
                {
                    holder.tvName.setTextColor(colorUnavailable);
                    holder.tvQuantity.setTextColor(colorUnavailable);
                    holder.tvPrice.setTextColor(colorUnavailable);
                    holder.spacer.setBackgroundColor(colorUnavailable);
                }

                setItemButtons(holder, item);
            }

            private void setItemButtons(ViewHolder holder, Item item)
            {
                holder.btnUp.setOnClickListener(null);
                holder.btnUp.setActivated(false);

                holder.btnDown.setOnClickListener(null);
                holder.btnDown.setActivated(false);

                holder.btnCheckBox.setOnClickListener(null);
                holder.btnCheckBox.setActivated(false);

                holder.tvName.setOnClickListener(null);
                holder.tvName.setActivated(false);

                holder.dragHandle.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        onItemMovedListener.onItemMoved(item, supermarket, selectedSupermarketParent);
                    }
                });
            }

            private void showItemPriceDif(ViewHolder holder, Item item, double newPrice)
            {
                double priceDif = newPrice - item.getTotal();

                String priceStr = Baskit.getTotalDisplayString(newPrice, true, false, false);
                String difStr = Baskit.getTotalDisplayString(priceDif, true, false, false);

                SpannableStringBuilder builder = new SpannableStringBuilder();
                builder.append(priceStr);

                if (supermarket == Baskit.UNASSIGNED_SUPERMARKET)
                {
                    holder.tvPrice.setVisibility(View.VISIBLE);
                    holder.tvPrice.setTextColor(colorChosen);
                }
                else if (priceDif != 0)
                {
                    String formattedDiff;

                    if (priceDif > 0)
                    {
                        formattedDiff = " (+" + difStr + ")";
                    }
                    else
                    {
                        formattedDiff = " (" + difStr + ")";
                    }

                    int start = builder.length();
                    builder.append(formattedDiff);
                    int end = builder.length();

                    builder.setSpan(
                            new ForegroundColorSpan(colorChosen),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }

                holder.tvPrice.setText(builder);
            }

            private double getPrice(Item item, Supermarket otherSupermarket)
            {
                if (item == null || otherSupermarket == null) return 0.0;

                ArrayList<Item> single = new ArrayList<>();
                single.add(item);

                Map<String, ArrayList<ItemVariant>> rowsMap =
                        APIHandler.getInstance().buildVariants(single);

                ArrayList<ItemVariant> rows =
                        rowsMap.get(item.getBaseName());

                if (rows != null)
                {
                    ItemVariant row = item.getSupermarketVariant(otherSupermarket, rows);

                    if (row != null)
                    {
                        return row.getPrice();
                    }
                }

                return 0.0;
            }
        }
    }
}