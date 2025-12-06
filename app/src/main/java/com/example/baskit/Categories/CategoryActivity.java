package com.example.baskit.Categories;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.baskit.AI.AIHandler;
import com.example.baskit.API.APIHandler;
import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.List.AddItemFragment;
import com.example.baskit.List.EditListFragment;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Map;

public class CategoryActivity extends AppCompatActivity
{
    List list;
    Map<String, Category> categories;
    Category category;

    ItemsListHandler itemsListHandler;
    TextView tvListName, tvCategoryName, tvTotal;
    ImageButton btnFinished, btnBack, btnEditList;
    Button btnAddItem;
    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    AddItemFragment addItemFragment;
    EditListFragment editListFragment;
    AIHandler aiHandler = AIHandler.getInstance();
    APIHandler apiHandler = APIHandler.getInstance();

    Map<String, Map<String, Map<String, Double>>> allItems;
    Map<String, String> itemsCodeNames;
    boolean initialized = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        new Thread(() ->
        {
            allItems = apiHandler.getItems();
            itemsCodeNames = apiHandler.getItemsCodeName(new ArrayList<>(allItems.keySet()));

            runOnUiThread(this::init);
        }).start();
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

    @Override
    protected void onResume()
    {
        super.onResume();

        if (!initialized)
        {
            showTotal();
        }
    }

    private void init()
    {
        btnFinished = findViewById(R.id.btn_finished);
        btnBack = findViewById(R.id.btn_back);
        btnEditList = findViewById(R.id.btn_edit);
        tvListName = findViewById(R.id.tv_list_name);
        tvCategoryName = findViewById(R.id.tv_category_name);
        btnAddItem = findViewById(R.id.btn_add_item);
        tvTotal = findViewById(R.id.tv_total);

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

                addItemFragment = new AddItemFragment(CategoryActivity.this,
                        CategoryActivity.this,
                        new ArrayList<>(itemsCodeNames.values()),
                        CategoryActivity.this::addItem);

                editListFragment = new EditListFragment(CategoryActivity.this,
                        CategoryActivity.this, new ArrayList<>(category.getItems().values()), list,
                        list.getName() + ", " + category.getName() + "");

                itemsListHandler = new ItemsListHandler(CategoryActivity.this,
                        CategoryActivity.this,
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
                        category = newCategory;
                        showTotal();
                        tvTotal.setVisibility(View.VISIBLE);
                        initialized = false;
                    }

                    @Override
                    public void onError(String error)
                    {
                        initialized = false;
                    }
                });
            }

            @Override
            public void onError(String error)
            {
                initialized = false;
            }
        });
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
                addItemFragment.show(getSupportFragmentManager(), "AddItemFragment");
            }
        });

        btnEditList.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                editListFragment.show(getSupportFragmentManager(), "EditListFragment");
            }
        });
    }

    private void finishedCategory()
    {
        list.removeCategory(category.getName());
        finish();
    }

    public void addItem(Item item)
    {
        addItemFragment.startProgressBar();
        item.updateId(getKeyByValue(itemsCodeNames, item.getName()));

        aiHandler.getCategoryName(item, CategoryActivity.this, categoryName ->
        {
            if (categoryName.equals(category.getName()))
            {
                if (!list.hasCategory(categoryName))
                {
                    dbHandler.addCategory(list, new Category(categoryName));
                }

                dbHandler.addItem(list, categoryName, item, new FirebaseDBHandler.DBCallback()
                {
                    @Override
                    public void onComplete()
                    {
                        runOnUiThread(() ->
                        {
                            addItemFragment.endProgressBar();
                            addItemFragment.dismiss();
                        });
                    }

                    @Override
                    public void onFailure(Exception e)
                    {
                        runOnUiThread(() ->
                        {
                            addItemFragment.endProgressBar();
                            addItemFragment.dismiss();
                            Toast.makeText(CategoryActivity.this, "שגיאה בניסיון להוסיף את הפריט", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
            else
            {
                addItemFragment.endProgressBar();
                addItemFragment.dismiss();
                Toast.makeText(CategoryActivity.this, "לא בקטגוריה המתאימה (" + categoryName + ")", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getKeyByValue(Map<String, String> map, String value)
    {
        for (Map.Entry<String, String> entry : map.entrySet())
        {
            if (entry.getValue().equals(value))
            {
                return entry.getKey();
            }
        }
        return null;
    }

    private void showTotal()
    {
        tvTotal.setText("סך הכל: " + Double.toString(category.getTotal()) + " ש״ח");
    }
}