package com.example.baskit.home;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.online_components.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.online_components.FirebaseAuthHandler;
import com.example.baskit.login.LoginActivity;
import com.example.baskit.main_components.Supermarket;
import com.example.baskit.MasterActivity;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"CallToPrintStackTrace", "deprecation"})
public class SettingsActivity extends MasterActivity
{
    Map<String, ArrayList<String>> choices;
    ArrayList<String> cities, all_cities;

    FirebaseAuthHandler authHandler;
    final APIHandler apiHandler = APIHandler.getInstance();

    SupermarketsListAdapter supermarketsAdapter;
    CitiesListAdapter citiesAdapter;

    Button btnChangeUsername;
    EditText etUsername;
    ImageButton btnHome, btnLogOut;
    Button btnAddSupermarket, btnRemoveSupermarket, btnAddCity, btnRemoveCity;
    RecyclerView recyclerSupermarkets, recyclerCities;
    View loadingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        loadingOverlay = new FrameLayout(this);
        loadingOverlay.setBackgroundColor(0x88000000); // semi-transparent black
        loadingOverlay.setClickable(true);
        loadingOverlay.setFocusable(true);
        loadingOverlay.setVisibility(View.GONE);

        ProgressBar progressBar = new ProgressBar(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        ((FrameLayout) loadingOverlay).addView(progressBar, params);

        addContentView(
                loadingOverlay,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );

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
        btnChangeUsername = findViewById(R.id.btn_change_username);
        etUsername = findViewById(R.id.et_username);

        etUsername.setText(authHandler.getUser().getName());

        runWhenServerActive(() ->
        {
            try
            {
                choices = Supermarket.getStringsFromSupermarkets(apiHandler.getUpdatedSupermarkets());
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
        btnHome.setOnClickListener(view -> finish());

        btnLogOut.setOnClickListener(view -> {
            authHandler.logOut();

            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.putExtra("fromLogout", true);

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        btnChangeUsername.setOnClickListener(v -> {
            String username = etUsername.getText().toString();

            if (!Baskit.isValidUserName(username, true))
            {
                return;
            }

            runWhenServerActive(() -> authHandler.changeUserName(username));
            Toast.makeText(SettingsActivity.this, Baskit.getAppStr(R.string.msg_username_changed) + username, Toast.LENGTH_SHORT).show();
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
                        supermarket ->
                        {
                            setLoading(true);
                            runWhenServerActive(() -> addSupermarketSection(supermarket, () ->
                                    new Thread(() ->
                                    {
                                        try
                                        {
                                            apiHandler.reset();

                                            choices = Supermarket.getStringsFromSupermarkets(apiHandler.getUpdatedSupermarkets());
                                            cities = apiHandler.getCities();

                                            SettingsActivity.this.runOnUiThread(() ->
                                            {
                                                supermarketsAdapter.updateData(choices);
                                                supermarketsAdapter.notifyDataSetChanged();

                                                citiesAdapter.updateData(cities);
                                                citiesAdapter.notifyDataSetChanged();

                                                setLoading(false);
                                            });
                                        }
                                        catch (Exception e)
                                        {
                                            Log.e("SettingsActivity", "Full refresh after add failed", e);
                                        }
                                    }).start()));
                        },
                        true,
                        cities,
                        choices
                );
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
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

                if (!Baskit.isOnline(SettingsActivity.this))
                {
                    return;
                }

                setLoading(true);

                new Thread(() ->
                        removeSupermarketSection(
                                new Supermarket(supermarketName, sectionName),
                                () ->
                                        new Thread(() ->
                                        {
                                            try
                                            {
                                                apiHandler.reset();

                                                choices = Supermarket.getStringsFromSupermarkets(apiHandler.getUpdatedSupermarkets());
                                                cities = apiHandler.getCities();

                                                runOnUiThread(() ->
                                                {
                                                    supermarketsAdapter.updateData(choices);
                                                    supermarketsAdapter.notifyDataSetChanged();

                                                    citiesAdapter.updateData(cities);
                                                    citiesAdapter.notifyDataSetChanged();

                                                    setLoading(false);
                                                });
                                            }
                                            catch (Exception e)
                                            {
                                                Log.e("SettingsActivity", "Full refresh after remove failed", e);
                                                runOnUiThread(() -> setLoading(false));
                                            }
                                        }).start())).start();
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
                        city_choices ->
                        {
                            setLoading(true);
                            runWhenServerActive(() ->
                            {
                                try
                                {
                                    apiHandler.setCities(city_choices);

                                    SettingsActivity.this.runOnUiThread(() ->
                                    {
                                        citiesAdapter.notifyDataSetChanged();
                                        setLoading(false);
                                    });
                                }
                                catch (IOException | JSONException e)
                                {
                                    Log.e("AddCityAlertDialog", "Failed to set cities", e);
                                    SettingsActivity.this.runOnUiThread(() -> setLoading(false));
                                    SettingsActivity.this.runOnUiThread(() ->
                                            Toast.makeText(SettingsActivity.this, Baskit.getAppStr(R.string.msg_general_error), Toast.LENGTH_SHORT).show());
                                }
                            });
                        }
                ).show();
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

                setLoading(true);

                removeCity(city, () ->
                        runOnUiThread(() ->
                        {
                            if (cities != null)
                            {
                                cities.remove(city);
                                citiesAdapter.updateData(cities);
                                citiesAdapter.notifyDataSetChanged();
                                setLoading(false);
                            }
                        }));
            }
        });
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void removeCity(String city, Runnable onComplete)
    {
        new Thread(() ->
        {
            try
            {
                ArrayList<String> cities = apiHandler.getCities();

                if (!cities.contains(city))
                {
                    return;
                }

                cities.remove(city);
                apiHandler.setCities(cities);
                apiHandler.reset();

                if (onComplete != null)
                {
                    new Handler(Looper.getMainLooper()).post(onComplete);
                }
            }
            catch (IOException | JSONException e)
            {
                e.printStackTrace();
            }
        }).start();
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void addSupermarketSection(Supermarket supermarket, Runnable onComplete)
    {
        try
        {
            Map<String, ArrayList<String>> branches = Supermarket.getStringsFromSupermarkets(apiHandler.getUpdatedSupermarkets());
            String supermarketName = supermarket.getSupermarket();
            String sectionName = supermarket.getSection();

            if (!branches.containsKey(supermarketName) || branches.get(supermarketName) == null) {
                branches.put(supermarketName, new ArrayList<>());
            }

            Objects.requireNonNull(branches.get(supermarketName)).add(sectionName);
            apiHandler.setBranches(branches);
            apiHandler.updateSupermarkets();
            apiHandler.reset();

            if (onComplete != null)
            {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        }
        catch (IOException | JSONException e)
        {
            e.printStackTrace();
        }
    }

    public void removeSupermarketSection(Supermarket supermarket, Runnable onComplete)
    {
        try
        {
            Map<String, ArrayList<String>> branches = Supermarket.getStringsFromSupermarkets(apiHandler.getUpdatedSupermarkets());
            String supermarketName = supermarket.getSupermarket();
            Objects.requireNonNull(branches.get(supermarketName)).remove(supermarket.getSection());

            if (Objects.requireNonNull(branches.get(supermarketName)).isEmpty())
            {
                branches.remove(supermarketName);
            }

            apiHandler.setBranches(branches);
            apiHandler.updateSupermarkets();
            apiHandler.reset();

            if (onComplete != null)
            {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        }
        catch (IOException | JSONException e)
        {
            Log.e("Remove supermarket", Objects.requireNonNull(e.getMessage()));
        }
    }

    private void setLoading(boolean loading)
    {
        runOnUiThread(() ->
        {
            btnAddSupermarket.setEnabled(!loading);
            btnRemoveSupermarket.setEnabled(!loading);
            btnAddCity.setEnabled(!loading);
            btnRemoveCity.setEnabled(!loading);

            recyclerSupermarkets.setEnabled(!loading);
            recyclerCities.setEnabled(!loading);

            etUsername.setEnabled(!loading);
            btnChangeUsername.setEnabled(!loading);

            if (loadingOverlay != null)
            {
                loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
        });
    }
}