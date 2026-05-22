package com.example.baskit.login;

import static com.example.baskit.online_components.FBRefs.refUsers;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.online_components.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.online_components.FirebaseAuthHandler;
import com.example.baskit.home.AddCityAlertDialog;
import com.example.baskit.home.AddSupermarketAlertDialog;
import com.example.baskit.home.SupermarketsListAdapter;
import com.example.baskit.home.CitiesListAdapter;
import com.example.baskit.main_components.Supermarket;
import com.example.baskit.main_components.User;
import com.example.baskit.MasterActivity;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"CallToPrintStackTrace", "deprecation"})
public class SignUpActivity extends MasterActivity
{
    User user;
    String username;

    ArrayList<String> cities, all_cities;
    Map<String, ArrayList<String>> choices;

    FirebaseAuthHandler authHandler;
    final APIHandler apiHandler = APIHandler.getInstance();

    CitiesListAdapter citiesAdapter;
    SupermarketsListAdapter supermarketsAdapter;

    EditText etUsername;
    RecyclerView recyclerCities, recyclerSupermarkets;
    Button btnAddCity, btnRemoveCity, btnAddSupermarket, btnRemoveSupermarket, btnSubmit;
    View loadingOverlay;


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
        loadingOverlay = findViewById(R.id.loading_overlay);
        setLoading(true);

        cities = new ArrayList<>();
        choices = new HashMap<>();
        updateSupermarketButtonState();

        runWhenServerActive(() ->
        {
            if (isFinishing() || isDestroyed())
            {
                return;
            }
            try
            {
                all_cities = apiHandler.getAllCities();
                citiesAdapter = new CitiesListAdapter(SignUpActivity.this, cities);
                supermarketsAdapter = new SupermarketsListAdapter(SignUpActivity.this, choices);

                runOnUiThread(() ->
                {
                    if (isFinishing() || isDestroyed())
                    {
                        return;
                    }

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
                    setLoading(false);
                });
            }
            catch (IOException | JSONException e)
            {
                Log.e("SignUpActivity", "Failed to load sign up data", e);

                runOnUiThread(() ->
                {
                    if (isFinishing() || isDestroyed())
                    {
                        return;
                    }

                    setLoading(false);
                });
            }
        });
    }

    private void setButton()
    {
        btnSubmit.setOnClickListener(v ->
        {
            if (isFinishing() || isDestroyed())
            {
                return;
            }

            username = etUsername.getText().toString().trim();

            if (!Baskit.isValidUserName(username, true))
            {
                return;
            }

            runProtectedRequest(
                    "signup_submit",
                    btnSubmit,
                    this::submit
            );
        });

        btnAddCity.setOnClickListener(v ->
        {
            if (cities == null)
            {
                return;
            }

            if (all_cities == null || all_cities.isEmpty())
            {
                return;
            }

            try {
                new AddCityAlertDialog(
                        SignUpActivity.this,
                        SignUpActivity.this,
                        cities,
                        all_cities,
                        city_choices ->
                        {
                            cities = city_choices;
                            citiesAdapter.notifyDataSetChanged();
                            updateSupermarketButtonState();
                        }
                ).show();
            }
            catch (IOException e)
            {
                Log.e("SignUpActivity", "Failed opening city dialog", e);
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
                updateSupermarketButtonState();
            }
        });

        btnAddSupermarket.setOnClickListener(v ->
        {
            if (supermarketsAdapter == null)
            {
                return;
            }

            if (cities == null || cities.isEmpty())
            {
                return;
            }

            try
            {
                new AddSupermarketAlertDialog(
                        SignUpActivity.this,
                        SignUpActivity.this,
                        supermarket ->
                        {
                            choices.putIfAbsent(supermarket.getSupermarket(), new ArrayList<>());
                            ArrayList<String> sectionsList = choices.computeIfAbsent(supermarket.getSupermarket(), k -> new ArrayList<>());

                            if (!sectionsList.contains(supermarket.getSection()))
                            {
                                sectionsList.add(supermarket.getSection());
                            }

                            supermarketsAdapter.updateData(choices);
                            supermarketsAdapter.notifyDataSetChanged();
                            updateSupermarketButtonState();
                        },
                        false,
                        cities,
                        choices
                );
            }
            catch (IOException e)
            {
                Log.e("SignUpActivity", "Failed opening supermarket dialog", e);
            }
        });

        btnRemoveSupermarket.setOnClickListener(new View.OnClickListener()
        {
            @SuppressLint("NotifyDataSetChanged")
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
                    updateSupermarketButtonState();
                }
            }
        });
    }

    private void updateSupermarketButtonState()
    {
        boolean hasCities = cities != null && !cities.isEmpty();
        boolean hasSupermarkets = choices != null && !choices.isEmpty();

        btnAddSupermarket.setEnabled(hasCities);
        btnAddSupermarket.setAlpha(hasCities ? 1f : 0.5f);

        btnRemoveCity.setEnabled(hasCities);
        btnRemoveCity.setAlpha(hasCities ? 1f : 0.5f);

        btnRemoveSupermarket.setEnabled(hasSupermarkets);
        btnRemoveSupermarket.setAlpha(hasSupermarkets ? 1f : 0.5f);
    }

    private void setLoading(boolean loading)
    {
        runOnUiThread(() ->
        {
            if (isFinishing() || isDestroyed())
            {
                return;
            }
            btnAddCity.setEnabled(!loading);
            btnSubmit.setEnabled(!loading);

            if (loading)
            {
                btnAddSupermarket.setEnabled(false);
                btnRemoveSupermarket.setEnabled(false);
                btnRemoveCity.setEnabled(false);
            }
            else
            {
                updateSupermarketButtonState();
            }

            recyclerSupermarkets.setEnabled(!loading);
            recyclerCities.setEnabled(!loading);
            etUsername.setEnabled(!loading);

            if (loadingOverlay != null)
            {
                loadingOverlay.setAlpha(loading ? 1f : 0f);
                loadingOverlay.setVisibility(View.VISIBLE);

                loadingOverlay.animate()
                        .alpha(loading ? 1f : 0f)
                        .setDuration(180)
                        .withEndAction(() ->
                        {
                            if (!loading)
                            {
                                loadingOverlay.setVisibility(View.GONE);
                            }
                        })
                        .start();
            }
        });
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private void submit()
    {
        if (isFinishing() || isDestroyed())
        {
            return;
        }

        user = authHandler.getUser();

        if (user == null)
        {
            setLoading(false);
            return;
        }

        user.setName(username);

        setLoading(true);

        refUsers.child(user.getId()).setValue(user)
                .addOnCompleteListener(taskDB ->
                {
                    if (!taskDB.isSuccessful())
                    {
                        runOnUiThread(() ->
                        {
                            if (isFinishing() || isDestroyed())
                            {
                                return;
                            }

                            setLoading(false);
                        });

                        return;
                    }

                    Thread submitThread = new Thread(() ->
                    {
                        try
                        {
                            apiHandler.setCities(cities != null ? cities : new ArrayList<>());
                        }
                        catch (IOException | JSONException e)
                        {
                            Log.e("SignUpActivity", "Failed saving cities", e);
                        }

                        try
                        {
                            apiHandler.setBranches(choices != null ? choices : new HashMap<>());
                            apiHandler.updateSupermarkets();
                            apiHandler.reset();
                        }
                        catch (IOException | JSONException e)
                        {
                            Log.e("SignUpActivity", "Failed updating supermarkets", e);
                        }

                        runOnUiThread(() ->
                        {
                            if (isFinishing() || isDestroyed())
                            {
                                return;
                            }

                            setLoading(false);
                            finish();
                        });
                    });

                    submitThread.setName("SignUpSubmit");
                    submitThread.start();
                });
    }
}