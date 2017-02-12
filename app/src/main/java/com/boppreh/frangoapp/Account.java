package com.boppreh.frangoapp;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.crypto.Cipher;

/**
 * Created by BoppreH on 2017-02-11.
 */
public class Account extends BaseAdapter implements ListAdapter {
    Activity activity;

    public byte[] userId;
    public String domain;
    public KeyPair keyPair;
    public List<Session> sessions;

    public Account(Activity activity, byte[] userId, String domain, KeyPair keyPair) {
        this(activity, userId, domain, keyPair, new ArrayList<Session>());
    }

    public Account(Activity activity, byte[] userId, String domain, KeyPair keyPair, List<Session> sessions) {
        this.activity = activity;
        this.userId = userId;
        this.domain = domain;
        this.keyPair = keyPair;
        this.sessions = sessions;
    }

    @Override
    public int getCount() {
        return sessions.size();
    }

    @Override
    public Session getItem(int position) {
        return sessions.get(position);
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
            view = inflater.inflate(R.layout.session, parent, false);
        }

        final Session session = getItem(position);
        Log.d("SESSION", session.timestamp.toString());

        //Handle TextView and display string from your list
        TextView listItemText = (TextView)view.findViewById(R.id.timestamp);
        listItemText.setText(session.timestamp.toString());

        //Handle buttons and add onClickListeners
        ImageButton deleteBtn = (ImageButton)view.findViewById(R.id.logout);

        deleteBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                sessions.remove(position);
                notifyDataSetChanged();
            }
        });

        return view;
    }
}
