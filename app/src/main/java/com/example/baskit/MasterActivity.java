package com.example.baskit;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.baskit.API.APIHandler;

public abstract class MasterActivity extends AppCompatActivity
{
    private AlertDialog offlineDialog;
    private AlertDialog serverDownDialog;

    private Runnable pendingAction;
    private Runnable pendingServerAction;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable serverRetryRunnable;
    private boolean serverCheckRunning = false;

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

                stopServerPolling();
                dismissServerDownDialog();
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

                if (pendingServerAction != null && !serverCheckRunning)
                {
                    startServerPolling(pendingServerAction);
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

    protected void runWhenServerActive(Runnable action)
    {
        if (action == null) return;

        pendingServerAction = action;

        if (!canUseApp())
        {
            return;
        }

        startServerPolling(action);
    }

    private void startServerPolling(Runnable action)
    {
        pendingServerAction = action;

        if (serverCheckRunning) return;
        serverCheckRunning = true;

        serverRetryRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                if (!canUseApp())
                {
                    stopServerPolling();
                    return;
                }

                new Thread(() ->
                {
                    boolean up;

                    try
                    {
                        up = APIHandler.getInstance().isServerActive();
                    }
                    catch (Exception e)
                    {
                        up = false;
                    }

                    boolean finalUp = up;
                    mainHandler.post(() ->
                    {
                        if (finalUp)
                        {
                            stopServerPolling();
                            dismissServerDownDialog();

                            Runnable toRun = pendingServerAction;
                            pendingServerAction = null;

                            if (toRun != null)
                            {
                                toRun.run();
                            }
                        }
                        else
                        {
                            showServerDownDialog();
                            mainHandler.postDelayed(this, 2000);
                        }
                    });
                }).start();
            }
        };

        serverRetryRunnable.run();
    }

    private void stopServerPolling()
    {
        serverCheckRunning = false;

        if (serverRetryRunnable != null)
        {
            mainHandler.removeCallbacks(serverRetryRunnable);
        }

        serverRetryRunnable = null;
    }

    private void showServerDownDialog()
    {
        if (serverDownDialog != null && serverDownDialog.isShowing()) return;

        serverDownDialog = new AlertDialog.Builder(this)
                .setTitle("Server unavailable")
                .setMessage("The server isn’t active right now. Please come back later.")
                .setCancelable(false)
                .create();

        serverDownDialog.show();
    }

    private void dismissServerDownDialog()
    {
        if (serverDownDialog != null && serverDownDialog.isShowing())
        {
            serverDownDialog.dismiss();
        }

        serverDownDialog = null;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        stopServerPolling();
        dismissServerDownDialog();

        if (offlineDialog != null && offlineDialog.isShowing())
        {
            offlineDialog.dismiss();
        }

        offlineDialog = null;
    }
}