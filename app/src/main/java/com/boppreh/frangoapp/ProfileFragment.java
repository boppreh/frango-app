package com.boppreh.frangoapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;

import com.google.zxing.integration.android.IntentIntegrator;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.profile, container, false);
        Bundle args = getArguments();
        int profileIndex = args.getInt("profile_index");
        AccountsAdapter adapter = new AccountsAdapter(inflater, ((MainActivity) getActivity()).fragment.profiles.get(profileIndex));
        ((ExpandableListView) rootView.findViewById(R.id.accounts)).setAdapter(adapter);

        Button clickButton = (Button) rootView.findViewById(R.id.scan);
        final IntentIntegrator integrator = new IntentIntegrator(((MainActivity) getActivity()), MainActivity.SCAN_ACCOUNT_LOGIN, profileIndex);
        clickButton.setOnClickListener(new View.OnClickListener() {
                                           @Override
                                           public void onClick(View v) {
                                               integrator.initiateScan();
                                           }
                                       }
        );
        return rootView;
    }
}
