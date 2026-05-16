package com.example.baskit.categories;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.Baskit;
import com.example.baskit.main_components.Item;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.Comparator;

@SuppressWarnings("deprecation")
public class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ViewHolder>
{
    protected final ArrayList<Item> items;

    protected final Activity activity;
    protected final Context context;
    protected final UpperClassFunctions upperClassFns;

    public interface UpperClassFunctions
    {
        void updateItemCategory(Item item);
        void removeItemCategory(Item item);
        void updateCategory();
        default void collapseAllSupermarkets() {}
        default void expandAllSupermarkets() {}
    }

    public ItemsAdapter(ArrayList<Item> items,
                        UpperClassFunctions upperClassFns,
                        Activity activity, Context context)
    {
        this.items = new ArrayList<>();
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
        public final TextView tvName;
        public final TextView tvQuantity;
        public final TextView tvPrice;
        public final ImageButton btnUp;
        public final ImageButton btnDown;
        public final ImageButton btnCheckBox;
        public final ImageButton dragHandle;
        public ItemViewAlertDialog itemViewAlertDialog;
        public final View spacer;

        public ViewHolder(View itemView)
        {
            super(itemView);

            tvName = itemView.findViewById(R.id.tv_item_name);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            btnUp = itemView.findViewById(R.id.btn_up);
            btnDown = itemView.findViewById(R.id.btn_down);
            btnCheckBox = itemView.findViewById(R.id.check_box);
            tvPrice = itemView.findViewById(R.id.tv_price);
            dragHandle = itemView.findViewById(R.id.drag_handle);
            spacer = itemView.findViewById(R.id.spacer);
        }
    }

    @NonNull
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
        holder.itemView.setOnDragListener(null);

        holder.tvName.setText(item.getDecodedName());
        holder.tvName.setTextColor(Baskit.getAppColor(context, android.R.attr.colorPrimary));

        int quantity = item.getQuantity();
        holder.tvQuantity.setText(String.valueOf(quantity));
        holder.tvQuantity.setTextColor(Baskit.getAppColor(context, com.google.android.material.R.attr.colorSecondary));

        if (quantity == 1)
        {
            holder.btnDown.setColorFilter(Baskit.getAppColor(context, com.google.android.material.R.attr.colorTertiary));
        }
        else
        {
            holder.btnDown.setColorFilter(Baskit.getAppColor(context, com.google.android.material.R.attr.colorSecondary));
        }

        if (!item.isUnassignedToSupermarket())
        {
            holder.tvPrice.setText(Baskit.getTotalDisplayString(item.getTotal(), item.isPriceKnown(), false, false));
            holder.tvPrice.setTextColor(Baskit.getAppColor(context, android.R.attr.colorPrimary));
            holder.tvPrice.setVisibility(View.VISIBLE);
        }
        else
        {
            holder.tvPrice.setVisibility(View.GONE);
        }

        if (item.isChecked())
        {
            holder.btnCheckBox.setImageResource(R.drawable.ic_check_box_checked);
            holder.itemView.setAlpha(0.5f);
        }
        else
        {
            holder.btnCheckBox.setImageResource(R.drawable.ic_check_box_unchecked);
            holder.itemView.setAlpha(1f);
        }

        setItemButtons(holder, item);
    }

    @Override
    public int getItemCount()
    {
        return items.size();
    }

    private void setItemButtons(ViewHolder holder, Item item)
    {
        holder.btnUp.setOnClickListener(v ->
        {
            int currPosition = holder.getAdapterPosition();
            if (currPosition == RecyclerView.NO_POSITION) return;

            item.setQuantity(item.raiseQuantity());
            holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
            holder.btnDown.setColorFilter(Baskit.getAppColor(context, com.google.android.material.R.attr.colorSecondary));

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
                removeItem(item);
            }
            else
            {
                if (quantity == 1)
                {
                    holder.btnDown.setColorFilter(Baskit.getAppColor(context, com.google.android.material.R.attr.colorTertiary));
                }

                holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
                upperClassFns.updateItemCategory(item);
            }
        });

        holder.btnCheckBox.setOnClickListener(v ->
        {
            item.setChecked(!item.isChecked());

            if (item.isChecked())
            {
                holder.btnCheckBox.setImageResource(R.drawable.ic_check_box_checked);
                holder.itemView.setAlpha(0.5f);
            }
            else
            {
                holder.btnCheckBox.setImageResource(R.drawable.ic_check_box_unchecked);
                holder.itemView.setAlpha(1f);
            }

            upperClassFns.updateItemCategory(item);
        });

        holder.tvName.setOnClickListener(v -> holder.itemViewAlertDialog.show(item));
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addItem(Item item)
    {
        if (!items.contains(item))
        {
            items.add(item);
            sortItems();
            notifyDataSetChanged();
        }
    }

    public void removeItem(Item item)
    {
        int position = items.indexOf(item);

        // Remove from adapter
        if (position >= 0 && position < items.size())
        {
            items.remove(position);
            notifyItemRemoved(position);
        }

        if (upperClassFns != null)
        {
            upperClassFns.removeItemCategory(item);
        }
    }

    private void sortItems()
    {
        items.sort(Comparator.comparing(Item::getId));
    }
}
