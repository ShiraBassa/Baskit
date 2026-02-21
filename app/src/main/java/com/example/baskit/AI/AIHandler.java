package com.example.baskit.AI;

import android.app.Activity;
import android.util.Log;

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
                String cleaned = result.replaceAll("\\[|\\]|'", "").trim();
                String[] items = cleaned.split("\\s*,\\s*");

                ArrayList<String> suggestions = new ArrayList<>();

                for (String item : items)
                {
                    if (!item.isEmpty())
                    {
                        suggestions.add(item.trim());
                    }
                }

                onGeminiResult.onResult(suggestions);
            }

            @Override
            public void onFailure(Throwable error)
            {
                Log.e("AI", error.toString());
            }
        });
    }

    public String createListSuggestionsPrompt(String listName)
    {
        return "אני יוצרת רשימות קניות לסופר כאשר לכל רשימה יש את השם שלה. " +
                "קבל שם רשימה ולפיו תציע לי עד 10 מוצרים בסיסיים הקשורים בה. " +
                "אם שם הרשימה לא מאפשר להציע מוצרים בסיסיים נאותים, תחזיר תשובה ריקה. " +
                "החזרת התוצאה צריכה להיות בפורמט רשימה של מחרוזות, לדוגמה: ['מוצר1', 'מוצר2']. " +
                "שם הרשימה: '" + listName + "'";
    }
}
