package com.example.baskit.Home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Firebase.FirebaseAuthHandler;
import com.example.baskit.List.CitiesListAdapter;
import com.example.baskit.List.SupermarketsListAdapter;
import com.example.baskit.Login.LoginActivity;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity
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

        new Thread(() -> {
            try
            {
                choices = apiHandler.getChoices();
                cities = apiHandler.getCities();
                all_cities = apiHandler.getAllCities();

                supermarketsAdapter = SupermarketsListAdapter.fromSupermarkets(choices, this);
                citiesAdapter = new CitiesListAdapter(cities);

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
            } catch (IOException | JSONException ignored) {}
        }).start();

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
                new AddSectionAlertDialog(
                        SettingsActivity.this,
                        SettingsActivity.this,
                        supermarketsAdapter
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

                Supermarket supermarket = supermarketsAdapter.getSelectedSupermarket();

                if (supermarket == null ||
                    supermarket.getSupermarket() == null || supermarket.getSupermarket().isEmpty() ||
                    supermarket.getSection() == null || supermarket.getSection().isEmpty())
                {
                    return;
                }

                authHandler.removeSupermarketSection(supermarket, () ->
                {
                    runOnUiThread(() ->
                    {
                        String supermarketName = supermarket.getSupermarket();
                        ArrayList<String> sections = choices.get(supermarketName);
                        if (sections != null)
                        {
                            sections.remove(supermarket.getSection());

                            if (sections.isEmpty())
                            {
                                choices.remove(supermarketName);
                            }

                            supermarketsAdapter.updateData(choices);
                            supermarketsAdapter.notifyDataSetChanged();
                        }
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
                        citiesAdapter
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