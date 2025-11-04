package com.example.baskit.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
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

import com.example.baskit.Categories.ItemsAdapter;
import com.example.baskit.Categories.ItemsAdapterChecked;
import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.R;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class EditListAlertDialog
{
    ImageButton adBtnCancel;
    Button adBtnSave;
    LinearLayout adLayout;
    AlertDialog.Builder adb;
    AlertDialog adEditList;
    Activity activity;
    Context context;
    ArrayList<Item> items;
    List list;
    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    private RecyclerView recyclerSupermarkets;
    private EditListSupermarketsAdapter supermarketsAdapter;

    public EditListAlertDialog(Activity activity, Context context, ArrayList<Item> items, List list)
    {
        this.activity = activity;
        this.context = context;
        this.list = list;

        this.items = new ArrayList<>();

        for (Item item : items)
        {
            this.items.add(item.clone());
        }

        adLayout = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.alert_dialog_edit_list, null);
        adBtnCancel = adLayout.findViewById(R.id.btn_cancel);
        adBtnSave = adLayout.findViewById(R.id.btn_save);
        recyclerSupermarkets = adLayout.findViewById(R.id.recycler_supermarkets);

        adb = new AlertDialog.Builder(context);
        adb.setView(adLayout);
        adEditList = adb.create();

        adBtnCancel.setOnClickListener(v -> finish());
        adBtnSave.setOnClickListener(v -> finish(true));

        supermarketsAdapter = new EditListSupermarketsAdapter(items);
        this.recyclerSupermarkets.setLayoutManager(new LinearLayoutManager(this.context));
        this.recyclerSupermarkets.setAdapter(supermarketsAdapter);
    }

    public void show()
    {
        adEditList.show();

        if (adEditList.getWindow() != null)
        {
            adEditList.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    public void finish()
    {
        adEditList.dismiss();
    }

    public void finish(boolean save)
    {
        if (save)
        {
            items = supermarketsAdapter.getNewItems();
            dbHandler.updateItems(list, items);
        }

        finish();
    }
}
