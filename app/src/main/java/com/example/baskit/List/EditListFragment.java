package com.example.baskit.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.Categories.ItemsAdapter;
import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.R;

import java.util.ArrayList;

public class EditListFragment extends DialogFragment
{

    private Activity activity;
    private Context context;
    private View fragmentView;
    private TextView tvTotal, tvName;
    private ImageButton btnCancel;
    private Button btnSave;

    private ArrayList<Item> items;
    private List list;
    private FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();

    private RecyclerView recyclerSupermarkets;
    private EditListSupermarketsAdapter supermarketsAdapter;
    private String title;

    public EditListFragment(Activity activity, Context context, ArrayList<Item> items, List list, String title)
    {
        this.activity = activity;
        this.context = context;

        this.items = new ArrayList<>();

        for (Item item : items)
        {
            this.items.add(item.clone());
        }

        this.list = list;
        this.title = title;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    )
    {
        fragmentView = inflater.inflate(R.layout.fragment_edit_list, container, false);

        btnCancel = fragmentView.findViewById(R.id.btn_cancel);
        btnSave = fragmentView.findViewById(R.id.btn_save);
        tvTotal = fragmentView.findViewById(R.id.tv_total);
        tvName = fragmentView.findViewById(R.id.tv_title);

        showTotal();
        tvTotal.setVisibility(View.VISIBLE);
        tvName.setText(title);
        tvName.setVisibility(View.VISIBLE);

        recyclerSupermarkets = fragmentView.findViewById(R.id.recycler_supermarkets);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveAndClose());

        supermarketsAdapter = new EditListSupermarketsAdapter(
                items,
                activity,
                context,
                new ItemsAdapter.UpperClassFunctions()
                {
                    @Override
                    public void updateItemCategory(Item item)
                    {
                        dbHandler.updateItem(list, item);
                        showTotal();
                    }

                    @Override
                    public void removeItemCategory(Item item)
                    {
                        dbHandler.removeItem(list, item);
                        showTotal();
                    }
                }
        );

        recyclerSupermarkets.setLayoutManager(new LinearLayoutManager(context));
        recyclerSupermarkets.setAdapter(supermarketsAdapter);

        return fragmentView;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        //fullscreen
        if (getDialog() != null && getDialog().getWindow() != null)
        {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    private void saveAndClose()
    {
        ArrayList<Item> newItems = supermarketsAdapter.getNewItems();
        dbHandler.updateItems(list, newItems);
        dismiss();
    }

    private void showTotal()
    {
        tvTotal.setText("סך הכל: " + Double.toString(list.getTotal()));
    }
}