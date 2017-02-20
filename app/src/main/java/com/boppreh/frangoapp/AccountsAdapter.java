package com.boppreh.frangoapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.boppreh.Crypto;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

class AccountsAdapter extends BaseExpandableListAdapter {

    private LayoutInflater inflater;
    private Profile profile;

    AccountsAdapter(LayoutInflater inflater, Profile profile) {
        this.inflater = inflater;
        this.profile = profile;
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

        final Account account = getGroup(groupPosition);
        final Session session = account.sessions.get(childPosition);

        TextView timestamp = (TextView) view.findViewById(R.id.timestamp);
        timestamp.setText(session.getIso8601());

        final ImageButton logout = (ImageButton) view.findViewById(R.id.logout);
        final MainActivity activity = ((MainActivity) inflater.getContext());

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        new AlertDialog.Builder(activity)
                                .setTitle("Confirm logout")
                                .setMessage("Logout from this " + account.domain + " session?")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        session.state = Session.State.REMOVING;
                                        Log.d("SESSIONS", session.getIso8601());
                                        AccountsAdapter.this.notifyDataSetChanged();

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
                                                AccountsAdapter.this.notifyDataSetChanged();
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
                                                    AccountsAdapter.this.notifyDataSetChanged();
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
        return profile.accounts.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return profile.accounts.size();
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

            final ImageButton delete = (ImageButton) view.findViewById(R.id.delete);
            final MainActivity activity = ((MainActivity) inflater.getContext());

            delete.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(activity)
                            .setTitle("Confirm account removal")
                            .setMessage("Do you really want to delete your account at " + account.domain + "?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    JSONObject body = new JSONObject();
                                    try {
                                        body.put("user_id", Crypto.toBase64(account.userId));
                                        byte[] recovery = Crypto.decrypt(profile.offlineMasterKey, account.recoveryCode);
                                        List<byte[]> recoveryParts = Crypto.splitAt(recovery, 32);
                                        byte[] seed = recoveryParts.get(0);
                                        byte[] revocationCode = recoveryParts.get(1);
                                        body.put("revocation_code", Crypto.toBase64(revocationCode));
                                    } catch (JSONException e) {
                                        activity.error("Failed encode account removal", e.getMessage());
                                        e.printStackTrace();
                                        return;
                                    } catch (Crypto.Exception e) {
                                        activity.error("Failed to decrypt recovery code", e.getMessage());
                                        e.printStackTrace();
                                        return;
                                    }

                                    activity.post("https://" + account.domain + "/frango/revoke", body, new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            profile.accounts.remove(account);
                                            notifyDataSetChanged();
                                            if (account.domain.equals("4mm.org")) {
                                                Log.d("DELETE", activity.deleteFile(account.getFilename()) + "");
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            if (error.networkResponse.statusCode == 404) {
                                                activity.error("Invalid account", "The account doesn't exist anymore. It'll be removed from the list.");
                                                profile.accounts.remove(account);
                                                notifyDataSetChanged();
                                                if (account.domain.equals("4mm.org")) {
                                                    Log.d("DELETE", activity.deleteFile(account.getFilename()) + "");
                                                }
                                            } else {
                                                activity.error("Error on account removal", error.getMessage());
                                            }
                                        }
                                    });
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                }
            });
        }

        ((TextView) view.findViewById(R.id.n_sessions)).setText(account.sessions.size() + " sessions");

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
