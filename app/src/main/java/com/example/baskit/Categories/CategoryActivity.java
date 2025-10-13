package com.example.baskit.Categories;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.baskit.DataRepository;
import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.List;
import com.example.baskit.R;

import java.util.ArrayList;

public class CategoryActivity extends AppCompatActivity
{
    DataRepository data;
    List list;
    ArrayList<Category> categories;
    Category category;

    ItemsListHandler itemsListHandler;
    View bottomBar;
    TextView tvListName, tvCategoryName;
    ImageButton btnFinished, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        init();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if (list.hasCategory(category.getName()))
        {
            list.getCategory(category.getName()).setItems(itemsListHandler.getAllItems());
            data.refreshList(list);
        }
    }

    private void init()
    {
        bottomBar = findViewById(R.id.bottom_bar);
        btnFinished = bottomBar.findViewById(R.id.btn_finished);
        btnBack = findViewById(R.id.btn_back);
        tvListName = findViewById(R.id.tv_list_name);
        tvCategoryName = findViewById(R.id.tv_category_name);

        data = DataRepository.getInstance();
        list = data.getList(getIntent().getStringExtra("listId"));
        categories = list.getCategories();
        category = list.getCategory(getIntent().getStringExtra("categoryName"));

        tvListName.setText(list.getName());
        tvCategoryName.setText(category.getName());

        setButtons();

        itemsListHandler = new ItemsListHandler(this,
                findViewById(R.id.recycler_unchecked),
                findViewById(R.id.recycler_checked),
                category.getItems(),
                this::finishedCategory);
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
    }

    private void finishedCategory()
    {
        list.removeCategory(category.getName());
        data.refreshList(list);
        finish();
    }
}