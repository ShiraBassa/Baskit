package com.example.baskit.Categories;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.Supermarket;

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

        holder.itemView.setOnLongClickListener(v -> {
            ClipData data = ClipData.newPlainText("", "");
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            v.startDragAndDrop(data, shadowBuilder, ogItem, 0);
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
