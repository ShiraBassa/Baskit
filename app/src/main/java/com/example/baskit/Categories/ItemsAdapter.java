package com.example.baskit.Categories;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.MainComponents.Item;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Collections;

public class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ViewHolder>
{
    public interface OnItemClickListener
    {
        void notifyCheckBox(Item item);
    }

    public interface UpperClassFunctions
    {
        void updateItemCategory(Item item);
        void removeItemCategory(Item item);
    }

    ItemsListHandler.EmptyCategoryCase emptyCategory;
    protected ArrayList<Item> items;
    protected OnItemClickListener listener;
    protected UpperClassFunctions upperClassFns;

    public ItemsAdapter(ArrayList<Item> items,
                        OnItemClickListener listener,
                        UpperClassFunctions upperClassFns,
                        ItemsListHandler.EmptyCategoryCase emptyCategory)
    {
        this.emptyCategory = emptyCategory;
        this.items = new ArrayList<>();
        this.listener = listener;
        this.upperClassFns = upperClassFns;

        for (Item item : items)
        {
            addItem(item);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        protected TextView tvName, tvQuantity, tvSupermarket, tvPrice;
        protected ImageButton btnUp, btnDown, btnCheckBox;

        public ViewHolder(View itemView)
        {
            super(itemView);

            tvName = itemView.findViewById(R.id.tv_item_name);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            btnUp = itemView.findViewById(R.id.btn_up);
            btnDown = itemView.findViewById(R.id.btn_down);
            btnCheckBox = itemView.findViewById(R.id.check_box);
            tvSupermarket = itemView.findViewById(R.id.tv_supermarket);
            tvPrice = itemView.findViewById(R.id.tv_price);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        Item item = items.get(position);

        holder.tvName.setText(item.getName());

        int quantity = item.getQuantity();
        holder.tvQuantity.setText(String.valueOf(quantity));

        if (quantity == 1)
        {
            holder.btnDown.setBackgroundColor(Color.LTGRAY);
        }
        else
        {
            holder.btnDown.setBackgroundColor(Color.TRANSPARENT);
        }

        if (item.hasSupermarket())
        {
            holder.tvSupermarket.setText("(" + item.getSupermarket() + ")");
            holder.tvPrice.setText(Double.toString(item.getPrice()));

            holder.tvSupermarket.setVisibility(View.VISIBLE);
            holder.tvPrice.setVisibility(View.VISIBLE);
        }

        setItemButtons(holder, item, position);
    }

    private void setItemButtons(ViewHolder holder, Item item, int position)
    {
        holder.btnUp.setOnClickListener(v ->
        {
            int currPosition = holder.getAdapterPosition();
            if (currPosition == RecyclerView.NO_POSITION) return;

            item.setQuantity(item.raiseQuantity());
            holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
            holder.btnDown.setBackgroundColor(Color.TRANSPARENT);

            upperClassFns.updateItemCategory(item);
        });

        holder.btnDown.setOnClickListener(v ->
        {
            int currPosition = holder.getAdapterPosition(); // always current
            if (currPosition == RecyclerView.NO_POSITION) return; // safety

            if (item.getQuantity() <= 0)
            {
                return;
            }

            int quantity = item.lowerQuantity();

            if (quantity == 0)
            {
                removeItem(currPosition);
                upperClassFns.removeItemCategory(item);
            }
            else
            {
                if (quantity == 1)
                {
                    holder.btnDown.setBackgroundColor(Color.LTGRAY);
                }

                holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
                upperClassFns.updateItemCategory(item);
            }
        });

        holder.btnCheckBox.setOnClickListener(v ->
        {
            listener.notifyCheckBox(item);
            upperClassFns.updateItemCategory(item);
        });
    }

    public void addItem(Item item)
    {
        if (!items.contains(item))
        {
            items.add(item);
            sortItems();
            notifyDataSetChanged();
        }
    }

    private void sortItems()
    {
        Collections.sort(items, (i1, i2) -> i1.getId().compareTo(i2.getId()));
    }

    public void removeItem(int position)
    {
        if (position >= 0 && position < items.size())
        {
            items.remove(position);
            notifyItemRemoved(position);
        }

        if (items.isEmpty())
        {
            emptyCategory.onFinishedCategory();
        }
    }

    public void removeItem(Item item)
    {
        int position = items.indexOf(item);

        if (position != -1)
        {
            removeItem(position);
        }
    }

    public void clearAll()
    {
        if (!items.isEmpty())
        {
            items.clear();
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount()
    {
        return items.size();
    }

    public void updateItems(ArrayList<Item> newItems)
    {
        items.clear();
        items.addAll(newItems);
        sortItems();
        notifyDataSetChanged();
    }
}
