package com.example.baskit.Home;

import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.MainComponents.List;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class HomeGridAdapter extends RecyclerView.Adapter<HomeGridAdapter.GridViewHolder>
{
    private ArrayList<List> lists;
    private java.util.List<String> listNames;
    private OnItemClickListener listener;

    public interface OnItemClickListener
    {
        void onItemClick(int position);
        void onItemLongClick(int position);
    }

    public HomeGridAdapter(ArrayList<List> lists, OnItemClickListener listener) {
        this.lists = lists;
        this.listener = listener;
        getListNames();
    }

    private void getListNames()
    {
        listNames = lists.stream()
                .map(List::getName)
                .collect(Collectors.toList());
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
            this.notifyItemInserted(lists.size()-1);
        }
    }

    public void add(List list)
    {
        this.add(list, true);
    }

    public static class GridViewHolder extends RecyclerView.ViewHolder {
        public Button button;

        public GridViewHolder(Button button) {
            super(button);
            this.button = button;
        }
    }

    @NonNull
    @Override
    public GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MaterialButton button = new MaterialButton(parent.getContext());
        button.setCornerRadius(16); // rounded corners
        button.setBackgroundColor(Color.LTGRAY);
        button.setTextColor(Color.BLACK);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(20f);

        int margin = (int) (8 * parent.getContext().getResources().getDisplayMetrics().density);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.MATCH_PARENT // temporary, we'll fix in onBind
        );
        params.setMargins(margin, margin, margin, margin);
        button.setLayoutParams(params);

        return new GridViewHolder(button);
    }

    @Override
    public void onBindViewHolder(@NonNull GridViewHolder holder, int position) {
        // Make square: height = width
        holder.button.post(() -> {
            int width = holder.button.getWidth();
            holder.button.getLayoutParams().height = width;
            holder.button.requestLayout();
        });

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
    public int getItemCount() {
        return listNames.size();
    }
}