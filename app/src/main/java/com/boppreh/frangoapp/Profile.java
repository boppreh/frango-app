package com.boppreh.frangoapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.Image;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response;
import com.boppreh.Crypto;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by BoppreH on 2017-02-12.
 */
public class Profile extends BaseExpandableListAdapter {

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ssZ");

    LayoutInflater inflater;
    MainActivity activity;
    List<Account> accounts;

    public Profile(MainActivity activity) {
        this.inflater = LayoutInflater.from(activity);
        this.activity = activity;
        this.accounts = new ArrayList<>();
    }

    @Override
    public Session getChild(int groupPosition, int childPosition) {
        return getGroup(groupPosition).sessions.get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return getGroup(groupPosition).sessions.size();
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, final ViewGroup parent) {
        if (view == null) {
            view = inflater.inflate(R.layout.session, parent, false);
            TextView timestamp = (TextView) view.findViewById(R.id.timestamp);
            final Account account = getGroup(groupPosition);
            final Session session = account.sessions.get(childPosition);
            if (session.state == Session.State.CREATING) {
                timestamp.setText("logging in...");
            } else if (session.state == Session.State.REMOVING) {
                timestamp.setText("logging out...");
            } else {
                timestamp.setText(DATE_FORMAT.format(session.timestamp));
            }

            final ImageButton logout = (ImageButton) view.findViewById(R.id.logout);
            logout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            new AlertDialog.Builder(activity)
                                    .setTitle("Confirm logout")
                                    .setMessage("Logout from this " + account.domain + " session?")
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            session.state = Session.State.REMOVING;
                                            Profile.this.notifyDataSetChanged();

                                            logout.setEnabled(false);
                                            JSONObject body = new JSONObject();
                                            String url = "https://" + account.domain + "/frango/logout";
                                            try {
                                                body.put("user_id", Crypto.toBase64(account.userId));
                                                body.put("session_hash", Crypto.toBase64(session.sessionHash));
                                                body.put("signature", Crypto.toBase64(Crypto.sign(account.keyPair.getPrivate(), session.sessionHash)));
                                            } catch (Crypto.Exception | JSONException e) {
                                                activity.error("Error on logout", e.getMessage());
                                                e.printStackTrace();
                                                return;
                                            }

                                            activity.post(url, body, new Response.Listener<String>() {
                                                @Override
                                                public void onResponse(String response) {
                                                    account.sessions.remove(session);
                                                    Profile.this.notifyDataSetChanged();
                                                }
                                            });
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, null).show();
                        }
                    });
                }
            });
        }

        return view;
    }

    @Override
    public Account getGroup(int groupPosition) {
        return accounts.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return accounts.size();
    }

    @Override
    public long getGroupId(final int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View view, ViewGroup
            parent) {
        Account account = getGroup(groupPosition);
        if (view == null) {
            view = inflater.inflate(R.layout.account, parent, false);
            ((TextView) view.findViewById(R.id.domain)).setText(account.domain);
        }

        if (account.isLoading) {
            ((TextView) view.findViewById(R.id.n_sessions)).setText("registering....");
        } else {
            ((TextView) view.findViewById(R.id.n_sessions)).setText(account.sessions.size() + " sessions");
        }


        return view;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
