package com.example.baskit.Categories;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.MainComponents.ItemInfo;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Map;

public class ItemViewPricesAdapter extends RecyclerView.Adapter<ItemViewPricesAdapter.ViewHolder>
{
    public static class PriceRow
    {
        private Supermarket supermarket;
        private double price;
        private ItemInfo info;

        public PriceRow(Supermarket supermarket, double price, ItemInfo info)
        {
            this.supermarket = supermarket;
            this.price = price;
            this.info = info;
        }

        public Supermarket getSupermarket() {
            return supermarket;
        }

        public void setSupermarket(Supermarket supermarket) {
            this.supermarket = supermarket;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public ItemInfo getInfo() {
            return info;
        }

        public void setInfo(ItemInfo info) {
            this.info = info;
        }
    }

    private ArrayList<PriceRow> priceRows;
    private OnSupermarketClickListener listener;
    private String selectedSupermarket, selectedSection;
    private int selectedPosition = -1;
    private final APIHandler apiHandler = APIHandler.getInstance();
    private Context context;
    private Map<String, Map<String, Double>> originalPricesMap;

    private ArrayList<ItemInfo> variations;

    public interface OnSupermarketClickListener
    {
        void onSupermarketClick(Supermarket supermarket, ItemInfo variation);
    }

    public ItemViewPricesAdapter(
            Context context,
            Map<String, Map<String, Double>> pricesMap,
            Supermarket preselected_supermarket,
            ArrayList<ItemInfo> variations,
            OnSupermarketClickListener onSupermarketClickListener)
    {
        this.originalPricesMap = pricesMap;
        if (pricesMap == null)
        {
            this.priceRows = new ArrayList<>();
            this.selectedSupermarket = null;
            this.selectedSection = null;
            return;
        }
        this.context = context;
        this.listener = onSupermarketClickListener;
        this.variations = variations;

        if (pricesMap != null && preselected_supermarket != null &&
                preselected_supermarket != Baskit.UNASSIGNED_SUPERMARKET &&
                pricesMap.containsKey(preselected_supermarket.getSupermarket()) &&
                pricesMap.
                        get(preselected_supermarket.getSupermarket()).
                        containsKey(preselected_supermarket.getSection()))
        {
            this.selectedSupermarket = preselected_supermarket.getSupermarket();
            this.selectedSection = preselected_supermarket.getSection();
        }
        else
        {
            this.selectedSupermarket = null;
            this.selectedSection = null;
        }

        priceRows = new ArrayList<>();

        for (String supermarketName : pricesMap.keySet())
        {
            Map<String, Double> sections = pricesMap.get(supermarketName);
            if (sections == null) continue;

            for (String sectionName : sections.keySet())
            {
                Double priceObj = sections.get(sectionName);
                if (priceObj == null) continue;
                double price = priceObj;
                Supermarket sm = new Supermarket(supermarketName, sectionName);

                ItemInfo matchedVariation = null;

                if (variations != null && !variations.isEmpty())
                {
                    // If there is only one variation, assign it directly
                    if (variations.size() == 1)
                    {
                        matchedVariation = variations.get(0);
                    }
                    else
                    {
                        // Try matching by code if section still contains it
                        for (ItemInfo info : variations)
                        {
                            if (sectionName != null && info.getCode() != null && sectionName.contains(info.getCode()))
                            {
                                matchedVariation = info;
                                break;
                            }
                        }
                    }
                }

                priceRows.add(new PriceRow(sm, price, matchedVariation));
            }
        }

        sortRows();
    }

    public ItemViewPricesAdapter(
            Context context,
            ArrayList<PriceRow> rows,
            OnSupermarketClickListener onSupermarketClickListener)
    {
        this.context = context;
        this.listener = onSupermarketClickListener;
        this.priceRows = (rows != null) ? rows : new ArrayList<>();
        this.originalPricesMap = null;
        this.variations = null;
        this.selectedSupermarket = null;
        this.selectedSection = null;

        sortRows();
    }

    private void sortRows()
    {
        if (priceRows != null)
        {
            priceRows.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
        }
    }

    public void resort()
    {
        this.selectedPosition = -1;
        sortRows();
        notifyDataSetChanged();
    }

    public void setRows(ArrayList<PriceRow> rows)
    {
        this.priceRows = (rows != null) ? rows : new ArrayList<>();
        resort();
    }

    public void setSelectedPosition(int position)
    {
        if (position >= 0 && position < priceRows.size())
        {
            this.selectedPosition = position;
        }
        else
        {
            this.selectedPosition = -1;
        }

        notifyDataSetChanged();
    }

    public Map<String, Map<String, Double>> getData()
    {
        return originalPricesMap;
    }

    public void resetSelection(String supermarketName, String sectionName)
    {
        boolean exists = false;

        for (PriceRow row : priceRows)
        {
            Supermarket sm = row.supermarket;

            if (sm.getSupermarket().equals(supermarketName) &&
                    sm.getSection().equals(sectionName)) {
                exists = true;
                break;
            }
        }

        if (exists &&
                supermarketName != null &&
                sectionName != null &&
                !supermarketName.equals(Baskit.UNASSIGNED_SUPERMARKET.getSupermarket()))
        {
            selectedSupermarket = supermarketName;
            selectedSection = sectionName;
        }
        else
        {
            selectedSupermarket = null;
            selectedSection = null;
        }

        notifyDataSetChanged();
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
        PriceRow row = priceRows.get(position);
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

        if (position == selectedPosition)
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
            if (position == selectedPosition)
            {
                selectedPosition = -1;
                notifyDataSetChanged();

                if (listener != null)
                {
                    listener.onSupermarketClick(null, null);
                }
                return;
            }

            selectedPosition = position;
            notifyDataSetChanged();

            if (listener != null)
            {
                listener.onSupermarketClick(supermarket, matchedInfo);
            }
        });
    }

    @Override
    public int getItemCount()
    {
        if (priceRows != null)
        {
            return priceRows.size();
        }

        return 0;
    }
}
