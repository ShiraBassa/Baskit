package com.example.baskit.List;

import android.app.Activity;
import android.content.ClipData;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SupermarketsListAdapter extends RecyclerView.Adapter<SupermarketsListAdapter.ViewHolder>
{
    private Map<String, ArrayList<String>> supermarkets = null;
    private Map<String, Map<String, Double>> supermarketsWithPrices = null;
    private Activity activity;
    private OnSupermarketClickListener OnSupermarketClickListener = null;
    private Supermarket selectedSupermarket = new Supermarket();

    public void setOnSupermarketClickListener(OnSupermarketClickListener listener)
    {
        this.OnSupermarketClickListener = listener;
    }

    public interface OnSupermarketClickListener
    {
        void onSupermarketClick(Supermarket supermarket);
    }

    private SupermarketsListAdapter() {}

    public static SupermarketsListAdapter fromSupermarkets(Map<String, ArrayList<String>> supermarkets, Activity activity)
    {
        SupermarketsListAdapter adapter = new SupermarketsListAdapter();
        adapter.setSupermarkets(supermarkets);
        adapter.setActivity(activity);

        return adapter;
    }

    public static SupermarketsListAdapter fromSupermarketsWithPrices(Map<String, Map<String, Double>> supermarketsWithPrices, Activity activity, OnSupermarketClickListener onSupermarketClickListener)
    {
        SupermarketsListAdapter adapter = new SupermarketsListAdapter();
        adapter.setSupermarketsWithPrices(supermarketsWithPrices);
        adapter.setActivity(activity);
        adapter.setOnSupermarketClickListener(onSupermarketClickListener);

        return adapter;
    }

    public Map<String, ArrayList<String>> getSupermarkets()
    {
        return supermarkets;
    }

    public Map<String, Map<String, Double>> getSupermarketsWithPrices()
    {
        return supermarketsWithPrices;
    }

    public void setActivity(Activity activity)
    {
        this.activity = activity;
    }

    public void setSupermarkets(Map<String, ArrayList<String>> supermarkets)
    {
        this.supermarkets = supermarkets;
    }

    private void setSupermarketsWithPrices(Map<String, Map<String, Double>> supermarketsWithPrices)
    {
        this.supermarketsWithPrices = supermarketsWithPrices;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView tvSupermarketName;
        RecyclerView recyclerSections;
        SectionsListAdapter sectionsAdapter;

        public ViewHolder(View itemView)
        {
            super(itemView);

            tvSupermarketName = itemView.findViewById(R.id.tv_supermarket_name);
            recyclerSections = itemView.findViewById(R.id.recycler_sections);

            recyclerSections.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.supermarkets_list_single, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        String supermarketName;

        if (supermarketsWithPrices != null)
        {
            supermarketName = new ArrayList<>(supermarketsWithPrices.keySet()).get(position);

           if (holder.sectionsAdapter == null)
           {
                holder.sectionsAdapter = SectionsListAdapter.fromSectionsWithPrices(
                        supermarketsWithPrices.get(supermarketName),
                        sectionName ->
                        {
                            if (OnSupermarketClickListener != null)
                            {
                                OnSupermarketClickListener.onSupermarketClick(
                                        new Supermarket(supermarketName, sectionName)
                                );
                            }
                        }
                );
                holder.recyclerSections.setAdapter(holder.sectionsAdapter);
            }
           else
           {
                holder.sectionsAdapter.updateDataWithPrices(
                        supermarketsWithPrices.get(supermarketName)
                );

                holder.sectionsAdapter.notifyDataSetChanged();
            }
        }
        else
        {
            supermarketName = new ArrayList<>(supermarkets.keySet()).get(position);

            if (holder.sectionsAdapter == null)
            {
                holder.sectionsAdapter = SectionsListAdapter.fromSections(
                        supermarkets.get(supermarketName), new SectionsListAdapter.OnSectionClickListener()
                        {
                            @Override
                            public void onSectionClick(String sectionName) {
                                if (selectedSupermarket.getSupermarket() != null &&
                                    selectedSupermarket.getSupermarket().equals(supermarketName) &&
                                    selectedSupermarket.getSection() != null &&
                                    selectedSupermarket.getSection().equals(sectionName)) {
                                    // Same item clicked — deselect
                                    selectedSupermarket.setSupermarket(null);
                                    selectedSupermarket.setSection(null);
                                } else {
                                    // New selection
                                    selectedSupermarket.setSupermarket(supermarketName);
                                    selectedSupermarket.setSection(sectionName);
                                }
                                notifyDataSetChanged(); // Refresh adapter to reflect selection/deselection
                            }
                        }
                );
                holder.recyclerSections.setAdapter(holder.sectionsAdapter);
            }
            else
            {
                holder.sectionsAdapter.updateData(
                        supermarkets.get(supermarketName)
                );

                holder.sectionsAdapter.notifyDataSetChanged();
            }
        }

        holder.tvSupermarketName.setText(supermarketName);
    }

    @Override
    public int getItemCount()
    {
        if (supermarketsWithPrices != null)
        {
            return supermarketsWithPrices.size();
        }
        else if (supermarkets != null)
        {
            return supermarkets.size();
        }
        else
        {
            return 0;
        }
    }

    public void updateData(Map<String, ArrayList<String>> newSupermarkets) {
        this.supermarkets = newSupermarkets;
    }

    public void updateDataWithPrices(Map<String, Map<String, Double>> newSupermarketsWithPrices)
    {
        this.supermarketsWithPrices = newSupermarketsWithPrices;
    }

    public Supermarket getSelectedSupermarket()
    {
        return selectedSupermarket;
    }
}
