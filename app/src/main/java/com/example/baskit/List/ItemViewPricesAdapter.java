package com.example.baskit.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;

public class ItemViewPricesAdapter extends RecyclerView.Adapter<ItemViewPricesAdapter.ViewHolder>
{
    private ArrayList<Map.Entry<Supermarket, Double>> pricesBySupermarket;
    private OnSupermarketClickListener listener;
    private String selectedSupermarket, selectedSection;
    private final APIHandler apiHandler = APIHandler.getInstance();

    public interface OnSupermarketClickListener
    {
        void onSupermarketClick(Supermarket supermarket);
    }

    public ItemViewPricesAdapter(Map<String, Map<String, Double>> pricesMap, Supermarket preselected_supermarket, OnSupermarketClickListener onSupermarketClickListener)
    {
        this.listener = onSupermarketClickListener;

        if (pricesMap != null && preselected_supermarket != null &&
                preselected_supermarket != Baskit.unassigned_supermarket &&
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

        pricesBySupermarket = new ArrayList<>();

        for (String supermarketName : pricesMap.keySet())
        {
            for (String sectionName : pricesMap.get(supermarketName).keySet())
            {
                pricesBySupermarket.add(new AbstractMap.SimpleEntry<>(new Supermarket(supermarketName, sectionName),
                        pricesMap.get(supermarketName).get(sectionName)));
            }
        }
    }

    public void resetSelection(String supermarketName, String sectionName)
    {
        boolean exists = false;

        for (Map.Entry<Supermarket, Double> entry : pricesBySupermarket) {
            Supermarket sm = entry.getKey();

            if (sm.getSupermarket().equals(supermarketName) &&
                    sm.getSection().equals(sectionName)) {
                exists = true;
                break;
            }
        }

        if (exists &&
                supermarketName != null &&
                sectionName != null &&
                !supermarketName.equals(Baskit.unassigned_supermarket.getSupermarket()))
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

        public ViewHolder(View itemView)
        {
            super(itemView);

            tvSupermarketName = itemView.findViewById(R.id.tv_supermarket_name);
            tvSectionName = itemView.findViewById(R.id.tv_section_name);
            tvPrice = itemView.findViewById(R.id.tv_price);
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
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        Map.Entry<Supermarket, Double> entry = pricesBySupermarket.get(position);
        Supermarket supermarket = entry.getKey();
        Double price = entry.getValue();

        holder.tvSupermarketName.setText(supermarket.getSupermarket());

        if (apiHandler.singleSectionInSupermarkets(supermarket))
        {
            holder.tvSectionName.setVisibility(View.GONE);
        }
        else
        {
            holder.tvSectionName.setText(supermarket.getSection());
        }

        holder.tvPrice.setText(Baskit.getTotalDisplayString(price, false));

        if (supermarket.getSupermarket().equals(selectedSupermarket) &&
                supermarket.getSection().equals(selectedSection))
        {
            holder.itemView.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.tan));
        }
        else
        {
            holder.itemView.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.white_smoke));
        }

        holder.itemView.setOnClickListener(v ->
        {
            boolean isSelected =
                    supermarket.getSupermarket().equals(selectedSupermarket) &&
                            supermarket.getSection().equals(selectedSection);

            if (isSelected)
            {
                selectedSupermarket = null;
                selectedSection = null;
            }
            else
            {
                selectedSupermarket = supermarket.getSupermarket();
                selectedSection = supermarket.getSection();
            }

            notifyDataSetChanged();

            if (listener != null)
            {
                listener.onSupermarketClick(
                        new Supermarket(
                                supermarket.getSupermarket(),
                                supermarket.getSection()
                        )
                );
            }
        });
    }

    @Override
    public int getItemCount()
    {
        if (pricesBySupermarket != null)
        {
            return pricesBySupermarket.size();
        }

        return 0;
    }
}
