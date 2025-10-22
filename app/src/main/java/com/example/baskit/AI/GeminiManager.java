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
                "gemini-2.0-flash",
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
                            activity.runOnUiThread(() ->
                                    callback.onFailure(((Result.Failure) result).exception)
                            );
                        }
                        else
                        {
                            String text = ((GenerateContentResponse) result).getText();
                            activity.runOnUiThread(() ->
                                    callback.onSuccess(text)
                            );
                        }
                    }
                });
            }
            catch (Exception e)
            {
                activity.runOnUiThread(() -> callback.onFailure(e));
            }
        }).start();
    }
}
