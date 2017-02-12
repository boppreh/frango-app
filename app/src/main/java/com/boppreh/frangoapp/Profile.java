package com.boppreh.frangoapp;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by BoppreH on 2017-02-12.
 */
public class Profile extends BaseAdapter implements ListAdapter {

    Activity activity;
    LinkedHashMap<String, Account> accounts;

    public Profile(Activity activity) {
        this.activity = activity;
        this.accounts = new LinkedHashMap<>();
    }

    @Override
    public int getCount() {
        return accounts.size();
    }

    @Override
    public Account getItem(int position) {
        return new ArrayList<>(accounts.values()).get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            view = inflater.inflate(R.layout.account, parent, false);
        }

        final Account account = getItem(position);

        //Handle TextView and display string from your list
        TextView listItemText = (TextView)view.findViewById(R.id.domain);
        listItemText.setText(account.domain);

        //Handle buttons and add onClickListeners
        ImageButton deleteBtn = (ImageButton)view.findViewById(R.id.logout);

        ListView sessions = (ListView) view.findViewById(R.id.sessions);
        sessions.setAdapter(account);

        deleteBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                accounts.remove(account.userId);
                notifyDataSetChanged();
            }
        });

        return view;
    }
}
