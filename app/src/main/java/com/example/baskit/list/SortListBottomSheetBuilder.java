package com.example.baskit.list;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.baskit.Baskit;
import com.example.baskit.main_components.Item.ItemVariant;
import com.example.baskit.main_components.SortableEntity;
import com.example.baskit.main_components.Supermarket;
import com.example.baskit.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import android.util.Log;

public class SortListBottomSheetBuilder
{
    private static BottomSheetDialog activeDialog;
    public interface ApplyListener
    {
        void onApplyCheapest();
        void onApplySupermarket(Supermarket sm);
    }

    @SuppressLint("SetTextI18n")
    public static void show(
            AppCompatActivity activity,
            SortableEntity entity,
            Map<String, ArrayList<ItemVariant>> variants,
            ArrayList<Supermarket> supermarkets,
            ApplyListener listener)
    {
        if (activity == null || activity.isFinishing() || activity.isDestroyed())
        {
            return;
        }
        if (activeDialog != null && activeDialog.isShowing())
        {
            return;
        }

        if (entity == null)
        {
            return;
        }

        if (variants == null)
        {
            variants = new HashMap<>();
        }

        if (supermarkets == null)
        {
            supermarkets = new ArrayList<>();
        }

        final Map<String, ArrayList<ItemVariant>> finalVariants = variants;
        final ArrayList<Supermarket> finalSupermarkets = supermarkets;

        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        activeDialog = dialog;
        dialog.setDismissWithAnimation(true);
        dialog.setOnDismissListener(d ->
        {
            if (activeDialog == dialog)
            {
                activeDialog = null;
            }
        });
        @SuppressLint("InflateParams") View view = activity.getLayoutInflater().inflate(R.layout.bottom_sheet_sort_list, null);
        dialog.setContentView(view);

        View cheapestLayout = view.findViewById(R.id.layout_cheapest);
        TextView cheapestTvSupermarket = cheapestLayout.findViewById(R.id.tv_supermarket);
        TextView cheapestTvPrice = cheapestLayout.findViewById(R.id.tv_price);

        LinearLayout supermarketContainer = view.findViewById(R.id.supermarkets_container);

        Thread sortPreviewThread = new Thread(() ->
        {
            Map<Supermarket, Double> totals = new HashMap<>();
            Map<Supermarket, Boolean> allKnown = new HashMap<>();

            try
            {
                SortableEntity cheapestPreview = entity.copy();
                cheapestPreview.setCheapestVariants(finalVariants);

                for (Supermarket sm : finalSupermarkets)
                {
                    if (sm == null)
                    {
                        continue;
                    }
                    SortableEntity preview = entity.copy();
                    preview.setSupermarketsVariants(sm, finalVariants);

                    totals.put(sm, preview.getTotal());
                    allKnown.put(sm, preview.allPricesKnown());
                }

                if (activity.isFinishing() || activity.isDestroyed())
                {
                    return;
                }

                activity.runOnUiThread(() ->
                {
                    if (activity.isFinishing() || activity.isDestroyed())
                    {
                        return;
                    }

                    if (!dialog.isShowing())
                    {
                        return;
                    }

                    cheapestTvSupermarket.setText(Baskit.getAppStr(R.string.cheapests));
                    double cheapestTotal = cheapestPreview.getTotal();

                    if (Double.isNaN(cheapestTotal) || Double.isInfinite(cheapestTotal))
                    {
                        cheapestTotal = 0.0;
                    }
                    cheapestTvPrice.setText(
                            Baskit.getTotalDisplayString(
                                    cheapestTotal,
                                    cheapestPreview.allPricesKnown(),
                                    false,
                                    false
                            )
                    );

                    cheapestLayout.setOnClickListener(v ->
                    {
                        if (listener != null)
                        {
                            listener.onApplyCheapest();
                        }

                        dialog.dismiss();
                    });

                    supermarketContainer.removeAllViews();

                    LayoutInflater inflater = LayoutInflater.from(activity);

                    ArrayList<Map.Entry<Supermarket, Double>> entries = new ArrayList<>(totals.entrySet());
                    entries.removeIf(entry -> entry == null || entry.getKey() == null);

                    entries.sort((a, b) ->
                    {
                        double valueA = a != null && a.getValue() != null ? a.getValue() : Double.MAX_VALUE;
                        double valueB = b != null && b.getValue() != null ? b.getValue() : Double.MAX_VALUE;

                        if (Double.isNaN(valueA) || Double.isInfinite(valueA))
                        {
                            valueA = Double.MAX_VALUE;
                        }

                        if (Double.isNaN(valueB) || Double.isInfinite(valueB))
                        {
                            valueB = Double.MAX_VALUE;
                        }

                        return Double.compare(valueA, valueB);
                    });

                    for (Map.Entry<Supermarket, Double> entry : entries)
                    {
                        if (entry == null)
                        {
                            continue;
                        }
                        Supermarket sm = entry.getKey();
                        double total = entry.getValue();

                        if (Double.isNaN(total) || Double.isInfinite(total))
                        {
                            total = 0.0;
                        }

                        View supermarketView = inflater.inflate(
                                R.layout.sort_list_supermarket,
                                supermarketContainer,
                                false
                        );

                        TextView name = supermarketView.findViewById(R.id.tv_supermarket);
                        TextView price = supermarketView.findViewById(R.id.tv_price);

                        String supermarketText = sm.toString();

                        name.setText(
                                supermarketText != null && !supermarketText.isBlank()
                                        ? supermarketText
                                        : Baskit.getAppStr(R.string.unknown_supermarket)
                        );
                        price.setText(
                                Baskit.getTotalDisplayString(
                                        total,
                                        Boolean.TRUE.equals(allKnown.get(sm)),
                                        false,
                                        false
                                )
                        );

                        boolean allPricesKnownForSm = Boolean.TRUE.equals(allKnown.get(sm));

                        if (!allPricesKnownForSm)
                        {
                            price.setAlpha(0.72f);
                            name.setAlpha(0.78f);
                        }
                        else
                        {
                            price.setAlpha(1f);
                            name.setAlpha(1f);
                        }

                        supermarketView.setOnClickListener(v ->
                        {
                            if (listener != null)
                            {
                                listener.onApplySupermarket(sm);
                            }

                            dialog.dismiss();
                        });

                        supermarketView.setClipToOutline(false);
                        supermarketContainer.addView(supermarketView);
                    }
                });
            }
            catch (Exception e)
            {
                Log.e("SortListBottomSheet", "Failed building sort preview", e);
                return;
            }
        });

        sortPreviewThread.setName("SortListPreview");
        sortPreviewThread.start();

        if (activity.isFinishing() || activity.isDestroyed())
        {
            return;
        }

        dialog.show();

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);

        if (bottomSheet != null)
        {
            ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            bottomSheet.setLayoutParams(params);

            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(false);
            behavior.setFitToContents(true);
        }
    }
}