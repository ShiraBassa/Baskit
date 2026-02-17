package com.example.baskit.List;

import static com.example.baskit.Baskit.getAppColor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.Baskit;
import com.example.baskit.Categories.ItemsAdapter;
import com.example.baskit.Categories.SupermarketItemsAdapter;
import com.example.baskit.Categories.SupermarketItemsAdapterPlan;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlanListItemsAdapter extends RecyclerView.Adapter<PlanListItemsAdapter.ViewHolder>
{
    com.example.baskit.MainComponents.List list;
    private final ArrayList<Supermarket> baseSupermarkets;
    ArrayList<Supermarket> supermarkets;
    protected Map<Supermarket, ArrayList<Item>> itemsBySupermarket;
    private Map<Supermarket, Boolean> expandedStates;
    private Map<String, Map<String, Map<String, Double>>> itemPrices;
    private Map<String, Integer> supermarketSectionCounts;

    Activity activity;
    Context context;
    ItemsAdapter.UpperClassFunctions upperClassFns;

    public static final Supermarket unassigned_supermarket = Baskit.UNASSIGNED_SUPERMARKET;
    Supermarket selectedSupermarket = null;

    private final SupermarketItemsAdapter.OnItemMovedListener listener = (draggedItem, from, to) ->
    {
        if (itemsBySupermarket.get(from) != null)
        {
            itemsBySupermarket.get(from).remove(draggedItem);
        }

        if (to == unassigned_supermarket)
        {
            draggedItem.setSupermarket(null);
            draggedItem.setPrice(0.0);
        }
        else
        {
            draggedItem.setSupermarket(to);
            updateItemPriceForSupermarket(draggedItem, to);
        }

        if (itemsBySupermarket.get(to) == null)
        {
            itemsBySupermarket.put(to, new ArrayList<>());
            expandedStates.put(to, true);
        }

        itemsBySupermarket.get(to).add(draggedItem);
        rebuildDisplaySupermarkets();
        notifyDataSetChanged();
    };

    public PlanListItemsAdapter(com.example.baskit.MainComponents.List list, Activity activity, Context context,
                                ItemsAdapter.UpperClassFunctions upperClassFns, ArrayList<Supermarket> supermarkets,
                                Map<String, Map<String, Map<String, Double>>> itemPrices)
    {
        this.list = list;
        this.activity = activity;
        this.context = context;
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
        this.itemPrices = itemPrices;

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
        ArrayList<Supermarket> nonEmpty = new ArrayList<>();
        ArrayList<Supermarket> empty = new ArrayList<>();

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

            if (list != null && !list.isEmpty())
            {
                nonEmpty.add(sm);
            }
            else
            {
                empty.add(sm);
            }
        }

        ArrayList<Supermarket> result = new ArrayList<>();
        result.addAll(nonEmpty);
        result.addAll(empty);

        this.supermarkets = result;
    }

    private void sortByExisting()
    {
        for (Item item : list.getRemainedItems())
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

    private void updateItemPriceForSupermarket(Item item, Supermarket supermarket)
    {
        if (item == null || supermarket == null) return;

        Map<String, Map<String, Double>> itemPriceData = itemPrices.get(item.getAbsoluteId());

        if (itemPriceData != null)
        {
            Map<String, Double> sections = itemPriceData.get(supermarket.getSupermarket());

            if (sections != null && sections.containsKey(supermarket.getSection()))
            {
                item.setPrice(sections.get(supermarket.getSection()));
            }
            else if (sections != null && !sections.isEmpty())
            {
                item.setPrice(sections.values().iterator().next());
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
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
        boolean isChosenSupermarket = false;

        if (supermarket == unassigned_supermarket)
        {
            holder.tvSupermarket.setText(supermarket.toString());
            holder.tvSupermarket.setTextColor(
                    Baskit.getAppColor(context, R.color.black)
            );
            holder.tvSupermarket.setAlpha(0.5f);
            holder.btnExpand.setAlpha(0.5f);
        }
        else
        {
            isChosenSupermarket = baseSupermarkets.contains(supermarket);

            if (!isChosenSupermarket)
            {
                holder.tvSupermarket.setText(supermarket.toString());
                holder.tvSupermarket.setAlpha(0.5f);
                holder.btnExpand.setAlpha(0.5f);
            }
            else if (isSingleSectionCurrently(supermarket))
            {
                holder.tvSupermarket.setText(supermarket.getSupermarket());
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
            holder.tvSupermarket.setTextColor(
                    Baskit.getAppColor(context, com.google.android.material.R.attr.colorPrimaryVariant)
            );
            holder.btnExpand.setColorFilter(
                    Baskit.getAppColor(context, com.google.android.material.R.attr.colorPrimaryVariant)
            );
        }
        else
        {
            holder.tvSupermarket.setTextColor(Color.BLACK);
            holder.btnExpand.setColorFilter(Color.BLACK);
        }

        ArrayList<Item> items = itemsBySupermarket.get(supermarket);

        SupermarketItemsAdapterPlan supermarketsAdapter =
                new SupermarketItemsAdapterPlan(items, activity, context, upperClassFns, supermarket, selectedSupermarket, itemPrices, listener);

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

    private boolean isSingleSectionCurrently(Supermarket current)
    {
        if (supermarketSectionCounts == null) return true;

        String name = current.getSupermarket();
        Integer count = supermarketSectionCounts.get(name);

        return count == null || count <= 1;
    }

    @Override
    public int getItemCount()
    {
        return supermarkets != null ? supermarkets.size() : 0;
    }
}
