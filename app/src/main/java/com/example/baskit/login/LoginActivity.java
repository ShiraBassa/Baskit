package com.example.baskit.login;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.baskit.Baskit;
import com.example.baskit.online_components.FirebaseAuthHandler;
import com.example.baskit.home.HomeActivity;
import com.example.baskit.main_components.User;
import com.example.baskit.MasterActivity;
import com.example.baskit.R;


public class LoginActivity extends MasterActivity implements FirebaseAuthHandler.AuthCallback
{
    boolean homeStarted = false;

    ActivityResultLauncher<Intent> signUpLauncher;

    FirebaseAuthHandler authHandler;

    Button btnSubmit;
    EditText etEmail, etPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        signUpLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result ->
                        finishLogin());

        authHandler = FirebaseAuthHandler.getInstance();

        if (getIntent().getBooleanExtra("fromLogout", false))
        {
            startLogin();
        }
        else
        {
            runWhenServerActive(() ->
                    authHandler.checkCurrUser(new FirebaseAuthHandler.AuthCallback()
                    {
                        @Override
                        public void onAuthSuccess()
                        {
                            User user = authHandler.getUser();

                            if (user.getName().isEmpty())
                            {
                                signUpLauncher.launch(new Intent(LoginActivity.this, SignUpActivity.class));
                            }
                            else
                            {
                                finishLogin();
                            }
                        }

                        @Override
                        public void onAuthError(String msg, FirebaseAuthHandler.ErrorType type)
                        {
                            if (type == FirebaseAuthHandler.ErrorType.SERVER)
                            {
                                runWhenServerActive(() ->
                                        authHandler.checkCurrUser(this));
                                return;
                            }
                            startLogin();
                        }
                    }));
        }
    }

    private void init()
    {
        btnSubmit = findViewById(R.id.btn_submit);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);

        setButtons();
    }

    private void setButtons()
    {
        btnSubmit.setOnClickListener(view -> {
            boolean focused = false;

            disableButtons();
            etEmail.setError(null);
            etPassword.setError(null);

            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            if (email.isEmpty())
            {
                enableButtons();
                etEmail.setError(Baskit.getAppStr(R.string.auth_enter_email));
                etEmail.requestFocus();
                focused = true;
            }

            if (password.isEmpty())
            {
                enableButtons();
                etPassword.setError(Baskit.getAppStr(R.string.auth_enter_password));

                if (!focused)
                {
                    etPassword.requestFocus();
                }
            }

            if (!email.isEmpty() && !password.isEmpty())
            {
                runWhenServerActive(() ->
                        authHandler.signInOrSignUp(email, password, LoginActivity.this));
            }
        });

        etEmail.setOnEditorActionListener((v, actionId, event) ->
        {
            etPassword.requestFocus();
            return true;
        });

        etPassword.setOnEditorActionListener((v, actionId, event) ->
        {
            btnSubmit.performClick();
            return true;
        });
    }

    private void disableButtons()
    {
        etEmail.setEnabled(false);
        etPassword.setEnabled(false);
        btnSubmit.setEnabled(false);
    }

    private void enableButtons()
    {
        etEmail.setEnabled(true);
        etPassword.setEnabled(true);
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

    @Override
    public void onAuthSuccess()
    {
        User user = authHandler.getUser();

        if (!Baskit.isValidUserName(user.getName(), true))
        {
            signUpLauncher.launch(new Intent(LoginActivity.this, SignUpActivity.class));
        }
        else
        {
            finishLogin();
        }
    }

    @Override
    public void onAuthError(String msg, FirebaseAuthHandler.ErrorType type)
    {
        if (type == FirebaseAuthHandler.ErrorType.SERVER)
        {
            runWhenServerActive(() ->
                    authHandler.checkCurrUser(LoginActivity.this));
            return;
        }
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
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                etEmail.requestFocus();
                break;
        }

        enableButtons();
    }
}