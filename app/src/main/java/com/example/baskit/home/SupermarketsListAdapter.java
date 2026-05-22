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
import java.util.function.BiConsumer;

@SuppressWarnings("ALL")
public class SupermarketsListAdapter extends RecyclerView.Adapter<SupermarketsListAdapter.ViewHolder>
{
    private String selectedSupermarketName, selectedSectionName;

    private Map<String, ArrayList<String>> supermarkets;

    private final Context context;
    private final BiConsumer<String, String> listener = null;

    public SupermarketsListAdapter(Context context, Map<String, ArrayList<String>> supermarkets)
    {
        this.context = context;
        this.supermarkets = supermarkets != null ? supermarkets : new java.util.HashMap<>();
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
        if (position < 0 || position >= getItemCount())
        {
            return;
        }

        String supermarketName;
        ArrayList<String> supermarketKeys = new ArrayList<>(supermarkets.keySet());

        if (position >= supermarketKeys.size())
        {
            return;
        }

        supermarketName = supermarketKeys.get(position);

        if (supermarketName == null)
        {
            supermarketName = Baskit.getAppStr(R.string.unknown_supermarket);
        }

        if (holder.sectionsAdapter == null)
        {
            holder.sectionsAdapter = new SectionsListAdapter(context, supermarketName,
                    supermarkets.get(supermarketName) != null
                            ? supermarkets.get(supermarketName)
                            : new ArrayList<>(),
                    (clickedSupermarketName, clickedSectionName) ->
                    {
                        SupermarketsListAdapter.this.onSectionClick(clickedSupermarketName, clickedSectionName);
                        notifyDataSetChanged();
                    }
            );

            holder.recyclerSections.setAdapter(holder.sectionsAdapter);
        }
        else
        {
            holder.sectionsAdapter.updateData(
                    supermarkets.get(supermarketName) != null
                            ? supermarkets.get(supermarketName)
                            : new ArrayList<>(),
                    supermarketName
            );
        }

        holder.sectionsAdapter.updateSelectedSection(selectedSupermarketName, selectedSectionName);
        String decodedName = Baskit.decodeKey(supermarketName);
        holder.tvSupermarketName.setText(
                decodedName != null && !decodedName.isBlank()
                        ? decodedName
                        : Baskit.getAppStr(R.string.unknown_supermarket)
        );
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
        this.supermarkets = newSupermarkets != null
                ? newSupermarkets
                : new java.util.HashMap<>();

        if (selectedSupermarketName != null &&
                !this.supermarkets.containsKey(selectedSupermarketName))
        {
            selectedSupermarketName = null;
            selectedSectionName = null;
        }

        notifyDataSetChanged();
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
        private final BiConsumer<String, String> listener;

        public SectionsListAdapter(Context context, String supermarketName, ArrayList<String> sections, BiConsumer<String, String> onSupermarketClickListener)
        {
            this.context = context;
            this.supermarketName = supermarketName;
            this.sections = sections != null ? sections : new ArrayList<>();
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
            if (position < 0 || position >= sections.size())
            {
                return;
            }
            String sectionName = sections.get(position);

            if (sectionName == null)
            {
                sectionName = Baskit.getAppStr(R.string.unknown_section);
            }
            boolean isSelected = sectionName.equals(selectedSection)
                    && supermarketName.equals(selectedSupermarket);

            holder.itemView.setActivated(isSelected);
            holder.itemView.setSelected(isSelected);

            holder.itemView.setOnClickListener(v -> {
                int adapterPos = holder.getAdapterPosition();

                if (adapterPos == RecyclerView.NO_POSITION) {
                    return;
                }
                if (adapterPos < 0 || adapterPos >= sections.size())
                {
                    return;
                }

                int previousPosition = selectedPosition;

                if (selectedPosition == adapterPos)
                {
                    selectedPosition = RecyclerView.NO_POSITION;
                }
                else
                {
                    selectedPosition = adapterPos;
                }

                if (previousPosition != RecyclerView.NO_POSITION)
                    notifyItemChanged(previousPosition);

                if (selectedPosition != RecyclerView.NO_POSITION)
                    notifyItemChanged(selectedPosition);

                if (listener != null)
                {
                    String sectionNameClick = sections.get(adapterPos);
                    listener.accept(supermarketName, sectionNameClick);
                }
            });

            String decodedSection = Baskit.decodeKey(sectionName);
            holder.tvSectionName.setText(
                    decodedSection != null && !decodedSection.isBlank()
                            ? decodedSection
                            : Baskit.getAppStr(R.string.unknown_section)
            );
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
            this.sections = sections != null ? sections : new ArrayList<>();
            this.supermarketName = supermarketName;
            if (selectedPosition >= this.sections.size())
            {
                selectedPosition = RecyclerView.NO_POSITION;
            }

            notifyDataSetChanged();
        }

    }
}
