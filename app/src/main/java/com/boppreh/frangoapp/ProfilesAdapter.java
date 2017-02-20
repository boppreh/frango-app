package com.boppreh.frangoapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

class ProfilesAdapter extends FragmentStatePagerAdapter {
    private final MainActivity mainActivity;

    public ProfilesAdapter(MainActivity mainActivity, FragmentManager supportFragmentManager) {
        super(supportFragmentManager);
        this.mainActivity = mainActivity;
    }

    @Override
    public Fragment getItem(int position) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putInt("profile_index", position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getCount() {
        return mainActivity.fragment.profiles.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mainActivity.fragment.profiles.get(position).name;
    }
}
