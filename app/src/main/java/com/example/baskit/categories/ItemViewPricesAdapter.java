package com.example.baskit.categories;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.online_components.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.main_components.Item.ItemInfo;
import com.example.baskit.main_components.Item.ItemVariant;
import com.example.baskit.main_components.Supermarket;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Comparator;

@SuppressWarnings("deprecation")
public class ItemViewPricesAdapter extends RecyclerView.Adapter<ItemViewPricesAdapter.ViewHolder>
{
    private ItemVariant selectedVariant = null;

    private final ArrayList<ItemVariant> itemVariants;

    private final APIHandler apiHandler = APIHandler.getInstance();

    private final Context context;
    private final OnSupermarketClickListener listener;

    public interface OnSupermarketClickListener
    {
        void onSupermarketClick(ItemVariant variant);
    }

    public ItemViewPricesAdapter(
            Context context,
            ArrayList<ItemVariant> variants,
            OnSupermarketClickListener onSupermarketClickListener)
    {
        this.context = context;
        this.listener = onSupermarketClickListener;
        this.itemVariants = (variants != null) ? variants : new ArrayList<>();

        sortVariants();
    }

    private void sortVariants()
    {
        if (itemVariants != null)
        {
            itemVariants.removeIf(variant -> variant == null || variant.getSupermarket() == null);

            itemVariants.sort((a, b) ->
            {
                double priceA = a != null ? a.getPrice() : Double.MAX_VALUE;
                double priceB = b != null ? b.getPrice() : Double.MAX_VALUE;

                if (Double.isNaN(priceA) || Double.isInfinite(priceA))
                {
                    priceA = Double.MAX_VALUE;
                }

                if (Double.isNaN(priceB) || Double.isInfinite(priceB))
                {
                    priceB = Double.MAX_VALUE;
                }

                return Double.compare(priceA, priceB);
            });
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        protected final TextView tvSupermarketName;
        protected final TextView tvSectionName;
        protected final TextView tvPrice;
        protected final TextView tvVariation;

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

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position)
    {
        if (itemVariants == null || position < 0 || position >= itemVariants.size()) {
            return;
        }

        ItemVariant variant = itemVariants.get(position);

        if (variant == null)
        {
            return;
        }

        Supermarket supermarket = variant.getSupermarket();

        if (supermarket == null)
        {
            return;
        }

        double price = variant.getPrice();

        if (Double.isNaN(price) || Double.isInfinite(price))
        {
            price = 0.0;
        }

        ItemInfo matchedInfo = variant.getInfo();

        if (matchedInfo != null)
        {
            String company = matchedInfo.getCompany();
            String measure = matchedInfo.getFullMeasureStr();

            String variationText = "";

            if (company != null && !company.isBlank())
            {
                variationText += company;
            }

            if (measure != null && !measure.isBlank())
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

        String supermarketName = supermarket.getDecodedSupermarket();

        holder.tvSupermarketName.setText(
                supermarketName != null && !supermarketName.isBlank()
                        ? supermarketName
                        : Baskit.getAppStr(R.string.unknown_supermarket)
        );

        if (apiHandler.singleSectionInSupermarkets(supermarket))
        {
            holder.tvSectionName.setVisibility(View.GONE);
        }
        else
        {
            String sectionName = supermarket.getDecodedSection();

            holder.tvSectionName.setText(
                    sectionName != null && !sectionName.isBlank()
                            ? sectionName
                            : Baskit.getAppStr(R.string.unknown_section)
            );

            holder.tvSectionName.setVisibility(View.VISIBLE);
        }

        holder.tvPrice.setText(Baskit.getTotalDisplayString(price, true, false, false));

        boolean isSelected = selectedVariant != null && selectedVariant.equals(variant);

        holder.itemView.setSelected(isSelected);
        holder.itemView.setActivated(isSelected);
        holder.itemView.refreshDrawableState();

        holder.itemView.setOnClickListener(v ->
        {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            ItemVariant clickedVariant = itemVariants.get(pos);

            if (clickedVariant == null)
            {
                return;
            }

            if (selectedVariant != null && selectedVariant.equals(clickedVariant))
            {
                ItemVariant previous = selectedVariant;
                selectedVariant = null;

                int previousIndex = itemVariants.indexOf(previous);

                if (previousIndex >= 0 && previousIndex < itemVariants.size())
                {
                    notifyItemChanged(previousIndex);
                }

                if (listener != null)
                {
                    listener.onSupermarketClick(null);
                }
                return;
            }

            ItemVariant previous = selectedVariant;
            selectedVariant = clickedVariant;

            int previousIndex = itemVariants.indexOf(previous);

            if (previousIndex >= 0 && previousIndex < itemVariants.size())
            {
                notifyItemChanged(previousIndex);
            }

            notifyItemChanged(pos);

            if (listener != null)
            {
                listener.onSupermarketClick(clickedVariant);
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return itemVariants == null ? 0 : itemVariants.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSelectedPosition(int position)
    {
        if (itemVariants != null && position >= 0 && position < itemVariants.size())
        {
            this.selectedVariant = itemVariants.get(position);
        }
        else
        {
            this.selectedVariant = null;
        }

        notifyDataSetChanged();
    }
}
