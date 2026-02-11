package com.example.baskit.Categories;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
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
