package com.example.baskit.list;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Base64;
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

import com.example.baskit.Baskit;
import com.example.baskit.MasterActivity;
import com.example.baskit.online_components.FirebaseDBHandler;
import com.example.baskit.main_components.List;
import com.example.baskit.main_components.List.Request;
import com.example.baskit.R;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ShareListAlertDialog
{
    final List list;

    final FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();

    final ShareListRequestsAdapter requestsAdapter;
    final AlertDialog ad;
    final androidx.appcompat.app.AlertDialog.Builder adb;

    final LinearLayout adLayout;
    final TextView tvListName;
    final Button btnCopyLink;
    final RecyclerView recyclerRequests;

    final Activity activity;

    @SuppressLint("InflateParams")
    public ShareListAlertDialog(List list, MasterActivity activity, Context context)
    {
        this.list = list;
        this.activity = activity;

        adLayout = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.alert_dialog_share_list, null);
        tvListName = adLayout.findViewById(R.id.tv_list_name);
        btnCopyLink = adLayout.findViewById(R.id.btn_copy_link);
        recyclerRequests = adLayout.findViewById(R.id.recycler_requests);

        requestsAdapter = new ShareListRequestsAdapter(list, new ShareListRequestsAdapter.UpperClassFunctions()
        {
            @Override
            public void acceptRequest(Request request)
            {
                activity.runWhenServerActive(() -> dbHandler.acceptRequest(list, request));
            }

            @Override
            public void declineRequest(Request request)
            {
                activity.runWhenServerActive(() -> dbHandler.declineRequest(list, request));
            }
        });

        recyclerRequests.setLayoutManager(new LinearLayoutManager(context));
        recyclerRequests.setAdapter(requestsAdapter);

        dbHandler.listenForRequests(list, updatedRequests ->
        {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            requestsAdapter.updateRequests(updatedRequests);
        });

        adb = new androidx.appcompat.app.AlertDialog.Builder(context);
        adb.setView(adLayout);
        ad = adb.create();

        setButton();
    }

    private void setButton()
    {
        btnCopyLink.setOnClickListener(v -> copyLink(createLink()));
    }

    public void show()
    {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (list == null) return;

        tvListName.setText(list.getName());

        ad.show();
    }

    private String createLink()
    {
        String listId = list != null ? list.getId() : null;
        if (listId == null) return null;

        String invitationCode = Base64.encodeToString(listId.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        return "https://www.baskit.com/joinlist?inviteCode=" + invitationCode;
    }

    private void copyLink(String link)
    {
        if (link == null)
        {
            Toast.makeText(activity, Baskit.getAppStr(R.string.msg_general_error), Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(Baskit.getAppStr(R.string.list_invitation_label), link);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(activity, Baskit.getAppStr(R.string.msg_link_copied), Toast.LENGTH_SHORT).show();
    }


    public static class ShareListRequestsAdapter extends RecyclerView.Adapter<ShareListRequestsAdapter.ViewHolder>
    {
        private ArrayList<Request> requests;

        private final UpperClassFunctions upperClassFns;

        public interface UpperClassFunctions
        {
            void acceptRequest(Request request);
            void declineRequest(Request request);
        }

        public ShareListRequestsAdapter(List list, UpperClassFunctions upperClassFns)
        {
            this.requests = list.getRequests();
            this.upperClassFns = upperClassFns;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder
        {
            protected final TextView tvUsername;
            protected final Button btnAccept;
            protected final Button btnDecline;

            public ViewHolder(View requestView)
            {
                super(requestView);

                tvUsername = requestView.findViewById(R.id.tv_username);
                btnAccept = requestView.findViewById(R.id.btn_accept);
                btnDecline = requestView.findViewById(R.id.btn_decline);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.share_request_view, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position)
        {
            Request request = requests.get(position);

            holder.tvUsername.setText(request.getUsername());
            holder.tvUsername.setVisibility(View.VISIBLE);

            holder.btnAccept.setOnClickListener(v -> upperClassFns.acceptRequest(request));

            holder.btnDecline.setOnClickListener(v -> upperClassFns.declineRequest(request));
        }

        @Override
        public int getItemCount()
        {
            return requests.size();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void updateRequests(ArrayList<Request> newRequests)
        {
            this.requests = newRequests;
            notifyDataSetChanged();
        }
    }
}
