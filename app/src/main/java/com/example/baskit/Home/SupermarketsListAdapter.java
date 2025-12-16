package com.example.baskit.Home;

import android.annotation.SuppressLint;
import android.content.Context;
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

public class SupermarketsListAdapter extends RecyclerView.Adapter<SupermarketsListAdapter.ViewHolder>
{
    private Map<String, ArrayList<String>> supermarkets;
    private OnSupermarketClickListener listener = null;
    private String selectedSupermarketName, selectedSectionName;
    private ArrayList<SectionsListAdapter> allSectionAdapters = new ArrayList<>();
    private Context context;

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
                        @Override
                        public void onSectionClick(String sectionName)
                        {
                            _onSectionClick(supermarketName, sectionName);

                            if (listener != null)
                            {
                                listener.onSupermarketClick(new Supermarket(supermarketName, sectionName));
                            }
                            notifyDataSetChanged();
                        }
                    }
            );

            allSectionAdapters.add(holder.sectionsAdapter);
            holder.recyclerSections.setAdapter(holder.sectionsAdapter);
        }
        else
        {
            holder.sectionsAdapter.updateData(
                    supermarkets.get(supermarketName)
            );
        }

        holder.sectionsAdapter.updateSelectedSection(selectedSupermarketName, selectedSectionName);
        holder.tvSupermarketName.setText(supermarketName);
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

    public void updateData(Map<String, ArrayList<String>> newSupermarkets) {
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

    private void _onSectionClick(String supermarketName, String sectionName)
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
}
