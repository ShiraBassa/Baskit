package com.example.baskit.AI;

public interface GeminiCallback
{
    void onSuccess(String result);
    void onFailure(Throwable error);
}
