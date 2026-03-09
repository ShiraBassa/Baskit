package com.example.baskit.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import androidx.annotation.NonNull;

import com.example.baskit.Baskit;
import com.example.baskit.Categories.ItemsAdapter;
import com.example.baskit.Categories.SupermarketItemsAdapter;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.PriceRow;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Map;

public class SupermarketItemsAdapterPlan extends ItemsAdapter
{
    private final Supermarket supermarket, selectedSupermarketParent;

    private final Map<String, Map<String, Map<String, Double>>> itemPrices;

    private final int colorBase;
    @SuppressLint("PrivateResource")
    private final int colorUnavailable;
    private final int colorChosen;

    private final SupermarketItemsAdapter.OnItemMovedListener onItemMovedListener;

    @SuppressLint("PrivateResource")
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

        Map<String, ArrayList<PriceRow>> rowsMap =
                com.example.baskit.API.APIHandler.getInstance().buildRows(single);

        ArrayList<PriceRow> rows =
                rowsMap.get(item.getBaseName());

        if (rows != null)
        {
            PriceRow row = item.getSupermarketRow(otherSupermarket, rows);

            if (row != null)
            {
                return row.getPrice();
            }
        }

        return 0.0;
    }
}
