package com.example.baskit.AI;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import com.example.baskit.BuildConfig;
import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class GeminiManager
{
    private static GeminiManager instance;
    private GenerativeModel gemini;

    private GeminiManager()
    {
        gemini = new GenerativeModel(
                "gemini-2.5-flash",
                BuildConfig.Gemini_API_Key
        );
    }

    public static GeminiManager getInstance()
    {
        if (instance == null)
        {
            instance = new GeminiManager();
        }

        return instance;
    }

    public void sendTextPrompt(Activity activity, String prompt, GeminiCallback callback)
    {
        new Thread(() ->
        {
            try {
                gemini.generateContent(prompt, new Continuation<GenerateContentResponse>()
                {
                    @NonNull
                    @Override
                    public CoroutineContext getContext()
                    {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NonNull Object result)
                    {
                        if (result instanceof Result.Failure)
                        {
                            Throwable error = ((Result.Failure) result).exception;

                            if (activity != null)
                            {
                                activity.runOnUiThread(() -> callback.onFailure(error));
                            }
                            else
                            {
                                callback.onFailure(error);
                            }
                        }
                        else
                        {
                            String text = ((GenerateContentResponse) result).getText();

                            if (activity != null)
                            {
                                activity.runOnUiThread(() -> callback.onSuccess(text));
                            }
                            else
                            {
                                callback.onSuccess(text);
                            }
                        }
                    }
                });
            }
            catch (Exception e)
            {
                if (activity != null)
                {
                    activity.runOnUiThread(() -> callback.onFailure(e));
                }
                else
                {
                    callback.onFailure(e);
                }
            }
        }).start();
    }
}
