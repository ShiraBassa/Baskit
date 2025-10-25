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

import com.example.baskit.MainComponents.Item;
import com.example.baskit.R;

import java.net.ConnectException;
import java.util.ArrayList;

public class AddItemAlertDialog
{
    TextView adTvQuantity;
    ImageButton adBtnCancel, adBtnUp, adBtnDown;
    Button adBtnAddItem;
    LinearLayout adLayout, adLoutQuantity;
    AlertDialog.Builder adb;
    AlertDialog adAddItem;
    AutoCompleteTextView adSearchItem;
    Item selectedItem;
    Activity activity;
    ArrayList<String> allItemNames;
    Context context;
    AddItemInterface addItemInterface;
    ProgressBar adProgressBar;

    public interface AddItemInterface
    {
        void addItem(Item item);
    }

    public AddItemAlertDialog(Activity activity, Context context, ArrayList<String> allItemNames, AddItemInterface addItemInterface)
    {
        this.activity = activity;
        this.context = context;
        this.allItemNames = allItemNames;
        this.addItemInterface = addItemInterface;

        adLayout = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.alert_dialog_add_item, null);
        adBtnCancel = adLayout.findViewById(R.id.btn_cancel);
        adBtnAddItem = adLayout.findViewById(R.id.btn_add_item);
        adSearchItem = adLayout.findViewById(R.id.searchItem);
        adBtnUp = adLayout.findViewById(R.id.btn_up);
        adBtnDown = adLayout.findViewById(R.id.btn_down);
        adTvQuantity = adLayout.findViewById(R.id.tv_quantity);
        adLoutQuantity = adLayout.findViewById(R.id.lout_quantity);
        adProgressBar = adLayout.findViewById(R.id.progressBar);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.add_item_dropdown_item, allItemNames)
        {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
            {
                if (convertView == null)
                {
                    convertView = activity.getLayoutInflater().inflate(R.layout.add_item_dropdown_item, parent, false);
                }

                TextView tvName = convertView.findViewById(R.id.tvItemName);
                String name = getItem(position);
                tvName.setText(name);

                return convertView;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
            {
                if (convertView == null)
                {
                    convertView = activity.getLayoutInflater().inflate(R.layout.add_item_dropdown_item, parent, false);
                }

                TextView tvName = convertView.findViewById(R.id.tvItemName);
                String name = getItem(position);

                if (name != null)
                {
                    tvName.setText(name);
                }

                return convertView;
            }
        };

        adSearchItem.setAdapter(adapter);
        adSearchItem.setThreshold(1); // start filtering after 1 character
        adSearchItem.setOnClickListener(v -> adSearchItem.showDropDown());

        adSearchItem.setOnFocusChangeListener((v, hasFocus) ->
        {
            if (!hasFocus)
            {
                String typed = adSearchItem.getText().toString().trim();
                boolean match = false;

                for (String name : allItemNames)
                {
                    if (typed.equals(name))
                    {
                        selectedItem = new Item(name);
                        selectedItem.setQuantity(1);
                        match = true;
                        break;
                    }
                }

                if (!match)
                {
                    adSearchItem.setText("");
                    adLoutQuantity.setVisibility(View.INVISIBLE);
                    selectedItem = null;
                }
            }
        });

        adSearchItem.setOnItemClickListener((parent, view, position, id) ->
        {
            String clickedName = (String) parent.getItemAtPosition(position);
            selectedItem = new Item(clickedName);
            selectedItem.setQuantity(1);

            adLoutQuantity.setVisibility(View.VISIBLE);
            adTvQuantity.setText("1");
            adBtnDown.setBackgroundColor(Color.LTGRAY);
        });

        adSearchItem.setDropDownHeight(ViewGroup.LayoutParams.WRAP_CONTENT);

        adb = new AlertDialog.Builder(context);
        adb.setView(adLayout);
        adAddItem = adb.create();

        adBtnCancel.setOnClickListener(v -> adAddItem.dismiss());

        adBtnAddItem.setOnClickListener(v ->
        {
            if (selectedItem != null)
            {
                addItemInterface.addItem(selectedItem);
            }
            else
            {
                adSearchItem.setError("בחר פריט מהרשימה");
            }
        });

        adBtnUp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (selectedItem != null)
                {
                    adTvQuantity.setText(Integer.toString(selectedItem.raiseQuantity()));
                    adBtnDown.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });

        adBtnDown.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (selectedItem != null)
                {
                    if (selectedItem.getQuantity() <= 1)
                    {
                        return;
                    }

                    int quantity = selectedItem.lowerQuantity();

                    if (quantity == 1)
                    {
                        adBtnDown.setBackgroundColor(Color.LTGRAY);
                    }

                    adTvQuantity.setText(Integer.toString(quantity));
                }
            }
        });
    }

    public void show()
    {
        adSearchItem.setText("");
        adLoutQuantity.setVisibility(View.INVISIBLE);
        selectedItem = null;

        adProgressBar.setVisibility(View.INVISIBLE);
        adBtnAddItem.setClickable(true);
        adBtnCancel.setClickable(true);
        adBtnUp.setClickable(true);
        adBtnDown.setClickable(true);

        adAddItem.show();
    }

    public void finish()
    {
        adAddItem.dismiss();
    }

    public void startProgressBar()
    {
        if (adProgressBar != null)
        {
            adProgressBar.setVisibility(View.VISIBLE);
            adBtnAddItem.setClickable(false);
            adBtnCancel.setClickable(false);
            adBtnUp.setClickable(false);
            adBtnDown.setClickable(false);
        }
    }

    public void endProgressBar()
    {
        if (adProgressBar != null)
        {
            adProgressBar.setVisibility(View.INVISIBLE);
        }
    }
}
