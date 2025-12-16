package com.example.baskit.Home;

import static com.example.baskit.Firebase.FBRefs.refUsers;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Firebase.FirebaseAuthHandler;
import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.Login.LoginActivity;
import com.example.baskit.MainComponents.List;
import com.example.baskit.List.ListActivity;
import com.example.baskit.MainComponents.Request;
import com.example.baskit.MainComponents.User;
import com.example.baskit.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
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
    String inviteCode;

    private ActivityResultLauncher<Intent> loginLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>()
                    {
                        @Override
                        public void onActivityResult(ActivityResult result)
                        {
                            if (result.getResultCode() == Activity.RESULT_OK)
                            {
                                user = authHandler.getUser();

                                new Thread(() -> {
                                    try {
                                        APIHandler.getInstance().preload();
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    runOnUiThread(() -> {
                                        setContentView(R.layout.activity_home);
                                        init();
                                    });
                                }).start();

                                sendJoinRequest(inviteCode);
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        authHandler = FirebaseAuthHandler.getInstance();
        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data != null)
        {
            String scheme = data.getScheme();
            String host = data.getHost();
            String path = data.getPath();
            inviteCode = data.getQueryParameter("inviteCode");

            if ("https".equals(scheme) && "www.baskit.com".equals(host) && "/joinlist".equals(path) && inviteCode != null)
            {
                Intent loginIntent = new Intent(this, LoginActivity.class);
                loginIntent.putExtra("fromLink", true);
                loginLauncher.launch(loginIntent);
            }
        }
        else
        {
            user = authHandler.getUser();

            new Thread(() ->
            {
                try {
                    APIHandler.getInstance().preload();
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                runOnUiThread(() ->
                {
                    setContentView(R.layout.activity_home);
                    init();
                });
            }).start();
        }
    }

    private void sendJoinRequest(String invitationCode)
    {
        String listId = new String(Base64.decode(invitationCode, Base64.NO_WRAP), StandardCharsets.UTF_8);

        dbHandler.sendJoinRequest(listId, user);
        Toast.makeText(this, "Waiting for approval...", Toast.LENGTH_SHORT).show();
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

        if (user != null) {
            tvTitle.setText("היי " + user.getName());
        }
        else
        {
            tvTitle.setText("היי"); // fallback
            Log.w("HomeActivity", "User is null in init()");
            return;
        }

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

        dbHandler.getListNames(user, new FirebaseDBHandler.GetListNamesCallback()
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
                String listId = user.getListIDs().get(position);

                dbHandler.getList(listId, new FirebaseDBHandler.GetListCallback()
                {
                    @Override
                    public void onListFetched(List newList)
                    {
                        dbHandler.removeList(newList);
                        user.removeList(listId);
                    }

                    @Override
                    public void onError(String error) {}
                });
            }
        });

        listsRecycler.setAdapter(listsGridAdapter);
        listsRecycler.setVisibility(View.VISIBLE);

        dbHandler.listenToListNames(user, new FirebaseDBHandler.GetListNamesListenerCallback()
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
        list.addUser(user.getId());

        dbHandler.addList(list, user);
    }
}