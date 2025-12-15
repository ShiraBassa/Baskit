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
import com.example.baskit.List.SupermarketsListAdapter;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddSectionAlertDialog
{
    private final String addStoreLabel = "+ הוסף חנות";
    Activity activity;
    Context context;
    LinearLayout adLayout;
    AlertDialog.Builder adb;
    AlertDialog ad;
    Button btnAdd;
    FirebaseAuthHandler authHandler;
    Spinner spinnerSupermarkets, spinnerSections;
    Map<String, ArrayList<String>> all_branches, unchosen_branches;
    ArrayList<String> supermarkets;
    ArrayList<String> all_stores, unchosen_stores;
    ArrayList<String> sections;
    Map<String, ArrayList<String>> choices;
    SupermarketsListAdapter supermarketsAdapter;
    private final APIHandler apiHandler = APIHandler.getInstance();

    public interface OnStoreAddedListener {
        void onStoreAdded(String storeName, ArrayList<String> branches);
    }

    public AddSectionAlertDialog(Activity activity,
                                 Context context,
                                 SupermarketsListAdapter supermarketsAdapter)
            throws JSONException, IOException
    {
        this.activity = activity;
        this.context = context;
        this.supermarketsAdapter = supermarketsAdapter;

        new Thread(() ->
        {
            try
            {
                getAPIInfo();
            }
            catch (JSONException e)
            {
                throw new RuntimeException(e);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }

            activity.runOnUiThread(this::init);
        }).start();
    }

    private void init()
    {
        authHandler = FirebaseAuthHandler.getInstance();

        supermarkets = new ArrayList<>(all_branches.keySet());
        supermarkets.add(addStoreLabel);

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
        all_branches = apiHandler.getBranches();
        all_stores = apiHandler.getStores();
        choices = apiHandler.getChoices();

        unchosen_stores = new ArrayList<>();
        unchosen_stores.addAll(all_stores);
        unchosen_stores.removeAll(all_branches.keySet());

        unchosen_branches = new HashMap<>();

        for (String store : all_branches.keySet())
        {
            if (!choices.containsKey(store))
            {
                unchosen_branches.put(store, all_branches.get(store));
            }
            else
            {
                unchosen_branches.put(store, new ArrayList<>());

                for (String branch : all_branches.get(store))
                {
                    if (!choices.get(store).contains(branch))
                    {
                        unchosen_branches.get(store).add(branch);
                    }
                }
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

                if (selectedSupermarket.equals(addStoreLabel))
                {
                    try
                    {
                        new AddSupermarketAlertDialog(activity, context, choices, unchosen_stores, new OnStoreAddedListener()
                        {
                            @Override
                            public void onStoreAdded(String storeName, ArrayList<String> branches)
                            {
                                new Thread(() ->
                                {
                                    try
                                    {
                                        getAPIInfo();
                                    }
                                    catch (JSONException | IOException e)
                                    {
                                        e.printStackTrace();
                                    }

                                    activity.runOnUiThread(() ->
                                    {
                                        supermarkets.clear();
                                        supermarkets.addAll(all_branches.keySet());
                                        supermarkets.add(addStoreLabel);

                                        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinnerSupermarkets.getAdapter();
                                        adapter.notifyDataSetChanged();

                                        spinnerSupermarkets.setSelection(supermarkets.indexOf(storeName));
                                    });
                                }).start();
                            }
                        }).show();
                    }
                    catch (JSONException e)
                    {
                        throw new RuntimeException(e);
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }

                    return;
                }

                sections = unchosen_branches.get(selectedSupermarket);

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
        btnAdd.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String supermarketName = (String) spinnerSupermarkets.getSelectedItem();

                if (supermarketName == null || addStoreLabel.equals(supermarketName))
                {
                    Toast.makeText(context, "נא לבחור סופרמרקט ומחלקה", Toast.LENGTH_SHORT).show();
                    return;
                }

                String sectionName = (String) spinnerSections.getSelectedItem();

                if (sectionName == null)
                {
                    Toast.makeText(context, "נא לבחור סופרמרקט ומחלקה", Toast.LENGTH_SHORT).show();
                    return;
                }

                Supermarket supermarket = new Supermarket(supermarketName, sectionName);

                authHandler.addSupermarketSection(supermarket, () ->
                {
                    activity.runOnUiThread(() ->
                    {
                        choices.putIfAbsent(supermarket.getSupermarket(), new ArrayList<>());
                        ArrayList<String> sectionsList = choices.get(supermarket.getSupermarket());

                        if (sectionsList == null)
                        {
                            sectionsList = new ArrayList<>();
                            choices.put(supermarket.getSupermarket(), sectionsList);
                        }

                        sectionsList.add(supermarket.getSection());

                        supermarketsAdapter.updateData(choices);
                        supermarketsAdapter.notifyDataSetChanged();
                        ad.dismiss();
                    });
                });
            }
        });
    }
}
