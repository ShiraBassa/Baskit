package com.example.baskit.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Map;

public class SectionsListAdapter extends RecyclerView.Adapter<SectionsListAdapter.ViewHolder>
{
    ArrayList<String> sections = null;
    Map<String, Double> sectionsWithPrices = null;

    private SectionsListAdapter() { }

    public static SectionsListAdapter fromSections(ArrayList<String> sections) {
        SectionsListAdapter adapter = new SectionsListAdapter();
        adapter.setSections(sections);
        return adapter;
    }

    public static SectionsListAdapter fromSectionsWithPrices(Map<String, Double> sectionsWithPrices) {
        SectionsListAdapter adapter = new SectionsListAdapter();
        adapter.setSectionsWithPrices(sectionsWithPrices);
        return adapter;
    }

    public void setSections(ArrayList<String> sections) {
        this.sections = sections;
    }

    public void setSectionsWithPrices(Map<String, Double> sectionsWithPrices) {
        this.sectionsWithPrices = sectionsWithPrices;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        protected TextView tvSectionName, tvItemPrice;

        public ViewHolder(View itemView)
        {
            super(itemView);

            tvSectionName = itemView.findViewById(R.id.tv_section_name);
            tvItemPrice = itemView.findViewById(R.id.tv_price);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.supermarket_list_section, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        if (sectionsWithPrices != null)
        {
            ArrayList<String> sectionNames = new ArrayList<>(sectionsWithPrices.keySet());
            String sectionName = sectionNames.get(position);

            holder.tvSectionName.setText(sectionName);
            holder.tvItemPrice.setText(Double.toString(sectionsWithPrices.get(sectionName)));
            holder.tvItemPrice.setVisibility(View.VISIBLE);
        }
        else
        {
            holder.tvSectionName.setText(sections.get(position));
        }
    }

    @Override
    public int getItemCount() {
        if (sectionsWithPrices != null) {
            return sectionsWithPrices.size();
        } else if (sections != null) {
            return sections.size();
        } else {
            return 0;
        }
    }
}
