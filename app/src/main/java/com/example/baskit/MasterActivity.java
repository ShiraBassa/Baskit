package com.example.baskit;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;

public abstract class MasterActivity extends AppCompatActivity
{
    private ConnectionDialogs dialogs;
    private EdgeSwipeHandler swipeHandler;

    private Runnable pendingAction;
    private final java.util.concurrent.ExecutorService bg = java.util.concurrent.Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        dialogs = new ConnectionDialogs(this);
        swipeHandler = new EdgeSwipeHandler(this);

        Baskit.onlineLive.observe(this, online ->
        {
            boolean isOnline = online != null && online;

            if (!isOnline)
            {
                dialogs.showOffline();
                dialogs.hideServerDown();
            }
            else
            {
                dialogs.hideOffline();

                if (isFinishing() || isDestroyed()) return;

                if (pendingAction != null)
                {
                    pendingAction.run();
                    pendingAction = null;
                }
            }
        });

        Baskit.serverAliveLive.observe(this, isUp ->
        {
            Boolean checking = Baskit.serverCheckingLive.getValue();

            if (checking != null && checking)
            {
                dialogs.hideServerDown();
                return;
            }

            if (isUp == null) return;

            if (!isUp)
            {
                dialogs.showServerDown();
            }
            else
            {
                dialogs.hideServerDown();

                if (pendingAction != null)
                {
                    pendingAction.run();
                    pendingAction = null;
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
    protected void onDestroy()
    {
        super.onDestroy();

        dialogs.hideOffline();
        dialogs.hideServerDown();
    }

    @Override
    public void startActivity(Intent intent)
    {
        super.startActivity(intent);

        if (Baskit.isLayoutLeft())
        {
            overridePendingTransition(R.anim.push_in_right, R.anim.push_out_left);
        }
        else
        {
            overridePendingTransition(R.anim.push_in_left, R.anim.push_out_right);
        }
    }

    @Override
    public void startActivity(Intent intent, Bundle options)
    {
        super.startActivity(intent, options);

        if (Baskit.isLayoutLeft())
        {
            overridePendingTransition(R.anim.push_in_right, R.anim.push_out_left);
        }
        else
        {
            overridePendingTransition(R.anim.push_in_left, R.anim.push_out_right);
        }
    }

    @Override
    public void finish()
    {
        if (swipeHandler != null && swipeHandler.swipeFinishing)
        {
            super.finish();
            overridePendingTransition(0, 0);
            return;
        }

        super.finish();
        if (Baskit.isLayoutLeft())
        {
            overridePendingTransition(R.anim.pop_in_left, R.anim.pop_out_right);
        }
        else
        {
            overridePendingTransition(R.anim.pop_in_right, R.anim.pop_out_left);
        }
    }

    public void runWhenServerActive(Runnable action)
    {
        if (action == null) return;

        Boolean isUp = Baskit.serverAliveLive.getValue();

        if (isUp != null && isUp)
        {
            bg.execute(action);
        }
        else
        {
            pendingAction = () -> bg.execute(action);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        if (enableSwipeBack() && swipeHandler.handle(ev)) return true;

        return super.dispatchTouchEvent(ev);
    }

    protected boolean enableSwipeBack()
    {
        return true;
    }

    protected boolean disableSystemBack()
    {
        return false;
    }


    public static class ConnectionDialogs
    {
        private final Context context;
        private AlertDialog offlineDialog;
        private AlertDialog serverDownDialog;

        public ConnectionDialogs(Context context)
        {
            this.context = context;
        }

        public void showOffline()
        {
            if (offlineDialog == null || !offlineDialog.isShowing())
            {
                offlineDialog = new AlertDialog.Builder(context)
                        .setTitle(Baskit.getAppStr(R.string.auth_no_connection))
                        .setMessage(Baskit.getAppStr(R.string.msg_connect_to_wifi))
                        .setCancelable(false)
                        .create();

                offlineDialog.show();
            }
        }

        public void hideOffline()
        {
            if (offlineDialog != null)
            {
                offlineDialog.dismiss();
                offlineDialog = null;
            }
        }

        public void showServerDown()
        {
            if (serverDownDialog == null || !serverDownDialog.isShowing())
            {
                serverDownDialog = new AlertDialog.Builder(context)
                        .setTitle(Baskit.getAppStr(R.string.auth_server_unavailable))
                        .setMessage(Baskit.getAppStr(R.string.msg_try_again))
                        .setCancelable(false)
                        .create();

                serverDownDialog.show();
            }
        }

        public void hideServerDown()
        {
            if (serverDownDialog != null)
            {
                serverDownDialog.dismiss();
                serverDownDialog = null;
            }
        }
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static class EdgeSwipeHandler
    {
        private final Activity activity;

        private float downX, downY;
        private boolean tracking = false;
        private boolean dragging = false;
        public boolean swipeFinishing = false;
        private android.view.VelocityTracker velocityTracker;

        public EdgeSwipeHandler(Activity activity)
        {
            this.activity = activity;
        }

        private int edgePx()
        {
            return (int) (24 * activity.getResources().getDisplayMetrics().density);
        }

        public boolean handle(MotionEvent ev)
        {
            View content = activity.findViewById(android.R.id.content);
            int w = content.getWidth();

            if (velocityTracker == null) {
                velocityTracker = android.view.VelocityTracker.obtain();
            }
            velocityTracker.addMovement(ev);

            switch (ev.getActionMasked())
            {
                case MotionEvent.ACTION_DOWN:
                    View parent = (View) content.getParent();
                    if (parent instanceof android.view.ViewGroup) {
                        android.view.ViewGroup vg = (android.view.ViewGroup) parent;
                        vg.setClipToPadding(false);
                        vg.setClipChildren(false);
                    }

                    content.animate().cancel();
                    content.setLayerType(View.LAYER_TYPE_NONE, null);
                    content.setTranslationX(0f);
                    content.setTranslationY(0f);

                    content.setAlpha(1f);
                    View root = (View) content.getParent();
                    if (root != null)
                    {
                        root.setAlpha(1f);
                    }

                    content.getOverlay().clear();

                    downX = ev.getX();
                    downY = ev.getY();

                    if (!Baskit.isLayoutLeft())
                    {
                        tracking = (downX >= w - edgePx());
                    }
                    else
                    {
                        tracking = (downX <= edgePx());
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (!tracking) break;

                    float dx = ev.getX() - downX;
                    float dy = ev.getY() - downY;

                    if (Math.abs(dy) > Math.abs(dx)) return false;

                    downY = ev.getY();

                    boolean isLTR = Baskit.isLayoutLeft();

                    if (isLTR && dx < 0) dx = 0;
                    if (!isLTR && dx > 0) dx = 0;

                    if (!dragging)
                    {
                        dragging = true;
                        content.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    }

                    float width = content.getWidth();

                    if (Baskit.isLayoutLeft())
                    {
                        float progressRaw = dx / width;
                        float resistance = (float) Math.pow(progressRaw, 0.85); // subtle resistance
                        dx = Math.max(0, Math.min(resistance * width, width));
                    }
                    else
                    {
                        float progressRaw = dx / -width;
                        float resistance = (float) Math.pow(progressRaw, 0.85);
                        dx = Math.min(0, Math.max(-resistance * width, -width));
                    }

                    float easedDx = (float) (dx * (0.92 + 0.08 * Math.pow(Math.abs(dx) / width, 0.7)));
                    content.setTranslationX(easedDx);
                    View root2 = (View) content.getParent();
                    float progress = Math.abs(easedDx) / width;
                    float eased = (float) Math.pow(progress, 0.8);
                    float parallax = dx * (0.08f + 0.04f * eased);
                    root2.setTranslationX(parallax);
                    content.setPivotX(Baskit.isLayoutLeft() ? 0 : content.getWidth()); // anchor movement visually from edge (more natural)

                    content.setClipToOutline(false);
                    content.setAlpha(1f);
                    root2.setAlpha(1f);

                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!dragging)
                    {
                        tracking = false;
                        recycleVelocity();
                        break;
                    }

                    content.animate().cancel();

                    float vx = 0f;
                    if (velocityTracker != null)
                    {
                        velocityTracker.computeCurrentVelocity(1000);
                        vx = velocityTracker.getXVelocity();
                    }

                    float currentTx = content.getTranslationX();
                    float width2 = content.getWidth();

                    boolean isLTR2 = Baskit.isLayoutLeft();

                    if (isLTR2) {
                        currentTx = Math.max(0, Math.min(currentTx, width2));
                    } else {
                        currentTx = Math.min(0, Math.max(currentTx, -width2));
                    }

                    boolean finishByDistance = Math.abs(currentTx) > width2 * 0.35f;
                    boolean finishByVelocity = isLTR2 ? vx > 600 : vx < -600;

                    boolean shouldFinish = finishByDistance || finishByVelocity;

                    if (shouldFinish)
                    {
                        swipeFinishing = true;

                        float remaining = isLTR2 ? (width2 - currentTx) : (-width2 - currentTx);
                        Math.max(80, Math.min(180, Math.abs(remaining) / width2 * 180));
                        float duration;
                        float target = isLTR2 ? width2 : -width2;

                        duration = Math.max(60, Math.min(160, Math.abs(remaining) / width2 * 160));

                        content.animate()
                                .translationX(target)
                                .setDuration((long) duration)
                                .withEndAction(() ->
                                {
                                    content.setLayerType(View.LAYER_TYPE_NONE, null);
                                    content.setTranslationX(0f);
                                    content.setTranslationY(0f);
                                    content.setAlpha(1f);
                                    View root3 = (View) content.getParent();
                                    root3.setAlpha(1f);
                                    root3.setTranslationX(0);
                                    root3.setTranslationY(0f);
                                    root3.setBackground(null);
                                    root3.setAlpha(1f);

                                    content.animate().cancel();
                                    root3.animate().cancel();
                                    activity.finish();
                                })
                                .start();
                    }
                    else
                    {
                        float duration = Math.max(60, Math.min(160, Math.abs(currentTx) / width2 * 160));
                        content.animate()
                                .translationX(0)
                                .setDuration((long) duration)
                                .withEndAction(() -> {
                                    content.setLayerType(View.LAYER_TYPE_NONE, null);
                                    content.setTranslationX(0f);
                                    content.setTranslationY(0f);
                                    content.setAlpha(1f);
                                    View root3 = (View) content.getParent();
                                    root3.setAlpha(1f);
                                    root3.setTranslationX(0);
                                    root3.setTranslationY(0f);
                                    root3.setBackground(null);
                                    root3.setAlpha(1f);
                                })
                                .start();
                    }

                    dragging = false;
                    tracking = false;
                    recycleVelocity();
                    break;
            }

            return false;
        }

        private void recycleVelocity()
        {
            if (velocityTracker != null)
            {
                velocityTracker.recycle();
                velocityTracker = null;
            }
        }
    }
}