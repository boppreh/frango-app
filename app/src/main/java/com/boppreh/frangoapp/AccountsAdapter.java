package com.boppreh.frangoapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.IOException;

class AccountsAdapter extends BaseExpandableListAdapter {

    private LayoutInflater inflater;
    private Profile profile;

    AccountsAdapter(LayoutInflater inflater, Profile profile) {
        this.inflater = inflater;
        this.profile = profile;
    }

    @Override
    public Session getChild(int groupPosition, int childPosition) {
        return getGroup(groupPosition).getSessions().get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return getGroup(groupPosition).getSessions().size();
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, final ViewGroup parent) {
        view = inflater.inflate(R.layout.session, parent, false);

        final IAccount account = getGroup(groupPosition);
        final Session session = account.getSessions().get(childPosition);
        this.notifyDataSetChanged();

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
                                .setMessage("Logout from this " + account.getName() + " session?")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        account.logout(session, activity, new IAccount.Notify() {
                                            @Override
                                            public void notifyUpdate(IAccount account) {
                                                profile.notifier.notifyUpdate();
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
    public IAccount getGroup(int groupPosition) {
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
        final IAccount account = getGroup(groupPosition);
        if (view == null) {
            view = inflater.inflate(R.layout.account, parent, false);
            ((TextView) view.findViewById(R.id.domain)).setText(account.getName());
            ((TextView) view.findViewById(R.id.n_sessions)).setText(account.getSubtitle());

            final ImageButton delete = (ImageButton) view.findViewById(R.id.delete);
            final MainActivity activity = ((MainActivity) inflater.getContext());

            delete.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(activity)
                            .setTitle("Confirm account removal")
                            .setMessage("Do you really want to delete your account at " + account.getName() + "?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    account.remove(profile.offlineMasterKey, activity, new IAccount.RemovalCallback() {
                                        @Override
                                        public void removeAndNotify(IAccount account) {
                                            profile.accounts.remove(account);
                                            profile.notifier.notifyUpdate();
                                        }
                                    });
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                }
            });
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
