package com.example.baskit.Categories;

import static com.example.baskit.Baskit.getAppColor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
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

import com.example.baskit.OnlineComponents.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.Item.ItemVariant;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class CategoryItemsAdapter extends RecyclerView.Adapter<CategoryItemsAdapter.ViewHolder>
{
    private Category category;

    private final ArrayList<Supermarket> baseSupermarkets;
    private ArrayList<Supermarket> supermarkets;
    private Map<Supermarket, ArrayList<Item>> itemsBySupermarket;

    private Map<Supermarket, Boolean> expandedStates;
    private Map<String, Integer> supermarketSectionCounts;

    private boolean draggable = false;

    private static final Supermarket unassigned_supermarket = Baskit.UNASSIGNED_SUPERMARKET;

    private final Activity activity;
    private final Context context;
    private ItemsAdapter.UpperClassFunctions upperClassFns;

    private final SupermarketItemsAdapter.OnItemMovedListener listener = (draggedItem, from, to) ->
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
            draggedItem.setSupermarketVariant(to, APIHandler.getInstance().buildVariant(draggedItem));
        }

        if (itemsBySupermarket.get(to) == null)
        {
            itemsBySupermarket.put(to, new ArrayList<>());
            expandedStates.put(to, true);
        }

        itemsBySupermarket.get(to).add(draggedItem);
        rebuildDisplaySupermarkets();
        notifyDataSetChanged();

        upperClassFns.updateItemCategory(draggedItem);
    };

    public CategoryItemsAdapter(Category category, Activity activity, Context context,
                                ItemsAdapter.UpperClassFunctions upperClassFns, ArrayList<Supermarket> supermarkets)
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

        // reset section counts
        supermarketSectionCounts.clear();

        for (Supermarket sm : itemsBySupermarket.keySet())
        {
            ArrayList<Item> list = itemsBySupermarket.get(sm);

            // count sections per supermarket name
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
        for (Item item : category.getItems())
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
        Supermarket supermarket = supermarkets.get(position);

        boolean isChosenSupermarket = false;

        // Unassigned supermarket should NEVER be treated as chosen
        if (supermarket == unassigned_supermarket)
        {
            holder.tvSupermarket.setText(supermarket.toString());
            holder.tvSupermarket.setTextColor(
                    Baskit.getAppColor(context, com.google.android.material.R.attr.colorPrimary)
            );
            holder.tvSupermarket.setAlpha(1f);
        }
        else
        {
            for (Supermarket base : baseSupermarkets)
            {
                if (base.getSupermarket().equals(supermarket.getSupermarket()))
                {
                    isChosenSupermarket = true;
                    break;
                }
            }

            // If this section does NOT belong to chosen supermarkets,
            // always show full "supermarket - section"
            if (!isChosenSupermarket)
            {
                holder.tvSupermarket.setText(supermarket.toString());
                holder.tvSupermarket.setTextColor(
                        Baskit.getAppColor(context, com.google.android.material.R.attr.colorPrimary)
                );
                holder.tvSupermarket.setAlpha(1f);
            }
            else if (isSingleSectionCurrently(supermarket))
            {
                holder.tvSupermarket.setText(supermarket.getDecodedSupermarket());
                holder.tvSupermarket.setTextColor(
                        Baskit.getAppColor(context, com.google.android.material.R.attr.colorOnSurface)
                );
            }
            else
            {
                holder.tvSupermarket.setText(supermarket.toString());
                holder.tvSupermarket.setTextColor(
                        Baskit.getAppColor(context, com.google.android.material.R.attr.colorOnSurface)
                );
            }
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

    private boolean containsSupermarket(Item item, Supermarket supermarket)
    {
        if (supermarket == unassigned_supermarket)
        {
            return true;
        }

        ArrayList<ItemVariant> rows = APIHandler.getInstance().buildVariant(item);
        if (rows == null) return false;

        for (ItemVariant row : rows)
        {
            if (item.isVariantOf(row, supermarket))
            {
                return true;
            }
        }

        return false;
    }

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


    public static class SupermarketItemsAdapter extends ItemsAdapter
    {
        private OnItemMovedListener listener;

        public interface OnItemMovedListener
        {
            void onItemMoved(Item item, Supermarket from, Supermarket to);
        }

        public SupermarketItemsAdapter(ArrayList<Item> items, OnItemMovedListener listener,
                                       Activity activity, Context context, UpperClassFunctions upperClassFns)
        {
            super(items, item -> {}, upperClassFns, activity, context);
            this.listener = listener;
        }

        @Override
        public void onBindViewHolder(@NonNull ItemsAdapter.ViewHolder holder, int position)
        {
            super.onBindViewHolder(holder, position);

            Item ogItem = items.get(position);
            Supermarket from = ogItem.getSupermarket();
            ImageView dragHandle = holder.itemView.findViewById(R.id.drag_handle);

            if (ogItem.isChecked())
            {
                dragHandle.setActivated(false);
                dragHandle.setEnabled(false);
                dragHandle.setOnLongClickListener(null);
                dragHandle.setAlpha(0.35f);
                return;
            }
            else
            {
                dragHandle.setActivated(true);
                dragHandle.setEnabled(true);
                dragHandle.setAlpha(1f);
            }

            dragHandle.setOnLongClickListener(v ->
            {
                if (ogItem.isChecked()) return false;

                ViewGroup rootLayout = activity.findViewById(android.R.id.content);

                holder.itemView.setDrawingCacheEnabled(true);
                android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(holder.itemView.getDrawingCache());
                holder.itemView.setDrawingCacheEnabled(false);

                ImageView ghostView = new ImageView(context);
                ghostView.setImageBitmap(bitmap);
                ghostView.setLayoutParams(new ViewGroup.LayoutParams(
                        holder.itemView.getWidth(),
                        holder.itemView.getHeight()));

                int[] rootLoc = new int[2];
                holder.itemView.getLocationOnScreen(rootLoc);

                int[] rootLayoutLoc = new int[2];
                rootLayout.getLocationOnScreen(rootLayoutLoc);
                int rootYOffset = rootLayoutLoc[1];

                ghostView.setX(rootLoc[0]);
                ghostView.setY(rootLoc[1] - rootYOffset);

                rootLayout.addView(ghostView);

                int itemHeight = holder.itemView.getHeight();
                int touchOffsetY = itemHeight / 2;

                v.setTag(R.id.drag_fake_view, ghostView);
                v.setTag(R.id.drag_touch_offset_y, touchOffsetY);
                v.setTag(R.id.drag_initial_x, rootLoc[0]);
                v.setTag(R.id.drag_root_y_offset, rootYOffset);
                v.setTag(R.id.drag_original_row, holder.itemView);

                View.DragShadowBuilder invisibleShadow = new View.DragShadowBuilder(holder.itemView)
                {
                    @Override
                    public void onDrawShadow(android.graphics.Canvas canvas) {}
                };

                ClipData data = ClipData.newPlainText("", "");
                upperClassFns.collapseAllSupermarkets();
                v.startDragAndDrop(data, invisibleShadow, v, 0);
                holder.itemView.setVisibility(View.INVISIBLE);

                v.setTag(R.id.drag_item, ogItem);
                v.setTag(R.id.drag_from_supermarket, from);

                return true;
            });

            holder.itemViewAlertDialog.setUpperClassFns(new UpperClassFunctions()
            {
                @Override
                public void updateItemCategory(Item item)
                {
                    Supermarket to = item.getSupermarket();

                    if (from != to)
                    {
                        listener.onItemMoved(item, from, to);
                    }
                    upperClassFns.updateItemCategory(item);
                }

                @Override
                public void removeItemCategory(Item item)
                {
                    upperClassFns.removeItemCategory(item);
                }

                @Override
                public void updateCategory() {}

                @Override
                public void removeCategory() {}
            });
        }
    }
}
