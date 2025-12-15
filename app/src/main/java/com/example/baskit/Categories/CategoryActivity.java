package com.example.baskit.Categories;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.AI.AIHandler;
import com.example.baskit.API.APIHandler;
import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.List.AddItemFragment;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class CategoryActivity extends AppCompatActivity
{
    List list;
    Map<String, Category> categories;
    Category category;

    TextView tvListName, tvCategoryName, tvTotal;
    ImageButton btnFinished, btnBack;
    Button btnCheapest;
    Button btnAddItem;
    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    AddItemFragment addItemFragment;
    AIHandler aiHandler = AIHandler.getInstance();
    APIHandler apiHandler = APIHandler.getInstance();

    Map<String, Map<String, Map<String, Double>>> allItems;
    Map<String, String> itemsCodeNames;
    boolean initialized = true;
    private RecyclerView recyclerItems;
    private CategoryItemsAdapter itemsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        new Thread(() ->
        {
            allItems = apiHandler.getItems();
            itemsCodeNames = apiHandler.getItemsCodeName();

            runOnUiThread(this::init);
        }).start();
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
        tvListName = findViewById(R.id.tv_list_name);
        tvCategoryName = findViewById(R.id.tv_category_name);
        btnAddItem = findViewById(R.id.btn_add_item);
        tvTotal = findViewById(R.id.tv_total);
        btnCheapest = findViewById(R.id.btn_arrange_cheapest);

        dbHandler.getList(getIntent().getStringExtra("listId"), new FirebaseDBHandler.GetListCallback()
        {
            @Override
            public void onListFetched(List newList) throws JSONException, IOException
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

                recyclerItems = findViewById(R.id.recycler_category_items);

                new Thread(() ->
                {
                    try
                    {
                        ArrayList<Supermarket> supermarkets = apiHandler.getSupermarkets();

                        CategoryActivity.this.runOnUiThread(() ->
                        {
                            itemsAdapter = new CategoryItemsAdapter(
                                    category,
                                    CategoryActivity.this,
                                    CategoryActivity.this,
                                    new ItemsAdapter.UpperClassFunctions()
                                    {
                                        @Override
                                        public void updateItemCategory(Item item)
                                        {
                                            if (category == null) return;
                                            dbHandler.updateItem(list, category, item);
                                        }

                                        @Override
                                        public void removeItemCategory(Item item)
                                        {
                                            if (category == null) return;
                                            dbHandler.removeItem(list, category, item);
                                        }

                                        @Override
                                        public void updateCategory()
                                        {
                                            if (category == null) return;
                                            dbHandler.updateItems(list, new ArrayList<>(category.getItems().values()));
                                        }

                                        @Override
                                        public void removeCategory()
                                        {
                                            dbHandler.removeCategory(list, category);
                                            finish();
                                        }
                                    },
                                    supermarkets,
                                    apiHandler.getItems()
                            );

                            recyclerItems.setLayoutManager(new LinearLayoutManager(CategoryActivity.this));
                            recyclerItems.setAdapter(itemsAdapter);

                        });

                    } catch (Exception ignored) {}
                }).start();

                dbHandler.listenToCategory(list, category, new FirebaseDBHandler.GetCategoryCallback()
                {
                    @Override
                    public void onCategoryFetched(Category newCategory)
                    {
                        category = newCategory;

                        if (category.getItems().isEmpty())
                        {
                            finish();
                            return;
                        }

                        showTotal();
                        tvTotal.setVisibility(View.VISIBLE);

                        if (initialized)
                        {
                            initialized = false;
                            return;
                        }

                        runOnUiThread(() -> {
                            itemsAdapter.updateItems(new ArrayList<>(category.getItems().values()));
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
                dbHandler.finishCategory(list, category);
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

        btnCheapest.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                itemsAdapter.arrangeByCheapest();
            }
        });
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
        double total = category.getTotal();
        int total_rounded = (int) total;

        if (total == total_rounded)
        {
            tvTotal.setText("סך הכל: " + Integer.toString(total_rounded) + "₪");
        }
        else
        {
            tvTotal.setText("סך הכל: " + Double.toString(total) + "₪");
        }
    }
}