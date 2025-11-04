package com.example.baskit.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Categories.ItemsAdapter;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class ItemViewAlertDialog
{
    TextView adTvQuantity, adTvItemName;
    Button adBtnSave;
    ImageButton adBtnCancel, adBtnUp, adBtnDown;
    LinearLayout adLayout, adLoutQuantity, adLoutQuantityWhole;
    AlertDialog.Builder adb;
    AlertDialog adAddItem;
    private RecyclerView recyclerSupermarkets;
    private SupermarketsListAdapter supermarketsAdapter;
    protected ItemsAdapter.UpperClassFunctions upperClassFns;
    Activity activity;
    Context context;
    APIHandler apiHandler = APIHandler.getInstance();
    Item item;
    boolean showQuantity;

    public ItemViewAlertDialog(Activity activity, Context context, ItemsAdapter.UpperClassFunctions upperClassFns, Item item, boolean showQuantity)
    {
        this.activity = activity;
        this.context = context;
        this.item = item;
        this.upperClassFns = upperClassFns;
        this.showQuantity = showQuantity;

        adLayout = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.alert_dialog_item_view, null);
        adBtnCancel = adLayout.findViewById(R.id.btn_cancel);
        adBtnSave = adLayout.findViewById(R.id.btn_save);
        adBtnUp = adLayout.findViewById(R.id.btn_up);
        adBtnDown = adLayout.findViewById(R.id.btn_down);
        adTvQuantity = adLayout.findViewById(R.id.tv_quantity);
        adLoutQuantity = adLayout.findViewById(R.id.lout_quantity);
        adLoutQuantityWhole = adLayout.findViewById(R.id.lout_quantity_whole);
        recyclerSupermarkets = adLayout.findViewById(R.id.recycler_supermarkets);
        adTvItemName = adLayout.findViewById(R.id.tv_item_name);

        new Thread(() ->
        {
            Map<String, Map<String, Double>> data = null;

            try
            {
                data = apiHandler.getItemPricesByName(item.getName());
            }
            catch (IOException | JSONException ignored) {}

            supermarketsAdapter = SupermarketsListAdapter.fromSupermarketsWithPrices(data, activity);

            activity.runOnUiThread(() -> {
                new Handler(Looper.getMainLooper()).post(() -> {
                    recyclerSupermarkets.setLayoutManager(new LinearLayoutManager(context));
                    recyclerSupermarkets.setAdapter(supermarketsAdapter);
                });
            });
        }).start();

        adb = new AlertDialog.Builder(context);
        adb.setView(adLayout);
        adAddItem = adb.create();

        adBtnCancel.setOnClickListener(v -> adAddItem.dismiss());
        adBtnSave.setOnClickListener(v ->
        {
            upperClassFns.updateItemCategory(item);
            adAddItem.dismiss();
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
                    adBtnDown.setBackgroundColor(Color.LTGRAY);
                }

                adTvQuantity.setText(Integer.toString(quantity));
            }
        });
    }

    public void show()
    {
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

        adAddItem.show();
    }
}
