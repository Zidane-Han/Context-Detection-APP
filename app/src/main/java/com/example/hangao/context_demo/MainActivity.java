package com.example.hangao.context_demo;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST_ID = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int NUMBER_OF_FRAGMENTS = 3;
    private static final int FRAGMENT_INDEX_SETTING = 0;
    private static final int FRAGMENT_INDEX_STATUS = 1;
    private static final int FRAGMENT_INDEX_LOGGER = 2;

    private GnssContainer mGpsContainer;
    private UiLogger mUiLogger;
    private FileLogger mFileLogger;
    private Fragment[] mFragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissionAndSetupFragments(this);
    }

    private void setupFragments() {
        mUiLogger = new UiLogger();
        mFileLogger = new FileLogger(getApplicationContext());
        mGpsContainer = new GnssContainer(getApplicationContext(), mUiLogger, mFileLogger);


        mFragments = new Fragment[NUMBER_OF_FRAGMENTS];
        SettingsFragment settingsFragment = new SettingsFragment();
        settingsFragment.setGpsContainer(mGpsContainer);
        mFragments[FRAGMENT_INDEX_SETTING] = settingsFragment;

        StatusFragment statusFragment = new StatusFragment();
        mFragments[FRAGMENT_INDEX_STATUS] = statusFragment;

        LoggerFragment loggerFragment = new LoggerFragment();
        loggerFragment.setUILogger(mUiLogger);
        loggerFragment.setFileLogger(mFileLogger);
        mFragments[FRAGMENT_INDEX_LOGGER] = loggerFragment;

        // The viewpager that will host the section contents.
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setOffscreenPageLimit(2);
        ViewPagerAdapter adapter = new ViewPagerAdapter(getFragmentManager());
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
