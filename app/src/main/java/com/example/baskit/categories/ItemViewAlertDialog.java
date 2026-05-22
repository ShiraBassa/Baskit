package com.example.baskit.categories;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.Baskit;
import com.example.baskit.online_components.APIHandler;
import com.example.baskit.main_components.Item;
import com.example.baskit.main_components.Item.ItemInfo;
import com.example.baskit.main_components.Item.ItemVariant;
import com.example.baskit.main_components.Supermarket;
import com.example.baskit.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

public class ItemViewAlertDialog
{
    Item item;

    final ArrayList<ItemVariant> variants = new ArrayList<>();
    final ArrayList<ItemVariant> allVariants = new ArrayList<>();
    final ArrayList<ItemInfo> currentVariations = new ArrayList<>();

    final boolean showQuantity;
    private static final int MAX_QUANTITY = 999;

    final APIHandler apiHandler = APIHandler.getInstance();

    final ChipGroup chipGroupWeights;
    final ChipGroup chipGroupCompanies;

    final AlertDialog.Builder adb;
    final AlertDialog adItemView;
    final RecyclerView recyclerSupermarkets;
    ItemViewPricesAdapter pricesAdapter;

    final TextView adTvQuantity;
    final TextView adTvItemName;
    final Button adBtnSave;
    final ImageButton adBtnCancel;
    final ImageButton adBtnUp;
    final ImageButton adBtnDown;
    final LinearLayout adLayout;
    final LinearLayout adLoutQuantityWhole;
    final ProgressBar adProgressLoading;

    @SuppressLint({"InflateParams", "NotifyDataSetChanged", "SetTextI18n"})
    public ItemViewAlertDialog(Activity activity, Context context, ItemsAdapter.UpperClassFunctions upperClassFns, Item _item, boolean showQuantity)
    {
        this.item = _item.clone();
        this.showQuantity = showQuantity;

        adLayout = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.alert_dialog_item_view, null);
        adBtnCancel = adLayout.findViewById(R.id.btn_cancel);
        adBtnSave = adLayout.findViewById(R.id.btn_save);
        adBtnUp = adLayout.findViewById(R.id.btn_up);
        adBtnDown = adLayout.findViewById(R.id.btn_down);
        adTvQuantity = adLayout.findViewById(R.id.tv_quantity);
        adLoutQuantityWhole = adLayout.findViewById(R.id.lout_quantity_whole);
        adProgressLoading = adLayout.findViewById(R.id.progress_loading);
        recyclerSupermarkets = adLayout.findViewById(R.id.recycler_supermarket);
        adTvItemName = adLayout.findViewById(R.id.tv_item_name);
        chipGroupWeights = adLayout.findViewById(R.id.chip_group_weights);
        chipGroupCompanies = adLayout.findViewById(R.id.chip_group_units);

        adProgressLoading.setVisibility(View.VISIBLE);
        recyclerSupermarkets.setVisibility(View.INVISIBLE);

        Thread loadingThread = new Thread(() ->
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

            variants.clear();
            allVariants.clear();

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
                            ItemVariant newVariant =
                                    new ItemVariant(
                                            sm,
                                            priceObj,
                                            info
                                    );
                            variants.add(newVariant);
                            allVariants.add(newVariant);
                        }
                    }
                }
                catch (Exception e)
                {
                    Log.e("ItemViewAlertDialog", "Failed loading prices", e);
                }
            }

            activity.runOnUiThread(() ->
            {
                if (activity.isFinishing() || activity.isDestroyed())
                {
                    return;
                }
                adProgressLoading.setVisibility(View.GONE);
                recyclerSupermarkets.setVisibility(View.VISIBLE);
                currentVariations.clear();
                currentVariations.addAll(variations);
                new VariationsManager(currentVariations,
                        chipGroupWeights,
                        chipGroupCompanies,
                        context,
                        this::applyVariationFilter).setupVariationFilters();

                pricesAdapter = new ItemViewPricesAdapter(
                        context,
                        variants,
                        (variant) ->
                        {
                            if (item == null) return;

                            if (variant == null)
                            {
                                item.setUnchosen();
                            }
                            else
                            {
                                item.fillVariant(variant);
                            }

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
                    for (int i = 0; i < variants.size(); i++)
                    {
                        ItemVariant variant = variants.get(i);

                        if (item.isIdenticalVariantOf(variant))
                        {
                            pricesAdapter.setSelectedPosition(i);
                            item.setPrice(variant.getPrice());
                            item.fillInfo(variant.getInfo());
                            break;
                        }
                    }
                }
            });
        });

        loadingThread.setName("ItemViewLoader");
        loadingThread.start();

        adb = new AlertDialog.Builder(context);
        adb.setView(adLayout);
        adItemView = adb.create();

        adBtnCancel.setOnClickListener(v -> adItemView.dismiss());
        adBtnSave.setOnClickListener(v ->
        {
            upperClassFns.updateItemCategory(item);
            adItemView.dismiss();
        });

        adBtnUp.setOnClickListener(view ->
        {
            if (item.getQuantity() >= MAX_QUANTITY)
            {
                return;
            }

            adTvQuantity.setText(Integer.toString(item.raiseQuantity()));
            adBtnDown.setVisibility(View.VISIBLE);
        });

        adBtnDown.setOnClickListener(view -> {
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
        });
    }

    @SuppressLint("SetTextI18n")
    public void show(Item _item)
    {
        if (_item == null)
        {
            return;
        }

        this.item = _item.clone();

        if (item == null)
        {
            return;
        }

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

        variants.clear();

        if (allVariants.isEmpty())
        {
            if (pricesAdapter != null)
            {
                pricesAdapter.setSelectedPosition(-1);
                pricesAdapter.notifyDataSetChanged();
            }
        }

        variants.addAll(allVariants);
        adProgressLoading.setVisibility(View.GONE);
        recyclerSupermarkets.setVisibility(View.VISIBLE);
        variants.sort((a, b) ->
        {
            double priceA = a != null ? a.getPrice() : Double.MAX_VALUE;
            double priceB = b != null ? b.getPrice() : Double.MAX_VALUE;

            if (Double.isNaN(priceA)) priceA = Double.MAX_VALUE;
            if (Double.isNaN(priceB)) priceB = Double.MAX_VALUE;

            return Double.compare(priceA, priceB);
        });

        boolean selectedVariantStillExists = false;

        if (item != null)
        {
            for (ItemVariant variant : variants)
            {
                if (item.isIdenticalVariantOf(variant))
                {
                    selectedVariantStillExists = true;
                    break;
                }
            }
        }

        if (!selectedVariantStillExists && pricesAdapter != null)
        {
            pricesAdapter.setSelectedPosition(-1);
        }

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

        if (pricesAdapter != null)
        {
            pricesAdapter.setSelectedPosition(-1); // clear previous adapter state

            for (int i = 0; i < variants.size(); i++)
            {
                ItemVariant variant = variants.get(i);

                if (item.isIdenticalVariantOf(variant))
                {
                    pricesAdapter.setSelectedPosition(i);
                    item.setPrice(variant.getPrice());
                    item.fillInfo(variant.getInfo());
                    break;
                }
            }
        }

        if (adItemView.isShowing())
        {
            return;
        }

        Context dialogContext = adItemView.getContext();

        if (dialogContext instanceof Activity)
        {
            Activity dialogActivity = (Activity) dialogContext;

            if (dialogActivity.isFinishing() || dialogActivity.isDestroyed())
            {
                return;
            }
        }

        if (showQuantity)
        {
            adLoutQuantityWhole.setVisibility(View.VISIBLE);
        }

        adItemView.show();
    }

    @SuppressWarnings("ExtractMethodRecommender")
    @SuppressLint("NotifyDataSetChanged")
    private void applyVariationFilter()
    {
        if (chipGroupWeights == null || chipGroupCompanies == null)
        {
            return;
        }
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

        variants.clear();

        for (ItemVariant r : allVariants)
        {
            if (r.getInfo() == null) continue;

            ItemInfo info = r.getInfo();

            String fullMeasure = info.getFullMeasureStr();

            boolean weightMatch = selectedWeights.isEmpty() ||
                    (fullMeasure != null && selectedWeights.contains(fullMeasure));

            boolean companyMatch = selectedCompanies.isEmpty() ||
                    (info.getCompany() != null && selectedCompanies.contains(info.getCompany()));

            if (weightMatch && companyMatch)
            {
                variants.add(r);
            }
        }

        java.util.Set<String> availableWeights = new java.util.HashSet<>();

        for (ItemVariant r : allVariants)
        {
            if (r.getInfo() == null) continue;

            ItemInfo info = r.getInfo();

            boolean companyMatch =
                    selectedCompanies.isEmpty() ||
                    (info.getCompany() != null && selectedCompanies.contains(info.getCompany()));

            if (companyMatch && info.getWeight() != null && info.getWeight() > 0)
            {
                String fullMeasure = info.getFullMeasureStr();

                if (fullMeasure != null)
                {
                    availableWeights.add(fullMeasure);
                }
            }
        }

        java.util.Set<String> availableCompanies = new java.util.HashSet<>();

        for (ItemVariant r : allVariants)
        {
            if (r.getInfo() == null) continue;

            ItemInfo info = r.getInfo();

            boolean weightMatch =
                    selectedWeights.isEmpty() ||
                            (info.getFullMeasureStr() != null &&
                                    selectedWeights.contains(info.getFullMeasureStr()));

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

        variants.sort((a, b) ->
        {
            double priceA = a != null ? a.getPrice() : Double.MAX_VALUE;
            double priceB = b != null ? b.getPrice() : Double.MAX_VALUE;

            if (Double.isNaN(priceA)) priceA = Double.MAX_VALUE;
            if (Double.isNaN(priceB)) priceB = Double.MAX_VALUE;

            return Double.compare(priceA, priceB);
        });

        boolean selectedVariantStillExists = false;

        if (item != null)
        {
            for (ItemVariant variant : variants)
            {
                if (item.isIdenticalVariantOf(variant))
                {
                    selectedVariantStillExists = true;
                    break;
                }
            }
        }

        if (!selectedVariantStillExists && pricesAdapter != null)
        {
            pricesAdapter.setSelectedPosition(-1);
        }

        if (pricesAdapter != null)
        {
            pricesAdapter.notifyDataSetChanged();
        }
    }

    public void dismiss()
    {
        if (adItemView != null && adItemView.isShowing())
        {
            adItemView.dismiss();
        }
    }

    public static class VariationsManager
    {
        final ArrayList<ItemInfo> currentVariations;
        final ChipGroup chipGroupWeights;
        final ChipGroup chipGroupCompanies;
        final Context context;
        final Runnable applyVariationFilter;

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
                    String fullMeasure = info.getFullMeasureStr();

                    if (fullMeasure != null)
                    {
                        weights.add(fullMeasure);
                    }
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
                    chip.setChipStrokeWidth(1.2f);
                    chip.setChipCornerRadius(30f);
                    chip.setTextSize(15f);
                    chip.setEnsureMinTouchTargetSize(true);

                    int colorSurface = Baskit.getAppColor(context, com.google.android.material.R.attr.colorSurface);
                    int colorPrimary = Baskit.getAppColor(context, androidx.appcompat.R.attr.colorPrimary);
                    int colorOnSurface = Baskit.getAppColor(context, com.google.android.material.R.attr.colorOnSurface);
                    int colorOnPrimary = Baskit.getAppColor(context, com.google.android.material.R.attr.colorOnPrimary);
                    int colorOutline = Baskit.getAppColor(context, com.google.android.material.R.attr.colorOutline);

                    android.content.res.ColorStateList bgStates = new android.content.res.ColorStateList(
                            new int[][]{
                                    new int[]{android.R.attr.state_checked},
                                    new int[]{}
                            },
                            new int[]{
                                    colorPrimary,
                                    colorSurface
                            }
                    );

                    android.content.res.ColorStateList textStates = new android.content.res.ColorStateList(
                            new int[][]{
                                    new int[]{android.R.attr.state_checked},
                                    new int[]{}
                            },
                            new int[]{
                                    colorOnPrimary,
                                    colorOnSurface
                            }
                    );

                    android.content.res.ColorStateList strokeStates = new android.content.res.ColorStateList(
                            new int[][]{
                                    new int[]{android.R.attr.state_checked},
                                    new int[]{}
                            },
                            new int[]{
                                    colorPrimary,
                                    colorOutline
                            }
                    );

                    chip.setChipBackgroundColor(bgStates);
                    chip.setTextColor(textStates);
                    chip.setChipStrokeColor(strokeStates);

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
                    chip.setChipStrokeWidth(1.2f);
                    chip.setChipCornerRadius(30f);
                    chip.setTextSize(15f);
                    chip.setEnsureMinTouchTargetSize(true);

                    int colorSurface = Baskit.getAppColor(context, com.google.android.material.R.attr.colorSurface);
                    int colorPrimary = Baskit.getAppColor(context, androidx.appcompat.R.attr.colorPrimary);
                    int colorOnSurface = Baskit.getAppColor(context, com.google.android.material.R.attr.colorOnSurface);
                    int colorOnPrimary = Baskit.getAppColor(context, com.google.android.material.R.attr.colorOnPrimary);
                    int colorOutline = Baskit.getAppColor(context, com.google.android.material.R.attr.colorOutline);

                    android.content.res.ColorStateList bgStates = new android.content.res.ColorStateList(
                            new int[][]{
                                    new int[]{android.R.attr.state_checked},
                                    new int[]{}
                            },
                            new int[]{
                                    colorPrimary,
                                    colorSurface
                            }
                    );

                    android.content.res.ColorStateList textStates = new android.content.res.ColorStateList(
                            new int[][]{
                                    new int[]{android.R.attr.state_checked},
                                    new int[]{}
                            },
                            new int[]{
                                    colorOnPrimary,
                                    colorOnSurface
                            }
                    );

                    android.content.res.ColorStateList strokeStates = new android.content.res.ColorStateList(
                            new int[][]{
                                    new int[]{android.R.attr.state_checked},
                                    new int[]{}
                            },
                            new int[]{
                                    colorPrimary,
                                    colorOutline
                            }
                    );

                    chip.setChipBackgroundColor(bgStates);
                    chip.setTextColor(textStates);
                    chip.setChipStrokeColor(strokeStates);

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
}
