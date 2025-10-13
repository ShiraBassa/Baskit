package com.example.baskit.Home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.baskit.R;

public class SettingsActivity extends AppCompatActivity
{
    LinearLayout supermarketsListContainer;
    LayoutInflater supermarketsListInflater;

    ImageButton btnHome;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        init();
    }

    private void init()
    {
        btnHome = findViewById(R.id.btn_home);

        supermarketsListContainer = findViewById(R.id.supermarkets_container);
        supermarketsListInflater = LayoutInflater.from(this);

        createFakeTable();
        setButton();
    }

    private void setButton()
    {
        btnHome.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                finish();
            }
        });
    }

    private void createFakeTable()
    {
        for (int i = 0; i < 5; i++)
        {
            addSupermarket();
        }
    }

    private void addSupermarket()
    {
        LinearLayout supermarketContainer = (LinearLayout) supermarketsListInflater.inflate(R.layout.supermarkets_list_single,
                supermarketsListContainer, false);
        supermarketsListContainer.addView(supermarketContainer);

        for (int i = 0; i < 3; i++)
        {
            addSection(supermarketContainer);
        }
    }

    private void addSection(LinearLayout supermarketContainer)
    {
        LinearLayout sectionsContainer = supermarketContainer.findViewById(R.id.sections_container);
        LinearLayout singleSectionContainer = (LinearLayout) supermarketsListInflater.inflate(R.layout.supermarket_list_section,
                sectionsContainer, false);
        sectionsContainer.addView(singleSectionContainer);
    }
}