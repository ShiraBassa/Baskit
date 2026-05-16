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

public class AddCityAlertDialog
{
    final ArrayList<String> all_cities;
    final ArrayList<String> city_choices;
    final ArrayList<String> unchosen_cities;

    final AlertDialog.Builder adb;
    final CitiesListAdapter citiesListAdapter;
    final AlertDialog ad;

    final Button btnAdd;
    final Spinner spinnerCities;
    final LinearLayout adLayout;

    final Activity activity;
    final Context context;
    final OnSubmit onSubmit;

    public interface OnSubmit
    {
        @SuppressLint("NotConstructor")
        void onSubmit(ArrayList<String> city_choices);
    }

    @SuppressLint("InflateParams")
    public AddCityAlertDialog(Activity activity,
                              Context context,
                              ArrayList<String> city_choices,
                              ArrayList<String> all_cities,
                              CitiesListAdapter citiesListAdapter,
                              OnSubmit onSubmit)
            throws IOException
    {
        this.activity = activity;
        this.context = context;
        this.city_choices = city_choices;
        this.citiesListAdapter = citiesListAdapter;
        this.all_cities = all_cities;
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
            onSubmit.onSubmit(city_choices);
            ad.dismiss();
        });
    }

    public void show()
    {
        ad.show();
    }
}
