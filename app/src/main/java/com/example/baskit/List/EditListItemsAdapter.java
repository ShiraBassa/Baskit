package com.example.baskit.List;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.ActionMenuItem;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.Categories.ItemsAdapter;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import java.util.ArrayList;

public class EditListItemsAdapter extends RecyclerView.Adapter<EditListItemsAdapter.ViewHolder>
{
    protected ArrayList<Item> items;
    private Supermarket supermarket;
    private OnItemMovedListener listener;
    Activity activity;
    Context context;
    ItemsAdapter.UpperClassFunctions upperClassFns;

    public interface OnItemMovedListener
    {
        void onItemMoved(Item item, Supermarket from, Supermarket to);
    }

    public EditListItemsAdapter(ArrayList<Item> items, Supermarket supermarket, OnItemMovedListener listener,
                                Activity activity, Context context, ItemsAdapter.UpperClassFunctions upperClassFns) {
        this.items = items;
        this.supermarket = supermarket;
        this.listener = listener;
        this.activity = activity;
        this.context = context;
        this.upperClassFns = upperClassFns;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        LinearLayout layoutQuantity;
        protected TextView tvName, tvSupermarket, tvPrice;
        protected ImageButton btnCheckBox;
        protected ItemViewAlertDialog itemViewAlertDialog;

        public ViewHolder(View itemView)
        {
            super(itemView);

            tvName = itemView.findViewById(R.id.tv_item_name);
            layoutQuantity = itemView.findViewById(R.id.layout_quantity);
            btnCheckBox = itemView.findViewById(R.id.check_box);
            tvSupermarket = itemView.findViewById(R.id.tv_supermarket_name);
            tvPrice = itemView.findViewById(R.id.tv_price);
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
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        Item item = items.get(position);
        holder.itemViewAlertDialog = new ItemViewAlertDialog(activity, context, upperClassFns, item, false);

        holder.tvName.setText(item.getName());
        holder.layoutQuantity.setVisibility(View.GONE);
        holder.tvSupermarket.setVisibility(View.GONE);

        if (item.hasSupermarket())
        {
            holder.tvPrice.setText(Double.toString(item.getPrice()));
            holder.tvPrice.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnLongClickListener(v -> {
            ClipData data = ClipData.newPlainText("", "");
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            v.startDragAndDrop(data, shadowBuilder, item, 0);
            return true;
        });

        holder.tvName.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                holder.itemViewAlertDialog.show();
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return items.size();
    }

    private void setItemSupermarket(Item item, Supermarket supermarket)
    {
        item.setSupermarket(supermarket);
        notifyDataSetChanged();
    }
}
