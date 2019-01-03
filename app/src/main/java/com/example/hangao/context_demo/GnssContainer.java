package com.example.hangao.context_demo;

import android.content.Context;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Bundle;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A container for GPS related API calls, it binds the {@link LocationManager} with {@link UiLogger}
 */
public class GnssContainer {

    public static final String TAG = "RawGPSapp";

    private static final long LOCATION_RATE_GPS_MS = TimeUnit.SECONDS.toMillis(1L);
    private static final long LOCATION_RATE_NETWORK_MS = TimeUnit.SECONDS.toMillis(60L);

    private boolean mLogLocations = true;
    private boolean mLogNavigationMessages = true;
    private boolean mLogMeasurements = true;
    private boolean mLogStatuses = true;
    private boolean mLogNmeas = true;

    private final List<GnssListener> mLoggers;

    private final LocationManager mLocationManager;
    private final LocationListener mLocationListener =
            new LocationListener() {

                @Override
                public void onProviderEnabled(String provider) {
                    if (mLogLocations) {
                        for (GnssListener logger : mLoggers) {
                            logger.onProviderEnabled(provider);
                        }
                    }
                }

                @Override
                public void onProviderDisabled(String provider) {
                    if (mLogLocations) {
                        for (GnssListener logger : mLoggers) {
                            logger.onProviderDisabled(provider);
                        }
                    }
                }

                @Override
                public void onLocationChanged(Location location) {
                    if (mLogLocations) {
                        for (GnssListener logger : mLoggers) {
                            logger.onLocationChanged(location);
                        }
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    if (mLogLocations) {
                        for (GnssListener logger : mLoggers) {
                            logger.onLocationStatusChanged(provider, status, extras);
                        }
                    }
                }
            };

    private final GnssMeasurementsEvent.Callback gnssMeasurementsEventListener =
            new GnssMeasurementsEvent.Callback() {
                @Override
                public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
                    if (mLogMeasurements) {
                        for (GnssListener logger : mLoggers) {
                            logger.onGnssMeasurementsReceived(event);
                        }
                    }
                }

                @Override
                public void onStatusChanged(int status) {
                    if (mLogMeasurements) {
                        for (GnssListener logger : mLoggers) {
                            logger.onGnssMeasurementsStatusChanged(status);
                        }
                    }
                }
            };

    private final GnssNavigationMessage.Callback gnssNavigationMessageListener =
            new GnssNavigationMessage.Callback() {
                @Override
                public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
                    if (mLogNavigationMessages) {
                        for (GnssListener logger : mLoggers) {
                            logger.onGnssNavigationMessageReceived(event);
                        }
                    }
                }

                @Override
                public void onStatusChanged(int status) {
                    if (mLogNavigationMessages) {
                        for (GnssListener logger : mLoggers) {
                            logger.onGnssNavigationMessageStatusChanged(status);
                        }
                    }
                }
            };

    private final GnssStatus.Callback gnssStatusListener =
            new GnssStatus.Callback() {
                @Override
                public void onStarted() {}

                @Override
                public void onStopped() {}

                @Override
                public void onSatelliteStatusChanged(GnssStatus status) {
                    for (GnssListener logger : mLoggers) {
                        logger.onGnssStatusChanged(status);
                    }

                }
            };

    private final OnNmeaMessageListener nmeaListener =
            new OnNmeaMessageListener() {
                @Override
                public void onNmeaMessage(String s, long l) {
                    if (mLogNmeas) {
                        for (GnssListener logger : mLoggers) {
                            logger.onNmeaReceived(l, s);
                        }
                    }
                }
            };

    public GnssContainer(Context context, GnssListener... loggers) {
        this.mLoggers = Arrays.asList(loggers);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public LocationManager getLocationManager() {
        return mLocationManager;
    }

    public void setLogLocations(boolean value) {
        mLogLocations = value;
    }

    public boolean canLogLocations() {
        return mLogLocations;
    }

    public void setLogNavigationMessages(boolean value) {
        mLogNavigationMessages = value;
    }

    public boolean canLogNavigationMessages() {
        return mLogNavigationMessages;
    }

    public void setLogMeasurements(boolean value) {
        mLogMeasurements = value;
    }

    public boolean canLogMeasurements() {
        return mLogMeasurements;
    }

    public void setLogStatuses(boolean value) {
        mLogStatuses = value;
    }

    public boolean canLogStatuses() {
        return mLogStatuses;
    }

    public void setLogNmeas(boolean value) {
        mLogNmeas = value;
    }

    public boolean canLogNmeas() {
        return mLogNmeas;
    }

    // TODO: check permission
    public void registerLocation() {
        boolean isGpsProviderEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGpsProviderEnabled) {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    LOCATION_RATE_NETWORK_MS,
                    0.0f /* minDistance */,
                    mLocationListener);
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_RATE_GPS_MS,
                    0.0f /* minDistance */,
                    mLocationListener);
        }
        logRegistration("LocationUpdates", isGpsProviderEnabled);
    }

    public void unregisterLocation() {
        mLocationManager.removeUpdates(mLocationListener);
    }

    public void registerMeasurements() {
        logRegistration(
                "GnssMeasurements",
                mLocationManager.registerGnssMeasurementsCallback(gnssMeasurementsEventListener));
    }

    public void unregisterMeasurements() {
        mLocationManager.unregisterGnssMeasurementsCallback(gnssMeasurementsEventListener);
    }

    public void registerNavigation() {
        logRegistration(
                "GpsNavigationMessage",
                mLocationManager.registerGnssNavigationMessageCallback(gnssNavigationMessageListener));
    }

    public void unregisterNavigation() {
        mLocationManager.unregisterGnssNavigationMessageCallback(gnssNavigationMessageListener);
    }

    public void registerGnssStatus() {
        logRegistration("GnssStatus", mLocationManager.registerGnssStatusCallback(gnssStatusListener));
    }

    public void unregisterGpsStatus() {
        mLocationManager.unregisterGnssStatusCallback(gnssStatusListener);
    }

    public void registerNmea() {
        logRegistration("Nmea", mLocationManager.addNmeaListener(nmeaListener));
    }

    public void unregisterNmea() {
        mLocationManager.removeNmeaListener(nmeaListener);
    }

    public void registerAll() {
        registerLocation();
        registerMeasurements();
        registerNavigation();
        registerGnssStatus();
        registerNmea();
    }

    public void unregisterAll() {
        unregisterLocation();
        unregisterMeasurements();
        unregisterNavigation();
        unregisterGpsStatus();
        unregisterNmea();
    }

    private void logRegistration(String listener, boolean result) {
        for (GnssListener logger : mLoggers) {
            logger.onListenerRegistration(listener, result);
        }
    }
}
