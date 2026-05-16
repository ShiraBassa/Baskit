package com.example.baskit.Categories;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.OnlineComponents.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.MainComponents.Item.ItemInfo;
import com.example.baskit.MainComponents.Item.ItemVariant;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Map;

public class ItemViewPricesAdapter extends RecyclerView.Adapter<ItemViewPricesAdapter.ViewHolder>
{
    private ItemVariant selectedRow = null;

    private Map<String, Map<String, Double>> originalPricesMap;
    private ArrayList<ItemVariant> itemVariants;

    private final APIHandler apiHandler = APIHandler.getInstance();

    private Context context;
    private OnSupermarketClickListener listener;

    public interface OnSupermarketClickListener
    {
        void onSupermarketClick(ItemVariant row);
    }

    public ItemViewPricesAdapter(
            Context context,
            ArrayList<ItemVariant> rows,
            OnSupermarketClickListener onSupermarketClickListener)
    {
        this.context = context;
        this.listener = onSupermarketClickListener;
        this.itemVariants = (rows != null) ? rows : new ArrayList<>();
        this.originalPricesMap = null;

        sortRows();
    }

    private void sortRows()
    {
        if (itemVariants != null)
        {
            itemVariants.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        protected TextView tvSupermarketName, tvSectionName, tvPrice;
        protected TextView tvVariation;

        public ViewHolder(View itemView)
        {
            super(itemView);

            tvSupermarketName = itemView.findViewById(R.id.tv_supermarket_name);
            tvSectionName = itemView.findViewById(R.id.tv_supermarket);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvVariation = itemView.findViewById(R.id.tv_variation);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_view_prices_single, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position)
    {
        if (itemVariants == null || position < 0 || position >= itemVariants.size()) {
            return;
        }

        ItemVariant row = itemVariants.get(position);
        Supermarket supermarket = row.getSupermarket();
        double price = row.getPrice();
        ItemInfo matchedInfo = row.getInfo();

        if (matchedInfo != null)
        {
            String company = matchedInfo.getCompany();
            String measure = matchedInfo.getFullMeasureStr();

            String variationText = "";

            if (company != null && !company.isEmpty())
            {
                variationText += company;
            }

            if (measure != null && !measure.isEmpty())
            {
                if (!variationText.isEmpty()) variationText += " | ";
                variationText += measure;
            }

            if (!variationText.isEmpty())
            {
                holder.tvVariation.setText(variationText);
                holder.tvVariation.setVisibility(View.VISIBLE);
            }
            else
            {
                holder.tvVariation.setVisibility(View.GONE);
            }
        }
        else
        {
            holder.tvVariation.setVisibility(View.GONE);
        }

        holder.tvSupermarketName.setText(supermarket.getDecodedSupermarket());

        if (apiHandler.singleSectionInSupermarkets(supermarket))
        {
            holder.tvSectionName.setVisibility(View.GONE);
        }
        else
        {
            holder.tvSectionName.setText(supermarket.getDecodedSection());
        }

        holder.tvPrice.setText(Baskit.getTotalDisplayString(price, true, false, false));

        boolean isSelected = selectedRow != null && selectedRow.equals(row);

        if (isSelected)
        {
            holder.itemView.setBackgroundColor(
                    Baskit.getAppColor(context, com.google.android.material.R.attr.colorSecondaryContainer)
            );
        }
        else
        {
            holder.itemView.setBackgroundColor(
                    Baskit.getAppColor(context, com.google.android.material.R.attr.colorSurface)
            );
        }

        holder.itemView.setOnClickListener(v ->
        {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            ItemVariant clickedRow = itemVariants.get(pos);

            if (selectedRow != null && selectedRow.equals(clickedRow))
            {
                selectedRow = null;
                notifyDataSetChanged();

                if (listener != null)
                {
                    listener.onSupermarketClick(null);
                }
                return;
            }

            selectedRow = clickedRow;
            notifyDataSetChanged();

            if (listener != null)
            {
                listener.onSupermarketClick(clickedRow);
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return itemVariants == null ? 0 : itemVariants.size();
    }

    public void setSelectedPosition(int position)
    {
        if (position >= 0 && position < itemVariants.size())
        {
            this.selectedRow = itemVariants.get(position);
        }
        else
        {
            this.selectedRow = null;
        }

        notifyDataSetChanged();
    }
}
