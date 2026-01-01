package com.example.baskit.Home;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.MasterActivity;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class AddSupermarketAlertDialog
{
    Activity activity;
    Context context;
    LinearLayout adLayout;
    AlertDialog.Builder adb;
    AlertDialog ad;
    Button btnAdd;
    Spinner spinnerSupermarkets;
    ArrayList<String> all_stores;
    Map<String, ArrayList<String>> choices;
    AddSectionAlertDialog.OnStoreAddedListener onStoreAddedListener;
    private final APIHandler apiHandler = APIHandler.getInstance();

    public AddSupermarketAlertDialog(Activity activity,
                                     Context context,
                                     Map<String, ArrayList<String>> choices,
                                     ArrayList<String> unchosen_stores,
                                     AddSectionAlertDialog.OnStoreAddedListener onStoreAddedListener)
            throws JSONException, IOException
    {
        this.activity = activity;
        this.context = context;
        this.choices = choices;
        this.onStoreAddedListener = onStoreAddedListener;
        this.all_stores = unchosen_stores;

        adLayout = (LinearLayout) activity.getLayoutInflater()
                .inflate(R.layout.alert_dialog_add_supermarket, null);
        spinnerSupermarkets = adLayout.findViewById(R.id.spinner_supermarkets);
        btnAdd = adLayout.findViewById(R.id.btn_add);

        adb = new AlertDialog.Builder(context);
        adb.setView(adLayout);
        ad = adb.create();

        setSpinner();
        setButton();
    }

    private void setSpinner()
    {
        ArrayAdapter<String> supermarketAdapter =
                new ArrayAdapter<>(context,
                        android.R.layout.simple_spinner_dropdown_item,
                        all_stores);

        spinnerSupermarkets.setAdapter(supermarketAdapter);

        spinnerSupermarkets.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id)
            {
                btnAdd.setEnabled(true);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent)
            {
                btnAdd.setEnabled(false);
            }
        });

        btnAdd.setEnabled(false);
    }

    private void setButton()
    {
        btnAdd.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String supermarketName = (String) spinnerSupermarkets.getSelectedItem();

                if (supermarketName == null)
                {
                    Toast.makeText(context, "נא לבחור סופרמרקט", Toast.LENGTH_SHORT).show();
                    return;
                }

                ArrayList<String> curr_supermarkets = new ArrayList<>(choices.keySet());
                curr_supermarkets.add(supermarketName);

                Baskit.notActivityRunWhenServerActive(() ->
                {
                    try
                    {
                        apiHandler.setStores(curr_supermarkets);

                        activity.runOnUiThread(() ->
                        {
                            if (!choices.containsKey(supermarketName))
                            {
                                choices.put(supermarketName, new ArrayList<>());
                            }

                            onStoreAddedListener.onStoreAdded(supermarketName, new ArrayList<>());

                            ad.dismiss();
                        });
                    }
                    catch (IOException | JSONException e)
                    {
                        Log.e("AddSupermarketAlertDialog", "Failed to set stores", e);
                        activity.runOnUiThread(() ->
                                Toast.makeText(context, "שגיאה בשמירת הסופרמרקט", Toast.LENGTH_SHORT).show());
                    }
                }, activity);
            }
        });
    }

    public void show()
    {
        ad.show();
    }
}
