package com.example.baskit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.baskit.API.APIHandler;

public abstract class MasterActivity extends AppCompatActivity
{
    private boolean tracking = false;
    private boolean serverCheckRunning = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private float downX, downY;
    private AlertDialog offlineDialog, serverDownDialog;
    private Runnable pendingAction, pendingServerAction, serverRetryRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Baskit.getOnlineLive().observe(this, online ->
        {
            boolean isOnline = online != null && online;

            if (!isOnline)
            {
                showOfflineDialog();

                stopServerPolling();
                dismissServerDownDialog();
            }
            else
            {
                dismissOfflineDialog();

                if (isFinishing() || isDestroyed()) return;

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

        if (disableSystemBack())
        {
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true)
            {
                @Override
                public void handleOnBackPressed() {}
            });
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        if (pendingServerAction != null && !serverCheckRunning && canUseApp())
        {
            startServerPolling(pendingServerAction);
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        stopServerPolling();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        stopServerPolling();
        dismissServerDownDialog();

        dismissOfflineDialog();
        bg.shutdownNow();
    }

    protected boolean canUseApp()
    {
        return Baskit.isOnline(getApplicationContext());
    }

    public void runIfOnline(Runnable action)
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

    private void showOfflineDialog()
    {
        if (isFinishing() || isDestroyed()) return;

        if (offlineDialog == null || !offlineDialog.isShowing())
        {
            offlineDialog = new AlertDialog.Builder(this)
                    .setTitle("אין חיבור לאינטרנט")
                    .setMessage("נא התחבר ל-WiFi או לרשת סלולרית")
                    .setCancelable(false)
                    .create();
            offlineDialog.show();
        }
    }

    private void dismissOfflineDialog()
    {
        if (offlineDialog != null && offlineDialog.isShowing())
        {
            offlineDialog.dismiss();
        }

        offlineDialog = null;
    }

    public void runWhenServerActive(Runnable action)
    {
        if (action == null) return;

        pendingServerAction = action;

        if (!canUseApp())
        {
            return;
        }

        startServerPolling(action);
    }

    private void showServerDownDialog()
    {
        if (isFinishing() || isDestroyed()) return;

        if (serverDownDialog != null && serverDownDialog.isShowing()) return;

        serverDownDialog = new AlertDialog.Builder(this)
                .setTitle("שרת לא זמין")
                .setMessage("השרת לא זמין כרגע. אנא נסה שוב מאוחר יותר")
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
                                bg.execute(() ->
                                {
                                    try
                                    {
                                        toRun.run();
                                    }
                                    catch (Exception e)
                                    {
                                        Log.e("API", e.getMessage());
                                    }
                                });
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

    private int edgePx()
    {
        return (int) (24 * getResources().getDisplayMetrics().density);
    }

    private int swipeThresholdPx()
    {
        return (int) (96 * getResources().getDisplayMetrics().density);
    }

    protected boolean enableSwipeBack()
    {
        return true;
    }

    protected boolean enableSwipeForward()
    {
        return true;
    }

    protected @Nullable Intent getForwardIntent()
    {
        return null; // override in activities that have a “forward”
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        int w = getWindow().getDecorView().getWidth();

        switch (ev.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();

                if (!enableSwipeBack() && !enableSwipeForward())
                {
                    tracking = false;
                }
                else
                {
                    tracking = (downX <= edgePx() && enableSwipeBack()) || (downX >= w - edgePx() && enableSwipeForward());
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (!tracking) break;

                float dx = ev.getX() - downX;
                float dy = ev.getY() - downY;

                if (Math.abs(dy) > Math.abs(dx))
                {
                    tracking = false;
                    break;
                }

                if (downX <= edgePx() && dx > swipeThresholdPx())
                {
                    if (!isTaskRoot())
                    {
                        tracking = false;
                        finish();

                        overridePendingTransition(R.anim.pop_in_left, R.anim.pop_out_right);
                        return true;
                    }
                    else
                    {
                        tracking = false;
                    }
                }

                if (downX >= w - edgePx() && dx < -swipeThresholdPx())
                {
                    Intent fwd = getForwardIntent();
                    tracking = false;

                    if (fwd != null)
                    {
                        startActivity(fwd);
                        overridePendingTransition(R.anim.push_in_right, R.anim.push_out_left);
                        return true;
                    }
                }

                break;

            case MotionEvent.ACTION_UP:

            case MotionEvent.ACTION_CANCEL:
                tracking = false;
                break;
        }

        return super.dispatchTouchEvent(ev);
    }


    protected boolean disableSystemBack()
    {
        return false;
    }
}