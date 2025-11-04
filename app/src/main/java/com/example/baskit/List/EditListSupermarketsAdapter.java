package com.example.baskit.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.ActionMenuItem;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Categories.ItemsAdapter;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

public class EditListSupermarketsAdapter extends RecyclerView.Adapter<EditListSupermarketsAdapter.ViewHolder>
{
    ArrayList<Item> items;
    ArrayList<Supermarket> supermarkets;
    protected Map<Supermarket, ArrayList<Item>> itemsBySupermarket;
    private Map<Supermarket, Boolean> expandedStates;
    private APIHandler apiHandler = APIHandler.getInstance();
    private Map<String, Map<String, Map<String, Double>>> itemsPrices = new HashMap<>();
    Activity activity;
    Context context;
    ItemsAdapter.UpperClassFunctions upperClassFns;

    public EditListSupermarketsAdapter(ArrayList<Item> items, Activity activity, Context context, ItemsAdapter.UpperClassFunctions upperClassFns)
    {
        itemsBySupermarket = new HashMap<>();
        supermarkets = new ArrayList<>();
        expandedStates = new HashMap<>();

        this.items = items;
        this.activity = activity;
        this.context = context;
        this.upperClassFns = upperClassFns;

        new Thread(() ->
        {
            getPrices();

            try
            {
                organizeItems();
            }
            catch (JSONException | IOException e)
            {
                throw new RuntimeException(e);
            }

            new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
        }).start();
    }

    private void organizeItems() throws JSONException, IOException
    {
        HashSet<Supermarket> seen = new HashSet<>();

        for(Item item : items)
        {
            Supermarket sm = item.getSupermarket();

            if(!seen.contains(sm))
            {
                seen.add(sm);
                supermarkets.add(sm);
                itemsBySupermarket.put(sm, new ArrayList<>());
                expandedStates.put(sm, false);
            }

            itemsBySupermarket.get(sm).add(item);
        }

        for (Supermarket supermarket : apiHandler.getSupermarkets())
        {
            if (!seen.contains(supermarket))
            {
                seen.add(supermarket);
                supermarkets.add(supermarket);
                itemsBySupermarket.put(supermarket, new ArrayList<>());
                expandedStates.put(supermarket, false);
            }
        }
    }

    private void getPrices()
    {
        for (Item item : items)
        {
            try
            {
                Map<String, Map<String, Double>> itemPriceData = apiHandler.getItemPricesByCode(item.getId().split("item_")[1]);
                itemsPrices.put(item.getId(), itemPriceData);

                if (!item.hasSupermarket())
                {
                    double lowest = Double.MAX_VALUE;
                    Supermarket lowestSupermarket = null;

                    for (Map.Entry<String, Map<String, Double>> SupermarketEntry : itemPriceData.entrySet())
                    {
                        String supermarket = SupermarketEntry.getKey();

                        for (Map.Entry<String, Double> sectionEntry : SupermarketEntry.getValue().entrySet())
                        {
                            String section = sectionEntry.getKey();
                            Double price = sectionEntry.getValue();

                            if (price < lowest)
                            {
                                lowest = price;
                                lowestSupermarket = new Supermarket(supermarket, section);
                            }
                        }
                    }

                    item.setPrice(lowest);
                    item.setSupermarket(lowestSupermarket);
                }
            }
            catch (JSONException | IOException ignored) {}
        }
    }

    private void updateItemPriceForSupermarket(Item item, Supermarket supermarket) {
        Map<String, Map<String, Double>> itemPriceData = itemsPrices.get(item.getId());
        if (itemPriceData != null) {
            Map<String, Double> sections = itemPriceData.get(supermarket.getSupermarket());
            if (sections != null && sections.containsKey(supermarket.getSection())) {
                item.setPrice(sections.get(supermarket.getSection()));
            } else {
                // If that exact section isn't found, try any section under that supermarket
                if (!sections.isEmpty()) {
                    item.setPrice(sections.values().iterator().next());
                }
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        protected TextView tvSupermarket;
        protected ImageButton btnExpand;
        protected RecyclerView recyclerItems;

        public ViewHolder(View itemView)
        {
            super(itemView);

            tvSupermarket = itemView.findViewById(R.id.tv_supermarket_name);
            btnExpand = itemView.findViewById(R.id.btn_expand);
            recyclerItems = itemView.findViewById(R.id.recycler_items);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.edit_list_supermarket, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        Supermarket supermarket = supermarkets.get(position);
        holder.tvSupermarket.setText(supermarket.toString());

        ArrayList<Item> items = itemsBySupermarket.get(supermarket);
        EditListItemsAdapter itemsAdapter = new EditListItemsAdapter(items, supermarket, listener, activity, context, upperClassFns);

        holder.recyclerItems.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.recyclerItems.setAdapter(itemsAdapter);

        boolean isExpanded = expandedStates.get(supermarket);
        holder.recyclerItems.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.btnExpand.setRotation(isExpanded ? 180 : 0);

        holder.btnExpand.setOnClickListener(v ->
        {
            expandedStates.put(supermarket, !isExpanded);
            notifyItemChanged(position);
        });

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // not needed
            }
        });
        itemTouchHelper.attachToRecyclerView(holder.recyclerItems);

        holder.itemView.setOnDragListener((v, event) ->
        {
            switch (event.getAction())
            {
                case DragEvent.ACTION_DRAG_STARTED:
                    new Handler(Looper.getMainLooper()).post(() ->
                    {
                        for (Supermarket sm : expandedStates.keySet())
                        {
                            expandedStates.put(sm, false);
                        }
                        notifyDataSetChanged();
                    });

                    break;

                case DragEvent.ACTION_DRAG_ENTERED:
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.gray));
                    break;

                case DragEvent.ACTION_DRAG_EXITED:
                    holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                    break;

                case DragEvent.ACTION_DROP:
                    Item draggedItem = (Item) event.getLocalState();
                    Supermarket targetSupermarket = supermarket;
                    Supermarket fromSupermarket = draggedItem.getSupermarket();

                    if (!fromSupermarket.equals(targetSupermarket)) {
                        listener.onItemMoved(draggedItem, fromSupermarket, targetSupermarket);
                    }

                    holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                    break;

                case DragEvent.ACTION_DRAG_ENDED:
                    holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                    break;
            }

            return true;
        });
    }

    @Override
    public int getItemCount()
    {
        return supermarkets.size();
    }

    public ArrayList<Item> getNewItems()
    {
        return items;
    }

    private final EditListItemsAdapter.OnItemMovedListener listener = (item, from, to) -> {
        if (itemsBySupermarket.get(from) != null) {
            itemsBySupermarket.get(from).remove(item);
        }

        item.setSupermarket(to);
        updateItemPriceForSupermarket(item, to);

        if (itemsBySupermarket.get(to) == null) {
            itemsBySupermarket.put(to, new ArrayList<>());
        }
        itemsBySupermarket.get(to).add(item);
        notifyDataSetChanged();
    };
}
