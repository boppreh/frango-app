package com.boppreh.frangoapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.boppreh.Crypto;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by BoppreH on 2017-02-12.
 */
public class Accounts extends BaseExpandableListAdapter {

    LayoutInflater inflater;
    MainActivity activity;
    List<Account> accounts;

    public Accounts(MainActivity activity) {
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
        view = inflater.inflate(R.layout.session, parent, false);
        TextView timestamp = (TextView) view.findViewById(R.id.timestamp);
        final Account account = getGroup(groupPosition);
        final Session session = account.sessions.get(childPosition);
        if (session.state == Session.State.CREATING) {
            timestamp.setText("logging in...");
        } else if (session.state == Session.State.REMOVING) {
            timestamp.setText("logging out...");
        } else {
            timestamp.setText(session.getIso8601());
        }
        Log.d("SESSIONS", session.getIso8601());

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
                                        Log.d("SESSIONS", session.getIso8601());
                                        Accounts.this.notifyDataSetChanged();

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
                                                Accounts.this.notifyDataSetChanged();
                                                try {
                                                    account.save(activity);
                                                } catch (IOException | JSONException e) {
                                                    activity.error("Failed to persist changes", e.getMessage());
                                                }
                                            }
                                        }, new Response.ErrorListener() {
                                            @Override
                                            public void onErrorResponse(VolleyError error) {
                                                if (error.networkResponse.statusCode == 404) {
                                                    activity.error("Invalid session", "The session doesn't exist anymore. It'll be removed from the list.");
                                                    Log.d("SESSIONS", session.getIso8601());
                                                    account.sessions.remove(session);
                                                    Accounts.this.notifyDataSetChanged();
                                                    try {
                                                        account.save(activity);
                                                    } catch (IOException | JSONException e) {
                                                        activity.error("Failed to persist changes", e.getMessage());
                                                    }
                                                } else {
                                                    activity.error("Error on log out", error.getMessage());
                                                }
                                            }
                                        });
                                    }
                                })
                                .setNegativeButton(android.R.string.no, null).show();
                    }
                });
            }
        });

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
        final Account account = getGroup(groupPosition);
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
