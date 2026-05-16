package com.example.baskit.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.baskit.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;

public class AddCityAlertDialog
{
    final ArrayList<String> city_choices;
    final ArrayList<String> unchosen_cities;

    final AlertDialog.Builder adb;
    final AlertDialog ad;

    final Button btnAdd;
    final Spinner spinnerCities;
    final LinearLayout adLayout;

    final Context context;
    final Consumer<ArrayList<String>> onSubmit;

    @SuppressLint("InflateParams")
    public AddCityAlertDialog(Activity activity,
                              Context context,
                              ArrayList<String> city_choices,
                              ArrayList<String> all_cities,
                              Consumer<ArrayList<String>> onSubmit)
            throws IOException
    {
        this.context = context;
        this.city_choices = city_choices;
        this.onSubmit = onSubmit;

        unchosen_cities = new ArrayList<>();
        unchosen_cities.addAll(all_cities);
        unchosen_cities.removeAll(city_choices);

        adLayout = (LinearLayout) activity.getLayoutInflater()
                .inflate(R.layout.alert_dialog_add_city, null);
        spinnerCities = adLayout.findViewById(R.id.spinner_cities);
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
                        unchosen_cities);

        spinnerCities.setAdapter(supermarketAdapter);

        spinnerCities.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener()
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
        btnAdd.setOnClickListener(v -> {
            String city = (String) spinnerCities.getSelectedItem();

            if (city == null)
            {
                Toast.makeText(context, "נא לבחור עיר", Toast.LENGTH_SHORT).show();
                return;
            }

            city_choices.add(city);
            onSubmit.accept(city_choices);
            ad.dismiss();
        });
    }

    public void show()
    {
        ad.show();
    }
}
