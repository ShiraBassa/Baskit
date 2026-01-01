package com.example.baskit.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.Categories.ItemsAdapter;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.MasterActivity;
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

        Baskit.notActivityRunWhenServerActive(() ->
        {
            Map<String, Map<String, Double>> data = null;

            try
            {
                String absId = item.getAbsoluteId();
                if (absId != null)
                {
                    data = apiHandler.getItemPricesByCode(absId);
                }
            }
            catch (IOException | JSONException e)
            {
                Log.e("ItemViewAlertDialog", "Failed to load item prices", e);
            }

            Map<String, Map<String, Double>> finalData = data;

            if (activity == null) return;

            activity.runOnUiThread(() ->
            {
                if (activity.isFinishing() || activity.isDestroyed()) return;

                pricesAdapter = new ItemViewPricesAdapter(context, finalData, item.getSupermarket(), new ItemViewPricesAdapter.OnSupermarketClickListener()
                {
                    @Override
                    public void onSupermarketClick(Supermarket supermarket)
                    {
                        if (supermarket == null || supermarket.getSupermarket() == null)
                        {
                            item.setSupermarket(null);
                            item.setPrice(0);
                            return;
                        }

                        item.setSupermarket(supermarket);

                        if (finalData == null)
                        {
                            item.setPrice(0);
                            return;
                        }

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
                });

                recyclerSupermarkets.setLayoutManager(new LinearLayoutManager(context));
                recyclerSupermarkets.setAdapter(pricesAdapter);

                if (finalData == null)
                {
                    Toast.makeText(context, "No prices found", Toast.LENGTH_SHORT).show();
                }
            });
        }, activity);

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
                    adBtnDown.setBackgroundColor(Baskit.getAppColor(context, R.color.quantity));
                }

                adTvQuantity.setText(Integer.toString(quantity));
            }
        });
    }

    public void show(Item _item)
    {
        this.item = _item.clone();

        if (pricesAdapter != null)
        {
            if (item.getSupermarket() != null)
            {
                pricesAdapter.resetSelection(
                        item.getSupermarket().getSupermarket(),
                        item.getSupermarket().getSection()
                );
            }
            else
            {
                pricesAdapter.resetSelection(null, null);
            }
        }

        adBtnSave.setClickable(true);
        adBtnCancel.setClickable(true);
        adBtnUp.setClickable(true);
        adBtnDown.setClickable(true);

        adTvItemName.setText(item.getDecodedName());

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
            adBtnDown.setBackgroundColor(Baskit.getAppColor(context, R.color.quantity));
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
