package com.example.baskit;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.text.BidiFormatter;
import androidx.core.text.TextDirectionHeuristicsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.baskit.MainComponents.Supermarket;
import com.google.android.material.snackbar.Snackbar;

public class Baskit extends Application
{
    private static Context context;
    public static final Supermarket unassigned_supermarket = new Supermarket("לא נבחר", "");
    public static final Supermarket other_supermarket = new Supermarket("אחר", "");

    private static final MutableLiveData<Boolean> onlineLive = new MutableLiveData<>(true);
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    public void onCreate()
    {
        super.onCreate();

        context = getApplicationContext();
        onlineLive.postValue(isOnlineNow(this));

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null)
        {
            networkCallback = new ConnectivityManager.NetworkCallback()
            {
                @Override
                public void onAvailable(Network network)
                {
                    onlineLive.postValue(isOnlineNow(Baskit.this));
                }

                @Override
                public void onLost(Network network)
                {
                    onlineLive.postValue(false);
                }

                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities caps)
                {
                    onlineLive.postValue(isOnlineNow(Baskit.this));
                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
                cm.registerDefaultNetworkCallback(networkCallback);
            }
        }

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

    public static LiveData<Boolean> getOnlineLive()
    {
        return onlineLive;
    }

    public static boolean isOnlineNow(Context context)
    {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        if (caps == null) return false;

        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
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

    private static String getTotalDisplayString(Double total, boolean allPricesKnown)
    {
        String str;

        if (total == 0)
        {
            str = "?";
        }
        else
        {
            if (total % 1.0 == 0)
            {
                str = String.format("%.0f", total);
            }
            else
            {
                str = String.format("%.2f", total);
            }

            if (!allPricesKnown)
            {
                str += "+";
            }
        }

        str += "₪";

        return str;
    }

    public static String getTotalDisplayString(Double total, boolean allPricesKnown, boolean withTitle)
    {
        String amount = getTotalDisplayString(total, allPricesKnown);
        String amountIsolated = "\u2066" + amount + "\u2069";

        if (withTitle)
        {
            String titleIsolated = "\u2067" + "סך הכל:" + "\u2069";
            return amountIsolated + " " + titleIsolated;
        }

        return amountIsolated;
    }
}