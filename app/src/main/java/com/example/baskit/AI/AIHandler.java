package com.example.baskit.AI;

import android.app.Activity;
import android.util.Log;

import org.json.JSONArray;

import java.util.ArrayList;

public class AIHandler
{
    private static AIHandler instance;

    private String prompt;

    private GeminiManager geminiManager = GeminiManager.getInstance();

    public static AIHandler getInstance()
    {
        if (instance == null)
        {
            instance = new AIHandler();
        }

        return instance;
    }

    private AIHandler() {}

    public interface OnGeminiResult
    {
        void onResult(ArrayList<String> suggestions);
    }

    public void getListSuggestions(String listName, Activity activity, OnGeminiResult onGeminiResult)
    {
        prompt = createListSuggestionsPrompt(listName);

        geminiManager.sendTextPrompt(activity, prompt, new GeminiCallback()
        {
            @Override
            public void onSuccess(String result)
            {
                ArrayList<String> suggestions = new ArrayList<>();

                try
                {
                    String normalized = result.trim();
                    String cleaned = normalized.replace("'", "").replace("\\", "").replaceAll("[^\\p{L}\\p{N}, ]", "").trim();
                    String[] items = cleaned.split(",");

                    for (String item : items)
                    {
                        String trimmed = item.trim();

                        if (!trimmed.isEmpty())
                        {
                            suggestions.add(trimmed);
                        }
                    }

                    onGeminiResult.onResult(suggestions);
                }
                catch (Exception e)
                {
                    Log.e("AI_PARSE", "Failed to parse Gemini response: " + result);
                    Log.e("AI_PARSE", e.toString());
                    onGeminiResult.onResult(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Throwable error)
            {
                Log.e("AI", error.toString());
                onGeminiResult.onResult(new ArrayList<>());
            }
        });
    }

    public String createListSuggestionsPrompt(String listName)
    {
        return "אני יוצרת רשימות קניות לסופר כאשר לכל רשימה יש את השם שלה. " +
                "קבל שם רשימה ולפיו תציע לי עד 10 מוצרים בסיסיים הקשורים בה. " +
                "אם שם הרשימה לא מאפשר להציע מוצרים בסיסיים נאותים, תחזיר תשובה ריקה. " +
                "החזרת התוצאה צריכה להיות בפורמט רשימה של מחרוזות, לדוגמה: ['מוצר1', 'מוצר2']. בלי סימנים מיותרים. " +
                "שם הרשימה: '" + listName + "'";
    }
}
