package com.example.baskit.Login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.baskit.Home.HomeActivity;
import com.example.baskit.R;


public class LoginActivity extends AppCompatActivity
{
    Button btn_submit;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        init();
    }

    private void init()
    {
        btn_submit = findViewById(R.id.btn_submit);

        set_btn();
    }

    private void set_btn()
    {
        btn_submit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            }
        });
    }
}