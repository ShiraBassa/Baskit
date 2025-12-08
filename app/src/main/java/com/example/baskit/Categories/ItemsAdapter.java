package com.example.baskit.Categories;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.List.ItemViewAlertDialog;
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
    private Activity activity;
    private Context context;

    public ItemsAdapter(ArrayList<Item> items,
                        OnItemClickListener listener,
                        UpperClassFunctions upperClassFns,
                        ItemsListHandler.EmptyCategoryCase emptyCategory,
                        Activity activity, Context context)
    {
        this.emptyCategory = emptyCategory;
        this.items = new ArrayList<>();
        this.listener = listener;
        this.upperClassFns = upperClassFns;
        this.activity = activity;
        this.context = context;

        for (Item item : items)
        {
            addItem(item);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        protected TextView tvName, tvQuantity, tvSupermarket, tvPrice;
        protected ImageButton btnUp, btnDown, btnCheckBox;
        protected ItemViewAlertDialog itemViewAlertDialog;

        public ViewHolder(View itemView)
        {
            super(itemView);

            tvName = itemView.findViewById(R.id.tv_item_name);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            btnUp = itemView.findViewById(R.id.btn_up);
            btnDown = itemView.findViewById(R.id.btn_down);
            btnCheckBox = itemView.findViewById(R.id.check_box);
            tvSupermarket = itemView.findViewById(R.id.tv_supermarket_name);
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
        holder.itemViewAlertDialog = new ItemViewAlertDialog(activity, context, upperClassFns, item, true);

        holder.tvName.setText(item.getName());
        holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.dark_teal));

        int quantity = item.getQuantity();
        holder.tvQuantity.setText(String.valueOf(quantity));
        holder.tvQuantity.setTextColor(ContextCompat.getColor(context, R.color.rich_mahogany));

        if (quantity == 1)
        {
            holder.btnDown.setBackgroundColor(ContextCompat.getColor(context, R.color.tan));
        }
        else
        {
            holder.btnDown.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        }

        if (item.hasSupermarket())
        {
            holder.tvSupermarket.setText("(" + item.getSupermarket() + ")");
            holder.tvSupermarket.setTextColor(ContextCompat.getColor(context, R.color.rich_mahogany));
            holder.tvPrice.setText(Double.toString(item.getPrice()));
            holder.tvPrice.setTextColor(ContextCompat.getColor(context, R.color.rich_mahogany));

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
            holder.btnDown.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));

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
                    holder.btnDown.setBackgroundColor(ContextCompat.getColor(context, R.color.tan));
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
        holder.btnCheckBox.setColorFilter(ContextCompat.getColor(context, R.color.dark_teal));

        holder.tvName.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                holder.itemViewAlertDialog.show();
            }
        });

        holder.btnUp.setColorFilter(ContextCompat.getColor(context, R.color.dark_teal));
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
