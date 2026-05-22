package com.example.baskit.list;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Base64;
import android.util.Log;
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

import com.google.firebase.database.ValueEventListener;

public class ShareListAlertDialog
{
    final List list;

    final FirebaseDBHandler dbHandler = FirebaseDBHandler.getInstance();

    ValueEventListener requestsListener;

    final ShareListRequestsAdapter requestsAdapter;
    final AlertDialog ad;
    final androidx.appcompat.app.AlertDialog.Builder adb;

    final LinearLayout adLayout;
    final TextView tvListName;
    final Button btnCopyLink;
    final RecyclerView recyclerRequests;

    final MasterActivity activity;

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
                if (request == null || list == null)
                {
                    return;
                }

                activity.runProtectedRequest(
                        "accept_request_" + request.getUserID(),
                        null,
                        () -> dbHandler.acceptRequest(list, request)
                );
            }

            @Override
            public void declineRequest(Request request)
            {
                if (request == null || list == null)
                {
                    return;
                }

                activity.runProtectedRequest(
                        "decline_request_" + request.getUserID(),
                        null,
                        () -> dbHandler.declineRequest(list, request)
                );
            }
        });

        recyclerRequests.setLayoutManager(new LinearLayoutManager(context));
        recyclerRequests.setAdapter(requestsAdapter);

        requestsListener = dbHandler.listenForRequests(list, updatedRequests ->
        {
            if (activity == null || activity.isFinishing() || activity.isDestroyed())
            {
                return;
            }

            activity.runOnUiThread(() ->
            {
                if (activity.isFinishing() || activity.isDestroyed())
                {
                    return;
                }

                requestsAdapter.updateRequests(
                        updatedRequests != null ? updatedRequests : new ArrayList<>()
                );
            });
        });

        adb = new androidx.appcompat.app.AlertDialog.Builder(context);
        adb.setView(adLayout);
        ad = adb.create();

        setButton();
    }

    private void setButton()
    {
        btnCopyLink.setOnClickListener(v ->
        {
            if (activity == null || activity.isFinishing() || activity.isDestroyed())
            {
                return;
            }

            activity.runProtectedRequest(
                    "copy_invite_link_" + (list != null ? list.getId() : "unknown"),
                    btnCopyLink,
                    () -> copyLink(createLink())
            );
        });
    }

    public void show()
    {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (list == null) return;

        String listName = list.getName();

        tvListName.setText(
                listName != null && !listName.isBlank()
                        ? listName
                        : Baskit.getAppStr(R.string.unnamed_list)
        );

        if (ad.isShowing())
        {
            return;
        }

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
        if (clipboard == null)
        {
            Toast.makeText(activity, Baskit.getAppStr(R.string.msg_general_error), Toast.LENGTH_SHORT).show();
            return;
        }
        ClipData clip = ClipData.newPlainText(Baskit.getAppStr(R.string.list_invitation_label), link);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(activity, Baskit.getAppStr(R.string.msg_link_copied), Toast.LENGTH_SHORT).show();
    }

    public void dismiss()
    {
        if (requestsListener != null && list != null)
        {
            dbHandler.removeRequestsListener(list.getId(), requestsListener);
            requestsListener = null;
        }

        if (ad != null && ad.isShowing())
        {
            ad.dismiss();
        }
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
            this.requests = list != null && list.getRequests() != null
                    ? list.getRequests()
                    : new ArrayList<>();
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
            if (position < 0 || position >= requests.size())
            {
                return;
            }

            Request request = requests.get(position);
            if (request == null)
            {
                return;
            }

            String username = request.getUsername();

            holder.tvUsername.setText(
                    username != null && !username.isBlank()
                            ? username
                            : Baskit.getAppStr(R.string.unknown_user)
            );
            holder.tvUsername.setVisibility(View.VISIBLE);

            holder.btnAccept.setOnClickListener(v ->
            {
                if (holder.getBindingAdapterPosition() == RecyclerView.NO_POSITION)
                {
                    return;
                }

                upperClassFns.acceptRequest(request);
            });

            holder.btnDecline.setOnClickListener(v ->
            {
                if (holder.getBindingAdapterPosition() == RecyclerView.NO_POSITION)
                {
                    return;
                }

                upperClassFns.declineRequest(request);
            });
        }

        @Override
        public int getItemCount()
        {
            return requests.size();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void updateRequests(ArrayList<Request> newRequests)
        {
            this.requests = newRequests != null ? newRequests : new ArrayList<>();
            notifyDataSetChanged();
        }
    }
}
