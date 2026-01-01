package com.example.baskit.SQLite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        // Reschedule next 06:00 sync after reboot
        DailySyncWorker.scheduleNext(context.getApplicationContext());
    }
}