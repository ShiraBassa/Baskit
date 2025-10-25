package com.example.baskit.Home;

import static com.example.baskit.Firebase.FBRefs.refUsers;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Firebase.FirebaseAuthHandler;
import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.MainComponents.List;
import com.example.baskit.List.ListActivity;
import com.example.baskit.MainComponents.User;
import com.example.baskit.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeActivity extends AppCompatActivity
{
    RecyclerView listsRecycler;
    HomeGridAdapter listsGridAdapter;

    AlertDialog.Builder adb;
    LinearLayout adLayout;
    AlertDialog adCreateList;
    Button adBtnCreate, btnCreateList;
    ImageButton adBtnCancel, btnSettings;
    EditText adEtName;
    TextView tvTitle;
    FirebaseAuthHandler authHandler;
    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        authHandler = FirebaseAuthHandler.getInstance();
        user = authHandler.getUser();

        init();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        user = authHandler.getUser();
    }

    private void init()
    {
        btnSettings = findViewById(R.id.btn_settings);
        btnCreateList = findViewById(R.id.btn_create_list);
        tvTitle = findViewById(R.id.tv_title);

        tvTitle.setText("היי " + authHandler.getUser().getName());

        dbHandler.listenToUserName(user, new FirebaseDBHandler.GetUserNameCallback()
        {
            @Override
            public void onUserNameFetched(String username)
            {
                user.setName(username);
                tvTitle.setText("היי " + authHandler.getUser().getName());
            }
        });

        createListAlertDialog();

        listsRecycler = findViewById(R.id.lists_grid);

        dbHandler.getListNames(user.getListIDs(), new FirebaseDBHandler.GetListNamesCallback()
        {
            @Override
            public void onNamesFetched(ArrayList<String> listNames)
            {
                setListsRecycler(listNames);
            }
        });

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

    private void setListsRecycler(ArrayList<String> listNamesRecycler)
    {
        listsRecycler.setLayoutManager(new GridLayoutManager(this, 3)); // 3 columns

        listsGridAdapter = new HomeGridAdapter(listNamesRecycler, new HomeGridAdapter.OnItemClickListener()
        {
            @Override
            public void onItemClick(int position)
            {
                Intent intent = new Intent(HomeActivity.this, ListActivity.class);
                intent.putExtra("listId", user.getListIDs().get(position));

                startActivity(intent);
            }

            @Override
            public void onItemLongClick(int position)
            {
                dbHandler.removeList(user.getListIDs().get(position), user);
            }
        });

        listsRecycler.setAdapter(listsGridAdapter);
        listsRecycler.setVisibility(View.VISIBLE);

        dbHandler.listenToListNames(user.getId(), new FirebaseDBHandler.GetListNamesListenerCallback()
        {
            @Override
            public void onInfoFetched(ArrayList<String> listNames)
            {
                if (listsGridAdapter != null)
                {
                    listsGridAdapter.updateList(listNames);
                }
            }
        });
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
        List list = new List(dbHandler.getUniqueId(), name);
        dbHandler.addList(list, user);
    }
}