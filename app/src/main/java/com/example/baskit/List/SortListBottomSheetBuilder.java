package com.example.baskit.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.baskit.Baskit;
import com.example.baskit.MainComponents.Item.ItemVariant;
import com.example.baskit.MainComponents.SortableEntity;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SortListBottomSheetBuilder
{
    public interface ApplyListener
    {
        void onApplyCheapest();
        void onApplySupermarket(Supermarket sm);
    }

    public static void show(
            AppCompatActivity activity,
            SortableEntity entity,
            Map<String, ArrayList<ItemVariant>> rows,
            ArrayList<Supermarket> supermarkets,
            ApplyListener listener)
    {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View view = activity.getLayoutInflater().inflate(R.layout.bottom_sheet_sort_list, null);
        dialog.setContentView(view);

        View cheapestLayout = view.findViewById(R.id.layout_cheapest);
        TextView cheapestTvSupermarket = cheapestLayout.findViewById(R.id.tv_supermarket);
        TextView cheapestTvPrice = cheapestLayout.findViewById(R.id.tv_price);

        LinearLayout supermarketContainer = view.findViewById(R.id.supermarkets_container);

        new Thread(() ->
        {
            Map<Supermarket, Double> totals = new HashMap<>();
            Map<Supermarket, Boolean> allKnown = new HashMap<>();

            SortableEntity cheapestPreview = entity.copy();
            cheapestPreview.setCheapestVariants(rows);

            for (Supermarket sm : supermarkets)
            {
                SortableEntity preview = entity.copy();
                preview.setSupermarketsVariants(sm, rows);

                totals.put(sm, preview.getTotal());
                allKnown.put(sm, preview.allPricesKnown());
            }

            activity.runOnUiThread(() ->
            {
                cheapestTvSupermarket.setText("הזולים ביותר");
                cheapestTvPrice.setText(
                        Baskit.getTotalDisplayString(
                                cheapestPreview.getTotal(),
                                cheapestPreview.allPricesKnown(),
                                false,
                                false
                        )
                );

                cheapestLayout.setOnClickListener(v ->
                {
                    listener.onApplyCheapest();
                    dialog.dismiss();
                });

                supermarketContainer.removeAllViews();

                LayoutInflater inflater = LayoutInflater.from(activity);

                ArrayList<Map.Entry<Supermarket, Double>> entries = new ArrayList<>(totals.entrySet());
                entries.sort((a, b) ->
                        Double.compare(a.getValue(), b.getValue()));

                for (Map.Entry<Supermarket, Double> entry : entries)
                {
                    Supermarket sm = entry.getKey();
                    double total = entry.getValue();

                    View supermarketView = inflater.inflate(
                            R.layout.sort_list_supermarket,
                            supermarketContainer,
                            false
                    );

                    TextView name = supermarketView.findViewById(R.id.tv_supermarket);
                    TextView price = supermarketView.findViewById(R.id.tv_price);

                    name.setText("- " + sm);
                    price.setText(
                            Baskit.getTotalDisplayString(
                                    total,
                                    allKnown.get(sm),
                                    false,
                                    false
                            )
                    );

                    supermarketView.setOnClickListener(v ->
                    {
                        listener.onApplySupermarket(sm);
                        dialog.dismiss();
                    });

                    supermarketContainer.addView(supermarketView);
                }
            });
        }).start();

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