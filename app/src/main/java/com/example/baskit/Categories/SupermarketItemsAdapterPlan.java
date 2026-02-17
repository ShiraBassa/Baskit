package com.example.baskit.Categories;

import android.graphics.Color;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import com.example.baskit.Baskit;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Map;

public class SupermarketItemsAdapterPlan extends ItemsAdapter
{
    private final Map<String, Map<String, Map<String, Double>>> itemPrices;
    private final Supermarket supermarket, selectedSupermarketParent;
    private final SupermarketItemsAdapter.OnItemMovedListener onItemMovedListener;

    public SupermarketItemsAdapterPlan(ArrayList<Item> items,
                                       Activity activity, Context context,
                                       UpperClassFunctions upperClassFns,
                                       Supermarket supermarket,
                                       Supermarket selectedSupermarketParent,
                                       Map<String, Map<String, Map<String, Double>>> itemPrices,
                                       SupermarketItemsAdapter.OnItemMovedListener onItemMovedListener)
    {
        super(items, item -> {}, upperClassFns, activity, context);

        this.supermarket = supermarket;
        this.selectedSupermarketParent = selectedSupermarketParent;
        this.itemPrices = itemPrices;
        this.onItemMovedListener = onItemMovedListener;
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

        holder.tvName.setTextColor(Color.BLACK);
        holder.tvQuantity.setTextColor(Color.BLACK);
        holder.tvPrice.setTextColor(Color.BLACK);
        holder.dragHandle.setColorFilter(Color.BLACK);

        if (selectedSupermarketParent == supermarket)
        {
            holder.tvName.setTextColor(
                    Baskit.getAppColor(context, com.google.android.material.R.attr.colorPrimaryVariant)
            );
            holder.tvPrice.setTextColor(
                    Baskit.getAppColor(context, com.google.android.material.R.attr.colorPrimaryVariant)
            );
            holder.tvQuantity.setTextColor(
                    Baskit.getAppColor(context, com.google.android.material.R.attr.colorPrimaryVariant)
            );
        }
        else
        {
            if (selectedSupermarketParent != null)
            {
                if (itemHasSelectedSupermarket(item))
                {
                    holder.tvPrice.setTextColor(
                            Baskit.getAppColor(context, com.google.android.material.R.attr.colorPrimaryVariant)
                    );
                    holder.dragHandle.setColorFilter(
                            Baskit.getAppColor(context, com.google.android.material.R.attr.colorPrimaryVariant)
                    );
                    holder.dragHandle.setVisibility(View.VISIBLE);
                }
                else
                {
                    holder.tvName.setTextColor(Color.GRAY);
                    holder.tvQuantity.setTextColor(Color.GRAY);
                    holder.tvPrice.setTextColor(Color.GRAY);
                }
            }
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

    private boolean itemHasSelectedSupermarket(Item item)
    {
        String absoluteId = item.getAbsoluteId();

        if (!itemPrices.containsKey(absoluteId)) return false;
        Map<String, Map<String, Double>> currItemPrices = itemPrices.get(absoluteId);

        if (!currItemPrices.containsKey(selectedSupermarketParent.getSupermarket()))
        {
            return false;
        }

        return currItemPrices.get(selectedSupermarketParent.getSupermarket()).containsKey(selectedSupermarketParent.getSection());
    }
}
