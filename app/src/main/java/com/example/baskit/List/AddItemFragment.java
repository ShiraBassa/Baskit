package com.example.baskit.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class AddItemFragment extends DialogFragment
{
    private View fragmentView;
    private TextView tvQuantity;
    private ImageButton btnCancel, btnUp, btnDown;
    private Button btnAddItem;
    private LinearLayout infoLayout;
    private ProgressBar progressBar;
    private AutoCompleteTextView searchItem;

    private RecyclerView recyclerSupermarkets;
    private ItemViewPricesAdapter pricesAdapter;

    private Item selectedItem;
    private Activity activity;
    private Context context;

    private ArrayList<String> allItemNames;
    private AddItemInterface addItemInterface;

    private APIHandler apiHandler = APIHandler.getInstance();

    public interface AddItemInterface
    {
        void addItem(Item item);
    }

    public AddItemFragment(Activity activity, Context context,
                           ArrayList<String> allItemNames,
                           AddItemInterface addItemInterface)
    {

        this.activity = activity;
        this.context = context;
        this.allItemNames = allItemNames;
        this.addItemInterface = addItemInterface;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
        {
            view.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState)
    {
        fragmentView = inflater.inflate(R.layout.fragment_add_item, container, false);

        btnCancel = fragmentView.findViewById(R.id.btn_cancel);
        btnAddItem = fragmentView.findViewById(R.id.btn_add_item);
        searchItem = fragmentView.findViewById(R.id.searchItem);
        btnUp = fragmentView.findViewById(R.id.btn_up);
        btnDown = fragmentView.findViewById(R.id.btn_down);
        tvQuantity = fragmentView.findViewById(R.id.tv_quantity);
        infoLayout = fragmentView.findViewById(R.id.lout_info);
        progressBar = fragmentView.findViewById(R.id.progressBar);
        recyclerSupermarkets = fragmentView.findViewById(R.id.recycler_supermarket);

        setupAutocomplete();
        setupButtons();

        return fragmentView;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        // fullscreen
        if (getDialog() != null && getDialog().getWindow() != null)
        {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        selectedItem = null;
        searchItem.setText("");
        recyclerSupermarkets.setAdapter(null);
        recyclerSupermarkets.setLayoutManager(null);
        infoLayout.setVisibility(View.INVISIBLE);
        tvQuantity.setText("");
        searchItem.clearFocus();
    }

    private void setupAutocomplete()
    {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.add_item_dropdown_item, allItemNames) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
            {
                if (convertView == null)
                {
                    convertView = activity.getLayoutInflater()
                            .inflate(R.layout.add_item_dropdown_item, parent, false);
                }

                TextView tvName = convertView.findViewById(R.id.tvItemName);
                tvName.setText(getItem(position));

                return convertView;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
            {
                if (convertView == null)
                {
                    convertView = activity.getLayoutInflater()
                            .inflate(R.layout.add_item_dropdown_item, parent, false);
                }

                TextView tvName = convertView.findViewById(R.id.tvItemName);
                tvName.setText(getItem(position));

                return convertView;
            }
        };

        searchItem.setAdapter(adapter);
        searchItem.setThreshold(1);
        searchItem.setOnClickListener(v -> searchItem.showDropDown());

        searchItem.setOnItemClickListener((parent, view, position, id) ->
        {
            String clickedName = (String) parent.getItemAtPosition(position);
            selectedItem = new Item(clickedName);
            tvQuantity.setText("1");
            btnDown.setBackgroundColor(Baskit.getAppColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer));

            selectedItem.setQuantity(1);

            recyclerSupermarkets.setLayoutManager(new LinearLayoutManager(context));
            recyclerSupermarkets.setAdapter(null);

            loadSupermarketPrices();
            infoLayout.setVisibility(View.VISIBLE);
            hideKeyboard();
        });

        searchItem.setDropDownHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void handleItemTyped()
    {
        String typed = searchItem.getText().toString().trim();

        ArrayList<String> exactMatches = new ArrayList<>();
        ArrayList<String> startsWithMatches = new ArrayList<>();
        ArrayList<String> containsMatches = new ArrayList<>();

        for (String name : allItemNames)
        {
            String lowerName = name.toLowerCase();
            String lowerTyped = typed.toLowerCase();
            if (lowerName.equals(lowerTyped)) exactMatches.add(name);
            else if (lowerName.startsWith(lowerTyped)) startsWithMatches.add(name);
            else containsMatches.add(name);
        }

        ArrayList<String> orderedItems = new ArrayList<>();
        orderedItems.addAll(exactMatches);
        orderedItems.addAll(startsWithMatches);
        orderedItems.addAll(containsMatches);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.add_item_dropdown_item, R.id.tvItemName, orderedItems);
        searchItem.setAdapter(adapter);

        if (!orderedItems.isEmpty())
        {
            searchItem.showDropDown();
        }
    }

    private void hideKeyboard()
    {
        View view = getView();

        if (view != null)
        {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void setupButtons()
    {
        btnCancel.setOnClickListener(v -> dismiss());

        btnAddItem.setOnClickListener(v ->
        {
            if (selectedItem != null)
            {
                startProgressBar();
                addItemInterface.addItem(selectedItem);
                dismiss();
            }
            else
            {
                searchItem.setError("בחר פריט מהרשימה");
            }
        });

        btnUp.setOnClickListener(v ->
        {
            if (selectedItem != null)
            {
                tvQuantity.setText(Integer.toString(selectedItem.raiseQuantity()));
                btnDown.setBackgroundColor(Color.TRANSPARENT);
            }
        });

        btnDown.setOnClickListener(v ->
        {
            if (selectedItem != null)
            {
                if (selectedItem.getQuantity() <= 1) return;

                int quantity = selectedItem.lowerQuantity();

                if (quantity == 1) btnDown.setBackgroundColor(Baskit.getAppColor(context, com.google.android.material.R.attr.colorSecondaryContainer));

                tvQuantity.setText(Integer.toString(quantity));
            }
        });
    }

    private void loadSupermarketPrices()
    {
        new Thread(() ->
        {
            Map<String, Map<String, Double>> data = null;

            try
            {
                data = apiHandler.getItemPricesByName(selectedItem.getName());
            }
            catch (IOException | JSONException ignored) {}

            Map<String, Map<String, Double>> finalData = data;

            pricesAdapter = new ItemViewPricesAdapter(context, finalData, null, new ItemViewPricesAdapter.OnSupermarketClickListener()
            {
                @Override
                public void onSupermarketClick(Supermarket supermarket)
                {
                    selectedItem.setSupermarket(supermarket);
                    selectedItem.setPrice(finalData.get(supermarket.getSupermarket()).get(supermarket.getSection()));
                }
            });

            activity.runOnUiThread(() ->
            {
                new Handler(Looper.getMainLooper()).post(() ->
                {
                    recyclerSupermarkets.setLayoutManager(new LinearLayoutManager(context));
                    recyclerSupermarkets.setAdapter(pricesAdapter);
                });
            });
        }).start();
    }

    public void startProgressBar()
    {
        if (progressBar != null)
        {
            progressBar.setVisibility(View.VISIBLE);
            btnAddItem.setClickable(false);
            btnCancel.setClickable(false);
            btnUp.setClickable(false);
            btnDown.setClickable(false);
        }
    }

    public void endProgressBar()
    {
        if (progressBar != null)
        {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }
}