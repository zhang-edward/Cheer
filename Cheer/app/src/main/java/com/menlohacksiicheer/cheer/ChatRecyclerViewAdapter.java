package com.menlohacksiicheer.cheer;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class ChatRecyclerViewAdapter<T extends Message> extends RecyclerView.Adapter<ChatRecyclerViewAdapter.ViewHolder> {

    private ArrayList<T> messages;
    private Context context;

    public ChatRecyclerViewAdapter(ArrayList<T> messages, Context context) {
        this.messages = messages;
        this.context = context;
    }

    @Override
    public ChatRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        if(viewType == 0) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_sender, parent, false);
        } else {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_recieve, parent, false);
        }
        final ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public int getItemViewType(int position) {
        if(!messages.get(position).incoming) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Message message = messages.get(position);
        String text = message.getMessage();
        String status = message.getStatus();
        TextView my_message = holder.textView;
        System.out.println(position + ", status = " + status + ", message = " + message.getMessage());
        if (getItemViewType(position) == 0) {
            holder.textView.setBackgroundResource(R.drawable.message_mine_rect);
        }
        else {
            holder.textView.setBackgroundResource(R.drawable.message_other_rect);
        }
        my_message.setText(text);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);

            textView = (TextView)itemView.findViewById(R.id.text_view);
        }
    }
}