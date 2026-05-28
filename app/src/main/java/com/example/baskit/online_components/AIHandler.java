package com.example.baskit.online_components;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.baskit.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class AIHandler
{
    private final GenerativeModel gemini;
    private static AIHandler instance;
    private final AtomicBoolean requestRunning = new AtomicBoolean(false);


    private AIHandler()
    {
        if (BuildConfig.Gemini_API_Key == null ||
                BuildConfig.Gemini_API_Key.isBlank())
        {
            throw new IllegalStateException("Gemini API key missing");
        }
        gemini = new GenerativeModel(
                "gemini-2.5-flash",
                BuildConfig.Gemini_API_Key
        );
    }

    public static synchronized void resetInstance()
    {
        instance = null;
    }

    public static synchronized AIHandler getInstance()
    {
        if (instance == null)
        {
            instance = new AIHandler();
        }

        return instance;
    }

    public void getListSuggestions(String listName, Activity activity, Consumer<ArrayList<String>> onGeminiResult)
    {
        if (onGeminiResult == null)
        {
            return;
        }

        if (listName == null || listName.isBlank())
        {
            onGeminiResult.accept(new ArrayList<>());
            return;
        }

        if (activity != null && (activity.isFinishing() || activity.isDestroyed()))
        {
            onGeminiResult.accept(new ArrayList<>());
            return;
        }

        String prompt = createListSuggestionsPrompt(listName);

        sendTextPrompt(activity, prompt, result ->
        {
            Set<String> uniqueSuggestions = new LinkedHashSet<>();

            try
            {
                if (result == null)
                {
                    onGeminiResult.accept(new ArrayList<>());
                    return;
                }

                String normalized = result.trim();
                String cleaned = normalized.replace("'", "").replace("\\", "").replaceAll("[^\\p{L}\\p{N}, ]", "").trim();
                String[] items = cleaned.split(",");

                for (String item : items)
                {
                    String trimmed = item.trim();

                    if (!trimmed.isEmpty())
                    {
                        if (trimmed.length() <= 50)
                        {
                            uniqueSuggestions.add(trimmed);
                        }
                    }
                }

                ArrayList<String> suggestions = new ArrayList<>(uniqueSuggestions);

                if (suggestions.size() > 10)
                {
                    suggestions = new ArrayList<>(suggestions.subList(0, 10));
                }

                onGeminiResult.accept(suggestions);
            }
            catch (Exception e)
            {
                Log.e("AI_PARSE", "Failed to parse Gemini response: " + result);
                Log.e("AI_PARSE", e.toString());
                onGeminiResult.accept(new ArrayList<>());
            }
        }
        );
    }

    public String createListSuggestionsPrompt(String listName)
    {
        if (listName == null)
        {
            listName = "";
        }
        return "אני יוצרת רשימות קניות לסופר כאשר לכל רשימה יש את השם שלה. " +
                "קבל שם רשימה ולפיו תציע לי עד 10 מוצרים בסיסיים הקשורים בה. " +
                "אם שם הרשימה לא מאפשר להציע מוצרים בסיסיים נאותים, תחזיר תשובה ריקה. " +
                "החזרת התוצאה צריכה להיות בפורמט רשימה של מחרוזות, לדוגמה: ['מוצר1', 'מוצר2']. בלי סימנים מיותרים. " +
                "שם הרשימה: '" + listName + "'";
    }

    public void sendTextPrompt(Activity activity, String prompt, Consumer<String> callback)
    {
        if (callback == null)
        {
            return;
        }

        if (prompt == null || prompt.isBlank())
        {
            failedRequest(activity, callback);
            return;
        }

        if (activity != null && (activity.isFinishing() || activity.isDestroyed()))
        {
            failedRequest(activity, callback);
            return;
        }

        if (!requestRunning.compareAndSet(false, true))
        {
            failedRequest(activity, callback);
            return;
        }

        new Thread(() ->
        {
            Thread.currentThread().setName("GeminiRequest");
            try
            {
                try
                {
                    gemini.generateContent(prompt, new Continuation<>() {
                        @NonNull
                        @Override
                        public CoroutineContext getContext() {
                            return EmptyCoroutineContext.INSTANCE;
                        }

                        @Override
                        public void resumeWith(@NonNull Object result) {
                            requestRunning.set(false);

                            if (activity != null &&
                                    (activity.isFinishing() || activity.isDestroyed()))
                            {
                                return;
                            }

                            try {
                                if (result instanceof Result.Failure) {
                                    Throwable error = ((Result.Failure) result).exception;

                                    if (error.toString().contains("503") ||
                                            error.toString().contains("UNAVAILABLE")) {
                                        Log.w("AI", "Gemini overloaded — fallback used");
                                        failedRequest(activity, callback);
                                        return;
                                    }

                                    Log.e("AI", "Gemini failure", error);
                                    failedRequest(activity, callback);
                                    return;
                                }

                                GenerateContentResponse response = (GenerateContentResponse) result;

                                String text = null;

                                try {
                                    text = response.getText();
                                } catch (Exception e) {
                                    Log.e("AI", "Failed to extract text", e);
                                }

                                if (text == null || text.trim().isEmpty()) {
                                    failedRequest(activity, callback);
                                    return;
                                }

                                String finalText = text;

                                if (activity != null) {
                                    activity.runOnUiThread(() -> callback.accept(finalText));
                                } else {
                                    callback.accept(finalText);
                                }
                            } catch (Exception e) {
                                requestRunning.set(false);
                                Log.e("AI", "Resume crash prevented", e);
                                failedRequest(activity, callback);
                            }
                        }
                    });
                }
                catch (Exception e)
                {
                    requestRunning.set(false);
                    Log.e("AI", "Gemini crashed before callback", e);
                    failedRequest(activity, callback);
                }
            }
            catch (Throwable t)
            {
                requestRunning.set(false);
                Log.e("AI", "Critical AI crash prevented", t);
                failedRequest(activity, callback);
            }

        }).start();
    }

    private void failedRequest(Activity activity, Consumer<String> callback)
    {
        requestRunning.set(false);

        if (callback == null)
        {
            return;
        }

        if (activity != null)
        {
            if (!activity.isFinishing() && !activity.isDestroyed())
            {
                activity.runOnUiThread(() -> callback.accept(""));
            }
        }
        else
        {
            callback.accept("");
        }
    }
}
