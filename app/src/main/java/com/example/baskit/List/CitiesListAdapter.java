package com.example.baskit.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.Baskit;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Map;

public class CitiesListAdapter extends RecyclerView.Adapter<CitiesListAdapter.ViewHolder>
{

    private ArrayList<String> cities;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private Context context;

    public CitiesListAdapter(Context context, ArrayList<String> cities)
    {
        this.context = context;
        this.cities = cities;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView tvCity;

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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        String city = cities.get(position);
        holder.tvCity.setText(city);

        holder.itemView.setBackgroundColor(
                selectedPosition == position
                        ? Baskit.getAppColor(context, com.google.android.material.R.attr.colorSecondaryContainer)
                        : Baskit.getAppColor(context, com.google.android.material.R.attr.colorSurface)
        );

        holder.itemView.setOnClickListener(v ->
        {
            if (selectedPosition == position)
            {
                Baskit.getAppColor(context, com.google.android.material.R.attr.colorSurface);
                selectedPosition = RecyclerView.NO_POSITION;
            }
            else
            {
                Baskit.getAppColor(context, com.google.android.material.R.attr.colorSecondaryContainer);
                selectedPosition = position;
            }

            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return cities == null ? 0 : cities.size();
    }

    public String getSelectedCity() {
        if (selectedPosition == RecyclerView.NO_POSITION) return null;
        return cities.get(selectedPosition);
    }

    public void updateData(ArrayList<String> newCities) {
        this.cities = newCities;
        notifyDataSetChanged();
    }
}