package com.example.baskit.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.Categories.ItemsAdapter;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.Map;

public class ItemViewAlertDialog
{
    TextView adTvQuantity, adTvItemName;
    Button adBtnSave;
    ImageButton adBtnCancel, adBtnUp, adBtnDown;
    LinearLayout adLayout, adLoutQuantity, adLoutQuantityWhole;
    AlertDialog.Builder adb;
    AlertDialog adItemView;
    private RecyclerView recyclerSupermarkets;
    private ItemViewPricesAdapter pricesAdapter;
    protected ItemsAdapter.UpperClassFunctions upperClassFns;
    Activity activity;
    Context context;
    APIHandler apiHandler = APIHandler.getInstance();
    Item item;
    boolean showQuantity;

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

        new Thread(() ->
        {
            Map<String, Map<String, Double>> data = null;

            try
            {
                data = apiHandler.getItemPricesByName(item.getName());
            }
            catch (IOException | JSONException ignored) {}

            Map<String, Map<String, Double>> finalData = data;

            activity.runOnUiThread(() ->
            {
                pricesAdapter = new ItemViewPricesAdapter(context, finalData, item.getSupermarket(), new ItemViewPricesAdapter.OnSupermarketClickListener()
                {
                    @Override
                    public void onSupermarketClick(Supermarket supermarket)
                    {
                        if (supermarket.getSupermarket() == null)
                        {
                            item.setSupermarket(Baskit.unassigned_supermarket);
                            item.setPrice(0);
                        }
                        else
                        {
                            item.setSupermarket(supermarket);

                            Map<String, Double> sectionPrices = finalData.get(supermarket.getSupermarket());

                            if (sectionPrices != null)
                            {
                                Double price = sectionPrices.get(supermarket.getSection());
                                item.setPrice(price != null ? price : 0);
                            }
                            else
                            {
                                item.setPrice(0);
                            }
                        }
                    }
                });

                recyclerSupermarkets.setLayoutManager(new LinearLayoutManager(context));
                recyclerSupermarkets.setAdapter(pricesAdapter);
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
                adBtnDown.setBackgroundColor(Color.TRANSPARENT);
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
                    adBtnDown.setBackgroundColor(Baskit.getAppColor(context, com.google.android.material.R.attr.colorSecondary));
                }

                adTvQuantity.setText(Integer.toString(quantity));
            }
        });
    }

    public void show(Item _item)
    {
        this.item = _item.clone();

        if (pricesAdapter != null && item.getSupermarket() != null)
        {
            pricesAdapter.resetSelection(
                    item.getSupermarket().getSupermarket(),
                    item.getSupermarket().getSection()
            );
        }

        adBtnSave.setClickable(true);
        adBtnCancel.setClickable(true);
        adBtnUp.setClickable(true);
        adBtnDown.setClickable(true);

        adTvItemName.setText(item.getName());

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
            adBtnDown.setBackgroundColor(Baskit.getAppColor(context, com.google.android.material.R.attr.colorSecondary));
        }
        else
        {
            adBtnDown.setBackgroundColor(Color.TRANSPARENT);
        }

        adItemView.show();
    }

    public void setUpperClassFns(ItemsAdapter.UpperClassFunctions upperClassFns)
    {
        this.upperClassFns = upperClassFns;
    }
}
