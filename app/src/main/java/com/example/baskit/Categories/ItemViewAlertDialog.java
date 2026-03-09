package com.example.baskit.Categories;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.ItemInfo;
import com.example.baskit.MainComponents.PriceRow;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Map;

public class ItemViewAlertDialog
{
    Item item;

    ArrayList<PriceRow> rows = new ArrayList<>();
    ArrayList<PriceRow> allRows = new ArrayList<>();
    ArrayList<ItemInfo> currentVariations = new ArrayList<>();

    boolean showQuantity;

    APIHandler apiHandler = APIHandler.getInstance();

    ChipGroup chipGroupWeights;
    ChipGroup chipGroupCompanies;

    AlertDialog.Builder adb;
    AlertDialog adItemView;
    RecyclerView recyclerSupermarkets;
    ItemViewPricesAdapter pricesAdapter;

    TextView adTvQuantity, adTvItemName;
    Button adBtnSave;
    ImageButton adBtnCancel, adBtnUp, adBtnDown;
    LinearLayout adLayout, adLoutQuantity, adLoutQuantityWhole;

    Activity activity;
    Context context;
    ItemsAdapter.UpperClassFunctions upperClassFns;

    public ItemViewAlertDialog(Activity activity, Context context, ItemsAdapter.UpperClassFunctions upperClassFns, Item _item, boolean showQuantity)
    {
        this.activity = activity;
        this.context = context;
        this.item = _item.clone();
        this.upperClassFns = upperClassFns;
        this.showQuantity = showQuantity;

        adLayout = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.alert_dialog_item_view, null);
        adBtnCancel = adLayout.findViewById(R.id.btn_cancel);
        adBtnSave = adLayout.findViewById(R.id.btn_save);
        adBtnUp = adLayout.findViewById(R.id.btn_up);
        adBtnDown = adLayout.findViewById(R.id.btn_down);
        adTvQuantity = adLayout.findViewById(R.id.tv_quantity);
        adLoutQuantity = adLayout.findViewById(R.id.lout_info);
        adLoutQuantityWhole = adLayout.findViewById(R.id.lout_quantity_whole);
        recyclerSupermarkets = adLayout.findViewById(R.id.recycler_supermarket);
        adTvItemName = adLayout.findViewById(R.id.tv_item_name);
        chipGroupWeights = adLayout.findViewById(R.id.chip_group_weights);
        chipGroupCompanies = adLayout.findViewById(R.id.chip_group_units);

        new Thread(() ->
        {
            ArrayList<ItemInfo> variations = new ArrayList<>();
            try
            {
                String baseName = item.getBaseName();
                if (baseName != null && !baseName.isEmpty())
                {
                    ArrayList<String> codes = apiHandler.getGroup(baseName);
                    if (codes != null)
                    {
                        for (String code : codes)
                        {
                            ItemInfo info = apiHandler.getItemInfo(code);
                            if (info != null && !variations.contains(info))
                            {
                                variations.add(info);
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                Log.e("ItemViewAlertDialog", "Failed to load variations", e);
            }

            rows.clear();
            allRows.clear();

            for (ItemInfo info : variations)
            {
                try
                {
                    Map<String, Map<String, Double>> data = apiHandler.getItemPricesByCode(info.getCode());
                    if (data == null) continue;
                    for (Map.Entry<String, Map<String, Double>> entry : data.entrySet())
                    {
                        String supermarketName = entry.getKey();
                        Map<String, Double> sections = entry.getValue();
                        if (sections == null) continue;
                        for (Map.Entry<String, Double> sectionEntry : sections.entrySet())
                        {
                            String sectionName = sectionEntry.getKey();
                            Double priceObj = sectionEntry.getValue();
                            if (priceObj == null) continue;
                            Supermarket sm = new Supermarket(supermarketName, sectionName);
                            PriceRow newRow =
                                    new PriceRow(
                                            sm,
                                            priceObj,
                                            info
                                    );
                            rows.add(newRow);
                            allRows.add(newRow);
                        }
                    }
                }
                catch (Exception ignored) {}
            }

            activity.runOnUiThread(() ->
            {
                currentVariations.clear();
                currentVariations.addAll(variations);
                setupVariationFilters();

                pricesAdapter = new ItemViewPricesAdapter(
                        context,
                        rows,
                        (supermarket, variation) ->
                        {
                            if (supermarket == null)
                            {
                                item.setUnchosen();
                                if (pricesAdapter != null) pricesAdapter.notifyDataSetChanged();
                                return;
                            }

                            item.setSupermarket(supermarket);

                            for (PriceRow row : rows)
                            {
                                if (row.getSupermarket().getSupermarket().equals(supermarket.getSupermarket()) &&
                                    row.getSupermarket().getSection().equals(supermarket.getSection()) &&
                                    row.getInfo() != null &&
                                    variation != null &&
                                    row.getInfo().getCode() != null &&
                                    row.getInfo().getCode().equals(variation.getCode()))
                                {
                                    item.setPrice(row.getPrice());
                                    break;
                                }
                            }

                            item.fillInfo(variation);

                            if (pricesAdapter != null)
                            {
                                pricesAdapter.notifyDataSetChanged();
                            }
                        }
                );

                recyclerSupermarkets.setLayoutManager(new LinearLayoutManager(context));
                recyclerSupermarkets.setAdapter(pricesAdapter);
                applyVariationFilter();

                // Auto-select current variant
                if (item.getAbsoluteId() != null && item.getSupermarket() != null)
                {
                    for (int i = 0; i < rows.size(); i++)
                    {
                        PriceRow row = rows.get(i);

                        if (item.isIdenticalVariantOf(row))
                        {
                            pricesAdapter.setSelectedPosition(i);
                            item.setPrice(row.getPrice());
                            item.fillInfo(row.getInfo());
                            break;
                        }
                    }
                }
            });
        }).start();

        adb = new AlertDialog.Builder(context);
        adb.setView(adLayout);
        adItemView = adb.create();

        adBtnCancel.setOnClickListener(v -> adItemView.dismiss());
        adBtnSave.setOnClickListener(v ->
        {
            upperClassFns.updateItemCategory(item);
            adItemView.dismiss();
        });

        adBtnUp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                adTvQuantity.setText(Integer.toString(item.raiseQuantity()));
                adBtnDown.setVisibility(View.VISIBLE);
            }
        });

        adBtnDown.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (item.getQuantity() <= 1)
                {
                    return;
                }

                int quantity = item.lowerQuantity();

                if (quantity == 1)
                {
                    adBtnDown.setVisibility(View.INVISIBLE);
                }

                adTvQuantity.setText(Integer.toString(quantity));
            }
        });
    }

    public void setUpperClassFns(ItemsAdapter.UpperClassFunctions upperClassFns)
    {
        this.upperClassFns = upperClassFns;
    }

    public void show(Item _item)
    {
        this.item = _item.clone();

        if (pricesAdapter != null)
        {
            pricesAdapter.setSelectedPosition(-1);
        }

        if (chipGroupWeights != null)
        {
            for (int i = 0; i < chipGroupWeights.getChildCount(); i++)
            {
                Chip chip = (Chip) chipGroupWeights.getChildAt(i);
                chip.setChecked(false);
            }
        }

        if (chipGroupCompanies != null)
        {
            for (int i = 0; i < chipGroupCompanies.getChildCount(); i++)
            {
                Chip chip = (Chip) chipGroupCompanies.getChildAt(i);
                chip.setChecked(false);
            }
        }

        rows.clear();
        rows.addAll(allRows);
        rows.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));

        applyVariationFilter();

        adBtnSave.setClickable(true);
        adBtnCancel.setClickable(true);
        adBtnUp.setClickable(true);
        adBtnDown.setClickable(true);
        adBtnDown.setVisibility(View.VISIBLE);

        String title = item.getDecodedName();

        if (item.getWeight() != null && item.getUnit() != null
                && !item.getUnit().isEmpty())
        {
            title += " • " + item.getWeight() + " " + item.getUnit();
        }

        if (item.getCompany() != null && !item.getCompany().isEmpty())
        {
            title += " • " + item.getCompany();
        }

        adTvItemName.setText(title);

        if (showQuantity)
        {
            adTvQuantity.setText(Integer.toString(item.getQuantity()));
        }
        else
        {
            adLoutQuantityWhole.setVisibility(View.GONE);
        }

        if (item.getQuantity() == 1)
        {
            adBtnDown.setVisibility(View.INVISIBLE);
        }

        if (pricesAdapter != null && rows != null)
        {
            pricesAdapter.setSelectedPosition(-1); // clear previous adapter state

            for (int i = 0; i < rows.size(); i++)
            {
                PriceRow row = rows.get(i);

                if (item.isIdenticalVariantOf(row))
                {
                    pricesAdapter.setSelectedPosition(i);
                    item.setPrice(row.getPrice());
                    item.fillInfo(row.getInfo());
                    break;
                }
            }
        }

        adItemView.show();
    }

    private void setupVariationFilters()
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
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> applyVariationFilter());
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
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> applyVariationFilter());
                chipGroupCompanies.addView(chip);
            }
            chipGroupCompanies.setVisibility(View.VISIBLE);
        }
        else
        {
            chipGroupCompanies.setVisibility(View.GONE);
        }
    }

    private void applyVariationFilter()
    {
        java.util.Set<String> selectedWeights = new java.util.HashSet<>();
        java.util.Set<String> selectedCompanies = new java.util.HashSet<>();

        for (int i = 0; i < chipGroupWeights.getChildCount(); i++)
        {
            Chip chip = (Chip) chipGroupWeights.getChildAt(i);
            if (chip.isChecked()) selectedWeights.add(chip.getText().toString());
        }

        for (int i = 0; i < chipGroupCompanies.getChildCount(); i++)
        {
            Chip chip = (Chip) chipGroupCompanies.getChildAt(i);
            if (chip.isChecked()) selectedCompanies.add(chip.getText().toString());
        }

        rows.clear();

        for (PriceRow r : allRows)
        {
            if (r.getInfo() == null) continue;

            ItemInfo info = r.getInfo();

            boolean weightMatch = selectedWeights.isEmpty() ||
                    selectedWeights.contains(info.getFullMeasureStr());

            boolean companyMatch = selectedCompanies.isEmpty() ||
                    (info.getCompany() != null && selectedCompanies.contains(info.getCompany()));

            if (weightMatch && companyMatch)
            {
                rows.add(r);
            }
        }

        java.util.Set<String> availableWeights = new java.util.HashSet<>();

        for (PriceRow r : allRows)
        {
            if (r.getInfo() == null) continue;

            ItemInfo info = r.getInfo();

            boolean companyMatch =
                    selectedCompanies.isEmpty() ||
                    (info.getCompany() != null && selectedCompanies.contains(info.getCompany()));

            if (companyMatch && info.getWeight() != null && info.getWeight() > 0)
            {
                availableWeights.add(info.getFullMeasureStr());
            }
        }

        java.util.Set<String> availableCompanies = new java.util.HashSet<>();

        for (PriceRow r : allRows)
        {
            if (r.getInfo() == null) continue;

            ItemInfo info = r.getInfo();

            boolean weightMatch =
                    selectedWeights.isEmpty() ||
                    selectedWeights.contains(info.getFullMeasureStr());

            if (weightMatch && info.getCompany() != null && !info.getCompany().isEmpty())
            {
                availableCompanies.add(info.getCompany());
            }
        }

        for (int i = 0; i < chipGroupWeights.getChildCount(); i++)
        {
            Chip chip = (Chip) chipGroupWeights.getChildAt(i);
            boolean enabled = availableWeights.contains(chip.getText().toString());
            chip.setEnabled(enabled);
            chip.setAlpha(enabled ? 1f : 0.3f);
        }

        for (int i = 0; i < chipGroupCompanies.getChildCount(); i++)
        {
            Chip chip = (Chip) chipGroupCompanies.getChildAt(i);
            boolean enabled = availableCompanies.contains(chip.getText().toString());
            chip.setEnabled(enabled);
            chip.setAlpha(enabled ? 1f : 0.3f);
        }

        rows.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
    }
}
