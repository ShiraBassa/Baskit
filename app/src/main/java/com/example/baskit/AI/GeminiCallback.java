package com.example.baskit.AI;

public interface GeminiCallback
{
    public void onSuccess(String result);
    public void onFailure(Throwable error);
}
