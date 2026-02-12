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
import com.example.baskit.Home.AddCityAlertDialog;
import com.example.baskit.Home.SettingsActivity;
import com.example.baskit.Home.SupermarketsListAdapter;
import com.example.baskit.List.CitiesListAdapter;
import com.example.baskit.Login.ErrorType;
import com.example.baskit.MainComponents.User;
import com.example.baskit.MasterActivity;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

public class SignUpActivity extends MasterActivity
{
    FirebaseAuthHandler authHandler;
    APIHandler apiHandler = APIHandler.getInstance();
    EditText etUsername;
    Button btnSubmit;
    String username;
    ArrayList<String> cities, all_cities;
    RecyclerView recyclerCities;
    Button btnAddCity, btnRemoveCity;
    CitiesListAdapter citiesAdapter;
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

        cities = new ArrayList<>();

        runWhenServerActive(() ->
        {
            try
            {
                all_cities = apiHandler.getAllCities();
                citiesAdapter = new CitiesListAdapter(this, cities);

                runOnUiThread(() ->
                {
                    LinearLayoutManager lmCities = new LinearLayoutManager(this);
                    lmCities.setAutoMeasureEnabled(true);

                    recyclerCities.setLayoutManager(lmCities);
                    recyclerCities.setHasFixedSize(false);
                    recyclerCities.setAdapter(citiesAdapter);

                    setButton();
                });
            }
            catch (IOException | JSONException e)
            {
                Log.e("SignUpActivity", "Failed to load cities data", e);
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
                    try
                    {
                        apiHandler.setCities(cities);
                    }
                    catch (Exception e)
                    {
                        Log.e("SignUpActivity", "Failed to set cities", e);
                    }

                    finish();
                });
    }
}