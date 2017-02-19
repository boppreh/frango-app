package com.boppreh.frangoapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

class ProfilesAdapter extends FragmentStatePagerAdapter {
    public ProfilesAdapter(FragmentManager supportFragmentManager) {
        super(supportFragmentManager);
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
        return MainActivity.instance.fragment.profiles.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return MainActivity.instance.fragment.profiles.get(position).name;
    }
}