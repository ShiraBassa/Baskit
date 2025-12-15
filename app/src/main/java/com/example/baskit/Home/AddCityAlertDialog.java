package com.example.baskit.Home;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Firebase.FirebaseAuthHandler;
import com.example.baskit.List.CitiesListAdapter;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

public class AddCityAlertDialog
{
    Activity activity;
    Context context;
    LinearLayout adLayout;
    AlertDialog.Builder adb;
    AlertDialog ad;
    Button btnAdd;
    FirebaseAuthHandler authHandler;
    Spinner spinnerCities;
    ArrayList<String> all_cities, city_choices, unchosen_cities;
    private final APIHandler apiHandler = APIHandler.getInstance();
    CitiesListAdapter citiesListAdapter;

    public AddCityAlertDialog(Activity activity,
                              Context context,
                              ArrayList<String> city_choices,
                              ArrayList<String> all_cities,
                              CitiesListAdapter citiesListAdapter)
            throws JSONException, IOException
    {
        this.activity = activity;
        this.context = context;
        this.city_choices = city_choices;
        this.citiesListAdapter = citiesListAdapter;
        this.all_cities = all_cities;

        unchosen_cities = new ArrayList<>();
        unchosen_cities.addAll(all_cities);
        unchosen_cities.removeAll(city_choices);

        authHandler = FirebaseAuthHandler.getInstance();

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
        btnAdd.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String city = (String) spinnerCities.getSelectedItem();

                if (city == null)
                {
                    Toast.makeText(context, "נא לבחור עיר", Toast.LENGTH_SHORT).show();
                    return;
                }

                city_choices.add(city);

                new Thread(() ->
                {
                    try
                    {
                        apiHandler.setCities(city_choices);
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                    catch (JSONException e)
                    {
                        throw new RuntimeException(e);
                    }

                    activity.runOnUiThread(() ->
                    {
                        citiesListAdapter.notifyDataSetChanged();
                        ad.dismiss();
                    });
                }).start();
            }
        });
    }

    public void show()
    {
        ad.show();
    }
}
