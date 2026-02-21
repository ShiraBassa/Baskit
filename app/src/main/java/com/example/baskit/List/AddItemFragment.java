package com.example.baskit.List;
import android.view.WindowManager;
import android.util.Log;

import com.example.baskit.Categories.ItemViewPricesAdapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
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
import java.util.LinkedHashMap;
import java.util.Locale;

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
    private ArrayList<String> listItemNames;

    private String decodeSanitizedKey(String s)
    {
        if (s == null) return null;

        String out = s;
        out = out.replace("__dot__", ".");
        out = out.replace("__dollar__", "$");
        out = out.replace("__hash__", "#");
        out = out.replace("__lbracket__", "[");
        out = out.replace("__rbracket__", "]");
        out = out.replace("__slash__", "/");
        return out;
    }

    public interface AddItemInterface
    {
        void addItem(Item item) throws IOException;
    }

    public AddItemFragment(Activity activity, Context context,
                           ArrayList<String> allItemNames,
                           ArrayList<String> listItemNames,
                           AddItemInterface addItemInterface)
    {
        this.activity = activity;
        this.context = context;
        this.allItemNames = allItemNames;
        this.listItemNames = listItemNames;
        this.addItemInterface = addItemInterface;

        init();
    }

    private void init()
    {
        LinkedHashMap<String, String> unique = new LinkedHashMap<>();

        if (listItemNames != null)
        {
            ArrayList<String> decodedList = new ArrayList<>();

            for (String n : listItemNames)
            {
                String d = decodeSanitizedKey(n);
                if (!isBadItemName(d)) decodedList.add(d.trim());
            }

            listItemNames = decodedList;
        }

        if (allItemNames != null)
        {
            for (String name : allItemNames)
            {
                String decoded = decodeSanitizedKey(name);
                if (isBadItemName(decoded) || (listItemNames != null && listItemNames.contains(decoded))) continue;

                String trimmed = decoded.trim();
                String key = trimmed.toLowerCase(Locale.ROOT);

                if (key.isEmpty() || key.equals("null")) continue;

                if (!unique.containsKey(key))
                {
                    unique.put(key, trimmed);
                }
            }
        }

        this.allItemNames = new ArrayList<>(unique.values());
    }

    private boolean isBadItemName(String s)
    {
        if (s == null) return true;
        String t = s.trim();
        return t.isEmpty() || t.equalsIgnoreCase("null");
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
        recyclerSupermarkets.setLayoutManager(new LinearLayoutManager(context));

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

            getDialog().getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            );
        }

        selectedItem = null;
        searchItem.setText("");
        infoLayout.setVisibility(View.INVISIBLE);
        tvQuantity.setText("");
        searchItem.setFocusableInTouchMode(true);
        searchItem.requestFocus();

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
                String raw = getItem(position);
                tvName.setText(decodeSanitizedKey(raw));

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
                String raw = getItem(position);
                tvName.setText(decodeSanitizedKey(raw));

                return convertView;
            }
        };

        searchItem.setAdapter(adapter);
        searchItem.setThreshold(1);
        searchItem.setOnClickListener(v -> searchItem.showDropDown());

        searchItem.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        searchItem.setOnEditorActionListener((v, actionId, event) ->
        {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
            {
                searchItem.clearFocus();
                hideKeyboard();
                searchItem.post(() -> searchItem.showDropDown());
                return true;
            }
            return false;
        });

        searchItem.setDropDownAnchor(searchItem.getId());
        searchItem.setDropDownVerticalOffset(0);

        searchItem.post(() ->
        {
            if (fragmentView == null) return;

            int fragmentHeight = fragmentView.getHeight();
            int searchBottom = searchItem.getBottom();

            int maxHeightInsideFragment = fragmentHeight - searchBottom;

            if (maxHeightInsideFragment > 0)
            {
                searchItem.setDropDownHeight(maxHeightInsideFragment);
            }
        });

        searchItem.setOnItemClickListener((parent, view, position, id) ->
        {
            String clickedName = Baskit.decodeKey((String) parent.getItemAtPosition(position));

            if (isBadItemName(clickedName))
            {
                searchItem.setError("שם פריט לא תקין");
                selectedItem = null;
                return;
            }

            selectedItem = new Item(clickedName);
            tvQuantity.setText("1");
            btnDown.setBackgroundColor(Baskit.getAppColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer));

            selectedItem.setQuantity(1);
            recyclerSupermarkets.setAdapter(null);

            loadSupermarketPrices();
            infoLayout.setVisibility(View.VISIBLE);
            searchItem.clearFocus();
            hideKeyboard();
        });
    }

    private void handleItemTyped()
    {
        String typed = searchItem.getText().toString().trim();
        if (typed.isEmpty()) return;

        ArrayList<String> exactMatches = new ArrayList<>();
        ArrayList<String> startsWithMatches = new ArrayList<>();
        ArrayList<String> containsMatches = new ArrayList<>();

        for (String name : allItemNames)
        {
            String decodedName = decodeSanitizedKey(name);
            if (isBadItemName(decodedName)) continue;

            String lowerName = decodedName.toLowerCase();
            String lowerTyped = typed.toLowerCase();
            if (lowerName.equals(lowerTyped)) exactMatches.add(decodedName);
            else if (lowerName.startsWith(lowerTyped)) startsWithMatches.add(decodedName);
            else containsMatches.add(decodedName);
        }

        ArrayList<String> orderedItems = new ArrayList<>();
        orderedItems.addAll(exactMatches);
        orderedItems.addAll(startsWithMatches);
        orderedItems.addAll(containsMatches);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.add_item_dropdown_item, orderedItems)
        {
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
                String raw = getItem(position);
                tvName.setText(decodeSanitizedKey(raw));

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
                String raw = getItem(position);
                tvName.setText(decodeSanitizedKey(raw));

                return convertView;
            }
        };

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
                try
                {
                    addItemInterface.addItem(selectedItem);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
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
        if (selectedItem == null || isBadItemName(selectedItem.getName())) return;
        if (activity == null) return;

        final String itemName = selectedItem.getName();

        Baskit.notActivityRunWhenServerActive(() ->
        {
            Map<String, Map<String, Double>> data = null;

            try
            {
                data = apiHandler.getItemPricesByName(itemName);
            }
            catch (IOException | JSONException e)
            {
                Log.e("AddItemFragment", "Failed to load item prices", e);
            }

            Map<String, Map<String, Double>> finalData = data;

            if (activity == null) return;

            activity.runOnUiThread(() ->
            {
                // Fragment might be detached by the time we return
                if (!isAdded() || fragmentView == null) return;

                pricesAdapter = new ItemViewPricesAdapter(context, finalData, null,
                        new ItemViewPricesAdapter.OnSupermarketClickListener()
                        {
                            @Override
                            public void onSupermarketClick(Supermarket supermarket)
                            {
                                if (selectedItem == null) return;

                                if (supermarket == null || supermarket.getSupermarket() == null)
                                {
                                    selectedItem.setSupermarket(null);
                                    selectedItem.setPrice(0);
                                    return;
                                }

                                selectedItem.setSupermarket(supermarket);

                                if (finalData == null) {
                                    selectedItem.setPrice(0);
                                    return;
                                }

                                Map<String, Double> sectionPrices = finalData.get(supermarket.getSupermarket());

                                if (sectionPrices != null)
                                {
                                    Double price = sectionPrices.get(supermarket.getSection());
                                    selectedItem.setPrice(price != null ? price : 0);
                                }
                                else
                                {
                                    selectedItem.setPrice(0);
                                }
                            }
                        });
                recyclerSupermarkets.setAdapter(pricesAdapter);
            });
        }, activity);
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

    public void updateData(ArrayList<String> newListItemNames)
    {
        listItemNames = newListItemNames;
        init();
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        fragmentView = null;
        recyclerSupermarkets = null;
        pricesAdapter = null;
    }
}