package com.example.baskit.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.baskit.Baskit;
import com.example.baskit.Categories.CategoryActivity;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.R;

import java.util.ArrayList;

public class CategoryAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<CategoryAdapter.ViewHolder>
{
    private final String listId;

    private ArrayList<Category> categories;

    private final Context context;

    CategoryAdapter(Context context, String listId, ArrayList<Category> categories)
    {
        this.context = context;
        this.listId = listId;
        this.categories = categories;
    }

    public static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder
    {
        TextView tvName, tvCount, tvPrice;
        LinearLayout loutInfo;

        ViewHolder(View v)
        {
            super(v);
            tvName = v.findViewById(R.id.tv_supermarket);
            tvCount = v.findViewById(R.id.tv_count);
            tvPrice = v.findViewById(R.id.tv_price);
            loutInfo = v.findViewById(R.id.lout_info);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType)
    {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_list_item, parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        Category category = categories.get(position);

        String text = "- " + category.getName();
        SpannableString spannable = new SpannableString(text);

        spannable.setSpan(
                new ForegroundColorSpan(
                        Baskit.getAppColor(holder.tvName.getContext(), com.google.android.material.R.attr.colorSecondary)
                ),
                0, 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        spannable.setSpan(
                new StyleSpan(Typeface.BOLD),
                0, 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        holder.tvName.setText(spannable);

        if (!category.isFinished())
        {
            holder.tvCount.setText(Integer.toString(category.countUnchecked()));
            holder.tvPrice.setText(Baskit.getTotalDisplayString(category.getTotal(), category.allPricesKnown(), false, false));
            holder.loutInfo.setVisibility(View.VISIBLE);
            holder.tvName.setAlpha(1f);
        }
        else
        {
            holder.loutInfo.setVisibility(View.GONE);
            holder.tvName.setAlpha(0.5f);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CategoryActivity.class);
            intent.putExtra("listId", listId);
            intent.putExtra("categoryName", category.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount()
    {
        return categories == null ? 0 : categories.size();
    }

    void update(ArrayList<Category> newData)
    {
        this.categories = newData;
        notifyDataSetChanged();
    }
}