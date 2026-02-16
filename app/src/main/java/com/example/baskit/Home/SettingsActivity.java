package com.example.baskit.Home;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.Firebase.FirebaseAuthHandler;
import com.example.baskit.List.CitiesListAdapter;
import com.example.baskit.Login.LoginActivity;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.MasterActivity;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class SettingsActivity extends MasterActivity
{
    FirebaseAuthHandler authHandler;

    private RecyclerView recyclerSupermarkets, recyclerCities;
    private SupermarketsListAdapter supermarketsAdapter;
    CitiesListAdapter citiesAdapter;
    APIHandler apiHandler = APIHandler.getInstance();

    ImageButton btnHome, btnLogOut;
    Button btnAddSupermarket, btnRemoveSupermarket, btnAddCity, btnRemoveCity;
    Map<String, ArrayList<String>> choices;
    ArrayList<String> cities, all_cities;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        authHandler = FirebaseAuthHandler.getInstance();

        init();
    }

    private void init()
    {
        btnHome = findViewById(R.id.btn_home);
        btnLogOut = findViewById(R.id.btn_log_out);
        btnAddSupermarket = findViewById(R.id.btn_add_supermarket);
        btnRemoveSupermarket = findViewById(R.id.btn_remove_supermarket);
        recyclerSupermarkets = findViewById(R.id.recycler_supermarket);
        recyclerCities = findViewById(R.id.recycler_cities);
        btnAddCity = findViewById(R.id.btn_add_city);
        btnRemoveCity = findViewById(R.id.btn_remove_city);

        runWhenServerActive(() ->
        {
            try
            {
                choices = apiHandler.getChoices();
                cities = apiHandler.getCities();
                all_cities = apiHandler.getAllCities();

                supermarketsAdapter = new SupermarketsListAdapter(this, choices);
                citiesAdapter = new CitiesListAdapter(this, cities);

                runOnUiThread(() ->
                {
                    LinearLayoutManager lmSupermarkets = new LinearLayoutManager(this);
                    lmSupermarkets.setAutoMeasureEnabled(true);

                    recyclerSupermarkets.setLayoutManager(lmSupermarkets);
                    recyclerSupermarkets.setHasFixedSize(false);
                    recyclerSupermarkets.setAdapter(supermarketsAdapter);

                    LinearLayoutManager lmCities = new LinearLayoutManager(this);
                    lmCities.setAutoMeasureEnabled(true);

                    recyclerCities.setLayoutManager(lmCities);
                    recyclerCities.setHasFixedSize(false);
                    recyclerCities.setAdapter(citiesAdapter);
                });
            }
            catch (IOException | JSONException e)
            {
                Log.e("SettingsActivity", "Failed to load settings data", e);
            }
        });

        setButtons();
    }

    private void setButtons()
    {
        btnHome.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                finish();
            }
        });

        btnLogOut.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                authHandler.logOut();

                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                intent.putExtra("fromLogout", true);

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        btnAddSupermarket.setOnClickListener(v ->
        {
            if (supermarketsAdapter == null)
            {
                return;
            }

            try
            {
                new AddSupermarketAlertDialog(
                        SettingsActivity.this,
                        SettingsActivity.this,
                        supermarketsAdapter,
                        new AddSupermarketAlertDialog.OnSubmit()
                        {
                            @Override
                            public void OnSubmit(Supermarket supermarket)
                            {
                                Baskit.notActivityRunIfOnline(() -> authHandler.addSupermarketSection(supermarket, () ->
                                {
                                    SettingsActivity.this.runOnUiThread(() ->
                                    {
                                        choices.putIfAbsent(supermarket.getSupermarket(), new ArrayList<>());
                                        ArrayList<String> sectionsList = choices.get(supermarket.getSupermarket());

                                        if (sectionsList == null)
                                        {
                                            sectionsList = new ArrayList<>();
                                            choices.put(supermarket.getSupermarket(), sectionsList);
                                        }

                                        if (!sectionsList.contains(supermarket.getSection()))
                                        {
                                            sectionsList.add(supermarket.getSection());
                                        }

                                        supermarketsAdapter.updateData(choices);
                                        supermarketsAdapter.notifyDataSetChanged();
                                    });
                                }), SettingsActivity.this);
                            }
                        },
                        cities
                );
            }
            catch (JSONException e)
            {
                throw new RuntimeException(e);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        });

        btnRemoveSupermarket.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (supermarketsAdapter == null)
                {
                    return;
                }

                String supermarketName = supermarketsAdapter.getSelectedSupermarketName();
                String sectionName = supermarketsAdapter.getSelectedSectionName();

                if (supermarketName == null || sectionName == null)
                {
                    return;
                }

                runIfOnline(() ->
                {
                    authHandler.removeSupermarketSection(new Supermarket(supermarketName, sectionName), () ->
                    {
                        runOnUiThread(() ->
                        {
                            ArrayList<String> sections = choices.get(supermarketName);
                            if (sections != null)
                            {
                                sections.remove(sectionName);

                                if (sections.isEmpty())
                                {
                                    choices.remove(supermarketName);
                                }

                                supermarketsAdapter.updateData(choices);
                                supermarketsAdapter.notifyDataSetChanged();
                            }
                        });
                    });
                });
            }
        });

        btnAddCity.setOnClickListener(v ->
        {
            if (cities == null)
            {
                return;
            }

            try
            {
                new AddCityAlertDialog(
                        SettingsActivity.this,
                        SettingsActivity.this,
                        cities,
                        all_cities,
                        citiesAdapter,
                        new AddCityAlertDialog.OnSubmit()
                        {
                            @Override
                            public void OnSubmit(ArrayList<String> city_choices)
                            {
                                Baskit.notActivityRunWhenServerActive(() ->
                                {
                                    try
                                    {
                                        apiHandler.setCities(city_choices);

                                        SettingsActivity.this.runOnUiThread(() ->
                                        {
                                            citiesAdapter.notifyDataSetChanged();
                                        });
                                    }
                                    catch (IOException | JSONException e)
                                    {
                                        Log.e("AddCityAlertDialog", "Failed to set cities", e);
                                        SettingsActivity.this.runOnUiThread(() ->
                                                Toast.makeText(SettingsActivity.this, "שגיאה בשמירת הערים", Toast.LENGTH_SHORT).show());
                                    }
                                }, SettingsActivity.this);
                            }
                        }
                ).show();
            }
            catch (JSONException e)
            {
                throw new RuntimeException(e);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        });

        btnRemoveCity.setOnClickListener(new View.OnClickListener()
        {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View v)
            {
                if (citiesAdapter == null)
                {
                    return;
                }

                String city = citiesAdapter.getSelectedCity();

                if (city == null || city.isEmpty())
                {
                    return;
                }

                authHandler.removeCity(city, () ->
                {
                    runOnUiThread(() ->
                    {
                        if (cities != null)
                        {
                            cities.remove(city);
                            citiesAdapter.updateData(cities);
                            citiesAdapter.notifyDataSetChanged();
                        }
                    });
                });
            }
        });
    }
}