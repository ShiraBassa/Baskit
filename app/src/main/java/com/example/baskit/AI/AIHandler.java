package com.example.baskit.AI;

import android.app.Activity;
import android.app.ProgressDialog;
import android.view.View;

import com.example.baskit.MainComponents.Item;

import java.util.Arrays;

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

    public void getCategoryName(String itemName, Activity activity, OnGeminiResult onGeminiResult)
    {
        prompt = createPrompt(itemName);

        geminiManager.sendTextPrompt(activity, prompt, new GeminiCallback()
        {
            @Override
            public void onSuccess(String result)
            {
                onGeminiResult.onResult(result.split("\n")[0]);
            }

            @Override
            public void onFailure(Throwable error) {}
        });
    }

    public void getCategoryName(Item item, Activity activity, OnGeminiResult onGeminiResult)
    {
        getCategoryName(item.getName(), activity, onGeminiResult);
    }

    private String createPrompt(String itemName)
    {
        return "אני יוצרת רשימת קניות מחולקת לפי מחלקות בסופר במטרה לארגן אותה." +
                "תשלח לי את הקגוריה של המוצר הבא: " + '"' + itemName + '"' + "." +
                "תשתמש בקטגוריות הבאות ואם אין תיצור אחת משלך (תעדיף להשתמש מהן):" + String.join(", ", defaultCategoryNames) + "." +
                "תחזיר רק את שם הקטגוריה.";
    }
}
