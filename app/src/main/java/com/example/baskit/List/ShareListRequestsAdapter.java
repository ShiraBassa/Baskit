package com.example.baskit.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.Firebase.FirebaseDBHandler;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.MainComponents.Request;
import com.example.baskit.R;

import java.util.ArrayList;

public class ShareListRequestsAdapter extends RecyclerView.Adapter<ShareListRequestsAdapter.ViewHolder>
{
    ArrayList<Request> requests;
    ArrayList<String> usernames;
    com.example.baskit.MainComponents.List list;
    Activity activity;
    Context context;
    UpperClassFunctions upperClassFns;

    public interface UpperClassFunctions
    {
        void acceptRequest(Request request);
        void declineRequest(Request request);
    }

    public ShareListRequestsAdapter(List list,
                                    Activity activity, Context context, UpperClassFunctions upperClassFns)
    {
        this.list = list;
        this.requests = list.getRequests();
        this.activity = activity;
        this.context = context;
        this.upperClassFns = upperClassFns;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        protected TextView tvUsername;
        protected Button btnAccept, btnDecline;

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

        holder.btnAccept.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                upperClassFns.acceptRequest(request);
            }
        });

        holder.btnDecline.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                upperClassFns.declineRequest(request);
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return requests.size();
    }

    public void updateRequests(ArrayList<Request> newRequests)
    {
        this.requests = newRequests;
        notifyDataSetChanged();
    }
}
