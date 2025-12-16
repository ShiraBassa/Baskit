package com.example.baskit;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;
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

    public static int getAppColor(Context context, int id)
    {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();

        // 1. Try to resolve it as a Theme Attribute (e.g., R.attr.colorPrimary)
        boolean found = theme.resolveAttribute(id, typedValue, true);

        if (found)
        {
            // If the attribute directly points to a color
            if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT)
            {
                return typedValue.data;
            }
            // If the attribute points to a resource
            else if (typedValue.resourceId != 0)
            {
                return context.getResources().getColor(typedValue.resourceId, theme);
            }
        }

        // 2. If not found as an attribute, assume it is a direct Resource ID (e.g., R.color.quantity)
        try
        {
            return ContextCompat.getColor(context, id);
        }
        catch (Exception e)
        {
            // 3. If both fail, return Red for debug
            return 0xFFFF0000;
        }
    }

    public static int getAppColor(Context context, int attributeId, int alpha)
    {
        return ColorUtils.setAlphaComponent(getAppColor(context, attributeId), alpha);
    }

    public static String getTotalDisplayString(Double total)
    {
        if (total % 1.0 == 0)
        {
            return String.format("%.0f", total) + "₪";
        }
        else
        {
            return String.format("%.2f", total) + "₪";
        }
    }

    public static String getTotalDisplayString(Double total, boolean withTitle)
    {
        String str = getTotalDisplayString(total);

        if (withTitle)
        {
            return "סך הכל: " + str;
        }

        return str;
    }
}