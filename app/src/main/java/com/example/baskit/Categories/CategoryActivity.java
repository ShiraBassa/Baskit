package com.example.baskit.Categories;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CategoryActivity extends AppCompatActivity
{
    List list;
    Map<String, Category> categories;
    Category category;

    ItemsListHandler itemsListHandler;
    TextView tvListName, tvCategoryName, adTvQuantity;
    ImageButton btnFinished, btnBack, adBtnCancel, adBtnUp, adBtnDown;
    Button btnAddItem, adBtnAddItem;
    LinearLayout adLayout, adLoutQuantity;
    AlertDialog.Builder adb;
    AlertDialog adAddItem;
    AutoCompleteTextView adSearchItem;
    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    Item selectedItem;

    ArrayList<String> allItemNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        allItemNames.add("פריט 1");
        allItemNames.add("פריט 2");
        allItemNames.add("פריט 3");
        allItemNames.add("פריט 4");

        init();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if (list.hasCategory(category.getName()))
        {
            list.getCategory(category.getName()).setItems(itemsListHandler.getAllItems());
        }
    }

    private void init()
    {
        btnFinished = findViewById(R.id.btn_finished);
        btnBack = findViewById(R.id.btn_back);
        tvListName = findViewById(R.id.tv_list_name);
        tvCategoryName = findViewById(R.id.tv_category_name);
        btnAddItem = findViewById(R.id.btn_add_item);

        dbHandler.getList(getIntent().getStringExtra("listId"), new FirebaseDBHandler.GetListCallback()
        {
            @Override
            public void onListFetched(List newList)
            {
                CategoryActivity.this.list = newList;

                categories = CategoryActivity.this.list.getCategories();
                category = newList.getCategory(getIntent().getStringExtra("categoryName"));

                tvListName.setText(newList.getName());
                tvCategoryName.setText(category.getName());
                tvListName.setVisibility(View.VISIBLE);
                tvCategoryName.setVisibility(View.VISIBLE);

                setButtons();
                createListAlertDialog();

                itemsListHandler = new ItemsListHandler(CategoryActivity.this,
                        findViewById(R.id.recycler_unchecked),
                        findViewById(R.id.recycler_checked),
                        CategoryActivity.this::finishedCategory,
                        newList,
                        category);

                dbHandler.listenToCategory(list, category, new FirebaseDBHandler.GetCategoryCallback()
                {
                    @Override
                    public void onCategoryFetched(Category newCategory)
                    {
                        itemsListHandler.update(newCategory);
                    }

                    @Override
                    public void onError(String error) {}
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void createListAlertDialog()
    {
        adLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.alert_dialog_add_item, null);
        adBtnCancel = adLayout.findViewById(R.id.btn_cancel);
        adBtnAddItem = adLayout.findViewById(R.id.btn_add_item);
        adSearchItem = adLayout.findViewById(R.id.searchItem);
        adBtnUp = adLayout.findViewById(R.id.btn_up);
        adBtnDown = adLayout.findViewById(R.id.btn_down);
        adTvQuantity = adLayout.findViewById(R.id.tv_quantity);
        adLoutQuantity = adLayout.findViewById(R.id.lout_quantity);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.add_item_dropdown_item, allItemNames)
        {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
            {
                if (convertView == null)
                {
                    convertView = getLayoutInflater().inflate(R.layout.add_item_dropdown_item, parent, false);
                }

                TextView tvName = convertView.findViewById(R.id.tvItemName);
                String name = getItem(position);

                if (name != null)
                {
                    tvName.setText(name);
                }

                return convertView;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
            {
                if (convertView == null)
                {
                    convertView = getLayoutInflater().inflate(R.layout.add_item_dropdown_item, parent, false);
                }

                TextView tvName = convertView.findViewById(R.id.tvItemName);
                String name = getItem(position);

                if (name != null)
                {
                    tvName.setText(name);
                }

                return convertView;
            }
        };

        adSearchItem.setAdapter(adapter);
        adSearchItem.setThreshold(1); // start filtering after 1 character
        adSearchItem.setOnClickListener(v -> adSearchItem.showDropDown());

        adSearchItem.setOnFocusChangeListener((v, hasFocus) ->
        {
            if (!hasFocus)
            {
                String typed = adSearchItem.getText().toString().trim();
                boolean match = false;

                for (String name : allItemNames)
                {
                    if (typed.equals(name))
                    {
                        selectedItem = new Item(name.replace("פריט ", ""), name);
                        selectedItem.setQuantity(1);
                        match = true;
                        break;
                    }
                }

                if (!match)
                {
                    adSearchItem.setText("");
                    adLoutQuantity.setVisibility(View.INVISIBLE);
                    selectedItem = null;
                }
            }
        });

        adSearchItem.setOnItemClickListener((parent, view, position, id) ->
        {
            String name = allItemNames.get(position);
            selectedItem = new Item(name.replace("פריט ", ""), name);
            selectedItem.setQuantity(1);

            adLoutQuantity.setVisibility(View.VISIBLE);
            adTvQuantity.setText("1");
            adBtnDown.setBackgroundColor(Color.LTGRAY);
        });

        adb = new AlertDialog.Builder(this);
        adb.setView(adLayout);
        adAddItem = adb.create();

        adBtnCancel.setOnClickListener(v -> adAddItem.dismiss());

        adBtnAddItem.setOnClickListener(v ->
        {
            if (selectedItem != null)
            {
                itemsListHandler.addItem(selectedItem);
                adAddItem.dismiss();
            }
            else
            {
                adSearchItem.setError("בחר פריט מהרשימה");
            }
        });

        adBtnUp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (selectedItem != null)
                {
                    adTvQuantity.setText(Integer.toString(selectedItem.raiseQuantity()));
                    adBtnDown.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });

        adBtnDown.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (selectedItem != null)
                {
                    if (selectedItem.getQuantity() <= 1)
                    {
                        return;
                    }

                    int quantity = selectedItem.lowerQuantity();

                    if (quantity == 1)
                    {
                       adBtnDown.setBackgroundColor(Color.LTGRAY);
                    }

                    adTvQuantity.setText(Integer.toString(quantity));
                }
            }
        });
    }

    private void startAlertDialog()
    {

        adAddItem.show();
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setButtons()
    {
        btnFinished.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                itemsListHandler.finished();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                finish();
            }
        });

        btnAddItem.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                adSearchItem.setText("");
                adLoutQuantity.setVisibility(View.INVISIBLE);
                selectedItem = null;
                startAlertDialog();
            }
        });
    }

    private void finishedCategory()
    {
        list.removeCategory(category.getName());
        finish();
    }
}