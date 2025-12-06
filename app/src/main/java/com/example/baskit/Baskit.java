package com.example.baskit;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

public class Baskit extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

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
}