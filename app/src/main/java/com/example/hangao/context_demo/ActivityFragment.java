package com.example.hangao.context_demo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ActivityFragment extends Fragment{

    public static final String DETECTED_ACTIVITY = ".DETECTED_ACTIVITY";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.frag_activity_main, container, false);

        return v;
    }
}
