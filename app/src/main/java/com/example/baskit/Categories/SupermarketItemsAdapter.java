package com.example.baskit.Categories;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.view.DragEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import java.util.ArrayList;

public class SupermarketItemsAdapter extends ItemsAdapter
{
    private OnItemMovedListener listener;

    public interface OnItemMovedListener
    {
        void onItemMoved(Item item, Supermarket from, Supermarket to);
    }

    public SupermarketItemsAdapter(ArrayList<Item> items, OnItemMovedListener listener,
                                   Activity activity, Context context, UpperClassFunctions upperClassFns) {
        super(items, item -> {}, upperClassFns, activity, context);
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull ItemsAdapter.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        Item ogItem = items.get(position);
        Supermarket from = ogItem.getSupermarket();
        ImageView dragHandle = holder.itemView.findViewById(R.id.drag_handle);

        dragHandle.setOnLongClickListener(v -> {
            ClipData data = ClipData.newPlainText("", "");
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);

            upperClassFns.collapseAllSupermarkets();

            v.startDragAndDrop(data, shadowBuilder, v, 0);
            v.setVisibility(View.INVISIBLE);
            v.setTag(R.id.drag_item, ogItem);
            v.setTag(R.id.drag_from_supermarket, from);

            return true;
        });

        holder.itemViewAlertDialog.setUpperClassFns(new UpperClassFunctions() {
            @Override
            public void updateItemCategory(Item item) {
                Supermarket to = item.getSupermarket();
                if (from != to) {
                    listener.onItemMoved(item, from, to);
                }
                upperClassFns.updateItemCategory(item);
            }

            @Override
            public void removeItemCategory(Item item) {
                upperClassFns.removeItemCategory(item);
            }

            @Override
            public void updateCategory() {}

            @Override
            public void removeCategory() {}
        });
    }
}
