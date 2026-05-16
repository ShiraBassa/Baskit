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

        if (Baskit.isLayoutDirectionLeft())
        {
            // LTR: forward = from right to left
            overridePendingTransition(R.anim.push_in_right, R.anim.push_out_left);
        }
        else
        {
            // RTL: forward = from left to right
            overridePendingTransition(R.anim.push_in_left, R.anim.push_out_right);
        }
    }

    @Override
    public void startActivity(Intent intent, Bundle options)
    {
        super.startActivity(intent, options);

        if (Baskit.isLayoutDirectionLeft())
        {
            // LTR: forward = from right to left
            overridePendingTransition(R.anim.push_in_right, R.anim.push_out_left);
        }
        else
        {
            // RTL: forward = from left to right
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
        if (Baskit.isLayoutDirectionLeft())
        {
            // LTR: back = to right
            overridePendingTransition(R.anim.pop_in_left, R.anim.pop_out_right);
        }
        else
        {
            // RTL: back = to left (mirror)
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
                        .setTitle("אין חיבור לאינטרנט")
                        .setMessage("נא התחבר ל-WiFi או לרשת סלולרית")
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
                        .setTitle("שרת לא זמין")
                        .setMessage("השרת לא זמין כרגע. נסה שוב מאוחר יותר")
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
        private android.graphics.drawable.GradientDrawable edgeShadow;

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
                    content.setElevation(0f);
                    content.setTranslationZ(0f);

                    content.setAlpha(1f);
                    View root = (View) content.getParent();
                    android.util.TypedValue bgValue = new android.util.TypedValue();
                    activity.getTheme().resolveAttribute(android.R.attr.colorBackground, bgValue, true);
                    root.setBackgroundColor(bgValue.data);

                    // Shadow orientation: LTR = LEFT_RIGHT, RTL = RIGHT_LEFT
                    edgeShadow = new android.graphics.drawable.GradientDrawable(
                            Baskit.isLayoutDirectionLeft()
                                    ? android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                                    : android.graphics.drawable.GradientDrawable.Orientation.RIGHT_LEFT,
                            new int[]{0x33000000, 0x00000000}); // soft shadow gradient

                    // Set initial bounds for shadow at edge of content
                    int shadowWidth = (int)(16 * activity.getResources().getDisplayMetrics().density);
                    if (Baskit.isLayoutDirectionLeft())
                    {
                        edgeShadow.setBounds(0, 0, shadowWidth, content.getHeight());
                    }
                    else
                    {
                        edgeShadow.setBounds(content.getWidth() - shadowWidth, 0, content.getWidth(), content.getHeight());
                    }
                    content.getOverlay().add(edgeShadow);

                    downX = ev.getX();
                    downY = ev.getY();

                    if (!Baskit.isLayoutDirectionLeft())
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

                    if (Math.abs(dy) > Math.abs(dx))
                    {
                        // Do not kill the gesture entirely; just ignore this move
                        return false;
                    }

                    downY = ev.getY(); // keep Y updated to reduce false vertical cancels

                    boolean isLTR = Baskit.isLayoutDirectionLeft();

                    // Allow back-and-forth dragging, but block "pushing" past edge
                    if (isLTR && dx < 0) dx = 0;
                    if (!isLTR && dx > 0) dx = 0;

                    if (!dragging)
                    {
                        dragging = true;
                        content.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    }

                    float width = content.getWidth();

                    // Clamp movement so it doesn't go too far, with subtle resistance near edges
                    if (Baskit.isLayoutDirectionLeft())
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
                    content.setPivotX(Baskit.isLayoutDirectionLeft() ? 0 : content.getWidth()); // anchor movement visually from edge (more natural)

                    // Update shadow bounds to stick to moving edge of content
                    if (edgeShadow != null)
                    {
                        int shadowWidth2 = (int)((12 + 6 * progress) * activity.getResources().getDisplayMetrics().density);

                        if (Baskit.isLayoutDirectionLeft())
                        {
                            // LTR: shadow on LEFT edge of content
                            edgeShadow.setBounds(
                                    0,
                                    0,
                                    shadowWidth2,
                                    content.getHeight());
                        }
                        else
                        {
                            // RTL: shadow on RIGHT edge of content
                            edgeShadow.setBounds(
                                    content.getWidth() - shadowWidth2,
                                    0,
                                    content.getWidth(),
                                    content.getHeight());
                        }
                    }

                    // Enable real shadow rendering
                    float velocity = 0f;
                    if (velocityTracker != null) {
                        velocityTracker.computeCurrentVelocity(1000);
                        velocity = Math.abs(velocityTracker.getXVelocity());
                    }
                    float velocityFactor = Math.min(1f, velocity / 2000f);

                    float dynamicZ = 10f + (14f * progress) + (10f * velocityFactor);

                    content.setTranslationZ(dynamicZ);
                    content.setElevation(6f);

                    // Ensure shadow is not clipped
                    content.setClipToOutline(false);

                    // No scaling: keep content at normal size for clean motion

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
                    if (velocityTracker != null) {
                        velocityTracker.computeCurrentVelocity(1000);
                        vx = velocityTracker.getXVelocity();
                    }

                    float currentTx = content.getTranslationX();
                    float width2 = content.getWidth();

                    boolean isLTR2 = Baskit.isLayoutDirectionLeft();

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

                        // Smooth finish animation from current position
                        float remaining = isLTR2 ? (width2 - currentTx) : (-width2 - currentTx);
                        Math.max(80, Math.min(180, Math.abs(remaining) / width2 * 180));
                        float duration;
                        float target = isLTR2 ? width2 : -width2;

                        // Slight velocity boost for snappier finish
                        duration = Math.max(60, Math.min(160, Math.abs(remaining) / width2 * 160));

                        content.animate()
                                .translationX(target)
                                .setDuration((long) duration)
                                .withEndAction(() -> {
                                    // content.setTranslationX(0); // REMOVE visual jump before finish
                                    content.setLayerType(View.LAYER_TYPE_NONE, null);
                                    content.setElevation(0f);
                                    content.setTranslationZ(0f);
                                    content.setAlpha(1f);
                                    View root3 = (View) content.getParent();
                                    root3.setTranslationX(0);

                                    if (edgeShadow != null)
                                    {
                                        content.getOverlay().remove(edgeShadow);
                                        edgeShadow = null;
                                    }

                                    activity.finish();
                                })
                                .start();
                    }
                    else
                    {
                        // Snap-back animation with slightly faster, more responsive duration
                        float duration = Math.max(60, Math.min(160, Math.abs(currentTx) / width2 * 160));
                        content.animate()
                                .translationX(0)
                                .setDuration((long) duration)
                                .withEndAction(() -> {
                                    content.setLayerType(View.LAYER_TYPE_NONE, null);
                                    content.setElevation(0f);
                                    content.setTranslationZ(0f);
                                    content.setAlpha(1f);
                                    View root3 = (View) content.getParent();
                                    root3.setTranslationX(0);

                                    if (edgeShadow != null)
                                    {
                                        content.getOverlay().remove(edgeShadow);
                                        edgeShadow = null;
                                    }
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