package com.example.baskit.Categories;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import androidx.core.content.ContextCompat;

import com.example.baskit.MainComponents.Item;
import com.example.baskit.R;

import java.util.ArrayList;

public class ItemsAdapterChecked extends ItemsAdapter
{
    public ItemsAdapterChecked(ArrayList<Item> items,
                               OnItemClickListener listener,
                               UpperClassFunctions upperClassFns,
                               ItemsListHandler.EmptyCategoryCase emptyCategory,
                               Activity activity,
                               Context context)
    {
        super(items, listener, upperClassFns, emptyCategory, activity, context);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        int checkedColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.checked);

        holder.tvName.setTextColor(checkedColor);
        holder.tvQuantity.setTextColor(checkedColor);
        holder.tvSupermarket.setTextColor(checkedColor);
        holder.tvPrice.setTextColor(checkedColor);
        holder.btnCheckBox.setImageResource(R.drawable.ic_check_box_checked);

        super.onBindViewHolder(holder, position);

        holder.btnUp.setVisibility(View.INVISIBLE);
        holder.btnUp.setClickable(false);
        holder.btnDown.setVisibility(View.INVISIBLE);
        holder.btnDown.setClickable(false);
    }
}
