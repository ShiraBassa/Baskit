package com.example.baskit.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Map;

public class CitiesListAdapter extends RecyclerView.Adapter<CitiesListAdapter.ViewHolder> {

    private ArrayList<String> cities;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public CitiesListAdapter(ArrayList<String> cities) {
        this.cities = cities;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCity;

        public ViewHolder(View itemView) {
            super(itemView);
            tvCity = itemView.findViewById(R.id.tv_city);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cities_list_single, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String city = cities.get(position);
        holder.tvCity.setText(city);

        holder.itemView.setBackgroundColor(
                selectedPosition == position
                        ? holder.itemView.getContext().getColor(R.color.tan)
                        : holder.itemView.getContext().getColor(R.color.white_smoke)
        );

        holder.itemView.setOnClickListener(v ->
        {
            if (selectedPosition == position)
            {
                holder.itemView.getContext().getColor(R.color.white_smoke);
                selectedPosition = RecyclerView.NO_POSITION;
            }
            else
            {
                holder.itemView.getContext().getColor(R.color.tan);
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