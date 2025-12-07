package com.example.baskit.Home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Firebase.FBRefs;
import com.example.baskit.Firebase.FirebaseAuthHandler;
import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.List.SupermarketsListAdapter;
import com.example.baskit.Login.LoginActivity;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity
{
    FirebaseAuthHandler authHandler;

    private RecyclerView recyclerSupermarkets;
    private SupermarketsListAdapter supermarketsAdapter;
    APIHandler apiHandler = APIHandler.getInstance();

    ImageButton btnHome, btnLogOut;
    Button btnAddSupermarket, btnRemoveSupermarket;
    Map<String, ArrayList<String>> choices;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        authHandler = FirebaseAuthHandler.getInstance();

        init();
    }

    private void init()
    {
        btnHome = findViewById(R.id.btn_home);
        btnLogOut = findViewById(R.id.btn_log_out);
        btnAddSupermarket = findViewById(R.id.btn_add_supermarket);
        btnRemoveSupermarket = findViewById(R.id.btn_remove_supermarket);
        recyclerSupermarkets = findViewById(R.id.recycler_supermarkets);

        new Thread(() -> {
            try
            {
                choices = apiHandler.getChoices();
                supermarketsAdapter = SupermarketsListAdapter.fromSupermarkets(choices, this);

                runOnUiThread(() ->
                {
                    recyclerSupermarkets.setLayoutManager(new LinearLayoutManager(this));
                    recyclerSupermarkets.setAdapter(supermarketsAdapter);
                });
            } catch (IOException | JSONException ignored) {}
        }).start();

        setButtons();
    }

    private void setButtons()
    {
        btnHome.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                finish();
            }
        });

        btnLogOut.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                authHandler.logOut();

                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                intent.putExtra("fromLogout", true);

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        btnAddSupermarket.setOnClickListener(v ->
        {
            if (supermarketsAdapter != null)
            {
                Supermarket supermarket = new Supermarket("שופרסל", "שלי מיתר");
                authHandler.addSupermarket(supermarket, () ->
                {
                    runOnUiThread(() ->
                    {
                        choices.putIfAbsent(supermarket.getSupermarket(), new ArrayList<>());
                        choices.get(supermarket.getSupermarket()).add(supermarket.getSection());

                        supermarketsAdapter.updateData(choices);
                        supermarketsAdapter.notifyDataSetChanged();

                    });
                });
            }
        });

        btnRemoveSupermarket.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

            }
        });
    }
}