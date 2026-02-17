package com.example.baskit.List;

import android.graphics.Color;

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
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;
import com.google.android.material.transition.Hold;

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

        holder.tvName.setTextColor(
                Baskit.getAppColor(context, R.color.plan_screen_base)
        );
        holder.tvQuantity.setTextColor(
                Baskit.getAppColor(context, R.color.plan_screen_base)
        );
        holder.tvPrice.setTextColor(
                Baskit.getAppColor(context, R.color.plan_screen_base)
        );
        holder.dragHandle.setColorFilter(
                Baskit.getAppColor(context, R.color.plan_screen_base)
        );

        if (selectedSupermarketParent == supermarket)
        {
            holder.tvName.setTextColor(
                    Baskit.getAppColor(context, R.color.plan_screen_chosen)
            );
            holder.tvPrice.setTextColor(
                    Baskit.getAppColor(context, R.color.plan_screen_chosen)
            );
            holder.tvQuantity.setTextColor(
                    Baskit.getAppColor(context, R.color.plan_screen_chosen)
            );
        }
        else if (selectedSupermarketParent != null && itemHasSelectedSupermarket(item))
        {
            holder.dragHandle.setColorFilter(
                    Baskit.getAppColor(context, R.color.plan_screen_chosen)
            );
            holder.dragHandle.setVisibility(View.VISIBLE);

            showItemPriceDif(holder, item);
        }
        else
        {
            holder.tvName.setTextColor(
                    Baskit.getAppColor(context, R.color.plan_screen_unavailable)
            );
            holder.tvQuantity.setTextColor(
                    Baskit.getAppColor(context, R.color.plan_screen_unavailable)
            );
            holder.tvPrice.setTextColor(
                    Baskit.getAppColor(context, R.color.plan_screen_unavailable)
            );
        }

        setItemButtons(holder, item);
    }

    private void showItemPriceDif(ViewHolder holder, Item item)
    {
        double priceOther = getPrice(item, selectedSupermarketParent);
        double priceDif = priceOther - item.getTotal();

        String priceStr = Baskit.getTotalDisplayString(priceOther, true, false, false);
        String difStr = Baskit.getTotalDisplayString(priceDif, true, false, false);

        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(priceStr);

        if (supermarket == Baskit.UNASSIGNED_SUPERMARKET)
        {
            holder.tvPrice.setVisibility(View.VISIBLE);
            holder.tvPrice.setTextColor(
                    Baskit.getAppColor(context, R.color.plan_screen_chosen)
            );
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
                    new ForegroundColorSpan(
                            Baskit.getAppColor(context, R.color.plan_screen_chosen)
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        holder.tvPrice.setText(builder);
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

    private double getPrice(Item item, Supermarket otherSupermarket)
    {
        String absoluteId = item.getAbsoluteId();

        if (!itemPrices.containsKey(absoluteId)) return 0;
        Map<String, Map<String, Double>> currItemPrices = itemPrices.get(absoluteId);

        if (!currItemPrices.get(otherSupermarket.getSupermarket()).containsKey(otherSupermarket.getSection())) return 0;
        return item.getTotal(currItemPrices.get(otherSupermarket.getSupermarket()).get(otherSupermarket.getSection()));
    }
}
