package com.example.hangao.context_demo;

import android.support.v4.app.Fragment;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.example.hangao.context_demo.GnssContainer;
import java.lang.reflect.InvocationTargetException;
import android.widget.Button;

/**
 * The UI fragment showing a set of configurable settings for the client to request GPS data.
 */
public class SettingFragment extends Fragment {

    public static final String TAG = ":SettingsFragment";
    private GnssContainer mGpsContainer;
    // private HelpDialog helpDialog;

    public void setGpsContainer(GnssContainer value) {
        mGpsContainer = value;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_activity, container, false /* attachToRoot */);

        final Switch registerLocation = (Switch) view.findViewById(R.id.register_location);
        final TextView registerLocationLabel =
                (TextView) view.findViewById(R.id.register_location_label);
        //set the switch to OFF
        registerLocation.setChecked(false);
        registerLocationLabel.setText("Switch is OFF");
        registerLocation.setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        if (isChecked) {
                            mGpsContainer.registerLocation();
                            mGpsContainer.registerGnssStatus();
                            registerLocationLabel.setText("Switch is ON");
                        } else {
                            mGpsContainer.unregisterLocation();
                            mGpsContainer.unregisterGpsStatus();
                            registerLocationLabel.setText("Switch is OFF");
                        }
                    }
                });

        final Switch registerMeasurements = (Switch) view.findViewById(R.id.register_measurements);
        final TextView registerMeasurementsLabel =
                (TextView) view.findViewById(R.id.register_measurement_label);
        //set the switch to OFF
        registerMeasurements.setChecked(false);
        registerMeasurementsLabel.setText("Switch is OFF");
        registerMeasurements.setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        if (isChecked) {
                            mGpsContainer.registerMeasurements();
                            registerMeasurementsLabel.setText("Switch is ON");
                        } else {
                            mGpsContainer.unregisterMeasurements();
                            registerMeasurementsLabel.setText("Switch is OFF");
                        }
                    }
                });


        java.lang.reflect.Method method;
        LocationManager locationManager = mGpsContainer.getLocationManager();
        try {
            method = locationManager.getClass().getMethod("getGnssYearOfHardware");
            int hwYear = (int) method.invoke(locationManager);


        } catch (NoSuchMethodException e) {
            logException("No such method exception: ", e);
            return null;
        } catch (IllegalAccessException e) {
            logException("Illegal Access exception: ", e);
            return null;
        } catch (InvocationTargetException e) {
            logException("Invocation Target Exception: ", e);
            return null;
        }

        String platfromVersionString = Build.VERSION.RELEASE;
        int apiLivelInt = Build.VERSION.SDK_INT;

        return view;
    }

    private void logException(String errorMessage, Exception e) {
        Log.e(TAG, errorMessage, e);
        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
    }
}

