package com.example.baskit.categories;

import static com.example.baskit.Baskit.getAppColor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DiffUtil;

import com.example.baskit.online_components.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.main_components.Category;
import com.example.baskit.main_components.Item;
import com.example.baskit.main_components.Item.ItemVariant;
import com.example.baskit.main_components.Supermarket;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

@SuppressWarnings("DataFlowIssue")
public class CategoryItemsAdapter extends RecyclerView.Adapter<CategoryItemsAdapter.ViewHolder>
{
    private final Category category;

    private final ArrayList<Supermarket> baseSupermarkets;
    private ArrayList<Supermarket> supermarkets;
    private Map<Supermarket, ArrayList<Item>> itemsBySupermarket;

    private Map<Supermarket, Boolean> expandedStates;
    private Map<String, Integer> supermarketSectionCounts;

    private boolean draggable = false;
    private String lastItemsSignature = "";
    private static final Supermarket unassigned_supermarket = Baskit.UNASSIGNED_SUPERMARKET;

    private final Activity activity;
    private final Context context;
    private final ItemsAdapter.UpperClassFunctions upperClassFns;

    @SuppressLint("NotifyDataSetChanged")
    private final SupermarketItemsAdapter.OnItemMovedListener listener;

    @SuppressLint("NotifyDataSetChanged")
    public CategoryItemsAdapter(Category category, Activity activity, Context context,
                                ItemsAdapter.UpperClassFunctions upperClassFns, ArrayList<Supermarket> supermarkets)
    {
        this.category = category;
        if (category == null)
        {
            throw new IllegalArgumentException("Category cannot be null");
        }
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

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void collapseAllSupermarkets()
            {
                if (expandedStates == null)
                {
                    return;
                }
                expandedStates.replaceAll((s, v) -> false);

                draggable = true;
                notifyDataSetChanged();
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void expandAllSupermarkets()
            {
                if (expandedStates == null)
                {
                    return;
                }
                expandedStates.replaceAll((s, v) -> true);

                draggable = false;
                notifyDataSetChanged();
            }
        };

        this.baseSupermarkets = supermarkets != null ? new ArrayList<>(supermarkets) : new ArrayList<>();
        this.supermarkets = new ArrayList<>(this.baseSupermarkets);

        this.listener = (draggedItem, from, to) ->
        {
            if (draggedItem == null || to == null || itemsBySupermarket == null)
            {
                return;
            }

            String draggedId = draggedItem.getAbsoluteId();

            for (Map.Entry<Supermarket, ArrayList<Item>> entry : itemsBySupermarket.entrySet())
            {
                if (entry == null || entry.getValue() == null)
                {
                    continue;
                }

                entry.getValue().removeIf(existingItem ->
                        existingItem != null &&
                                Objects.equals(existingItem.getAbsoluteId(), draggedId));
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

            Objects.requireNonNull(itemsBySupermarket.get(to)).add(draggedItem);
            draggedItem.setChecked(false);
            ArrayList<Supermarket> oldSupermarkets =
                    new ArrayList<>(supermarkets);

            rebuildDisplaySupermarkets();

            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                    new SupermarketDiffCallback(oldSupermarkets, supermarkets)
            );

            diffResult.dispatchUpdatesTo(CategoryItemsAdapter.this);

            this.upperClassFns.updateItemCategory(draggedItem);
        };

        restart();
        sortByExisting();
    }

    private static class SupermarketDiffCallback extends DiffUtil.Callback
    {
        private final ArrayList<Supermarket> oldList;
        private final ArrayList<Supermarket> newList;

        public SupermarketDiffCallback(ArrayList<Supermarket> oldList,
                                       ArrayList<Supermarket> newList)
        {
            this.oldList = oldList != null ? oldList : new ArrayList<>();
            this.newList = newList != null ? newList : new ArrayList<>();
        }

        @Override
        public int getOldListSize()
        {
            return oldList.size();
        }

        @Override
        public int getNewListSize()
        {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition,
                                       int newItemPosition)
        {
            Supermarket oldItem = oldList.get(oldItemPosition);
            Supermarket newItem = newList.get(newItemPosition);

            return Objects.equals(oldItem, newItem);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition,
                                          int newItemPosition)
        {
            Supermarket oldItem = oldList.get(oldItemPosition);
            Supermarket newItem = newList.get(newItemPosition);

            return Objects.equals(oldItem, newItem);
        }
    }

    private void restart()
    {
        if (baseSupermarkets == null)
        {
            supermarkets = new ArrayList<>();
            itemsBySupermarket = new HashMap<>();
            expandedStates = new HashMap<>();
            supermarketSectionCounts = new HashMap<>();
            return;
        }

        itemsBySupermarket = new HashMap<>();
        expandedStates = new HashMap<>();
        supermarketSectionCounts = new HashMap<>();

        for (Supermarket supermarket : baseSupermarkets)
        {
            if (supermarket == null)
            {
                continue;
            }
            itemsBySupermarket.put(supermarket, new ArrayList<>());
            expandedStates.put(supermarket, true);
        }

        itemsBySupermarket.put(unassigned_supermarket, new ArrayList<>());
        expandedStates.put(unassigned_supermarket, true);

        rebuildDisplaySupermarkets();
    }

    private void rebuildDisplaySupermarkets()
    {
        if (itemsBySupermarket == null)
        {
            supermarkets = new ArrayList<>();
            return;
        }

        ArrayList<Supermarket> nonEmptyBase = new ArrayList<>();
        ArrayList<Supermarket> emptyBase = new ArrayList<>();
        ArrayList<Supermarket> dynamicNonEmpty = new ArrayList<>();
        ArrayList<Supermarket> dynamicEmpty = new ArrayList<>();

        if (supermarketSectionCounts == null)
        {
            supermarketSectionCounts = new HashMap<>();
        }
        else
        {
            supermarketSectionCounts.clear();
        }

        for (Supermarket supermarket : baseSupermarkets)
        {
            if (supermarket == null)
            {
                continue;
            }

            ArrayList<Item> items = itemsBySupermarket.get(supermarket);

            String name = supermarket.getSupermarket();

            if (name == null)
            {
                name = Baskit.getAppStr(R.string.unknown_supermarket);
            }

            supermarketSectionCounts.put(
                    name,
                    supermarketSectionCounts.getOrDefault(name, 0) + 1
            );

            if (items != null && !items.isEmpty())
            {
                nonEmptyBase.add(supermarket);
            }
            else
            {
                emptyBase.add(supermarket);
            }
        }

        for (Map.Entry<Supermarket, ArrayList<Item>> entry : itemsBySupermarket.entrySet())
        {
            Supermarket supermarket = entry.getKey();

            if (supermarket == null ||
                    supermarket == unassigned_supermarket ||
                    baseSupermarkets.contains(supermarket))
            {
                continue;
            }

            ArrayList<Item> items = entry.getValue();

            String name = supermarket.getSupermarket();

            if (name == null)
            {
                name = Baskit.getAppStr(R.string.unknown_supermarket);
            }

            supermarketSectionCounts.put(
                    name,
                    supermarketSectionCounts.getOrDefault(name, 0) + 1
            );

            if (items != null && !items.isEmpty())
            {
                dynamicNonEmpty.add(supermarket);
            }
            else
            {
                dynamicEmpty.add(supermarket);
            }
        }

        ArrayList<Item> unassignedItems = itemsBySupermarket.get(unassigned_supermarket);

        ArrayList<Supermarket> ordered = new ArrayList<>();

        ordered.addAll(nonEmptyBase);
        ordered.addAll(dynamicNonEmpty);
        ordered.addAll(emptyBase);
        ordered.addAll(dynamicEmpty);

        if (unassignedItems != null && !unassignedItems.isEmpty())
        {
            ordered.add(unassigned_supermarket);
        }
        else
        {
            ordered.add(unassigned_supermarket);
        }

        supermarkets = ordered;
    }

    private String buildItemsSignature(ArrayList<Item> items)
    {
        if (items == null)
        {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (Item item : items)
        {
            if (item == null)
            {
                continue;
            }

            builder.append(item.getAbsoluteId())
                    .append('|')
                    .append(item.getBaseName())
                    .append('|')
                    .append(item.getQuantity())
                    .append('|')
                    .append(item.isChecked())
                    .append('|');

            Supermarket supermarket = item.getSupermarket();

            if (supermarket != null)
            {
                builder.append(supermarket.toString());
            }

            builder.append('|')
                    .append(item.getPrice())
                    .append(';');
        }

        return builder.toString();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void sortByExisting()
    {
        if (category == null || category.getItems() == null)
        {
            return;
        }

        if (itemsBySupermarket != null)
        {
            for (ArrayList<Item> supermarketItems : itemsBySupermarket.values())
            {
                if (supermarketItems != null)
                {
                    supermarketItems.clear();
                }
            }
        }

        for (Item item : category.getItems())
        {
            if (item == null)
            {
                continue;
            }
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

        ArrayList<Supermarket> oldSupermarkets =
                new ArrayList<>(supermarkets != null
                        ? supermarkets
                        : new ArrayList<>());

        rebuildDisplaySupermarkets();

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new SupermarketDiffCallback(oldSupermarkets, supermarkets)
        );

        diffResult.dispatchUpdatesTo(this);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        protected final TextView tvSupermarket;
        protected final ImageButton btnExpand;
        protected final RecyclerView recyclerItems;

        protected SupermarketItemsAdapter adapter;

        protected ItemTouchHelper itemTouchHelper;

        public ViewHolder(View itemView)
        {
            super(itemView);

            tvSupermarket = itemView.findViewById(R.id.tv_supermarket_name);
            btnExpand = itemView.findViewById(R.id.btn_expand);
            recyclerItems = itemView.findViewById(R.id.recycler_items);
            recyclerItems.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_supermarket, parent, false);

        ViewHolder holder = new ViewHolder(view);

        holder.adapter = new SupermarketItemsAdapter(
                new ArrayList<>(),
                activity,
                context,
                upperClassFns
        );

        holder.recyclerItems.setAdapter(holder.adapter);

        holder.itemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(
                        ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                        0)
                {
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
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder,
                                         int direction) {}
                });

        holder.itemTouchHelper.attachToRecyclerView(holder.recyclerItems);

        return holder;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position)
    {
        if (position < 0 || position >= supermarkets.size())
        {
            return;
        }

        if (activity.isFinishing() || activity.isDestroyed())
        {
            return;
        }
        holder.itemView.setOnDragListener(null);
        Supermarket supermarket = supermarkets.get(position);
        if (supermarket == null)
        {
            return;
        }

        boolean isChosenSupermarket = false;

        // Unassigned supermarket should NEVER be treated as chosen
        if (supermarket == unassigned_supermarket)
        {
            holder.tvSupermarket.setText(supermarket.toString());
            holder.tvSupermarket.setTextColor(
                    Baskit.getAppColor(context, androidx.appcompat.R.attr.colorPrimary)
            );
            holder.tvSupermarket.setAlpha(0.96f);
        }
        else
        {
            for (Supermarket base : baseSupermarkets)
            {
                if (base != null &&
                        Objects.equals(base.getSupermarket(), supermarket.getSupermarket()))
                {
                    isChosenSupermarket = true;
                    break;
                }
            }

            if (!isChosenSupermarket)
            {
                holder.tvSupermarket.setText(supermarket.toString());
                holder.tvSupermarket.setTextColor(
                        Baskit.getAppColor(context, androidx.appcompat.R.attr.colorPrimary)
                );
                holder.tvSupermarket.setAlpha(0.96f);
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
        if (items == null)
        {
            items = new ArrayList<>();
        }
        if (holder.adapter != null)
        {
            holder.adapter.updateItems(items);
        }

        Boolean expanded = expandedStates.get(supermarket);
        boolean isExpanded = expanded == null || expanded;
        holder.recyclerItems.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.btnExpand.setRotation(isExpanded ? 180 : 0);

        if (ifFinishedSupermarket(items))
        {
            holder.tvSupermarket.setAlpha(0.55f);
            holder.btnExpand.setAlpha(0.55f);
        }
        else
        {
            holder.tvSupermarket.setAlpha(0.96f);
            holder.btnExpand.setAlpha(0.92f);
        }

        holder.btnExpand.setOnClickListener(v ->
        {
            if (expandedStates == null)
            {
                return;
            }
            expandedStates.put(supermarket, !isExpanded);
            notifyItemChanged(position);
        });


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
                    if (draggedItem != null &&
                            !containsSupermarket(draggedItem, supermarkets.get(position)))
                    {
                        holder.itemView.setBackgroundColor(
                                Baskit.getAppColor(context, com.google.android.material.R.attr.colorErrorContainer));
                        holder.itemView.setAlpha(0.92f);
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
                    if (draggedItem != null &&
                            containsSupermarket(draggedItem, supermarkets.get(position)))
                    {
                        holder.itemView.setBackgroundColor(getAppColor(context, com.google.android.material.R.attr.colorSecondaryContainer));
                        holder.itemView.setAlpha(0.96f);
                    }

                    break;

                case DragEvent.ACTION_DRAG_EXITED:
                    if (draggedItem != null &&
                            containsSupermarket(draggedItem, supermarkets.get(position)))
                    {
                        holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                        holder.itemView.setAlpha(1f);
                    }

                    break;

                case DragEvent.ACTION_DROP:

                    if (draggedItem == null)
                    {
                        break;
                    }
                    if (containsSupermarket(draggedItem, supermarket))
                    {
                        if (fromSupermarket == null || !fromSupermarket.equals(supermarket))
                        {
                            listener.onItemMoved(draggedItem, fromSupermarket, supermarket);
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

                    View ogVariant = (View) dragHandle.getTag(R.id.drag_og_variant);

                    if (ogVariant != null)
                    {
                        ogVariant.setVisibility(View.VISIBLE);
                    }

                    if (upperClassFns == null)
                    {
                        break;
                    }
                    upperClassFns.expandAllSupermarkets();
                    holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                    holder.itemView.setAlpha(1f);

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
        if (current == null)
        {
            return true;
        }
        if (supermarketSectionCounts == null) return true;

        String name = current.getSupermarket();
        Integer count = supermarketSectionCounts.get(name);

        return count == null || count <= 1;
    }

    private boolean containsSupermarket(Item item, Supermarket supermarket)
    {
        if (item == null || supermarket == null)
        {
            return false;
        }
        if (supermarket == unassigned_supermarket)
        {
            return true;
        }

        ArrayList<ItemVariant> variants = APIHandler.getInstance().buildVariant(item);
        if (variants == null) return false;

        for (ItemVariant variant : variants)
        {
            if (item.isVariantOf(variant, supermarket))
            {
                return true;
            }
        }

        return false;
    }

    public void updateItems(ArrayList<Item> newItems)
    {
        ArrayList<Item> safeItems =
                newItems != null ? newItems : new ArrayList<>();

        String newSignature = buildItemsSignature(safeItems);

        if (newSignature.equals(lastItemsSignature))
        {
            return;
        }

        lastItemsSignature = newSignature;

        this.category.setItemsFromFlat(safeItems);

        restart();
        sortByExisting();
    }

    private boolean ifFinishedSupermarket(ArrayList<Item> items)
    {
        if (items == null || items.isEmpty())
        {
            return false;
        }
        for (Item item : items)
        {
            if (item == null)
            {
                continue;
            }
            if (!item.isChecked())
            {
                return false;
            }
        }

        return true;
    }


    public static class SupermarketItemsAdapter extends ItemsAdapter
    {
        public interface OnItemMovedListener
        {
            void onItemMoved(Item item, Supermarket from, Supermarket to);
        }

        private static class ItemDiffCallback extends DiffUtil.Callback
        {
            private final ArrayList<Item> oldList;
            private final ArrayList<Item> newList;

            public ItemDiffCallback(ArrayList<Item> oldList,
                                    ArrayList<Item> newList)
            {
                this.oldList = oldList != null ? oldList : new ArrayList<>();
                this.newList = newList != null ? newList : new ArrayList<>();
            }

            @Override
            public int getOldListSize()
            {
                return oldList.size();
            }

            @Override
            public int getNewListSize()
            {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition,
                                           int newItemPosition)
            {
                Item oldItem = oldList.get(oldItemPosition);
                Item newItem = newList.get(newItemPosition);

                if (oldItem == null || newItem == null)
                {
                    return false;
                }

                return Objects.equals(
                        oldItem.getAbsoluteId(),
                        newItem.getAbsoluteId()
                );
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition,
                                              int newItemPosition)
            {
                Item oldItem = oldList.get(oldItemPosition);
                Item newItem = newList.get(newItemPosition);

                return Objects.equals(oldItem, newItem);
            }
        }

        public SupermarketItemsAdapter(ArrayList<Item> items, Activity activity, Context context, UpperClassFunctions upperClassFns)
        {
            super(items, upperClassFns, activity, context);
        }

        public void updateItems(ArrayList<Item> newItems)
        {
            ArrayList<Item> safeNewItems =
                    newItems != null ? newItems : new ArrayList<>();

            ArrayList<Item> oldItems = new ArrayList<>(this.items);

            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                    new ItemDiffCallback(oldItems, safeNewItems)
            );

            this.items.clear();
            this.items.addAll(safeNewItems);

            diffResult.dispatchUpdatesTo(this);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemsAdapter.ViewHolder holder, int position)
        {
            if (position < 0 || position >= items.size())
            {
                return;
            }
            super.onBindViewHolder(holder, position);

            Item ogItem = items.get(position);
            if (ogItem == null)
            {
                return;
            }
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
                if (rootLayout == null)
                {
                    return false;
                }

                if (holder.itemView.getWidth() <= 0 || holder.itemView.getHeight() <= 0)
                {
                    holder.itemView.setDrawingCacheEnabled(false);
                    return false;
                }
                holder.itemView.setDrawingCacheEnabled(true);
                android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(holder.itemView.getDrawingCache());
                holder.itemView.setDrawingCacheEnabled(false);
                if (bitmap == null)
                {
                    holder.itemView.setDrawingCacheEnabled(false);
                    return false;
                }

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
                v.setTag(R.id.drag_og_variant, holder.itemView);

                View.DragShadowBuilder invisibleShadow = new View.DragShadowBuilder(holder.itemView)
                {
                    @Override
                    public void onDrawShadow(@NonNull android.graphics.Canvas canvas) {}
                };

                ClipData data = ClipData.newPlainText("", "");
                upperClassFns.collapseAllSupermarkets();
                v.startDragAndDrop(data, invisibleShadow, v, 0);
                holder.itemView.setVisibility(View.INVISIBLE);

                v.setTag(R.id.drag_item, ogItem);
                v.setTag(R.id.drag_from_supermarket, from);

                return true;
            });
        }
    }
}
