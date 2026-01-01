package com.example.baskit.SQLite;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Firebase.FirebaseAuthHandler;
import com.example.baskit.Login.ErrorType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * JobScheduler-based daily sync service.
 *
 * NOTE:
 * - This class is a JobService (NOT a WorkManager Worker).
 * - You must register it in AndroidManifest.xml as a service with
 *   android.permission.BIND_JOB_SERVICE.
 */
public class DailySyncWorker extends JobService
{
    private static final String TAG = "DailySyncJob";

    // Keep a stable job id (unique within your app)
    private static final int JOB_ID_DAILY_SYNC = 6001;

    private ExecutorService executor;
    private final FirebaseAuthHandler authHandler = FirebaseAuthHandler.getInstance();

    /**
     * Schedule the next run for ~06:00 (Asia/Jerusalem).
     * This uses a one-off job with minimum latency until the next 06:00,
     * and the job reschedules itself again at the end.
     */
    public static void scheduleNext(@NonNull Context context)
    {
        long delayMs = millisUntilNext6am();

        ComponentName component = new ComponentName(context, DailySyncWorker.class);

        JobInfo job = new JobInfo.Builder(JOB_ID_DAILY_SYNC, component)
                // Run when we have *some* network
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                // One-off job that will run after delayMs
                .setMinimumLatency(delayMs)
                // Safety deadline so it doesn't get stuck forever (24 hours after target)
                .setOverrideDeadline(delayMs + 24L * 60L * 60L * 1000L)
                // Persist across reboot (requires RECEIVE_BOOT_COMPLETED + reschedule on boot on some OEMs)
                .setPersisted(true)
                .build();

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) return;

        // Replace any existing scheduled job with same id
        scheduler.cancel(JOB_ID_DAILY_SYNC);
        int res = scheduler.schedule(job);
        if (res == JobScheduler.RESULT_FAILURE)
        {
            Log.e(TAG, "Failed to schedule daily sync job");
        }
        else
        {
            Log.d(TAG, "Scheduled daily sync in " + delayMs + "ms");
        }
    }

    private static long millisUntilNext6am()
    {
        TimeZone tz = TimeZone.getTimeZone("Asia/Jerusalem");

        Calendar now = Calendar.getInstance(tz);
        Calendar next = Calendar.getInstance(tz);

        next.set(Calendar.HOUR_OF_DAY, 6);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        if (!next.after(now))
        {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        return Math.max(0, next.getTimeInMillis() - now.getTimeInMillis());
    }

    @Override
    public boolean onStartJob(JobParameters params)
    {
        if (executor == null)
        {
            executor = Executors.newSingleThreadExecutor();
        }

        executor.execute(() ->
        {
            try
            {
                // Ensure we have a valid Firebase user + token set on APIHandler
                authHandler.checkCurrUser(new FirebaseAuthHandler.AuthCallback()
                {
                    @Override
                    public void onAuthSuccess()
                    {
                        executor.execute(() ->
                        {
                            boolean needsRescheduleInner = false;

                            try
                            {
                                APIHandler api = APIHandler.getInstance();
                                api.preload();

                                Log.d(TAG, "Daily sync completed");
                            }
                            catch (Exception e)
                            {
                                Log.e(TAG, "Daily sync failed", e);
                                needsRescheduleInner = true;
                            }
                            finally
                            {
                                // Always schedule the next run (keeps it anchored around ~06:00)
                                try
                                {
                                    scheduleNext(getApplicationContext());
                                }
                                catch (Exception ignored) {}

                                jobFinished(params, needsRescheduleInner);
                            }
                        });
                    }

                    @Override
                    public void onAuthError(String msg, ErrorType type)
                    {
                        // If not logged in, there is nothing to sync.
                        boolean needsRescheduleInner = type != ErrorType.NOT_LOGGED;

                        if (msg != null && !msg.isEmpty())
                        {
                            Log.w(TAG, "Auth failed: " + msg + " (" + type + ")");
                        }
                        else
                        {
                            Log.w(TAG, "Auth failed (" + type + ")");
                        }

                        // Keep the daily schedule anchored
                        try
                        {
                            scheduleNext(getApplicationContext());
                        }
                        catch (Exception ignored) {}

                        jobFinished(params, needsRescheduleInner);
                    }
                });
            }
            catch (Exception e)
            {
                Log.e(TAG, "Daily sync failed", e);
            }
            finally
            {
                // jobFinished is handled inside the auth callback
            }
        });

        // Work continues asynchronously
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params)
    {
        // If the job is stopped (e.g., constraints lost), ask system to retry.
        return true;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (executor != null)
        {
            executor.shutdownNow();
            executor = null;
        }
    }
}