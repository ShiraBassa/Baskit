package com.example.baskit.Firebase;

import static com.example.baskit.Firebase.FBRefs.refUsers;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.Home.AddCityAlertDialog;
import com.example.baskit.Home.AddSupermarketAlertDialog;
import com.example.baskit.Home.SettingsActivity;
import com.example.baskit.Home.SupermarketsListAdapter;
import com.example.baskit.List.CitiesListAdapter;
import com.example.baskit.Login.ErrorType;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.MainComponents.User;
import com.example.baskit.MasterActivity;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends MasterActivity
{
    FirebaseAuthHandler authHandler;
    APIHandler apiHandler = APIHandler.getInstance();
    EditText etUsername;
    Button btnSubmit;
    String username;
    ArrayList<String> cities, all_cities;
    RecyclerView recyclerCities, recyclerSupermarkets;
    Button btnAddCity, btnRemoveCity, btnAddSupermarket, btnRemoveSupermarket;
    CitiesListAdapter citiesAdapter;
    Map<String, ArrayList<String>> choices;
    SupermarketsListAdapter supermarketsAdapter;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        authHandler = FirebaseAuthHandler.getInstance();
        user = authHandler.getUser();

        init();
    }

    private void init()
    {
        etUsername = findViewById(R.id.et_username);
        btnSubmit = findViewById(R.id.btn_submit);
        recyclerCities = findViewById(R.id.recycler_cities);
        btnAddCity = findViewById(R.id.btn_add_city);
        btnRemoveCity = findViewById(R.id.btn_remove_city);
        btnAddSupermarket = findViewById(R.id.btn_add_supermarket);
        btnRemoveSupermarket = findViewById(R.id.btn_remove_supermarket);
        recyclerSupermarkets = findViewById(R.id.recycler_supermarket);

        cities = new ArrayList<>();
        choices = new HashMap<>();

        runWhenServerActive(() ->
        {
            try
            {
                all_cities = apiHandler.getAllCities();
                citiesAdapter = new CitiesListAdapter(this, cities);
                supermarketsAdapter = new SupermarketsListAdapter(this, choices);

                runOnUiThread(() ->
                {
                    LinearLayoutManager lmCities = new LinearLayoutManager(this);
                    lmCities.setAutoMeasureEnabled(true);

                    recyclerCities.setLayoutManager(lmCities);
                    recyclerCities.setHasFixedSize(false);
                    recyclerCities.setAdapter(citiesAdapter);

                    LinearLayoutManager lmSupermarkets = new LinearLayoutManager(this);
                    lmSupermarkets.setAutoMeasureEnabled(true);

                    recyclerSupermarkets.setLayoutManager(lmSupermarkets);
                    recyclerSupermarkets.setHasFixedSize(false);
                    recyclerSupermarkets.setAdapter(supermarketsAdapter);

                    setButton();
                });
            }
            catch (IOException | JSONException e)
            {
                Log.e("SignUpActivity", "Failed to load sign up data", e);
            }
        });
    }

    private void setButton()
    {
        btnSubmit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                username = etUsername.getText().toString();

                if (checkInputs())
                {
                    submit();
                }
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
                        SignUpActivity.this,
                        SignUpActivity.this,
                        cities,
                        all_cities,
                        citiesAdapter,
                        new AddCityAlertDialog.OnSubmit()
                        {
                            @Override
                            public void OnSubmit(ArrayList<String> city_choices)
                            {
                                cities = city_choices;
                                citiesAdapter.notifyDataSetChanged();
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

                cities.remove(city);
                citiesAdapter.updateData(cities);
                citiesAdapter.notifyDataSetChanged();
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
                        SignUpActivity.this,
                        SignUpActivity.this,
                        supermarketsAdapter,
                        new AddSupermarketAlertDialog.OnSubmit()
                        {
                            @Override
                            public void OnSubmit(Supermarket supermarket)
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
            }
        });
    }

    private boolean checkInputs()
    {
        return true;
    }

    private void submit()
    {
        user = authHandler.getUser();
        user.setName(username);
        user.setCities(cities);

        refUsers.child(user.getId()).setValue(user)
                .addOnCompleteListener(taskDB ->
                {
                    new Thread(() ->
                    {
                        try
                        {
                            apiHandler.setCities(cities);
                        }
                        catch (IOException e)
                        {
                            throw new RuntimeException(e);
                        }
                        catch (JSONException e)
                        {
                            throw new RuntimeException(e);
                        }

                        authHandler.setSupermarkets(choices, null);
                        finish();
                    }).start();
                });
    }
}