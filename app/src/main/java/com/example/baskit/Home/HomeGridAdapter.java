package com.example.baskit.Home;

import static com.example.baskit.Baskit.getAppColor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.text.LineBreaker;
import android.os.Build;
import android.text.Layout;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.MainComponents.List;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class HomeGridAdapter extends RecyclerView.Adapter<HomeGridAdapter.GridViewHolder>
{
    private ArrayList<String> listNames;
    private OnItemClickListener listener;
    private Context context;

    public interface OnItemClickListener
    {
        void onItemClick(int position);
        void onItemLongClick(int position);
    }

    public HomeGridAdapter(Context context, ArrayList<String> listNames, OnItemClickListener listener)
    {
        this.context = context;
        this.listNames = listNames;
        this.listener = listener;
    }

    public void remove(int position, boolean notifyItemRemoved)
    {
        listNames.remove(position);

        if (notifyItemRemoved)
        {
            this.notifyItemRemoved(position);
        }
    }

    public void remove(int position)
    {
        this.remove(position, true);
    }

    public void add(List list, boolean notifyItemInserted)
    {
        listNames.add(list.getName());

        if (notifyItemInserted)
        {
            this.notifyItemInserted(listNames.size()-1);
        }
    }

    public void add(List list)
    {
        this.add(list, true);
    }

    public static class GridViewHolder extends RecyclerView.ViewHolder
    {
        public Button button;

        public GridViewHolder(Button button) {
            super(button);
            this.button = button;
        }
    }

    @SuppressLint("ResourceType")
    @NonNull
    @Override
    public GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        MaterialButton button = new SquareMaterialButton(parent.getContext());
        button.setCornerRadius(24);

        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getAppColor(context, com.google.android.material.R.attr.colorSurface)));

        button.setStrokeWidth(5);
        button.setStrokeColor(android.content.res.ColorStateList.valueOf(
                getAppColor(context, com.google.android.material.R.attr.colorPrimaryVariant)
        ));

        button.setTextColor(getAppColor(context, com.google.android.material.R.attr.colorOnSurface));

        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextSize(20f);
        button.setSingleLine(false);
        button.setHorizontallyScrolling(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            button.setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY);
            button.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
        }

        button.setGravity(Gravity.CENTER);
        button.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);
        int pad = (int) (12 * parent.getContext().getResources().getDisplayMetrics().density);
        button.setPadding(pad, pad, pad, pad);

        button.setRippleColor(android.content.res.ColorStateList.valueOf(
                getAppColor(context, com.google.android.material.R.attr.colorPrimary)
        ));

        int margin = (int) (12 * parent.getContext().getResources().getDisplayMetrics().density);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(margin, margin, margin, margin);
        button.setLayoutParams(params);

        return new GridViewHolder(button);
    }

    @Override
    public void onBindViewHolder(@NonNull GridViewHolder holder, int position)
    {
        holder.button.setText(listNames.get(position));

        holder.button.setOnClickListener(v -> {
            int newPosition = holder.getAdapterPosition();
            if (newPosition != RecyclerView.NO_POSITION) {
                listener.onItemClick(newPosition);
            }
        });

        holder.button.setOnLongClickListener(v -> {
            int newPosition = holder.getAdapterPosition();
            if (newPosition != RecyclerView.NO_POSITION) {
                listener.onItemLongClick(newPosition);
            }
            return true;
        });
    }

    @Override
    public int getItemCount()
    {
        return listNames.size();
    }

    public void updateList(ArrayList<String> newList)
    {
        this.listNames.clear();
        this.listNames.addAll(newList);
        notifyDataSetChanged();
    }
}