package com.boppreh.frangoapp;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;

import com.google.zxing.integration.android.IntentIntegrator;

import java.io.IOException;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.profile, container, false);
        Bundle args = getArguments();
        int profileIndex = args.getInt("profile_index");
        final MainActivity activity = (MainActivity) getActivity();
        final Profile profile = activity.fragment.profiles.get(profileIndex);
        final AccountsAdapter adapter = new AccountsAdapter(inflater, profile);
        profile.notifier = new Profile.Notify() {
            @Override
            public void notifyUpdate() {
                adapter.notifyDataSetChanged();
                try {
                    activity.serialize("profile_" + profile.id, profile);
                } catch (IOException e) {
                    activity.error("Failed to serialize profile", e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        ((ExpandableListView) rootView.findViewById(R.id.accounts)).setAdapter(adapter);

        Button clickButton = (Button) rootView.findViewById(R.id.scan);
        final IntentIntegrator integrator = new IntentIntegrator(getActivity(), MainActivity.SCAN_ACCOUNT_LOGIN);

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
