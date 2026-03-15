package com.example.baskit;

import android.annotation.SuppressLint;
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
import android.widget.Toast;
import android.text.TextUtils;
import java.util.Locale;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.baskit.Home.SettingsActivity;
import com.example.baskit.MainComponents.Supermarket;

public class Baskit extends Application
{
    public static final String PRIVATE_NETWORK_URL = "192.168.1.247";
    public static final String EMULATOR_URL = "10.0.2.2";
    //public static final String SERVER_URL = "http://" + EMULATOR_URL + ":10000";
    public static final String SERVER_URL = "https://baskit-ac3x.onrender.com";
    public static final int HOME_GRID_NUM_BOXES = 2;
    public static final Supermarket UNASSIGNED_SUPERMARKET = new Supermarket("לא נבחר", "");

    private static Context context;
    private static final MutableLiveData<Boolean> onlineLive = new MutableLiveData<>(true);
    private ConnectivityManager.NetworkCallback networkCallback;

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

    public static int getAppColor(Context context, int id)
    {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        boolean found = theme.resolveAttribute(id, typedValue, true);

        if (found)
        {
            if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) // If the attribute directly points to a color
            {
                return typedValue.data;
            }
            else if (typedValue.resourceId != 0) // If the attribute points to a resource
            {
                return context.getResources().getColor(typedValue.resourceId, theme);
            }
        }

        // If not found as an attribute, assume it is a direct Resource ID
        try
        {
            return ContextCompat.getColor(context, id);
        }
        catch (Exception e)
        {
            // If both fail, return Red for debug
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

            if (!isLayoutDirectionLeft())
            {
                return titleIsolated + " " + amountIsolated;
            }
            else
            {
                return amountIsolated + " " + titleIsolated;
            }
        }

        return amountIsolated;
    }

    public static boolean isLayoutDirectionLeft()
    {
        return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_LTR;
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

    public static boolean isValidUserName(String username, boolean showError)
    {
        boolean valid = !username.isBlank();

        if (!valid)
        {
            Toast.makeText(context, "יש להזין שם משתמש" + username, Toast.LENGTH_SHORT).show();
        }

        return valid;
    }
}