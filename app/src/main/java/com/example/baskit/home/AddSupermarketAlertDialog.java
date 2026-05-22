package com.example.baskit.home;

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
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.baskit.Baskit;
import com.example.baskit.online_components.APIHandler;
import com.example.baskit.main_components.Supermarket;
import com.example.baskit.MasterActivity;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AddSupermarketAlertDialog
{
    Map<String, ArrayList<String>> choices;
    ArrayList<String> cities;

    ArrayList<String> unchosen_stores;
    Map<String, ArrayList<String>> unchosen_branches;
    ArrayList<String> supermarkets;
    ArrayList<String> sections;

    final boolean runInBackground;
    final APIHandler apiHandler = APIHandler.getInstance();

    AlertDialog.Builder adb;

    LinearLayout adLayout;
    AlertDialog ad;
    Button btnAdd;
    Spinner spinnerSupermarkets, spinnerSections;
    TextView tvEmpty;

    final Activity activity;
    final Context context;
    final Consumer<Supermarket> onSubmit;

    public AddSupermarketAlertDialog(MasterActivity activity,
                                     Context context,
                                     Consumer<Supermarket> onSubmit,
                                     boolean runInBackground)
    {
        this.activity = activity;
        this.context = context;
        this.onSubmit = onSubmit;
        this.runInBackground = runInBackground;

        activity.runWhenServerActive(() ->
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
                        Toast.makeText(context, Baskit.getAppStr(R.string.msg_general_error), Toast.LENGTH_SHORT).show());
            }
        });
    }

    public AddSupermarketAlertDialog(MasterActivity activity,
                                     Context context,
                                     Consumer<Supermarket> onSubmit,
                                     boolean runInBackground,
                                     ArrayList<String> cities,
                                     Map<String, ArrayList<String>> currentChoices)
            throws IOException
    {
        this(activity, context, onSubmit, runInBackground);
        this.cities = cities;
        this.choices = currentChoices;
    }

        @SuppressLint("InflateParams")
        private void init()
    {
        supermarkets = new ArrayList<>(unchosen_stores);

        adLayout = (LinearLayout) activity.getLayoutInflater()
                .inflate(R.layout.alert_dialog_add_section, null);
        spinnerSupermarkets = adLayout.findViewById(R.id.spinner_supermarkets);
        spinnerSections = adLayout.findViewById(R.id.spinner_sections);
        btnAdd = adLayout.findViewById(R.id.btn_add);
        tvEmpty = adLayout.findViewById(R.id.tv_empty);

        adb = new AlertDialog.Builder(context);
        adb.setView(adLayout);
        ad = adb.create();

        setSpinners();
        setButton();

        show();
    }

    private void getAPIInfo() throws JSONException, IOException
    {
        unchosen_branches = new HashMap<>();
        unchosen_stores = new ArrayList<>();

        Map<String, ArrayList<String>> allBranches;

        if (cities == null)
        {
            allBranches =
                    apiHandler.getAllBranches(null);
        }
        else
        {
            allBranches =
                    apiHandler.getAllBranches(cities);
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
        if (supermarkets == null || supermarkets.isEmpty())
        {
            spinnerSupermarkets.setVisibility(View.GONE);
            spinnerSections.setVisibility(View.GONE);
            btnAdd.setEnabled(false);

            if (tvEmpty != null)
            {
                tvEmpty.setVisibility(View.VISIBLE);
            }

            return;
        }

        spinnerSupermarkets.setVisibility(View.VISIBLE);
        spinnerSections.setVisibility(View.VISIBLE);

        if (tvEmpty != null)
        {
            tvEmpty.setVisibility(View.GONE);
        }

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
            btnAdd.setEnabled(false);

            try
            {
                String supermarketName = (String) spinnerSupermarkets.getSelectedItem();
                String sectionName = (String) spinnerSections.getSelectedItem();

                if (supermarketName == null || supermarketName.isBlank() ||
                        sectionName == null || sectionName.isBlank())
                {
                    Toast.makeText(context, Baskit.getAppStr(R.string.msg_select_supermarket_and_section), Toast.LENGTH_SHORT).show();
                    btnAdd.setEnabled(true);
                    return;
                }

                Supermarket supermarket = new Supermarket(supermarketName, sectionName);

                if (runInBackground)
                {
                    dismiss();

                    Thread submitThread = new Thread(() ->
                    {
                        try
                        {
                            if (onSubmit != null)
                            {
                                onSubmit.accept(supermarket);
                            }
                        }
                        catch (Exception e)
                        {
                            Log.e("AddSupermarketAlertDialog", "Failed submitting supermarket", e);

                            activity.runOnUiThread(() ->
                            {
                                if (activity.isFinishing() || activity.isDestroyed())
                                {
                                    return;
                                }

                                Toast.makeText(context, Baskit.getAppStr(R.string.msg_general_error), Toast.LENGTH_SHORT).show();
                            });
                        }
                    });

                    submitThread.setName("AddSupermarketSubmit");
                    submitThread.start();
                }
                else
                {
                    if (onSubmit != null)
                    {
                        onSubmit.accept(supermarket);
                    }

                    dismiss();
                }
            }
            catch (Exception e)
            {
                Log.e("AddSupermarketAlertDialog", "Failed adding supermarket", e);

                Toast.makeText(context, Baskit.getAppStr(R.string.msg_general_error), Toast.LENGTH_SHORT).show();

                btnAdd.setEnabled(true);
            }
        });
    }

    public void show()
    {
        if (ad == null || ad.isShowing())
        {
            return;
        }

        Context dialogContext = ad.getContext();

        if (dialogContext instanceof Activity)
        {
            Activity dialogActivity = (Activity) dialogContext;

            if (dialogActivity.isFinishing() || dialogActivity.isDestroyed())
            {
                return;
            }
        }

        ad.show();
    }

    public void dismiss()
    {
        if (ad != null && ad.isShowing())
        {
            ad.dismiss();
        }
    }
}
