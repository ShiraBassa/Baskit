package com.example.baskit;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import androidx.core.graphics.ColorUtils;

import com.example.baskit.MainComponents.Supermarket;

public class Baskit extends Application
{
    private static Context context;
    public static final Supermarket unassigned_supermarket = new Supermarket("לא נבחר", "");
    public static final Supermarket other_supermarket = new Supermarket("אחר", "");

    @Override
    public void onCreate()
    {
        super.onCreate();

        context = getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
        {
            registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks()
            {
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState)
                {
                    activity.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                }

                @Override public void onActivityStarted(Activity activity) {}
                @Override public void onActivityResumed(Activity activity) {}
                @Override public void onActivityPaused(Activity activity) {}
                @Override public void onActivityStopped(Activity activity) {}
                @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
                @Override public void onActivityDestroyed(Activity activity) {}
            });
        }
    }

    public static Context getContext()
    {
        return context;
    }

    public static int getThemeColor(Context context, int attributeId) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attributeId, typedValue, true);

        // If the attribute directly points to a color, it's in typedValue.data
        if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return typedValue.data;
        }
        // If the attribute points to a color resource, get it from the resources
        else {
            return context.getResources().getColor(typedValue.resourceId, theme);
        }
    }

    public static int getThemeColor(Context context, int attributeId, int alpha)
    {
        int baseColor = getThemeColor(context, com.google.android.material.R.attr.colorError);
        int colorWithAlpha = ColorUtils.setAlphaComponent(baseColor, alpha);

        return colorWithAlpha;
    }
}