package com.example.hangao.context_demo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST_ID = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int NUMBER_OF_FRAGMENTS = 3;
    private static final int FRAGMENT_INDEX_ACTIVITY = 0;
    private static final int FRAGMENT_INDEX_SETTING = 1;
    private static final int FRAGMENT_INDEX_STATUS = 2;


    private GnssContainer mGpsContainer;
    private UiLogger mUiLogger;
    //private FileLogger mFileLogger;
    private Fragment[] mFragments;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissionAndSetupFragments(this);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the
     * sections/tabs/pages.
     */
    public class ViewPagerAdapter extends FragmentStatePagerAdapter {

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case FRAGMENT_INDEX_SETTING:
                    return mFragments[FRAGMENT_INDEX_SETTING];
                case FRAGMENT_INDEX_STATUS:
                    return mFragments[FRAGMENT_INDEX_STATUS];
                case FRAGMENT_INDEX_ACTIVITY:
                    return mFragments[FRAGMENT_INDEX_ACTIVITY];
                default:
                    throw new IllegalArgumentException("Invalid section: " + position);
            }
        }

        @Override
        public int getCount() {
            // Show total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale locale = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_activity).toUpperCase(locale);
                case 1:
                    return getString(R.string.title_enir).toUpperCase(locale);
                case 2:
                    return getString(R.string.title_sate).toUpperCase(locale);
                default:
                    return super.getPageTitle(position);
            }
        }
    }

    private void setupFragments() {
        mUiLogger = new UiLogger();
        //mFileLogger = new FileLogger(getApplicationContext());
        //mGpsContainer = new GnssContainer(getApplicationContext(), mUiLogger, mFileLogger);
        mGpsContainer = new GnssContainer(getApplicationContext(), mUiLogger);

        mFragments = new Fragment[NUMBER_OF_FRAGMENTS];

        ActivityFragment activityFragment = new ActivityFragment();
        mFragments[FRAGMENT_INDEX_ACTIVITY] = activityFragment;

        EnvnFragment envnFragment = new EnvnFragment();
        envnFragment.setGpsContainer(mGpsContainer);
        mFragments[FRAGMENT_INDEX_SETTING] = envnFragment;

        StatusFragment statusFragment = new StatusFragment();
        mFragments[FRAGMENT_INDEX_STATUS] = statusFragment;

        //LoggerFragment loggerFragment = new LoggerFragment();
        //loggerFragment.setUILogger(mUiLogger);
        //loggerFragment.setFileLogger(mFileLogger);
        //mFragments[FRAGMENT_INDEX_LOGGER] = loggerFragment;

        // The viewpager that will host the section contents.
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setOffscreenPageLimit(2);
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);

        // Set a listener via setOnTabSelectedListener(OnTabSelectedListener) to be notified when any
        // tab's selection state has been changed.
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));

        // Use a TabLayout.TabLayoutOnPageChangeListener to forward the scroll and selection changes to
        // this layout
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_ID) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupFragments();
            }
        }
    }

    private boolean hasPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Permissions granted at install time.
            return true;
        }
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissionAndSetupFragments(final Activity activity) {
        if (hasPermissions(activity)) {
            setupFragments();
        } else {
            ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, LOCATION_REQUEST_ID);
        }
    }
}
