package com.example.baskit.Home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.MainComponents.Category;
import com.example.baskit.DataRepository;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.List.ListActivity;
import com.example.baskit.R;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity
{
    ArrayList<List> lists;
    DataRepository data;
    RecyclerView listsRecycler;
    HomeGridAdapter listsGridAdapter;

    AlertDialog.Builder adb;
    LinearLayout adLayout;
    AlertDialog adCreateList;
    Button adBtnCreate, btnCreateList;
    ImageButton adBtnCancel, btnSettings;
    EditText adEtName;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        init();
    }

    private void init()
    {
        btnSettings = findViewById(R.id.btn_settings);
        btnCreateList = findViewById(R.id.btn_create_list);
        createListAlertDialog();

        data = DataRepository.getInstance();
        lists = data.getLists();

        lists.add(new List("1", "בית"));
        lists.add(new List("2", "חג"));
        lists.add(new List("3", "ראש השנה"));
        lists.add(new List("4", "פיקניק"));

        lists.get(0).addCategory(new Category("בשר"));
        lists.get(0).addCategory(new Category("מתוקים"));
        lists.get(1).addCategory(new Category("מוצרי חלב"));
        lists.get(1).getCategories().get(0).addItem(new Item("חלב"));
        lists.get(1).getCategories().get(0).addItem(new Item("יוגורט"));
        lists.get(1).addCategory(new Category("ירקות"));
        lists.get(1).addCategory(new Category("פירות"));

        listsRecycler = findViewById(R.id.lists_grid);

        setListsRecycler();
        setButtons();
    }

    private void setButtons()
    {
        btnSettings.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
            }
        });

        btnCreateList.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                adCreateList.show();
            }
        });
    }

    private void setListsRecycler()
    {
        listsRecycler.setLayoutManager(new GridLayoutManager(this, 3)); // 3 columns

        listsGridAdapter = new HomeGridAdapter(lists, new HomeGridAdapter.OnItemClickListener()
        {
            @Override
            public void onItemClick(int position)
            {
                data.refreshLists(lists);

                Intent intent = new Intent(HomeActivity.this, ListActivity.class);
                intent.putExtra("listId", lists.get(position).getId());

                startActivity(intent);
            }

            @Override
            public void onItemLongClick(int position)
            {
                data.removeList(lists.get(position));
                listsGridAdapter.remove(position);
            }
        });

        listsRecycler.setAdapter(listsGridAdapter);
    }

    private void createListAlertDialog()
    {
        adLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.alert_dialog_create_list, null);
        adBtnCancel = adLayout.findViewById(R.id.btn_cancel);
        adBtnCreate = adLayout.findViewById(R.id.btn_create);
        adEtName = adLayout.findViewById(R.id.et_name);

        adb = new AlertDialog.Builder(this);
        adb.setView(adLayout);

        adCreateList = adb.create();

        adBtnCancel.setOnClickListener(v -> adCreateList.dismiss());

        adBtnCreate.setOnClickListener(v ->
        {
            createList(adEtName.getText().toString());
            adCreateList.dismiss();
        });
    }

    private void createList(String name)
    {
        List list = new List(Integer.toString(lists.size()+1), name);
        lists.add(list);
        listsGridAdapter.add(list);
    }
}