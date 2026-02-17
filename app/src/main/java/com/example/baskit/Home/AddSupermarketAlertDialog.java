package com.example.baskit.Home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.Firebase.FirebaseAuthHandler;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
    Map<String, ArrayList<String>> unchosen_branches;
    ArrayList<String> supermarkets;
    ArrayList<String> all_stores, unchosen_stores;
    ArrayList<String> sections;
    Map<String, ArrayList<String>> choices;
    SupermarketsListAdapter supermarketsAdapter;
    private final APIHandler apiHandler = APIHandler.getInstance();
    private OnSubmit onSubmit;
    private boolean runInBackground;
    private ArrayList<String> cities;

    public interface OnSubmit
    {
        void onSubmit(Supermarket supermarket);
    }

    public AddSupermarketAlertDialog(Activity activity,
                                     Context context,
                                     SupermarketsListAdapter supermarketsAdapter,
                                     OnSubmit onSubmit,
                                     boolean runInBackground)
            throws JSONException, IOException
    {
        this.activity = activity;
        this.context = context;
        this.supermarketsAdapter = supermarketsAdapter;
        this.onSubmit = onSubmit;
        this.runInBackground = runInBackground;

        Baskit.notActivityRunWhenServerActive(() ->
        {
            try
            {
                getAPIInfo();
                activity.runOnUiThread(this::init);
            }
            catch (JSONException | IOException e)
            {
                Log.e("AddSectionAlertDialog", "Failed to load store/branch data", e);
                activity.runOnUiThread(() ->
                        Toast.makeText(context, "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show());
            }
        }, activity);
    }

    public AddSupermarketAlertDialog(Activity activity,
                                     Context context,
                                     SupermarketsListAdapter supermarketsAdapter,
                                     OnSubmit onSubmit,
                                     boolean runInBackground,
                                     ArrayList<String> cities)
            throws JSONException, IOException
    {
        this(activity, context, supermarketsAdapter, onSubmit, runInBackground);
        this.cities = cities;
    }

        private void init()
    {
        authHandler = FirebaseAuthHandler.getInstance();

        supermarkets = new ArrayList<>(unchosen_stores);

        adLayout = (LinearLayout) activity.getLayoutInflater()
                .inflate(R.layout.alert_dialog_add_section, null);
        spinnerSupermarkets = adLayout.findViewById(R.id.spinner_supermarkets);
        spinnerSections = adLayout.findViewById(R.id.spinner_sections);
        btnAdd = adLayout.findViewById(R.id.btn_add);

        adb = new AlertDialog.Builder(context);
        adb.setView(adLayout);
        ad = adb.create();

        setSpinners();
        setButton();

        ad.show();
    }

    private void getAPIInfo() throws JSONException, IOException
    {
        all_stores = apiHandler.getStores();
        choices = apiHandler.getChoices();

        unchosen_branches = new HashMap<>();
        unchosen_stores = new ArrayList<>();

        Map<String, ArrayList<String>> allBranches;

        if (cities == null)
        {
            allBranches =
                    apiHandler.getAllBranchesBulk();
        }
        else
        {
            allBranches =
                    apiHandler.getAllBranchesBulk(cities);
        }

        for (String store : allBranches.keySet())
        {
            try
            {
                ArrayList<String> allBranchesForStore = allBranches.get(store);
                ArrayList<String> selectedBranches = choices.get(store);

                ArrayList<String> availableBranches = new ArrayList<>();

                if (allBranchesForStore != null)
                {
                    for (String branch : allBranchesForStore)
                    {
                        if (selectedBranches == null || !selectedBranches.contains(branch))
                        {
                            availableBranches.add(branch);
                        }
                    }
                }

                if (!availableBranches.isEmpty())
                {
                    unchosen_branches.put(store, availableBranches);
                    unchosen_stores.add(store);
                }
            }
            catch (Exception e)
            {
                Log.e("AddSectionAlertDialog", "Failed loading branches for " + store, e);
            }
        }
    }

    private void setSpinners()
    {
        ArrayAdapter<String> supermarketAdapter =
                new ArrayAdapter<>(context,
                        android.R.layout.simple_spinner_dropdown_item,
                        supermarkets);

        spinnerSupermarkets.setAdapter(supermarketAdapter);

        spinnerSupermarkets.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id)
            {
                String selectedSupermarket = supermarkets.get(position);

                sections = unchosen_branches.get(selectedSupermarket);

                if (sections == null || sections.isEmpty())
                {
                    spinnerSections.setAdapter(null);
                    spinnerSections.setEnabled(false);
                    return;
                }

                ArrayAdapter<String> sectionAdapter =
                        new ArrayAdapter<>(context,
                                android.R.layout.simple_spinner_dropdown_item,
                                sections);

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
        btnAdd.setOnClickListener(v ->
        {
            String supermarketName = (String) spinnerSupermarkets.getSelectedItem();
            String sectionName = (String) spinnerSections.getSelectedItem();

            if (supermarketName == null || sectionName == null)
            {
                Toast.makeText(context, "נא לבחור סופרמרקט ומחלקה", Toast.LENGTH_SHORT).show();
                return;
            }

            Supermarket supermarket = new Supermarket(supermarketName, sectionName);

            if (runInBackground)
            {
                ad.dismiss();

                new Thread(() ->
                {
                    try
                    {
                        onSubmit.onSubmit(supermarket);
                    }
                    catch (Exception e)
                    {
                        activity.runOnUiThread(() ->
                                Toast.makeText(context, "שגיאה בהוספה", Toast.LENGTH_SHORT).show());
                    }
                }).start();
            }
            else
            {
                try
                {
                    onSubmit.onSubmit(supermarket);
                    ad.dismiss();
                }
                catch (Exception e)
                {
                    Toast.makeText(context, "שגיאה בהוספה", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
