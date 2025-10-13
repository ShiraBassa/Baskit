package com.example.baskit.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.baskit.Categories.CategoryActivity;
import com.example.baskit.DataRepository;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.List;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class ListActivity extends AppCompatActivity
{
    View bottomBar;
    TextView tvListName;
    ImageButton btnBack, btnFinished;

    DataRepository data;
    List list;
    String listId;
    ArrayList<Category> categories;
    java.util.List<String> categoryNames;

    LinearLayout categoriesListContainer;
    LayoutInflater categoriesListInflater;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        createInit();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        resumeInit();
    }

    private void createInit()
    {
        tvListName = findViewById(R.id.tv_list_name);
        btnBack = findViewById(R.id.btn_back);
        bottomBar = findViewById(R.id.bottom_bar);
        btnFinished = bottomBar.findViewById(R.id.btn_finished);

        data = DataRepository.getInstance();
        listId = getIntent().getStringExtra("listId");

        categoriesListContainer = findViewById(R.id.categories_container);
        categoriesListInflater = LayoutInflater.from(this);

        setButton();
    }

    private void resumeInit()
    {
        list = data.getList(listId);

        if (list == null)
        {
            list = new List();
            categoriesListContainer.removeAllViews();
            return;
        }

        categories = list.getCategories();

        if (categories != null)
        {
            if (!categories.isEmpty())
            {
                categoryNames = list.getCategories().stream()
                        .map(Category::getName)
                        .collect(Collectors.toList());
            }
        }
        else
        {
            categoryNames = new ArrayList<>();
        }

        tvListName.setText(list.getName());
        setCategoriesInflater();
    }

    private void setButton()
    {
        btnBack.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                finish();
            }
        });

        btnFinished.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Category category;

                for (int i=0; i<list.getCategories().size(); i++)
                {
                    category = list.getCategories().get(i);

                    if (category.isFinished())
                    {
                        i--;
                    }

                    category.finished(list);
                }

                data.refreshList(list);
                resumeInit();
            }
        });
    }

    private void setCategoriesInflater()
    {
        categoriesListContainer.removeAllViews();

        for (Category category : categories)
        {
            inflaterAddItem(category);
        }
    }

    private void inflaterAddItem(Category category)
    {
        View categoryView = categoriesListInflater.inflate(R.layout.category_list_item_default, categoriesListContainer, false);

        TextView tv_name = categoryView.findViewById(R.id.tv_name);
        TextView tv_count = categoryView.findViewById(R.id.tv_count);

        tv_name.setText(category.getName());
        tv_count.setText(Integer.toString(category.getItems().size()));

        tv_name.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                data.refreshList(list);

                Intent intent = new Intent(ListActivity.this, CategoryActivity.class);
                intent.putExtra("listId", list.getId());
                intent.putExtra("categoryName", category.getName());

                startActivity(intent);
            }
        });

        categoriesListContainer.addView(categoryView);
    }
}