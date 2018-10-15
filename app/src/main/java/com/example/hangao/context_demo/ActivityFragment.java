package com.example.hangao.context_demo;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

public class ActivityFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener{

    public static final String DETECTED_ACTIVITY = ".DETECTED_ACTIVITY";

    private Context mContext;
    private ActivityRecognitionClient mActivityRecognitionClient;
    private ActivitiesAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.frag_activity_main, container, false);

        //mContext = inflater.getContext();
        mContext = getContext();

        Button mRequestActivityUpdatesButton = (Button) v.findViewById(R.id.get_activity);
        //Retrieve the ListView where we’ll display our activity data//
        ListView detectedActivitiesListView = (ListView) v.findViewById(R.id.activities_listview);

        ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(mContext).getString(
                        DETECTED_ACTIVITY, ""));

        // bind Button to the listener
        mRequestActivityUpdatesButton.setOnClickListener(mButtonClickListener);

        //Bind the adapter to the ListView//
        mAdapter = new ActivitiesAdapter(mContext, detectedActivities);
        detectedActivitiesListView.setAdapter(mAdapter);
        mActivityRecognitionClient = new ActivityRecognitionClient(mContext);

        // register listener
        PreferenceManager.getDefaultSharedPreferences(mContext)
                .registerOnSharedPreferenceChangeListener(this);
        updateDetectedActivitiesList();

        return v;
    }


    // Button listener
    private View.OnClickListener mButtonClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            //Set the activity detection interval. I’m using 3 seconds//
            Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                    3000, getActivityDetectionPendingIntent());
            task.addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    updateDetectedActivitiesList();
                }
            });
        }
    };

    //Get a PendingIntent//
    private PendingIntent getActivityDetectionPendingIntent() {
        //Send the activity data to our DetectedActivitiesIntentService class//
        Intent intent = new Intent(mContext, ActivityIntentService.class);
        return PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    //Process the list of activities//
    protected void updateDetectedActivitiesList() {
        ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(mContext)
                        .getString(DETECTED_ACTIVITY, ""));

        mAdapter.updateActivities(detectedActivities);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(DETECTED_ACTIVITY)) {
            updateDetectedActivitiesList();
        }
    }
}
