package com.example.baskit.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.Baskit;
import com.example.baskit.main_components.Supermarket;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Map;

@SuppressWarnings("ALL")
public class SupermarketsListAdapter extends RecyclerView.Adapter<SupermarketsListAdapter.ViewHolder>
{
    private String selectedSupermarketName, selectedSectionName;

    private Map<String, ArrayList<String>> supermarkets;

    private final Context context;
    private final OnSupermarketClickListener listener = null;

    public interface OnSupermarketClickListener
    {
        void onSupermarketClick(Supermarket supermarket);
    }

    public SupermarketsListAdapter(Context context, Map<String, ArrayList<String>> supermarkets)
    {
        this.context = context;
        this.supermarkets = supermarkets;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView tvSupermarketName;
        RecyclerView recyclerSections;
        SectionsListAdapter sectionsAdapter;

        public ViewHolder(View itemView)
        {
            super(itemView);

            tvSupermarketName = itemView.findViewById(R.id.tv_supermarket_name);
            recyclerSections = itemView.findViewById(R.id.recycler_sections);

            recyclerSections.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.supermarkets_list_single, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position)
    {
        String supermarketName;

        supermarketName = new ArrayList<>(supermarkets.keySet()).get(position);

        if (holder.sectionsAdapter == null)
        {
            holder.sectionsAdapter = new SectionsListAdapter(context, supermarketName,
                    supermarkets.get(supermarketName), new SectionsListAdapter.OnSectionClickListener()
                    {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void onSectionClick(String clickedSupermarketName, String clickedSectionName)
                        {
                            SupermarketsListAdapter.this.onSectionClick(clickedSupermarketName, clickedSectionName);

                            notifyDataSetChanged();
                        }
                    }
            );

            holder.recyclerSections.setAdapter(holder.sectionsAdapter);
        }
        else
        {
            holder.sectionsAdapter.updateData(
                    supermarkets.get(supermarketName),
                    supermarketName
            );
        }

        holder.sectionsAdapter.updateSelectedSection(selectedSupermarketName, selectedSectionName);
        holder.tvSupermarketName.setText(Baskit.decodeKey(supermarketName));
    }

    @Override
    public int getItemCount()
    {
        if (supermarkets != null)
        {
            return supermarkets.size();
        }
        else
        {
            return 0;
        }
    }

    public void updateData(Map<String, ArrayList<String>> newSupermarkets)
    {
        this.supermarkets = newSupermarkets;
    }

    public String getSelectedSupermarketName()
    {
        return selectedSupermarketName;
    }

    public String getSelectedSectionName()
    {
        return selectedSectionName;
    }

    private void onSectionClick(String supermarketName, String sectionName)
    {
        if (selectedSupermarketName != null && selectedSectionName != null &&
                selectedSupermarketName.equals(supermarketName) &&
                selectedSectionName.equals(sectionName))
        {
            // Same item clicked — deselect
            selectedSupermarketName = null;
            selectedSectionName = null;
        }
        else
        {
            // New selection
            selectedSupermarketName = supermarketName;
            selectedSectionName = sectionName;
        }
    }


    public static class SectionsListAdapter extends RecyclerView.Adapter<SectionsListAdapter.ViewHolder>
    {
        private int selectedPosition = RecyclerView.NO_POSITION;
        private String selectedSupermarket = null;
        private String selectedSection = null;

        private String supermarketName;

        private ArrayList<String> sections;

        private final Context context;
        private final SectionsListAdapter.OnSectionClickListener listener;

        public interface OnSectionClickListener
        {
            void onSectionClick(String selectedSupermarketName, String selectedSectionName);
        }

        public SectionsListAdapter(Context context, String supermarketName, ArrayList<String> sections, SectionsListAdapter.OnSectionClickListener onSupermarketClickListener)
        {
            this.context = context;
            this.supermarketName = supermarketName;
            this.sections = sections;
            this.listener = onSupermarketClickListener;
        }

        @SuppressLint("NotifyDataSetChanged")
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

                tvSectionName = itemView.findViewById(R.id.tv_supermarket);
            }
        }

        @NonNull
        @Override
        public SectionsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.supermarket_list_section, parent, false);

            return new SectionsListAdapter.ViewHolder(view);
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onBindViewHolder(@NonNull SectionsListAdapter.ViewHolder holder, int position)
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
                    listener.onSectionClick(supermarketName, sectionNameClick);
                }
            });

            holder.tvSectionName.setText(Baskit.decodeKey(sectionName));
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

        public void updateData(ArrayList<String> sections, String supermarketName)
        {
            this.sections = sections;
            this.supermarketName = supermarketName;
        }

    }
}
