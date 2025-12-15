package com.example.baskit.Home;

import android.content.Context;
import android.util.AttributeSet;

public class SquareMaterialButton extends com.google.android.material.button.MaterialButton {

    public SquareMaterialButton(Context context) {
        super(context);
    }

    public SquareMaterialButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareMaterialButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}