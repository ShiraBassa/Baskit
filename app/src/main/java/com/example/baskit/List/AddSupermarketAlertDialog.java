package com.example.baskit.List;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Firebase.FirebaseAuthHandler;
import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    FirebaseAuthHandler authHandler;
    Spinner spinnerSupermarkets, spinnerSections;
    Map<String, ArrayList<String>> all_branches;
    ArrayList<String> all_supermarkets;
    ArrayList<String> all_sections;
    Map<String, ArrayList<String>> choices;
    SupermarketsListAdapter supermarketsAdapter;

    public AddSupermarketAlertDialog(Activity activity,
                                     Context context,
                                     Map<String, ArrayList<String>> choices,
                                     SupermarketsListAdapter supermarketsAdapter,
                                     Map<String, ArrayList<String>> all_branches)
            throws JSONException, IOException
    {
        this.activity = activity;
        this.context = context;
        this.choices = choices;
        this.supermarketsAdapter = supermarketsAdapter;
        this.all_branches = all_branches;

        authHandler = FirebaseAuthHandler.getInstance();

        all_supermarkets = new ArrayList<>(all_branches.keySet());

        adLayout = (LinearLayout) activity.getLayoutInflater()
                .inflate(R.layout.alert_dialog_add_supermarket, null);
        spinnerSupermarkets = adLayout.findViewById(R.id.spinner_supermarkets);
        spinnerSections = adLayout.findViewById(R.id.spinner_sections);
        btnAdd = adLayout.findViewById(R.id.btn_add);

        adb = new AlertDialog.Builder(context);
        adb.setView(adLayout);
        ad = adb.create();

        setSpinners();
        setButton();
    }

    private void setSpinners()
    {
        ArrayAdapter<String> supermarketAdapter =
                new ArrayAdapter<>(context,
                        android.R.layout.simple_spinner_dropdown_item,
                        all_supermarkets);

        spinnerSupermarkets.setAdapter(supermarketAdapter);

        spinnerSupermarkets.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id)
            {
                String selectedSupermarket = all_supermarkets.get(position);
                all_sections = all_branches.get(selectedSupermarket);

                ArrayAdapter<String> sectionAdapter =
                        new ArrayAdapter<>(context,
                                android.R.layout.simple_spinner_dropdown_item,
                                all_sections);

                spinnerSections.setAdapter(sectionAdapter);
                spinnerSections.setEnabled(true);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent)
            {
                spinnerSections.setEnabled(false);
            }
        });

        spinnerSections.setEnabled(false);
    }

    private void setButton()
    {
        btnAdd.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String supermarketName = (String) spinnerSupermarkets.getSelectedItem();
                String sectionName = (String) spinnerSections.getSelectedItem();

                if (supermarketName == null || sectionName == null) {
                    Toast.makeText(context, "נא לבחור סופרמרקט ומחלקה", Toast.LENGTH_SHORT).show();
                    return;
                }

                Supermarket supermarket = new Supermarket(supermarketName, sectionName);

                authHandler.addSupermarket(supermarket, () ->
                {
                    activity.runOnUiThread(() -> {
                        choices.putIfAbsent(supermarket.getSupermarket(), new ArrayList<>());
                        ArrayList<String> sectionsList = choices.get(supermarket.getSupermarket());
                        if (sectionsList == null) {
                            sectionsList = new ArrayList<>();
                            choices.put(supermarket.getSupermarket(), sectionsList);
                        }
                        sectionsList.add(supermarket.getSection());

                        supermarketsAdapter.updateData(choices);
                        supermarketsAdapter.notifyDataSetChanged();
                        ad.dismiss();
                    });
                });
            }
        });
    }

    public void show()
    {
        ad.show();
    }
}
