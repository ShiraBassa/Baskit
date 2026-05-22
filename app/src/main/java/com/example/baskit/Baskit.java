package com.example.baskit;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;
import android.text.TextUtils;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.lifecycle.MutableLiveData;

import com.example.baskit.main_components.Supermarket;

public class Baskit extends Application
{
    public static final String SERVER_URL = "https://baskit-ac3x.onrender.com";
    public static final int HOME_GRID_NUM_BOXES = 2;
    public static final Supermarket UNASSIGNED_SUPERMARKET = new Supermarket("לא נבחר", "");

    private static Baskit instance;

    public static final MutableLiveData<Boolean> onlineLive = new MutableLiveData<>(true);
    public static final MutableLiveData<Boolean> serverAliveLive = new MutableLiveData<>(true);
    public static final MutableLiveData<Boolean> serverCheckingLive = new MutableLiveData<>(false);
    private final android.os.Handler serverHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final java.util.concurrent.ExecutorService serverBg = java.util.concurrent.Executors.newSingleThreadExecutor();
    private boolean serverPollingRunning = false;
    private Runnable serverPollingRunnable;

    @Override
    public void onCreate()
    {
        super.onCreate();

        instance = this;
        onlineLive.postValue(isOnline(Baskit.this));

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null)
        {
            ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    onlineLive.postValue(isOnline(Baskit.this));
                }

                @Override
                public void onLost(@NonNull Network network) {
                    onlineLive.postValue(isOnline(Baskit.this));
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
                    onlineLive.postValue(isOnline(Baskit.this));
                }
            };

            cm.registerDefaultNetworkCallback(networkCallback);
        }

        startServerPolling();

        onlineLive.observeForever(isOnline ->
        {
            boolean online = isOnline != null && isOnline;

            if (online)
            {
                startServerPolling();
            }
            else
            {
                stopServerPolling();
                serverCheckingLive.postValue(false);
                serverAliveLive.postValue(false);
            }
        });
    }

    public static Context getContext()
    {
        return instance.getApplicationContext();
    }

    public static boolean isOnline(Context context)
    {
        if (context == null) return false;

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

    public static String getAppStr(int id)
    {
        return getContext().getString(id);
    }

    @SuppressLint("DefaultLocale")
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
            String title = getAppStr(R.string.total_title);
            String titleIsolated;

            if (isLayoutRight())
            {
                titleIsolated = "\u2067" + title + "\u2069";
            }
            else
            {
                titleIsolated = "\u2066" + title + "\u2069";
            }

            return titleIsolated + " " + amountIsolated;
        }

        return amountIsolated;
    }

    public static boolean isLayoutLeft()
    {
        return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                == View.LAYOUT_DIRECTION_LTR;
    }

    public static boolean isLayoutRight()
    {
        return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                == View.LAYOUT_DIRECTION_RTL;
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

        if (!valid && showError)
        {
            Toast.makeText(
                    getContext(),
                    getAppStr(R.string.error_enter_username),
                    Toast.LENGTH_SHORT
            ).show();
        }

        return valid;
    }

    private void startServerPolling()
    {
        if (serverPollingRunning) return;
        serverPollingRunning = true;

        serverPollingRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                if (!isOnline(Baskit.this))
                {
                    stopServerPolling();
                    serverAliveLive.postValue(false);
                    return;
                }

                serverBg.execute(() ->
                {
                    boolean up;

                    serverCheckingLive.postValue(true);

                    try
                    {
                        up = com.example.baskit.online_components.APIHandler.getInstance().isServerActive();
                    }
                    catch (Exception e)
                    {
                        up = false;
                    }

                    serverAliveLive.postValue(up);
                    serverCheckingLive.postValue(false);

                    serverHandler.postDelayed(this, 5000);
                });
            }
        };

        serverPollingRunnable.run();
    }

    private void stopServerPolling()
    {
        serverPollingRunning = false;

        if (serverPollingRunnable != null)
        {
            serverHandler.removeCallbacks(serverPollingRunnable);
        }

        serverCheckingLive.postValue(false);
        serverPollingRunnable = null;
    }
}