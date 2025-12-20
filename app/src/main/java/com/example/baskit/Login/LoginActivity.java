package com.example.baskit.Login;

import android.app.Activity;
import android.content.Intent;
import android.content.LocusId;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.baskit.Firebase.FBRefs;
import com.example.baskit.Firebase.FirebaseAuthHandler;
import com.example.baskit.Home.HomeActivity;
import com.example.baskit.MainComponents.User;
import com.example.baskit.MasterActivity;
import com.example.baskit.R;


public class LoginActivity extends MasterActivity implements FirebaseAuthHandler.AuthCallback
{
    Button btnSubmit;
    EditText etEmail, etPassword;
    FirebaseAuthHandler authHandler;
    private boolean homeStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        runIfOnline(() ->
        {
            runWhenServerActive(() ->
            {
                authHandler = FirebaseAuthHandler.getInstance();

                if (getIntent().getBooleanExtra("fromLogout", false))
                {
                    startLogin();
                }
                else
                {
                    authHandler.checkCurrUser(new FirebaseAuthHandler.AuthCallback()
                    {
                        @Override
                        public void onAuthSuccess()
                        {
                            finishLogin();
                        }

                        @Override
                        public void onAuthError(String msg, ErrorType type)
                        {
                            startLogin();
                        }
                    });
                }
            });
        });
    }

    private void init()
    {
        btnSubmit = findViewById(R.id.btn_submit);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);

        set_btn();
    }

    private void set_btn()
    {
        btnSubmit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                boolean focused = false;

                btnSubmit.setEnabled(false);
                btnSubmit.setError(null);
                etEmail.setError(null);
                etPassword.setError(null);

                String email = etEmail.getText().toString();
                String password = etPassword.getText().toString();

                if (email.isEmpty())
                {
                    etEmail.setError("Please enter your email");
                    btnSubmit.setEnabled(true);
                    etEmail.requestFocus();
                    focused = true;
                }

                if (password.isEmpty())
                {
                    etPassword.setError("Please enter your password");
                    btnSubmit.setEnabled(true);

                    if (!focused)
                    {
                        etPassword.requestFocus();
                    }
                }

                if (!email.isEmpty() && !password.isEmpty())
                {
                    authHandler.signInOrSignUp(email, password, LoginActivity.this);
                }
            }
        });

        etEmail.setOnEditorActionListener((v, actionId, event) -> {
            etPassword.requestFocus();
            return true;
        });

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            btnSubmit.performClick();
            return true;
        });
    }

    @Override
    public void onAuthSuccess()
    {
        finishLogin();
    }

    @Override
    public void onAuthError(String msg, ErrorType type)
    {
        switch (type)
        {
            case EMAIL:
                etEmail.setError(msg);
                etEmail.requestFocus();
                break;

            case PASSWORD:
                etPassword.setError(msg);
                etPassword.requestFocus();
                break;

            case GENERAL:
                btnSubmit.setError(msg);
                btnSubmit.requestFocus();
                break;
        }

        btnSubmit.setEnabled(true);
    }

    private void startLogin()
    {
        setContentView(R.layout.activity_login);
        init();
    }

    private void finishLogin()
    {
        if (!homeStarted)
        {
            homeStarted = true;
            boolean fromLink = getIntent().getBooleanExtra("fromLink", false);

            if (fromLink)
            {
                Intent intent = new Intent();
                setResult(Activity.RESULT_OK, intent);
            }
            else
            {
                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            }

            finish();
        }
    }
}