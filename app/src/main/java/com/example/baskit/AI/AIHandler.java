package com.example.baskit.AI;

import android.app.Activity;
import android.util.Log;

import com.example.baskit.MainComponents.Item;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class AIHandler
{
    private static AIHandler instance;

    private String prompt;

    private final String[] defaultCategoryNames = {
            "מוצרי חלב",
            "בשר ועוף",
            "דגים ופירות ים",
            "ירקות",
            "פירות",
            "מעדניה",
            "מאפים ולחמים",
            "קפואים",
            "קטניות ודגנים",
            "פסטה ואורז",
            "שימורים וממרחים",
            "תבלינים ושמנים",
            "חטיפים וממתקים",
            "שתייה קלה",
            "קפה ותה",
            "אלכוהול ויין",
            "מוצרי ניקיון",
            "טואלטיקה והיגיינה",
            "בריאות ותרופות",
            "תינוקות וילדים",
            "חיות מחמד",
            "כלים חד פעמיים",
            "מוצרי אפייה",
            "מוצרי בית וכלים",
            "קוסמטיקה וטיפוח"
    };

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
        void onResult(String categoryName);
    }

    public void getItemCategoryAI(String itemName, Activity activity, OnGeminiResult onGeminiResult)
    {
        prompt = createItemCategoryPrompt(itemName);

        geminiManager.sendTextPrompt(activity, prompt, new GeminiCallback()
        {
            @Override
            public void onSuccess(String result)
            {
                onGeminiResult.onResult(result.split("\n")[0]);
            }

            @Override
            public void onFailure(Throwable error)
            {
                Log.e("AI", error.toString());
            }
        });
    }

    public void getItemCategoryAI(Item item, Activity activity, OnGeminiResult onGeminiResult)
    {
        getItemCategoryAI(item.getName(), activity, onGeminiResult);
    }

    public String classifyBatchBlocking(String promptText) throws InterruptedException
    {
        final Object lock = new Object();
        final StringBuilder resultHolder = new StringBuilder();
        final boolean[] finished = {false};

        geminiManager.sendTextPrompt(null, promptText, new GeminiCallback()
        {
            @Override
            public void onSuccess(String result)
            {
                synchronized (lock)
                {
                    resultHolder.append(result);
                    finished[0] = true;
                    lock.notify();
                }
            }

            @Override
            public void onFailure(Throwable error)
            {
                synchronized (lock)
                {
                    finished[0] = true;
                    lock.notify();
                }
                Log.e("AI_BATCH", error.toString());
            }
        });

        synchronized (lock)
        {
            long timeoutMs = 60000;
            long start = System.currentTimeMillis();

            while (!finished[0])
            {
                long elapsed = System.currentTimeMillis() - start;
                long remaining = timeoutMs - elapsed;

                if (remaining <= 0)
                {
                    break;
                }

                lock.wait(remaining);
            }
        }

        if (!finished[0])
        {
            throw new RuntimeException("Gemini request timed out");
        }

        return resultHolder.toString();
    }

    private String createItemCategoryPrompt(String itemName)
    {
        return "אני יוצרת רשימת קניות מחולקת לפי מחלקות בסופר במטרה לארגן אותה." +
                "תשלח לי את הקגוריה של המוצר הבא: " + '"' + itemName + '"' + "." +
                "תשתמש בקטגוריות הבאות ואם אין תיצור אחת משלך (תעדיף להשתמש מהן):" + String.join(", ", defaultCategoryNames) + "." +
                "תחזיר רק את שם הקטגוריה.";
    }

    public String createBatchCategoryPromptFromNames(List<String> itemNames)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("אני יוצרת רשימת קניות מחולקת לפי מחלקות בסופר.\n");
        builder.append("קבל רשימת שמות מוצרים.\n");
        builder.append("עליך לשייך כל מוצר לקטגוריה מתאימה.\n");
        builder.append("השתמש בעיקר בקטגוריות הבאות ואם אין התאמה תיצור קטגוריה חדשה:\n");
        builder.append(String.join(", ", defaultCategoryNames));
        builder.append(".\n\n");

        builder.append("החזר JSON בלבד בפורמט הבא ללא טקסט נוסף וללא הסברים:\n");
        builder.append("{ \"קטגוריה\": [\"שם מוצר 1\", \"שם מוצר 2\"] }\n\n");
        builder.append("כל קטגוריה היא מפתח, והערך הוא מערך של שמות המוצרים ששייכים אליה.\n");
        builder.append("אסור להחזיר Markdown או ```json.\n");
        builder.append("JSON תקין בלבד.\n\n");

        builder.append("המוצרים:\n");

        for (String name : itemNames)
        {
            builder.append("- ").append(name).append("\n");
        }

        return builder.toString();
    }
}
