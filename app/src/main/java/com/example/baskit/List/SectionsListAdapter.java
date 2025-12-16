package com.example.baskit.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Map;

public class SectionsListAdapter extends RecyclerView.Adapter<SectionsListAdapter.ViewHolder>
{
    ArrayList<String> sections = null;
    Map<String, Double> sectionsWithPrices = null;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private OnSectionClickListener listener = null;

    private String selectedSupermarket = null;
    private String selectedSection = null;
    String supermarketName;

    public interface OnSectionClickListener
    {
        void onSectionClick(String sectionName);
    }

    private void setOnSectionClickListener(OnSectionClickListener listener)
    {
        this.listener = listener;
    }

    public SectionsListAdapter() {}

    public void fromSections(ArrayList<String> sections)
    {
        this.sections = sections;
    }

    public void fromSections(String supermarketName, ArrayList<String> sections, OnSectionClickListener onSupermarketClickListener)
    {
        this.supermarketName = supermarketName;
        this.sections = sections;
        this.listener = onSupermarketClickListener;
    }

    public void fromSectionsWithPrices(String supermarketName, Map<String, Double> sectionsWithPrices, OnSectionClickListener onSectionClickListener) {
        this.supermarketName = supermarketName;
        this.sectionsWithPrices = sectionsWithPrices;
        this.listener = onSectionClickListener;
    }

    public void setSections(ArrayList<String> sections)
    {
        this.sections = sections;
    }

    public void setSectionsWithPrices(Map<String, Double> sectionsWithPrices)
    {
        this.sectionsWithPrices = sectionsWithPrices;
    }

    public void updateSelectedSection(String supermarket, String section) {
        this.selectedSupermarket = supermarket;
        this.selectedSection = section;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        protected TextView tvSectionName, tvPrice;

        public ViewHolder(View itemView)
        {
            super(itemView);

            tvSectionName = itemView.findViewById(R.id.tv_section_name);
            tvPrice = itemView.findViewById(R.id.tv_price);
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
        String sectionName;

        if (sectionsWithPrices != null)
        {
            ArrayList<String> sectionNames = new ArrayList<>(sectionsWithPrices.keySet());
            sectionName = sectionNames.get(position);

            holder.tvPrice.setText(Double.toString(sectionsWithPrices.get(sectionName)) + "₪");
            holder.tvPrice.setVisibility(View.VISIBLE);
        }
        else
        {
            sectionName = sections.get(position);
        }

        if (sections != null && sectionName.equals(selectedSection) &&
                selectedSupermarket != null && supermarketName.equals(selectedSupermarket))
        {
            holder.itemView.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.tan));
        }
        else if (sectionsWithPrices != null && sectionName.equals(selectedSection) &&
                selectedSupermarket != null && supermarketName.equals(selectedSupermarket))
        {
            holder.itemView.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.tan));
        }
        else
        {
            holder.itemView.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.white_smoke));
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
                String sectionNameClick;

                if (sectionsWithPrices != null)
                {
                    ArrayList<String> sectionNames = new ArrayList<>(sectionsWithPrices.keySet());
                    sectionNameClick = sectionNames.get(adapterPos);
                }
                else
                {
                    sectionNameClick = sections.get(adapterPos);
                }

                listener.onSectionClick(sectionNameClick);
            }
        });

        holder.tvSectionName.setText(sectionName);
    }

    @Override
    public int getItemCount()
    {
        if (sectionsWithPrices != null)
        {
            return sectionsWithPrices.size();
        }
        else if (sections != null)
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

    public void updateDataWithPrices(Map<String, Double> newSectionsWithPrices)
    {
        this.sectionsWithPrices = newSectionsWithPrices;
    }
}
