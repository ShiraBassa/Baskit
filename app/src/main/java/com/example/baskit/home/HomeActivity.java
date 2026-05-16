package com.example.baskit.home;

import static com.example.baskit.Baskit.getAppColor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.text.LineBreaker;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
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
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.online_components.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.online_components.FirebaseAuthHandler;
import com.example.baskit.online_components.FirebaseDBHandler;
import com.example.baskit.login.LoginActivity;
import com.example.baskit.list.ListActivity;
import com.example.baskit.main_components.User;
import com.example.baskit.MasterActivity;
import com.example.baskit.R;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class HomeActivity extends MasterActivity
{
    User user;
    String inviteCode;

    FirebaseAuthHandler authHandler;
    final FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();

    AlertDialog.Builder adb;

    LinearLayout adLayout;
    AlertDialog adCreateList;
    Button adBtnCreate;
    MaterialButton btnCreateList;
    ImageButton adBtnCancel, btnSettings;
    EditText adEtName;
    TextView tvTitle;
    RecyclerView listsRecycler;
    GridAdapter listsGridAdapter;

    private final ActivityResultLauncher<Intent> loginLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                authHandler.checkCurrUser(new FirebaseAuthHandler.AuthCallback() {
                                    @Override
                                    public void onAuthSuccess() {
                                        user = authHandler.getUser();

                                        runWhenServerActive(() ->
                                        {
                                            try {
                                                APIHandler.getInstance().preload();
                                            } catch (JSONException | IOException e) {
                                                Log.e("HomeActivity", "Preload failed", e);
                                            }

                                            runOnUiThread(() ->
                                            {
                                                if (listsRecycler == null) {
                                                    setContentView(R.layout.activity_home);
                                                    init();
                                                }
                                            });
                                        });

                                        runWhenServerActive(() ->
                                        {
                                            if (user != null && inviteCode != null) {
                                                sendJoinRequest(inviteCode);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onAuthError(String msg, FirebaseAuthHandler.ErrorType type) {
                                        Log.e("HomeActivity", "Auth failed after login: " + msg);
                                    }
                                });
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
                setContentView(R.layout.activity_home);
                init();
                Intent loginIntent = new Intent(this, LoginActivity.class);
                loginIntent.putExtra("fromLink", true);
                loginLauncher.launch(loginIntent);
            }
        }
        else
        {
            user = authHandler.getUser();

            setContentView(R.layout.activity_home);
            init();

            runWhenServerActive(() ->
            {
                try
                {
                    APIHandler.getInstance().preload();
                }
                catch (JSONException | IOException e)
                {
                    Log.e("HomeActivity", "Preload failed", e);
                }
            });
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume()
    {
        super.onResume();

        user = authHandler.getUser();

        if (tvTitle == null) return;

        if (user != null)
        {
            tvTitle.setText("היי " + user.getName());
        }
        else
        {
            tvTitle.setText("היי");
            Log.w("HomeActivity", "User is null in init()");
        }

        if (user != null)
        {
            runWhenServerActive(() ->
                    dbHandler.getListNames(user, listNames -> runOnUiThread(() ->
                    {
                        if (listsGridAdapter != null)
                        {
                            listsGridAdapter.updateList(listNames);
                        }
                    })));
        }
    }

    @Override
    protected boolean enableSwipeBack()
    {
        return false;
    }

    @Override
    protected boolean disableSystemBack()
    {
        return true;
    }

    @SuppressLint("SetTextI18n")
    private void init()
    {
        btnSettings = findViewById(R.id.btn_settings);
        btnCreateList = findViewById(R.id.btn_create_list);
        tvTitle = findViewById(R.id.tv_title);

        if (user != null)
        {
            tvTitle.setText("היי " + user.getName());
        }
        else
        {
            tvTitle.setText("היי");
            Log.w("HomeActivity", "User is null in init()");
            return;
        }

        dbHandler.listenToUserName(user, username -> {
            user.setName(username);
            tvTitle.setText("היי " + authHandler.getUser().getName());
        });

        createAddListAlertDialog();
        setButtons();

        listsRecycler = findViewById(R.id.lists_grid);
        listsRecycler.setLayoutManager(new GridLayoutManager(this, Baskit.HOME_GRID_NUM_BOXES));

        listsGridAdapter = new GridAdapter(this, new ArrayList<>(), position -> openListScreen(user.getListIDs().get(position)));

        listsRecycler.setAdapter(listsGridAdapter);

        runWhenServerActive(() ->
                dbHandler.getListNames(user, listNames -> runOnUiThread(() ->
                {
                    if (listsGridAdapter != null)
                    {
                        listsGridAdapter.updateList(listNames);
                    }
                })));

        runWhenServerActive(() ->
                dbHandler.listenToListNames(user, listNames -> runOnUiThread(() ->
                {
                    if (listsGridAdapter != null)
                    {
                        listsGridAdapter.updateList(listNames);
                    }
                })));
    }

    private void setButtons()
    {
        btnSettings.setOnClickListener(view -> startActivity(new Intent(HomeActivity.this, SettingsActivity.class)));

        btnCreateList.setOnClickListener(view -> {
            adEtName.setText("");
            adCreateList.show();
        });
    }

    @SuppressLint("InflateParams")
    private void createAddListAlertDialog()
    {
        adLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.alert_dialog_create_list, null);
        adBtnCancel = adLayout.findViewById(R.id.btn_cancel);
        adBtnCreate = adLayout.findViewById(R.id.btn_save_name);
        adEtName = adLayout.findViewById(R.id.et_name);

        adb = new AlertDialog.Builder(this);
        adb.setView(adLayout);

        adCreateList = adb.create();

        adBtnCancel.setOnClickListener(v -> adCreateList.dismiss());

        adBtnCreate.setOnClickListener(v ->
        {
            adBtnCreate.setActivated(false);

            runWhenServerActive(() ->
                    authHandler.createList(adEtName.getText().toString(), HomeActivity.this, newList -> {
                        adCreateList.dismiss();
                        Toast.makeText(HomeActivity.this,
                                "הרשימה " + newList.getName() + " נוצרה",
                                Toast.LENGTH_SHORT).show();
                        openListScreen(newList.getId());
                    }));
        });
    }

    private void openListScreen(String listID)
    {
        Intent intent = new Intent(HomeActivity.this, ListActivity.class);
        intent.putExtra("listId", listID);

        startActivity(intent);
    }

    private void sendJoinRequest(String invitationCode)
    {
        String listId = new String(Base64.decode(invitationCode, Base64.NO_WRAP), StandardCharsets.UTF_8);
        dbHandler.sendJoinRequest(listId, user, HomeActivity.this);
    }


    @SuppressWarnings("SuspiciousNameCombination")
    public static class GridAdapter extends RecyclerView.Adapter<GridAdapter.GridViewHolder>
    {
        private final ArrayList<String> listNames;

        private final Context context;
        private final GridAdapter.OnItemClickListener listener;

        public interface OnItemClickListener
        {
            void onItemClick(int position);
        }

        public GridAdapter(Context context, ArrayList<String> listNames, GridAdapter.OnItemClickListener listener)
        {
            this.context = context;
            this.listNames = listNames;
            this.listener = listener;
        }

        @RequiresApi(api = Build.VERSION_CODES.Q)
        @SuppressLint("ResourceType")
        @NonNull
        @Override
        public GridAdapter.GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            MaterialButton button = new MaterialButton(parent.getContext())
            {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
                {
                    super.onMeasure(widthMeasureSpec, widthMeasureSpec);
                }
            };
            button.setCornerRadius(28);

            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getAppColor(context, com.google.android.material.R.attr.colorSurface)));

            button.setStrokeWidth(8);
            button.setStrokeColor(android.content.res.ColorStateList.valueOf(
                    getAppColor(context, android.R.attr.colorPrimary)
            ));

            button.setTextColor(getAppColor(context, com.google.android.material.R.attr.colorSecondary));

            button.setTypeface(Typeface.DEFAULT_BOLD);
            button.setTextSize(24f);
            button.setSingleLine(false);
            button.setHorizontallyScrolling(false);
            button.setAutoSizeTextTypeUniformWithConfiguration(
                    12,
                    40,
                    1,
                    android.util.TypedValue.COMPLEX_UNIT_SP
            );

            button.setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY);
            button.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);

            button.setGravity(Gravity.CENTER);
            button.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);
            int pad = (int) (12 * parent.getContext().getResources().getDisplayMetrics().density);
            button.setPadding(pad, pad, pad, pad);

            button.setRippleColor(android.content.res.ColorStateList.valueOf(
                    getAppColor(context, com.google.android.material.R.attr.colorSecondary)
            ));

            int margin = (int) (12 * parent.getContext().getResources().getDisplayMetrics().density);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(margin, margin, margin, margin);
            button.setLayoutParams(params);

            return new GridAdapter.GridViewHolder(button);
        }

        public static class GridViewHolder extends RecyclerView.ViewHolder
        {
            public final Button button;

            public GridViewHolder(Button button)
            {
                super(button);
                this.button = button;
            }
        }

        @Override
        public void onBindViewHolder(@NonNull GridAdapter.GridViewHolder holder, int position)
        {
            holder.button.setText(listNames.get(position));

            holder.button.setOnClickListener(v ->
            {
                @SuppressWarnings("deprecation") int newPosition = holder.getAdapterPosition();

                if (newPosition != RecyclerView.NO_POSITION)
                {
                    listener.onItemClick(newPosition);
                }
            });

            holder.button.setOnLongClickListener(v -> {
                @SuppressWarnings("deprecation") int newPosition = holder.getAdapterPosition();
                if (newPosition != RecyclerView.NO_POSITION) {
                    listener.onItemClick(newPosition);
                }
                return true;
            });
        }

        @Override
        public int getItemCount()
        {
            return listNames.size();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void updateList(ArrayList<String> newList)
        {
            this.listNames.clear();
            this.listNames.addAll(newList);
            notifyDataSetChanged();
        }
    }
}