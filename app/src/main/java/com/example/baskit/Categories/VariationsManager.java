package com.example.baskit.Categories;

import android.content.Context;
import android.view.View;

import com.example.baskit.MainComponents.ItemInfo;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;

public class VariationsManager
{
    ArrayList<ItemInfo> currentVariations = new ArrayList<>();
    ChipGroup chipGroupWeights;
    ChipGroup chipGroupCompanies;
    Context context;
    Runnable applyVariationFilter;

    public VariationsManager(ArrayList<ItemInfo> currentVariations,
                             ChipGroup chipGroupWeights,
                             ChipGroup chipGroupCompanies,
                             Context context,
                             Runnable applyVariationFilter)
    {
        this.currentVariations = currentVariations;
        this.chipGroupWeights = chipGroupWeights;
        this.chipGroupCompanies = chipGroupCompanies;
        this.context = context;
        this.applyVariationFilter = applyVariationFilter;
    }

    public void setupVariationFilters()
    {
        if (currentVariations == null || currentVariations.isEmpty()) return;

        if (chipGroupWeights == null || chipGroupCompanies == null)
        {
            return;
        }

        chipGroupWeights.removeAllViews();
        chipGroupCompanies.removeAllViews();

        chipGroupWeights.setSingleSelection(false);
        chipGroupCompanies.setSingleSelection(false);

        java.util.LinkedHashSet<String> weights = new java.util.LinkedHashSet<>();
        java.util.LinkedHashSet<String> companies = new java.util.LinkedHashSet<>();

        for (ItemInfo info : currentVariations)
        {
            if (info.getWeight() != null && info.getWeight() > 0)
            {
                weights.add(info.getFullMeasureStr());
            }

            if (info.getCompany() != null && !info.getCompany().isEmpty())
            {
                companies.add(info.getCompany());
            }
        }

        if (weights.size() > 1)
        {
            for (String w : weights)
            {
                Chip chip = new Chip(context);
                chip.setText(w);
                chip.setCheckable(true);
                chip.setClickable(true);
                chip.setChipStrokeWidth(2f);
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> applyVariationFilter.run());
                chipGroupWeights.addView(chip);
            }
            chipGroupWeights.setVisibility(View.VISIBLE);
        }
        else
        {
            chipGroupWeights.setVisibility(View.GONE);
        }

        if (companies.size() > 1)
        {
            for (String c : companies)
            {
                Chip chip = new Chip(context);
                chip.setText(c);
                chip.setCheckable(true);
                chip.setClickable(true);
                chip.setChipStrokeWidth(2f);
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> applyVariationFilter.run());
                chipGroupCompanies.addView(chip);
            }
            chipGroupCompanies.setVisibility(View.VISIBLE);
        }
        else
        {
            chipGroupCompanies.setVisibility(View.GONE);
        }

        applyVariationFilter.run();
    }
}