package com.example.baskit.Categories;

import static com.example.baskit.Baskit.getThemeColor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class CategoryItemsAdapter extends RecyclerView.Adapter<CategoryItemsAdapter.ViewHolder>
{
    Category category;
    ArrayList<Supermarket> supermarkets;
    protected Map<Supermarket, ArrayList<Item>> itemsBySupermarket;
    private Map<Supermarket, Boolean> expandedStates;
    private APIHandler apiHandler = APIHandler.getInstance();
    private Map<String, Map<String, Map<String, Double>>> itemPrices;

    Activity activity;
    Context context;
    ItemsAdapter.UpperClassFunctions upperClassFns;
    Supermarket unassigned = new Supermarket("לא נבחר", "");
    Supermarket other = new Supermarket("אחר", "");

    public CategoryItemsAdapter(Category category, Activity activity, Context context,
                                ItemsAdapter.UpperClassFunctions upperClassFns, ArrayList<Supermarket> supermarkets,
                                Map<String, Map<String, Map<String, Double>>> itemPrices)
    {
        this.category = category;
        this.activity = activity;
        this.context = context;
        this.upperClassFns = upperClassFns;
        this.supermarkets = supermarkets;

        ArrayList<Supermarket> displaySupermarkets = new ArrayList<>(supermarkets);
        displaySupermarkets.add(unassigned);
        this.supermarkets = displaySupermarkets;

        this.itemPrices = itemPrices;

        if (supermarkets != null && !supermarkets.isEmpty())
        {
            int oldSize = supermarkets.size();
            notifyItemRangeRemoved(0, oldSize);
        }

        restart();
        sortByExisting();
    }

    private void restart()
    {
        itemsBySupermarket = new HashMap<>();
        expandedStates = new HashMap<>();

        itemsBySupermarket.put(unassigned, new ArrayList<>());
        expandedStates.put(unassigned, true);

        for (Supermarket supermarket : supermarkets)
        {
            itemsBySupermarket.put(supermarket, new ArrayList<>());
            expandedStates.put(supermarket, true);
        }

        itemsBySupermarket.put(other, new ArrayList<>());
        expandedStates.put(other, true);
    }

    public void arrangeByCheapest()
    {
        setCheapest();

        activity.runOnUiThread(() ->
        {
            restart();

            for (Item item : category.getItems().values())
            {
                Supermarket supermarket = item.getSupermarket();
                itemsBySupermarket.get(supermarket).add(item);
            }

            upperClassFns.updateCategory();
            notifyDataSetChanged();
        });
    }

    private void sortByExisting()
    {
        for (Item item : category.getItems().values())
        {
            Supermarket supermarket = item.getSupermarket();
            ArrayList<Item> targetList;

            if (supermarket == null)
            {
                targetList = itemsBySupermarket.get(unassigned);
            }
            else if (!itemsBySupermarket.containsKey(supermarket))
            {
                targetList = itemsBySupermarket.get(other);
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

        ArrayList<Item> otherItems = itemsBySupermarket.get(other);
        if (otherItems != null && !otherItems.isEmpty() && !supermarkets.contains(other)) {
            supermarkets.add(other);
        }

        notifyDataSetChanged();
    }

    private void setCheapest()
    {
        for (Item item : category.getItems().values())
        {
            double lowest = Double.MAX_VALUE;
            Supermarket lowestSupermarket = null;

            for (Map.Entry<String, Map<String, Double>> SupermarketEntry : itemPrices.get(item.getAbsoluteId()).entrySet())
            {
                String supermarket = SupermarketEntry.getKey();

                for (Map.Entry<String, Double> sectionEntry : SupermarketEntry.getValue().entrySet())
                {
                    String section = sectionEntry.getKey();
                    Double price = sectionEntry.getValue();

                    if (price < lowest)
                    {
                        lowest = price;
                        lowestSupermarket = new Supermarket(supermarket, section);
                    }
                }

                item.setPrice(lowest);
                item.setSupermarket(lowestSupermarket);
            }
        }
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
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        Supermarket supermarket = supermarkets.get(position);
        holder.tvSupermarket.setText(supermarket.toString());

        ArrayList<Item> items = itemsBySupermarket.get(supermarket);
        SupermarketItemsAdapter supermarketsAdapter = new SupermarketItemsAdapter(items, listener, activity, context, upperClassFns);

        holder.recyclerItems.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.recyclerItems.setAdapter(supermarketsAdapter);

        boolean isExpanded = expandedStates.get(supermarket);
        holder.recyclerItems.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.btnExpand.setRotation(isExpanded ? 180 : 0);

        holder.btnExpand.setOnClickListener(v ->
        {
            expandedStates.put(supermarket, !isExpanded);
            notifyItemChanged(position);
        });

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // not needed
            }
        });
        itemTouchHelper.attachToRecyclerView(holder.recyclerItems);

        holder.itemView.setOnDragListener((v, event) ->
        {
            Item draggedItem = (Item) event.getLocalState();

            switch (event.getAction())
            {
                case DragEvent.ACTION_DRAG_STARTED:
                    new Handler(Looper.getMainLooper()).post(() ->
                    {
                        for (Supermarket sm : expandedStates.keySet())
                        {
                            expandedStates.put(sm, false);
                        }

                        notifyDataSetChanged();

                        if (!containsSupermarket(draggedItem, supermarkets.get(position)))
                        {
                            holder.itemView.setBackgroundColor(
                                    getThemeColor(context, com.google.android.material.R.attr.colorError));
                        }
                    });

                    break;

                case DragEvent.ACTION_DRAG_ENTERED:
                    if (containsSupermarket(draggedItem, supermarkets.get(position)))
                    {
                        holder.itemView.setBackgroundColor(getThemeColor(context, com.google.android.material.R.attr.colorSecondaryContainer));
                    }

                    break;

                case DragEvent.ACTION_DRAG_EXITED:
                    if (containsSupermarket(draggedItem, supermarkets.get(position)))
                    {
                        holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                    }

                    break;

                case DragEvent.ACTION_DROP:
                    Supermarket targetSupermarket = supermarket;
                    Supermarket fromSupermarket = draggedItem.getSupermarket();

                    if (containsSupermarket(draggedItem, targetSupermarket))
                    {
                        if (fromSupermarket == null || !fromSupermarket.equals(targetSupermarket))
                        {
                            listener.onItemMoved(draggedItem, fromSupermarket, targetSupermarket);
                        }
                    }

                    break;

                case DragEvent.ACTION_DRAG_ENDED:
                    new Handler(Looper.getMainLooper()).post(() ->
                    {
                        for (Supermarket sm : expandedStates.keySet())
                        {
                            expandedStates.put(sm, true);
                        }

                        notifyDataSetChanged();
                        holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                    });

                    break;
            }

            return true;
        });
    }

    private boolean containsSupermarket(Item item, Supermarket supermarket)
    {
        if (supermarket == unassigned)
        {
            return true;
        }

        String id = item.getAbsoluteId();
        String supermarketName = supermarket.getSupermarket();
        String sectionName = supermarket.getSection();

        if (itemPrices.containsKey(id) &&
                itemPrices.get(id).containsKey(supermarketName) &&
                itemPrices.get(id).get(supermarketName).containsKey(sectionName))
        {
            return true;
        }

        return false;
    }

    @Override
    public int getItemCount()
    {
        return supermarkets != null ? supermarkets.size() : 0;
    }

    private final SupermarketItemsAdapter.OnItemMovedListener listener = (draggedItem, from, to) ->
    {
        if (itemsBySupermarket.get(from) != null)
        {
            itemsBySupermarket.get(from).remove(draggedItem);
        }

        if (to == unassigned) {
            draggedItem.setSupermarket(null);
            draggedItem.setPrice(0.0);
        } else {
            draggedItem.setSupermarket(to);
            updateItemPriceForSupermarket(draggedItem, to);
        }

        if (itemsBySupermarket.get(to) == null)
        {
            itemsBySupermarket.put(to, new ArrayList<>());
        }

        itemsBySupermarket.get(to).add(draggedItem);
        notifyDataSetChanged();

        upperClassFns.updateItemCategory(draggedItem);
    };

    public void updateItems(ArrayList<Item> newItems)
    {
        this.category.setItemsFromFlat(newItems);

        restart();
        sortByExisting();
    }
}
