package com.example.baskit.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.Baskit;
import com.example.baskit.R;

import java.util.ArrayList;

public class CitiesListAdapter extends RecyclerView.Adapter<CitiesListAdapter.ViewHolder>
{
    private int selectedPosition = RecyclerView.NO_POSITION;

    private ArrayList<String> cities;

    private final Context context;

    public CitiesListAdapter(Context context, ArrayList<String> cities)
    {
        this.context = context;
        this.cities = cities;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        final TextView tvCity;

        public ViewHolder(View itemView)
        {
            super(itemView);
            tvCity = itemView.findViewById(R.id.tv_city);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cities_list_single, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position)
    {
        String city = cities.get(position);
        holder.tvCity.setText(city);
        holder.itemView.setActivated(selectedPosition == position);
        holder.itemView.setSelected(selectedPosition == position);

        holder.itemView.setOnClickListener(v ->
        {
            int previousPosition = selectedPosition;

            if (selectedPosition == position)
            {
                selectedPosition = RecyclerView.NO_POSITION;
            }
            else
            {
                selectedPosition = position;
            }

            if (previousPosition != RecyclerView.NO_POSITION)
                notifyItemChanged(previousPosition);

            if (selectedPosition != RecyclerView.NO_POSITION)
                notifyItemChanged(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return cities == null ? 0 : cities.size();
    }

    public String getSelectedCity()
    {
        if (selectedPosition == RecyclerView.NO_POSITION) return null;
        return cities.get(selectedPosition);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(ArrayList<String> newCities)
    {
        this.cities = newCities;
        notifyDataSetChanged();
    }
}