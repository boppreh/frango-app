package com.boppreh.frangoapp;

import android.app.Fragment;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class MainFragment extends Fragment {
    public List<Profile> profiles = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
}
