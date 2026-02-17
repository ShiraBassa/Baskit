package com.example.baskit.List;

import android.os.Bundle;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.Categories.ItemsAdapter;
import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.MasterActivity;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Map;

public class PlanListActivity extends MasterActivity
{
    ImageButton btnCancel;
    Button btnSave;
    List list;
    TextView tvListName, tvTotal;
    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    APIHandler apiHandler = APIHandler.getInstance();

    Map<String, Map<String, Map<String, Double>>> allItems;
    Map<String, String> itemsCodeNames;
    boolean initialized = true;
    private RecyclerView recyclerItems;
    private PlanListItemsAdapter itemsAdapter;
    String listId;
    LinearLayout categoriesListContainer;
    LayoutInflater categoriesListInflater;
    private boolean itemsLoaded = false;
    private boolean listListenerAttached = false;
    private boolean uiInitialized = false;
    private double oldTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_list);

        listId = getIntent().getStringExtra("listId");

        if (listId == null)
        {
            Toast.makeText(this, "Missing list id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        createInit();
        uiInitialized = true;

        runWhenServerActive(() ->
        {
            try
            {
                allItems = apiHandler.getItems();
                itemsCodeNames = apiHandler.getItemsCodeName();
                itemsLoaded = true;
            }
            catch (Exception e)
            {
                Log.e("ListActivity", "Failed to load items catalogs", e);
                itemsLoaded = false;
            }

            runOnUiThread(() ->
            {
                resumeInit();
            });
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Only attach if we never attached before.
        if (itemsLoaded && uiInitialized && !listListenerAttached)
        {
            resumeInit();
        }

        if (!initialized && tvTotal != null && list != null)
        {
            tvTotal.setText(Baskit.getTotalDisplayString(list.getTotal(), list.allPricesKnown(), true, true));
            tvTotal.setVisibility(View.VISIBLE);
        }
    }

    private void createInit()
    {
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        tvListName = findViewById(R.id.tv_list_name);
        tvTotal = findViewById(R.id.tv_total);

        categoriesListContainer = findViewById(R.id.categories_container);
        categoriesListInflater = LayoutInflater.from(this);

        setButtons();
    }

    private void resumeInit()
    {
        if (listId == null) return;

        runIfOnline(() ->
        {
            dbHandler.getList(listId, new FirebaseDBHandler.GetListCallback()
            {
                @Override
                public void onListFetched(List newList)
                {
                    PlanListActivity.this.list = new List(newList);

                    oldTotal = list.getTotal();
                    tvTotal.setText(Baskit.getTotalDisplayString(list.getTotal(), true, false, false));
                    tvTotal.setVisibility(View.VISIBLE);

                    if (newList == null)
                    {
                        Toast.makeText(PlanListActivity.this, "List not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    tvListName.setText(newList.getName());
                    tvListName.setVisibility(View.VISIBLE);

                    recyclerItems = findViewById(R.id.recycler_supermarket_items);
                    recyclerItems.setLayoutManager(new LinearLayoutManager(PlanListActivity.this));

                    runWhenServerActive(() ->
                    {
                        ArrayList<Supermarket> supermarkets;
                        Map<String, Map<String, Map<String, Double>>> itemsCatalog;

                        try
                        {
                            supermarkets = apiHandler.getSupermarkets();
                        }
                        catch (Exception e)
                        {
                            Log.e("PlanListActivity", "Failed to load supermarkets", e);
                            supermarkets = new ArrayList<>();
                        }

                        itemsCatalog = allItems;

                        ArrayList<Supermarket> finalSupermarkets = supermarkets;
                        Map<String, Map<String, Map<String, Double>>> finalItemsCatalog = itemsCatalog;

                        runOnUiThread(() ->
                        {
                            itemsAdapter = new PlanListItemsAdapter(
                                    list,
                                    PlanListActivity.this,
                                    PlanListActivity.this,
                                    new ItemsAdapter.UpperClassFunctions()
                                    {
                                        @Override
                                        public void updateItemCategory(Item item) {}

                                        @Override
                                        public void removeItemCategory(Item item) {}

                                        @Override
                                        public void updateCategory()
                                        {
                                            displayTotalDif();
                                        }

                                        @Override
                                        public void removeCategory() {}
                                    },
                                    finalSupermarkets,
                                    finalItemsCatalog
                            );

                            recyclerItems.setAdapter(itemsAdapter);
                        });
                    });

                }

                @Override
                public void onError(String error)
                {
                    initialized = false;
                }
            });
        });
    }

    private void displayTotalDif()
    {
        double priceDif = list.getTotal() - oldTotal;
        String priceStr = Baskit.getTotalDisplayString(list.getTotal(), true, false, false);
        String difStr = Baskit.getTotalDisplayString(priceDif, true, false, false);

        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(priceStr);

        if (priceDif != 0)
        {
            String formattedDiff;

            if (priceDif > 0)
            {
                formattedDiff = " (+" + difStr + ")";
            }
            else
            {
                formattedDiff = " (" + difStr + ")";
            }

            int start = builder.length();
            builder.append(formattedDiff);
            int end = builder.length();

            builder.setSpan(
                    new ForegroundColorSpan(
                            Baskit.getAppColor(PlanListActivity.this, com.google.android.material.R.attr.colorPrimaryVariant)
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        tvTotal.setText(builder);
    }

    private void finish(boolean save)
    {
        if (save && list != null)
        {
            dbHandler.updateList(list);
        }

        finish();
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setButtons()
    {
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> finish(true));
    }
}