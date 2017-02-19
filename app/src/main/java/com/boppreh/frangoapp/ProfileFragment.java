package com.boppreh.frangoapp;

import android.app.Activity;
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
        AccountsAdapter adapter = new AccountsAdapter(inflater, ((MainActivity) getActivity()).fragment.profiles.get(profileIndex).accounts);
        ((ExpandableListView) rootView.findViewById(R.id.accounts)).setAdapter(adapter);

        final IntentIntegrator integrator = new IntentIntegrator((Activity) inflater.getContext());
        Button clickButton = (Button) rootView.findViewById(R.id.scan);
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
