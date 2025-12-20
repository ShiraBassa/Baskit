package com.example.baskit;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public abstract class MasterActivity extends AppCompatActivity
{
    private AlertDialog offlineDialog;
    private Runnable pendingAction;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Baskit.getOnlineLive().observe(this, online ->
        {
            boolean isOnline = online != null && online;

            if (!isOnline)
            {
                if (offlineDialog == null || !offlineDialog.isShowing())
                {
                    offlineDialog = new AlertDialog.Builder(this)
                            .setTitle("No Internet Connection")
                            .setMessage("Please connect to Wi‑Fi or mobile data to use the app.")
                            .setCancelable(false)
                            .create();
                    offlineDialog.show();
                }
            }
            else
            {
                if (offlineDialog != null && offlineDialog.isShowing())
                {
                    offlineDialog.dismiss();
                    offlineDialog = null;
                }

                if (pendingAction != null)
                {
                    Runnable action = pendingAction;
                    pendingAction = null;
                    action.run();
                }
            }
        });
    }

    protected boolean canUseApp()
    {
        return Baskit.isOnlineNow(getApplicationContext());
    }

    protected void runIfOnline(Runnable action)
    {
        if (action == null) return;

        if (canUseApp())
        {
            action.run();
        }
        else
        {
            pendingAction = action;
        }
    }
}