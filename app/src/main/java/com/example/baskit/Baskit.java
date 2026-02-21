package com.example.baskit;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.baskit.MainComponents.Supermarket;

public class Baskit extends Application
{
    private static Context context;
    public static final Supermarket UNASSIGNED_SUPERMARKET = new Supermarket("לא נבחר", "");
    private static final MutableLiveData<Boolean> onlineLive = new MutableLiveData<>(true);
    private ConnectivityManager.NetworkCallback networkCallback;
    public static final String PRIVATE_NETWORK_URL = "192.168.1.248";
    public static final String EMULATOR_URL = "10.0.2.2";

    public static final String SERVER_URL = "http://" + PRIVATE_NETWORK_URL + ":5001";
    public static final int HOME_GRID_NUM_BOXES = 2;

    @Override
    public void onCreate()
    {
        super.onCreate();

        context = getApplicationContext();
        onlineLive.postValue(isOnline(Baskit.this));

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null)
        {
            networkCallback = new ConnectivityManager.NetworkCallback()
            {
                @Override
                public void onAvailable(Network network)
                {
                    onlineLive.postValue(isOnline(Baskit.this));
                }

                @Override
                public void onLost(Network network)
                {
                    onlineLive.postValue(isOnline(Baskit.this));
                }

                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities caps)
                {
                    onlineLive.postValue(isOnline(Baskit.this));
                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
                cm.registerDefaultNetworkCallback(networkCallback);
            }
            else
            {
                NetworkRequest request = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();
                cm.registerNetworkCallback(request, networkCallback);
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

    public static boolean isOnline(Context context)
    {
        if (context == null) return false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        // Pre-Marshmallow fallback (no getActiveNetwork() / VALIDATED capability)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
        {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }

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

    private static String getTotalDisplayString(Double total, boolean allPricesKnown, boolean allowZero)
    {
        String str;

        if (total == 0 && !allowZero && !allPricesKnown)
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

    public static String getTotalDisplayString(Double total, boolean allPricesKnown, boolean withTitle, boolean allowZero)
    {
        String amount = getTotalDisplayString(total, allPricesKnown, allowZero);
        String amountIsolated = "\u2066" + amount + "\u2069";

        if (withTitle)
        {
            String titleIsolated = "\u2067" + "סך הכל:" + "\u2069";
            return amountIsolated + " " + titleIsolated;
        }

        return amountIsolated;
    }

    public static void notActivityRunWhenServerActive(Runnable work, Activity activity)
    {
        if (activity instanceof MasterActivity)
        {
            ((MasterActivity) activity).runWhenServerActive(work);
        }
        else
        {
            new Thread(work).start();
        }
    }

    public static void notActivityRunIfOnline(Runnable work, Activity activity)
    {
        if (activity instanceof MasterActivity)
        {
            ((MasterActivity) activity).runIfOnline(work);
        }
        else
        {
            work.run();
        }
    }

    public static String encodeKey(String s)
    {
        if (s == null) return null;
        String out = s;
        out = out.replace(".", "__dot__");
        out = out.replace("$", "__dollar__");
        out = out.replace("#", "__hash__");
        out = out.replace("[", "__lbracket__");
        out = out.replace("]", "__rbracket__");
        out = out.replace("/", "__slash__");
        return out;
    }

    public static String decodeKey(String s)
    {
        if (s == null) return null;
        String out = s;
        out = out.replace("__dot__", ".");
        out = out.replace("__dollar__", "$");
        out = out.replace("__hash__", "#");
        out = out.replace("__lbracket__", "[");
        out = out.replace("__rbracket__", "]");
        out = out.replace("__slash__", "/");
        return out;
    }
}