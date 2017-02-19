package com.boppreh.frangoapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

class AccountsAdapter extends BaseExpandableListAdapter {

    private LayoutInflater inflater;
    private List<Account> accounts;

    AccountsAdapter(LayoutInflater inflater, List<Account> accounts) {
        this.inflater = inflater;
        this.accounts = accounts;
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

            final ImageButton delete = (ImageButton) view.findViewById(R.id.delete);
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
