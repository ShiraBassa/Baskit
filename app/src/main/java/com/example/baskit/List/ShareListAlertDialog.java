package com.example.baskit.List;

import static android.content.Context.CLIPBOARD_SERVICE;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.MainComponents.List;
import com.example.baskit.MainComponents.Request;
import com.example.baskit.R;

import java.util.ArrayList;
import java.util.UUID;

public class ShareListAlertDialog
{
    Activity activity;
    Context context;
    LinearLayout adLayout;
    TextView tvListName;
    Button btnCopyLink;
    RecyclerView recyclerRequests;
    ShareListRequestsAdapter requestsAdapter;
    List list;
    androidx.appcompat.app.AlertDialog.Builder adb;
    AlertDialog ad;
    FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();

    public ShareListAlertDialog(List list, Activity activity, Context context)
    {
        this.list = list;
        this.activity = activity;
        this.context = context;

        adLayout = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.alert_dialog_share_list, null);
        tvListName = adLayout.findViewById(R.id.tv_list_name);
        btnCopyLink = adLayout.findViewById(R.id.btn_copy_link);
        recyclerRequests = adLayout.findViewById(R.id.recycler_requests);

        requestsAdapter = new ShareListRequestsAdapter(list, activity, context, new ShareListRequestsAdapter.UpperClassFunctions()
        {
            @Override
            public void acceptRequest(Request request)
            {
                dbHandler.acceptRequest(list, request);
            }

            @Override
            public void declineRequest(Request request)
            {
                dbHandler.declineRequest(list, request);
            }
        });

        recyclerRequests.setLayoutManager(new LinearLayoutManager(context));
        recyclerRequests.setAdapter(requestsAdapter);

        dbHandler.listenForRequests(list, updatedRequests ->
        {
            requestsAdapter.updateRequests(updatedRequests);
        });

        adb = new androidx.appcompat.app.AlertDialog.Builder(context);
        adb.setView(adLayout);
        ad = adb.create();

        setButton();
    }

    private void setButton()
    {
        btnCopyLink.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                copyLink(createLink());
            }
        });
    }

    public void show()
    {
        tvListName.setText(list.getName());

        ad.show();
    }

    private String createLink()
    {
        String inviteCode = UUID.randomUUID().toString().replace("-", "");
        String link = "baskit://joinList?inviteCode=" + inviteCode;

        return link;
    }

    private void copyLink(String link)
    {
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("List Invite", link);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(activity, "Invite link copied!", Toast.LENGTH_SHORT).show();
    }
}
