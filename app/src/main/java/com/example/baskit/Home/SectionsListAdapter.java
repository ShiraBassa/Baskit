package com.example.baskit.Home;

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

public class SectionsListAdapter extends RecyclerView.Adapter<SectionsListAdapter.ViewHolder>
{
    ArrayList<String> sections;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private OnSectionClickListener listener;

    private String selectedSupermarket = null;
    private String selectedSection = null;
    String supermarketName;
    private Context context;

    public interface OnSectionClickListener
    {
        void onSectionClick(String sectionName);
    }

    public SectionsListAdapter(Context context, String supermarketName, ArrayList<String> sections, OnSectionClickListener onSupermarketClickListener)
    {
        this.context = context;
        this.supermarketName = supermarketName;
        this.sections = sections;
        this.listener = onSupermarketClickListener;
    }

    public void updateSelectedSection(String supermarket, String section)
    {
        this.selectedSupermarket = supermarket;
        this.selectedSection = section;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        protected TextView tvSectionName;

        public ViewHolder(View itemView)
        {
            super(itemView);

            tvSectionName = itemView.findViewById(R.id.tv_section_name);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.supermarket_list_section, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        String sectionName = sections.get(position);

        if (sectionName.equals(selectedSection) && supermarketName.equals(selectedSupermarket))
        {
            holder.itemView.setBackgroundColor(Baskit.getAppColor(context, com.google.android.material.R.attr.colorSecondaryContainer));
        }
        else
        {
            holder.itemView.setBackgroundColor(Baskit.getAppColor(context, com.google.android.material.R.attr.colorSurface));
        }

        holder.itemView.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();

            if (adapterPos == RecyclerView.NO_POSITION) {
                return;
            }

            if (selectedPosition == adapterPos)
            {
                // Deselect if the same item is clicked
                selectedPosition = RecyclerView.NO_POSITION;
            }
            else
            {
                selectedPosition = adapterPos;
            }

            // Refresh all items to reset backgrounds
            notifyDataSetChanged();

            if (listener != null)
            {
                String sectionNameClick = sections.get(adapterPos);
                listener.onSectionClick(sectionNameClick);
            }
        });

        holder.tvSectionName.setText(sectionName);
    }

    @Override
    public int getItemCount()
    {
        if (sections != null)
        {
            return sections.size();
        }
        else
        {
            return 0;
        }
    }

    public void updateData(ArrayList<String> newSections) {
        this.sections = newSections;
    }

}
