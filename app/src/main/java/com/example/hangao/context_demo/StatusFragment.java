package com.example.hangao.context_demo;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.GnssStatus;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;


/** monitor the satellite signals */

public class StatusFragment extends Fragment {

    private final static String TAG = "StatusFragment";
    private static final int PRN_COLUMN = 0;
    private static final int CONSTELLATION_COLUMN = 1;
    private static final int CN0_COLUMN = 2;
    private static final int ELEVATION_COLUMN = 3;
    private static final int AZIMUTH_COLUMN = 4;
    private static final int COLUMN_COUNT = 5;

    private LocationManager mLocationManager;
    private GnssStatus.Callback mGnssStatusListener;

    private Resources mRes;

    private SvGridAdapter mAdapter;

    private int mSvCount, mPrns[], mConstellationType[], mUsedInFixCount;

    private float mSnrCn0s[], mSvElevations[], mSvAzimuths[];

    private String mSnrCn0Title;

    private boolean mHasEphemeris[], mHasAlmanac[], mUsedInFix[];

    private long mFixTime;

    private boolean mNavigating, mGotFix;

    private Drawable mFlagUsa, mFlagRussia, mFlagJapan, mFlagChina, mFlagGalileo;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRes = getResources();
        View v = inflater.inflate(R.layout.frag_monitor, container, false);

        mFlagUsa = getResources().getDrawable(R.drawable.ic_flag_usa, null);
        mFlagRussia = getResources().getDrawable(R.drawable.ic_flag_russia, null);
        mFlagJapan = getResources().getDrawable(R.drawable.ic_flag_japan, null);
        mFlagChina = getResources().getDrawable(R.drawable.ic_flag_china, null);
        mFlagGalileo = getResources().getDrawable(R.drawable.ic_flag_galileo, null);

        GridView gridView = (GridView) v.findViewById(R.id.sv_status);
        mAdapter = new SvGridAdapter(getActivity());
        gridView.setAdapter(mAdapter);
        gridView.setFocusable(false);
        gridView.setFocusableInTouchMode(false);

        mLocationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);

        addGnssStatusListener();

        return v;
    }

    private void addGnssStatusListener() {
        mGnssStatusListener = new GnssStatus.Callback() {

            @Override
            public void onSatelliteStatusChanged(GnssStatus status) {
                setStarted(true);

                mSnrCn0Title = mRes.getString(R.string.status_cn0_column_label);

                if (mPrns == null) {
                    /**
                     * We need to allocate arrays big enough so we don't overflow them.  Per
                     * https://developer.android.com/reference/android/location/GnssStatus.html#getSvid(int)
                     * 255 should be enough to contain all known satellites world-wide.
                     */
                    final int MAX_LENGTH = 255;
                    mPrns = new int[MAX_LENGTH];
                    mSnrCn0s = new float[MAX_LENGTH];
                    mSvElevations = new float[MAX_LENGTH];
                    mSvAzimuths = new float[MAX_LENGTH];
                    mConstellationType = new int[MAX_LENGTH];
                    mHasEphemeris = new boolean[MAX_LENGTH];
                    mHasAlmanac = new boolean[MAX_LENGTH];
                    mUsedInFix = new boolean[MAX_LENGTH];
                }

                final int length = status.getSatelliteCount();
                mSvCount = 0;
                mUsedInFixCount = 0;
                while (mSvCount < length) {
                    int prn = status.getSvid(mSvCount);
                    mPrns[mSvCount] = prn;
                    mConstellationType[mSvCount] = status.getConstellationType(mSvCount);
                    mSnrCn0s[mSvCount] = status.getCn0DbHz(mSvCount);
                    mSvElevations[mSvCount] = status.getElevationDegrees(mSvCount);
                    mSvAzimuths[mSvCount] = status.getAzimuthDegrees(mSvCount);
                    mHasEphemeris[mSvCount] = status.hasEphemerisData(mSvCount);
                    mHasAlmanac[mSvCount] = status.hasAlmanacData(mSvCount);
                    mUsedInFix[mSvCount] = status.usedInFix(mSvCount);
                    if (status.usedInFix(mSvCount)) {
                        mUsedInFixCount++;
                    }

                    mSvCount++;
                }

                mAdapter.notifyDataSetChanged();


            }
        };

        try{
            mLocationManager.registerGnssStatusCallback(mGnssStatusListener);
        } catch (SecurityException e){
            setStarted(false);
        }


    }


    private void setStarted(boolean navigating) {
        if (navigating != mNavigating) {
            if (navigating) {

            } else {
                /**
                 mLatitudeView.setText(EMPTY_LAT_LONG);
                 mLongitudeView.setText(EMPTY_LAT_LONG);
                 mFixTime = 0;
                 updateFixTime();
                 mTTFFView.setText("");
                 mAltitudeView.setText("");
                 mAccuracyView.setText("");
                 mSpeedView.setText("");
                 mBearingView.setText("");
                 */
                mSvCount = 0;
                mAdapter.notifyDataSetChanged();
            }
            mNavigating = navigating;
        }
    }

    /**
     public void onSatelliteStatusChanged(GnssStatus status) {
     setStarted(true);

     mSnrCn0Title = mRes.getString(R.string.status_cn0_column_label);

     if (mPrns == null) {
     /**
     * We need to allocate arrays big enough so we don't overflow them.  Per
     * https://developer.android.com/reference/android/location/GnssStatus.html#getSvid(int)
     * 255 should be enough to contain all known satellites world-wide.

     final int MAX_LENGTH = 255;
     mPrns = new int[MAX_LENGTH];
     mSnrCn0s = new float[MAX_LENGTH];
     mSvElevations = new float[MAX_LENGTH];
     mSvAzimuths = new float[MAX_LENGTH];
     mConstellationType = new int[MAX_LENGTH];
     mHasEphemeris = new boolean[MAX_LENGTH];
     mHasAlmanac = new boolean[MAX_LENGTH];
     mUsedInFix = new boolean[MAX_LENGTH];
     }

     final int length = status.getSatelliteCount();
     mSvCount = 0;
     mUsedInFixCount = 0;
     while (mSvCount < length) {
     int prn = status.getSvid(mSvCount);
     mPrns[mSvCount] = prn;
     mConstellationType[mSvCount] = status.getConstellationType(mSvCount);
     mSnrCn0s[mSvCount] = status.getCn0DbHz(mSvCount);
     mSvElevations[mSvCount] = status.getElevationDegrees(mSvCount);
     mSvAzimuths[mSvCount] = status.getAzimuthDegrees(mSvCount);
     mHasEphemeris[mSvCount] = status.hasEphemerisData(mSvCount);
     mHasAlmanac[mSvCount] = status.hasAlmanacData(mSvCount);
     mUsedInFix[mSvCount] = status.usedInFix(mSvCount);
     if (status.usedInFix(mSvCount)) {
     mUsedInFixCount++;
     }

     mSvCount++;
     }

     mAdapter.notifyDataSetChanged();
     }
     */

    private class SvGridAdapter extends BaseAdapter {

        private Context mContext;

        public SvGridAdapter(Context c) {
            mContext = c;
        }

        public int getCount() {
            // add 1 for header row
            return (mSvCount + 1) * COLUMN_COUNT;
        }

        public Object getItem(int position) {
            Log.d(TAG, "getItem(" + position + ")");
            return "foo";
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView = null;
            ImageView imageView = null;

            int row = position / COLUMN_COUNT;
            int column = position % COLUMN_COUNT;

            if (convertView != null) {
                if (convertView instanceof ImageView) {
                    imageView = (ImageView) convertView;
                } else if (convertView instanceof TextView) {
                    textView = (TextView) convertView;
                }
            }

            CharSequence text = null;

            if (row == 0) {
                switch (column) {
                    case PRN_COLUMN:
                        text = mRes.getString(R.string.status_prn_column_label);
                        break;
                    case CONSTELLATION_COLUMN:
                        text = mRes.getString(R.string.status_constellation_image_label);
                        break;
                    case CN0_COLUMN:
                        text = mSnrCn0Title;
                        break;
                    case ELEVATION_COLUMN:
                        text = mRes.getString(R.string.status_elevation_column_label);
                        break;
                    case AZIMUTH_COLUMN:
                        text = mRes.getString(R.string.status_azimuth_column_label);
                        break;
                }
            } else {
                row--;
                switch (column) {
                    case PRN_COLUMN:
                        text = Integer.toString(mPrns[row]);
                        break;
                    case CONSTELLATION_COLUMN:
                        if (imageView == null) {
                            imageView = new ImageView(mContext);
                            imageView.setScaleType(ImageView.ScaleType.FIT_START);
                        }

                        switch (mConstellationType[row]) {
                            case 1:
                                imageView.setImageDrawable(mFlagUsa);
                                break;
                            case 3:
                                imageView.setImageDrawable(mFlagRussia);
                                break;
                            case 4:
                                imageView.setImageDrawable(mFlagJapan);
                                break;
                            case 5:
                                imageView.setImageDrawable(mFlagChina);
                                break;
                            case 6:
                                imageView.setImageDrawable(mFlagGalileo);
                                break;
                        }
                        return imageView;

                    case CN0_COLUMN:
                        if (mSnrCn0s[row] != 0.0f) {
                            text = Float.toString(mSnrCn0s[row]);
                        } else {
                            text = "";
                        }
                        break;
                    case ELEVATION_COLUMN:
                        if (mSvElevations[row] != 0.0f) {
                            text = getString(R.string.status_elevation_column_value,
                                    Float.toString(mSvElevations[row]));
                        } else {
                            text = "";
                        }
                        break;
                    case AZIMUTH_COLUMN:
                        if (mSvAzimuths[row] != 0.0f) {
                            text = getString(R.string.status_azimuth_column_value,
                                    Float.toString(mSvAzimuths[row]));
                        } else {
                            text = "";
                        }
                        break;
                }
            }

            if (textView == null) {
                textView = new TextView(mContext);
            }

            textView.setText(text);
            return textView;
        }
    }
}