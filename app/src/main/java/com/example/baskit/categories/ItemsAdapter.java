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

        if (items == null)
        {
            return;
        }

        for (Item item : items)
        {
            if (item == null)
            {
                continue;
            }

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
        if (position < 0 || position >= items.size())
        {
            return;
        }

        if (activity != null && (activity.isFinishing() || activity.isDestroyed()))
        {
            return;
        }

        Item item = items.get(position);

        if (item == null)
        {
            return;
        }

        holder.itemViewAlertDialog = new ItemViewAlertDialog(activity, context, upperClassFns, item, true);
        holder.itemView.setOnDragListener(null);

        String decodedName = item.getDecodedName();

        holder.tvName.setText(
                decodedName != null && !decodedName.isBlank()
                        ? decodedName
                        : Baskit.getAppStr(R.string.unnamed_item)
        );

        int quantity = item.getQuantity();
        holder.tvQuantity.setText(String.valueOf(quantity));

        if (quantity == 1)
        {
            holder.btnDown.setColorFilter(Baskit.getAppColor(context, androidx.appcompat.R.attr.colorError));
            holder.btnDown.setAlpha(0.92f);
        }
        else
        {
            holder.btnDown.setColorFilter(Baskit.getAppColor(context, android.R.attr.colorPrimary));
            holder.btnDown.setAlpha(1f);
        }

        if (!item.isUnassignedToSupermarket())
        {
            double total = item.getTotal();

            if (Double.isNaN(total) || Double.isInfinite(total))
            {
                total = 0.0;
            }

            holder.tvPrice.setText(
                    Baskit.getTotalDisplayString(total, item.isPriceKnown(), false, false)
            );
            holder.tvPrice.setTextColor(Baskit.getAppColor(context, androidx.appcompat.R.attr.colorPrimary));
            holder.tvPrice.setVisibility(View.VISIBLE);
        }
        else
        {
            holder.tvPrice.setVisibility(View.GONE);
        }

        boolean checked = item.isChecked();

        if (checked)
        {
            holder.btnCheckBox.setImageResource(R.drawable.ic_check_box_checked);
            holder.itemView.setAlpha(0.58f);
        }
        else
        {
            holder.btnCheckBox.setImageResource(R.drawable.ic_check_box_unchecked);
            holder.itemView.setAlpha(0.98f);
        }

        holder.btnUp.setEnabled(!checked);
        holder.btnDown.setEnabled(!checked);
        holder.tvName.setEnabled(!checked);
        holder.dragHandle.setEnabled(!checked);

        holder.btnUp.setAlpha(checked ? 0.45f : 1f);
        holder.btnDown.setAlpha(checked ? 0.45f : 1f);
        holder.tvName.setAlpha(checked ? 0.7f : 1f);
        holder.dragHandle.setAlpha(checked ? 0.35f : 1f);

        setItemButtons(holder, item);
    }

    @Override
    public int getItemCount()
    {
        return items.size();
    }

    private void setItemButtons(ViewHolder holder, Item item)
    {
        if (holder == null || item == null)
        {
            return;
        }

        holder.btnUp.setOnClickListener(v ->
        {
            int currPosition = holder.getAdapterPosition();
            if (currPosition == RecyclerView.NO_POSITION) return;
            if (item.isChecked()) return;

            if (activity != null && (activity.isFinishing() || activity.isDestroyed()))
            {
                return;
            }

            item.setQuantity(item.raiseQuantity());
            holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
            holder.tvPrice.setText(
                    Baskit.getTotalDisplayString(item.getTotal(), item.isPriceKnown(), false, false)
            );
            holder.btnDown.setColorFilter(Baskit.getAppColor(context, android.R.attr.colorPrimary));
            holder.btnDown.setAlpha(1f);

            if (upperClassFns != null)
            {
                upperClassFns.updateItemCategory(item);
            }
        });

        holder.btnDown.setOnClickListener(v ->
        {
            int currPosition = holder.getAdapterPosition();
            if (currPosition == RecyclerView.NO_POSITION) return;
            if (item.isChecked()) return;

            if (activity != null && (activity.isFinishing() || activity.isDestroyed()))
            {
                return;
            }

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
                    holder.btnDown.setColorFilter(Baskit.getAppColor(context, androidx.appcompat.R.attr.colorError));
                    holder.btnDown.setAlpha(0.92f);
                }

                holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
                holder.tvPrice.setText(
                        Baskit.getTotalDisplayString(item.getTotal(), item.isPriceKnown(), false, false)
                );

                if (upperClassFns != null)
                {
                    upperClassFns.updateItemCategory(item);
                }
            }
        });

        holder.btnCheckBox.setOnClickListener(v ->
        {
            int currPosition = holder.getAdapterPosition();

            if (currPosition == RecyclerView.NO_POSITION)
            {
                return;
            }

            if (activity != null && (activity.isFinishing() || activity.isDestroyed()))
            {
                return;
            }

            item.setChecked(!item.isChecked());

            if (item.isChecked())
            {
                holder.btnCheckBox.setImageResource(R.drawable.ic_check_box_checked);
                holder.itemView.setAlpha(0.58f);
            }
            else
            {
                holder.btnCheckBox.setImageResource(R.drawable.ic_check_box_unchecked);
                holder.itemView.setAlpha(0.98f);
            }

            if (upperClassFns != null)
            {
                upperClassFns.updateItemCategory(item);
            }
        });

        holder.tvName.setOnClickListener(v ->
        {
            if (holder.itemViewAlertDialog == null)
            {
                return;
            }

            if (activity != null && (activity.isFinishing() || activity.isDestroyed()))
            {
                return;
            }

            holder.itemViewAlertDialog.show(item);
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addItem(Item item)
    {
        if (item == null)
        {
            return;
        }
        if (!items.contains(item))
        {
            items.add(item);
            sortItems();
            notifyDataSetChanged();
        }
    }

    public void removeItem(Item item)
    {
        if (item == null)
        {
            return;
        }
        int position = items.indexOf(item);

        // Remove from adapter
        if (position >= 0 && position < items.size())
        {
            items.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, items.size());
        }

        if (upperClassFns != null)
        {
            upperClassFns.removeItemCategory(item);
            upperClassFns.updateCategory();
        }
    }

    private void sortItems()
    {
        items.removeIf(item -> item == null || item.getId() == null);
        items.sort((a, b) ->
        {
            String idA = a != null ? a.getId() : "";
            String idB = b != null ? b.getId() : "";

            return idA.compareTo(idB);
        });
    }
}
