package com.example.baskit.Categories;

import static com.example.baskit.Baskit.getAppColor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Baskit;
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
    private final ArrayList<Supermarket> baseSupermarkets;
    ArrayList<Supermarket> supermarkets;
    protected Map<Supermarket, ArrayList<Item>> itemsBySupermarket;
    private Map<Supermarket, Boolean> expandedStates;
    private Map<String, Map<String, Map<String, Double>>> itemPrices;

    Activity activity;
    Context context;
    ItemsAdapter.UpperClassFunctions upperClassFns;

    public static final Supermarket unassigned_supermarket = Baskit.unassigned_supermarket;
    public static final Supermarket other_supermarket = Baskit.other_supermarket;

    boolean isDropped;
    boolean draggable = false;
    private final APIHandler apiHandler = APIHandler.getInstance();

    public CategoryItemsAdapter(Category category, Activity activity, Context context,
                                ItemsAdapter.UpperClassFunctions upperClassFns, ArrayList<Supermarket> supermarkets,
                                Map<String, Map<String, Map<String, Double>>> itemPrices)
    {
        this.category = category;
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
                if (category.isEmpty())
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

                draggable = true;
                notifyDataSetChanged();
            }

            @Override
            public void expandAllSupermarkets()
            {
                for (Supermarket sm : expandedStates.keySet())
                {
                    expandedStates.put(sm, true);
                }

                draggable = false;
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

        for (Supermarket supermarket : baseSupermarkets)
        {
            itemsBySupermarket.put(supermarket, new ArrayList<>());
            expandedStates.put(supermarket, true);
        }

        itemsBySupermarket.put(unassigned_supermarket, new ArrayList<>());
        expandedStates.put(unassigned_supermarket, true);

        itemsBySupermarket.put(other_supermarket, new ArrayList<>());
        expandedStates.put(other_supermarket, true);

        rebuildDisplaySupermarkets();
    }

    private void rebuildDisplaySupermarkets()
    {
        ArrayList<Supermarket> nonEmpty = new ArrayList<>();
        ArrayList<Supermarket> empty = new ArrayList<>();

        for (Supermarket sm : baseSupermarkets)
        {
            ArrayList<Item> list = itemsBySupermarket.get(sm);

            if (list != null && !list.isEmpty())
            {
                nonEmpty.add(sm);
            }
            else
            {
                empty.add(sm);
            }
        }

        ArrayList<Supermarket> specialNonEmpty = new ArrayList<>();
        ArrayList<Supermarket> specialEmpty = new ArrayList<>();

        ArrayList<Item> unassigned = itemsBySupermarket.get(unassigned_supermarket);
        if (unassigned != null && !unassigned.isEmpty()) specialNonEmpty.add(unassigned_supermarket);
        else specialEmpty.add(unassigned_supermarket);

        ArrayList<Item> other = itemsBySupermarket.get(other_supermarket);
        if (other != null && !other.isEmpty()) specialNonEmpty.add(other_supermarket);
        else specialEmpty.add(other_supermarket);

        ArrayList<Supermarket> result = new ArrayList<>();
        result.addAll(nonEmpty);
        result.addAll(empty);
        result.addAll(specialNonEmpty);
        result.addAll(specialEmpty);

        this.supermarkets = result;
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

                if (supermarket == null)
                {
                    itemsBySupermarket.get(unassigned_supermarket).add(item);
                }
                else if (!itemsBySupermarket.containsKey(supermarket))
                {
                    itemsBySupermarket.get(other_supermarket).add(item);
                }
                else
                {
                    itemsBySupermarket.get(supermarket).add(item);
                }
            }

            rebuildDisplaySupermarkets();
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
                targetList = itemsBySupermarket.get(unassigned_supermarket);
            }
            else if (!itemsBySupermarket.containsKey(supermarket))
            {
                targetList = itemsBySupermarket.get(other_supermarket);
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

    private void setCheapest()
    {
        double lowest;
        Supermarket lowestSupermarket;

        for (Item item : category.getItems().values())
        {
            if (item.isChecked())
            {
                continue;
            }

            Map<String, Map<String, Double>> currItemPrices = itemPrices.get(item.getAbsoluteId());

            if (currItemPrices == null || currItemPrices.isEmpty())
            {
                lowest = 0.0;
                lowestSupermarket = unassigned_supermarket;
            }
            else
            {
                lowest = Double.MAX_VALUE;
                lowestSupermarket = null;

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
                }
            }

            item.setPrice(lowest);
            item.setSupermarket(lowestSupermarket);
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
        protected ImageButton btnExpand, dragHandle;
        protected RecyclerView recyclerItems;

        public ViewHolder(View itemView)
        {
            super(itemView);

            tvSupermarket = itemView.findViewById(R.id.tv_supermarket_name);
            btnExpand = itemView.findViewById(R.id.btn_expand);
            recyclerItems = itemView.findViewById(R.id.recycler_items);
            dragHandle = itemView.findViewById(R.id.drag_handle);
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
        holder.itemView.setOnDragListener(null);
        isDropped = false;
        Supermarket supermarket = supermarkets.get(position);

        if (apiHandler.singleSectionInSupermarkets(supermarket))
        {
            holder.tvSupermarket.setText(supermarket.getSupermarket());
        }
        else
        {
            holder.tvSupermarket.setText(supermarket.toString());
        }

        ArrayList<Item> items = itemsBySupermarket.get(supermarket);
        SupermarketItemsAdapter supermarketsAdapter = new SupermarketItemsAdapter(items, listener, activity, context, upperClassFns);

        holder.recyclerItems.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.recyclerItems.setAdapter(supermarketsAdapter);

        Boolean expanded = expandedStates.get(supermarket);
        boolean isExpanded = expanded == null || expanded;
        holder.recyclerItems.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.btnExpand.setRotation(isExpanded ? 180 : 0);

        if (ifFinishedSupermarket(items))
        {
            holder.tvSupermarket.setAlpha(0.5f);
            holder.btnExpand.setAlpha(0.5f);
        }
        else
        {
            holder.tvSupermarket.setAlpha(1f);
            holder.btnExpand.setAlpha(1f);
        }

        holder.btnExpand.setOnClickListener(v ->
        {
            expandedStates.put(supermarket, !isExpanded);
            notifyItemChanged(position);
        });

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean isLongPressDragEnabled()
            {
                return draggable;
            }

            @Override
            public boolean isItemViewSwipeEnabled()
            {
                return false;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target)
            {
                return draggable;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
        });

        itemTouchHelper.attachToRecyclerView(holder.recyclerItems);

        if (!draggable) return;

        holder.itemView.setOnDragListener((v, event) ->
        {
            View dragHandle = (View) event.getLocalState();

            ImageView ghostView = (ImageView) dragHandle.getTag(R.id.drag_fake_view);
            int touchOffsetY = (int) dragHandle.getTag(R.id.drag_touch_offset_y);
            int fixedX = (int) dragHandle.getTag(R.id.drag_initial_x);
            int rootYOffset = (int) dragHandle.getTag(R.id.drag_root_y_offset);
            Item draggedItem = (Item) dragHandle.getTag(R.id.drag_item);
            Supermarket fromSupermarket = (Supermarket) dragHandle.getTag(R.id.drag_from_supermarket);

            switch (event.getAction())
            {
                case DragEvent.ACTION_DRAG_STARTED:
                    if (!containsSupermarket(draggedItem, supermarkets.get(position)))
                    {
                        holder.itemView.setBackgroundColor(
                                Baskit.getAppColor(context, com.google.android.material.R.attr.colorError, 75));
                    }
                    break;

                case DragEvent.ACTION_DRAG_LOCATION:
                    int[] targetLoc = new int[2];
                    holder.itemView.getLocationOnScreen(targetLoc);
                    float screenTouchY = targetLoc[1] + event.getY();

                    if (ghostView != null)
                    {
                        float newGhostY = screenTouchY - rootYOffset - touchOffsetY;

                        ghostView.setY(newGhostY);
                        ghostView.setX(fixedX);
                    }
                    break;

                case DragEvent.ACTION_DRAG_ENTERED:
                    if (containsSupermarket(draggedItem, supermarkets.get(position)))
                    {
                        holder.itemView.setBackgroundColor(getAppColor(context, com.google.android.material.R.attr.colorSecondaryContainer));
                    }

                    break;

                case DragEvent.ACTION_DRAG_EXITED:
                    if (containsSupermarket(draggedItem, supermarkets.get(position)))
                    {
                        holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                    }

                    break;

                case DragEvent.ACTION_DROP:
                    isDropped = true;
                    Supermarket targetSupermarket = supermarket;

                    if (containsSupermarket(draggedItem, targetSupermarket))
                    {
                        if (fromSupermarket == null || !fromSupermarket.equals(targetSupermarket))
                        {
                            listener.onItemMoved(draggedItem, fromSupermarket, targetSupermarket);
                        }
                    }

                    break;

                case DragEvent.ACTION_DRAG_ENDED:
                    if (ghostView != null)
                    {
                        ViewGroup root = (ViewGroup) ghostView.getParent();

                        if (root != null)
                        {
                            root.removeView(ghostView);
                        }
                    }

                    View originalRow = (View) dragHandle.getTag(R.id.drag_original_row);

                    if (originalRow != null)
                    {
                        originalRow.setVisibility(View.VISIBLE);
                    }

                    upperClassFns.expandAllSupermarkets();
                    holder.itemView.setBackgroundColor(Color.TRANSPARENT);

                    break;
            }

            return true;
        });
    }

    private boolean containsSupermarket(Item item, Supermarket supermarket)
    {
        if (supermarket == unassigned_supermarket)
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
        }

        itemsBySupermarket.get(to).add(draggedItem);
        rebuildDisplaySupermarkets();
        notifyDataSetChanged();

        upperClassFns.updateItemCategory(draggedItem);
    };

    public void updateItems(ArrayList<Item> newItems)
    {
        this.category.setItemsFromFlat(newItems);

        restart();
        sortByExisting();
    }

    private boolean ifFinishedSupermarket(ArrayList<Item> items)
    {
        for (Item item : items)
        {
            if (!item.isChecked())
            {
                return false;
            }
        }

        return true;
    }
}
