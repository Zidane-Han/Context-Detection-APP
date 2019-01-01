package com.example.hangao.context_demo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.util.MathHelper;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.ObservationReal;
import be.ac.ulg.montefiore.run.jahmm.OpdfGaussianMixture;
import be.ac.ulg.montefiore.run.jahmm.OpdfGaussianMixtureFactory;

/**
 ** A plot fragment to show cn0 of visible satellite signals and environment detection.
 */
public class SettingFragment extends Fragment {

    public static final String TAG = ":SettingsFragment";
    private GnssContainer mGpsContainer;

    private LocationManager mLocManager;
    //private GnssMeasurementsEvent.Callback mGnssMeasurementListener;

    public void setGpsContainer(GnssContainer value) {
        mGpsContainer = value;
    }

    /** Total number of kinds of plot tabs */
    private static final int NUMBER_OF_TABS = 1;

    /** The position of the CN0 over time plot tab */
    private static final int CN0_TAB = 0;

    /** The number of Gnss constellations */
    private static final int NUMBER_OF_CONSTELLATIONS = 6;

    /** The X range of the plot, we are keeping the latest one minute visible */
    private static final double TIME_INTERVAL_SECONDS = 60;

    /** The index in data set we reserved for the plot containing all constellations */
    private static final int DATA_SET_INDEX_ALL = 0;

    /** The number of satellites we pick for the strongest satellite signal strength calculation */
    private static final int NUMBER_OF_STRONGEST_SATELLITES = 4;

    /** Data format used to format the data in the text view */
    private static final DecimalFormat sDataFormat =
            new DecimalFormat("##.#", new DecimalFormatSymbols(Locale.US));

    private GraphicalView mChartView;

    /** The average of the average of strongest satellite signal strength over history */
    private double mAverageCn0 = 0;

    /** Total number of {@link GnssMeasurementsEvent} has been recieved*/
    private int mMeasurementCount = 0;
    private double mInitialTimeSeconds = -1;
    private TextView mEnvironmentDetect;
    private TextView mAnalysisView;
    private double mLastTimeReceivedSeconds = 0;
    private final ColorMap mColorMap = new ColorMap();
    private DataSetManager mDataSetManager;
    private XYMultipleSeriesRenderer mCurrentRenderer;
    private LinearLayout mLayout;
    private int mCurrentTab = 0; // 0-6: all, GPS, SBAS, GLONASS, QZSS, BEIDOU, GALILEO

    private Hmm HMMmodel;
    private List<ObservationReal> Sequences = new ArrayList<>();




    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_iodetect, container, false /* attachToRoot */);

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

        mDataSetManager
                = new DataSetManager(NUMBER_OF_TABS, NUMBER_OF_CONSTELLATIONS, getContext(), mColorMap);


        // Set up the Graph View
        XYMultipleSeriesRenderer renderer
                = mDataSetManager.getRenderer(mCurrentTab, DATA_SET_INDEX_ALL);
        XYMultipleSeriesDataset dataSet
                = mDataSetManager.getDataSet(mCurrentTab, DATA_SET_INDEX_ALL);
        if (mLastTimeReceivedSeconds > TIME_INTERVAL_SECONDS) {
            renderer.setXAxisMax(mLastTimeReceivedSeconds);
            renderer.setXAxisMin(mLastTimeReceivedSeconds - TIME_INTERVAL_SECONDS);
        }
        mCurrentRenderer = renderer;
        mChartView = ChartFactory.getLineChartView(getContext(), dataSet, renderer);

        // update layout
        mEnvironmentDetect = view.findViewById(R.id.environdetect);
        mEnvironmentDetect.setTextColor(Color.BLACK);
        mAnalysisView = view.findViewById(R.id.analysis);
        mAnalysisView.setTextColor(Color.BLACK);
        mLayout = view.findViewById(R.id.plot);
        mLayout.addView(mChartView);

        // set the listener for the updated GNSS information
        mLocManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        addGnssMeasurementListerner();

        return view;
    }

    private void logException(String errorMessage, Exception e) {
        Log.e(TAG, errorMessage, e);
        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
    }


    // GNSS measurement listener
    private void addGnssMeasurementListerner() {
        HMMmodel = HMMinitial();

        GnssStatus.Callback mGnssStatusListener = new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(GnssStatus status) {
                updateEnvironment(status, HMMmodel);
            }
        };

        GnssMeasurementsEvent.Callback mGnssMeasurementListener = new GnssMeasurementsEvent.Callback() {

            @Override
            public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
                updateCnoTab(event);
            }
        };

        try{
            mLocManager.registerGnssMeasurementsCallback(mGnssMeasurementListener);
            mLocManager.registerGnssStatusCallback(mGnssStatusListener);
        } catch (SecurityException e) {
            System.out.println("Security authorization");
        }
    }

    private Hmm<ObservationReal> HMMinitial() {
        // 0 -- indoor; 1 -- intermediate; 2 -- outdoor
        Hmm<ObservationReal> hmm = new Hmm<>(3, new OpdfGaussianMixtureFactory(2));

        // initial probability
        hmm.setPi(0, 0.25);
        hmm.setPi(1, 0.5);
        hmm.setPi(2, 0.25);

        // transition probability
        hmm.setAij(0, 0, 0.66);
        hmm.setAij(0, 1, 0.33);
        hmm.setAij(0, 2, 0);
        hmm.setAij(1, 0, 0.33);
        hmm.setAij(1, 1, 0.33);
        hmm.setAij(1, 2, 0.33);
        hmm.setAij(2, 0, 0);
        hmm.setAij(2, 1, 0.33);
        hmm.setAij(2, 2, 0.66);

        // emission probability
        double[] means_indoor = {88.95, 88.95};
        double[] means_mediate = {142.55, 142.55};
        double[] means_outdoor = {242.08, 607.35};

        double[] variances_indoor = {1025.37, 1025.37};
        double[] variances_mediate = {625, 625};
        double[] variances_outdoor = {2697.4, 5218.4};

        double[] proportion = {0.5, 0.5};

        hmm.setOpdf(0, new OpdfGaussianMixture(means_indoor, variances_indoor, proportion));
        hmm.setOpdf(1, new OpdfGaussianMixture(means_mediate, variances_mediate, proportion));
        hmm.setOpdf(2, new OpdfGaussianMixture(means_outdoor, variances_outdoor, proportion));

        return hmm;
    }

    /**
     * update environment detection results
     */
    protected void updateEnvironment(GnssStatus status, Hmm HMMmodel) {
        double mCNR25 = 0;
        int mNUM25 = 0;
        int length = status.getSatelliteCount();
        int mSvId = 0;
        int mTempConstellation;

        while (mSvId < length) {
            mTempConstellation = status.getConstellationType(mSvId);

            if ((mTempConstellation == 1 || mTempConstellation == 3) && status.getCn0DbHz(mSvId) >= 25) {
                mCNR25 = mCNR25 + status.getCn0DbHz(mSvId);
                mNUM25++;
            }
            mSvId++;
        }

        // environment detection
        Sequences.add(new ObservationReal(mCNR25));
        // cut the length of observations
        if (Sequences.size() > 5) {
            Sequences = Sequences.subList(Sequences.size()-5, Sequences.size());
        }


        int[] state = HMMmodel.mostLikelyStateSequence(Sequences);

        switch (state[state.length - 1]) {
            case 0:
                mEnvironmentDetect.setText("Environment: Indoor");
                break;
            case 1:
                mEnvironmentDetect.setText("Environment: Intermediate");
                break;
            case 2:
                mEnvironmentDetect.setText("Environment: Outdoor");
                break;
        }
    }


    /**
     *  Updates the CN0 versus Time plot data from a {@link GnssMeasurement}
     */
    protected void updateCnoTab(GnssMeasurementsEvent event) {
        long timeInSeconds =
                TimeUnit.NANOSECONDS.toSeconds(event.getClock().getTimeNanos());
        if (mInitialTimeSeconds < 0) {
            mInitialTimeSeconds = timeInSeconds;
        }

        // Building the texts message in analysis text view
        List<GnssMeasurement> measurements =
                sortByCarrierToNoiseRatio(new ArrayList<>(event.getMeasurements()));
        SpannableStringBuilder builder = new SpannableStringBuilder();
        double currentAverage = 0;
        if (measurements.size() >= NUMBER_OF_STRONGEST_SATELLITES) {
            mAverageCn0 =
                    (mAverageCn0 * mMeasurementCount
                            + (measurements.get(0).getCn0DbHz()
                            + measurements.get(1).getCn0DbHz()
                            + measurements.get(2).getCn0DbHz()
                            + measurements.get(3).getCn0DbHz())
                            / NUMBER_OF_STRONGEST_SATELLITES)
                            / (++mMeasurementCount);
            currentAverage =
                    (measurements.get(0).getCn0DbHz()
                            + measurements.get(1).getCn0DbHz()
                            + measurements.get(2).getCn0DbHz()
                            + measurements.get(3).getCn0DbHz())
                            / NUMBER_OF_STRONGEST_SATELLITES;
        }

        builder.append(getString(R.string.satellite_number_sum_hint,
                measurements.size()) + "\n");
        builder.append(getString(R.string.current_average_hint,
                sDataFormat.format(currentAverage) + "\n"));
        for (int i = 0; i < NUMBER_OF_STRONGEST_SATELLITES && i < measurements.size(); i++) {
            int start = builder.length();
            builder.append(
                    mDataSetManager.getConstellationPrefix(measurements.get(i).getConstellationType())
                            + measurements.get(i).getSvid()
                            + ": "
                            + sDataFormat.format(measurements.get(i).getCn0DbHz())
                            + "\n");
            int end = builder.length();
            builder.setSpan(
                    new ForegroundColorSpan(
                            mColorMap.getColor(
                                    measurements.get(i).getSvid(), measurements.get(i).getConstellationType())),
                    start,
                    end,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        mAnalysisView.setText(builder);

        // Adding incoming data into Dataset && update metrics
        mLastTimeReceivedSeconds = timeInSeconds - mInitialTimeSeconds;
        double mCNR25 = 0;
        int mNUM25 = 0;
        for (GnssMeasurement measurement : measurements) {
            int constellationType = measurement.getConstellationType();
            int svID = measurement.getSvid();
            if (constellationType != GnssStatus.CONSTELLATION_UNKNOWN) {
                mDataSetManager.addValue(
                        CN0_TAB,
                        constellationType,
                        svID,
                        mLastTimeReceivedSeconds,
                        measurement.getCn0DbHz());
            }

            // update CNR25 metrics
            if ((constellationType == GnssStatus.CONSTELLATION_GPS || constellationType == GnssStatus.CONSTELLATION_GLONASS) && measurement.getCn0DbHz() >= 25) {
                mCNR25 = mCNR25 + measurement.getCn0DbHz();
                mNUM25 = mNUM25 + 1;
            }
        }

        mDataSetManager.fillInDiscontinuity(CN0_TAB, mLastTimeReceivedSeconds);

        // Checks if the plot has reached the end of frame and resize
        if (mLastTimeReceivedSeconds > mCurrentRenderer.getXAxisMax()) {
            mCurrentRenderer.setXAxisMax(mLastTimeReceivedSeconds);
            mCurrentRenderer.setXAxisMin(mLastTimeReceivedSeconds - TIME_INTERVAL_SECONDS);
        }

        mChartView.invalidate();
    }

    private List<GnssMeasurement> sortByCarrierToNoiseRatio(List<GnssMeasurement> measurements) {
        Collections.sort(
                measurements,
                new Comparator<GnssMeasurement>() {
                    @Override
                    public int compare(GnssMeasurement o1, GnssMeasurement o2) {
                        return Double.compare(o2.getCn0DbHz(), o1.getCn0DbHz());
                    }
                });
        return measurements;
    }

    /**
     * An utility class provides and keeps record of all color assignments to the satellite in the
     * plots. Each satellite will receive a unique color assignment through out every graph.
     */
    private static class ColorMap {

        private ArrayMap<Integer, Integer> mColorMap = new ArrayMap<>();
        private int mColorsAssigned = 0;
        /**
         * Source of Kelly's contrasting colors:
         * https://medium.com/@rjurney/kellys-22-colours-of-maximum-contrast-58edb70c90d1
         */
        private static final String[] CONTRASTING_COLORS = {
                "#222222", "#F3C300", "#875692", "#F38400", "#A1CAF1", "#BE0032", "#C2B280", "#848482",
                "#008856", "#E68FAC", "#0067A5", "#F99379", "#604E97", "#F6A600", "#B3446C", "#DCD300",
                "#882D17", "#8DB600", "#654522", "#E25822", "#2B3D26"
        };
        private final Random mRandom = new Random();

        private int getColor(int svId, int constellationType) {
            // Assign the color from Kelly's 21 contrasting colors to satellites first, if all color
            // has been assigned, use a random color and record in {@link mColorMap}.
            if (mColorMap.containsKey(constellationType * 1000 + svId)) {
                return mColorMap.get(getUniqueSatelliteIdentifier(constellationType, svId));
            }
            if (this.mColorsAssigned < CONTRASTING_COLORS.length) {
                int color = Color.parseColor(CONTRASTING_COLORS[mColorsAssigned++]);
                mColorMap.put(getUniqueSatelliteIdentifier(constellationType, svId), color);
                return color;
            }
            int color = Color.argb(255, mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256));
            mColorMap.put(getUniqueSatelliteIdentifier(constellationType, svId), color);
            return color;
        }
    }

    private static int getUniqueSatelliteIdentifier(int constellationType, int svID){
        return constellationType * 1000 + svID;
    }

    /**
     * An utility class stores and maintains all the data sets and corresponding renders.
     * We use 0 as the {@code dataSetIndex} of all constellations and 1 - 6 as the
     * {@code dataSetIndex} of each satellite constellations
     */
    private static class DataSetManager {
        /** The Y min and max of each plot */
        private static final int[][] RENDER_HEIGHTS = {{5, 45}, {-60, 60}};
        /**
         *  <ul>
         *    <li>A list of constellation prefix</li>
         *    <li>G : GPS, US Constellation</li>
         *    <li>S : Satellite-based Augmentation System</li>
         *    <li>R : GLONASS, Russia Constellation</li>
         *    <li>J : QZSS, Japan Constellation</li>
         *    <li>C : BEIDOU China Constellation</li>
         *    <li>E : GALILEO EU Constellation</li>
         *  </ul>
         */
        private static final String[] CONSTELLATION_PREFIX = {"G", "S", "R", "J", "C", "E"};

        private final List<ArrayMap<Integer, Integer>>[] mSatelliteIndex;
        private final List<ArrayMap<Integer, Integer>>[] mSatelliteConstellationIndex;
        private final List<XYMultipleSeriesDataset>[] mDataSetList;
        private final List<XYMultipleSeriesRenderer>[] mRendererList;
        private final Context mContext;
        private final ColorMap mColorMap;

        public DataSetManager(int numberOfTabs, int numberOfConstellations,
                              Context context, ColorMap colorMap) {
            mDataSetList = new ArrayList[numberOfTabs];
            mRendererList = new ArrayList[numberOfTabs];
            mSatelliteIndex = new ArrayList[numberOfTabs];
            mSatelliteConstellationIndex = new ArrayList[numberOfTabs];
            mContext = context;
            mColorMap = colorMap;

            // Preparing data sets and renderer for all six constellations
            for (int i = 0; i < numberOfTabs; i++) {
                mDataSetList[i] = new ArrayList<>();
                mRendererList[i] = new ArrayList<>();
                mSatelliteIndex[i] = new ArrayList<>();
                mSatelliteConstellationIndex[i] = new ArrayList<>();
                for (int k = 0; k <= numberOfConstellations; k++) {
                    mSatelliteIndex[i].add(new ArrayMap<Integer, Integer>());
                    mSatelliteConstellationIndex[i].add(new ArrayMap<Integer, Integer>());
                    XYMultipleSeriesRenderer tempRenderer = new XYMultipleSeriesRenderer();
                    setUpRenderer(tempRenderer, i);
                    mRendererList[i].add(tempRenderer);
                    XYMultipleSeriesDataset tempDataSet = new XYMultipleSeriesDataset();
                    mDataSetList[i].add(tempDataSet);
                }
            }
        }

        // The constellation type should range from 1 to 6
        private String getConstellationPrefix(int constellationType) {
            if (constellationType <= GnssStatus.CONSTELLATION_UNKNOWN
                    || constellationType > NUMBER_OF_CONSTELLATIONS) {
                return "";
            }
            return CONSTELLATION_PREFIX[constellationType - 1];
        }

        /** Returns the multiple series data set at specific tab and index */
        private XYMultipleSeriesDataset getDataSet(int tab, int dataSetIndex) {
            return mDataSetList[tab].get(dataSetIndex);
        }

        /** Returns the multiple series renderer set at specific tab and index */
        private XYMultipleSeriesRenderer getRenderer(int tab, int dataSetIndex) {
            return mRendererList[tab].get(dataSetIndex);
        }

        /**
         * Adds a value into the both the data set containing all constellations and individual data set
         * of the constellation of the satellite
         */
        private void addValue(int tab, int constellationType, int svID,
                              double timeInSeconds, double value) {
            XYMultipleSeriesDataset dataSetAll = getDataSet(tab, DATA_SET_INDEX_ALL);
            XYMultipleSeriesRenderer rendererAll = getRenderer(tab, DATA_SET_INDEX_ALL);
            value = Double.parseDouble(sDataFormat.format(value));
            if (hasSeen(constellationType, svID, tab)) {
                // If the satellite has been seen before, we retrieve the dataseries it is add and add new
                // data
                dataSetAll
                        .getSeriesAt(mSatelliteIndex[tab].get(constellationType).get(svID))
                        .add(timeInSeconds, value);
                mDataSetList[tab]
                        .get(constellationType)
                        .getSeriesAt(mSatelliteConstellationIndex[tab].get(constellationType).get(svID))
                        .add(timeInSeconds, value);
            } else {
                // If the satellite has not been seen before, we create new dataset and renderer before
                // adding data
                mSatelliteIndex[tab].get(constellationType).put(svID, dataSetAll.getSeriesCount());
                mSatelliteConstellationIndex[tab]
                        .get(constellationType)
                        .put(svID, mDataSetList[tab].get(constellationType).getSeriesCount());
                XYSeries tempSeries = new XYSeries(CONSTELLATION_PREFIX[constellationType - 1] + svID);
                tempSeries.add(timeInSeconds, value);
                dataSetAll.addSeries(tempSeries);
                mDataSetList[tab].get(constellationType).addSeries(tempSeries);
                XYSeriesRenderer tempRenderer = new XYSeriesRenderer();
                tempRenderer.setLineWidth(5);
                tempRenderer.setColor(mColorMap.getColor(svID, constellationType));
                rendererAll.addSeriesRenderer(tempRenderer);
                mRendererList[tab].get(constellationType).addSeriesRenderer(tempRenderer);
            }
        }

        /**
         * Creates a discontinuity of the satellites that has been seen but not reported in this batch
         * of measurements
         */
        private void fillInDiscontinuity(int tab, double referenceTimeSeconds) {
            for (XYMultipleSeriesDataset dataSet : mDataSetList[tab]) {
                for (int i = 0; i < dataSet.getSeriesCount(); i++) {
                    if (dataSet.getSeriesAt(i).getMaxX() < referenceTimeSeconds) {
                        dataSet.getSeriesAt(i).add(referenceTimeSeconds, MathHelper.NULL_VALUE);
                    }
                }
            }
        }

        /**
         * Returns a boolean indicating whether the input satellite has been seen.
         */
        private boolean hasSeen(int constellationType, int svID, int tab) {
            return mSatelliteIndex[tab].get(constellationType).containsKey(svID);
        }

        /**
         * Set up a {@link XYMultipleSeriesRenderer} with the specs customized per plot tab.
         */
        private void setUpRenderer(XYMultipleSeriesRenderer renderer, int tabNumber) {
            renderer.setXAxisMin(0);
            renderer.setXAxisMax(60);
            renderer.setYAxisMin(RENDER_HEIGHTS[tabNumber][0]);
            renderer.setYAxisMax(RENDER_HEIGHTS[tabNumber][1]);
            renderer.setYAxisAlign(Paint.Align.RIGHT, 0);
            renderer.setLegendTextSize(30);
            renderer.setLabelsTextSize(30);
            renderer.setYLabelsColor(0, Color.BLACK);
            renderer.setXLabelsColor(Color.BLACK);
            renderer.setFitLegend(true);
            renderer.setShowGridX(true);
            renderer.setMargins(new int[] {10, 10, 30, 10});
            // setting the plot untouchable
            renderer.setZoomEnabled(false, false);
            renderer.setPanEnabled(false, true);
            renderer.setClickEnabled(false);
            renderer.setMarginsColor(Color.WHITE);
            renderer.setChartTitle(mContext.getResources().getString(R.string.plot_titles));
            renderer.setChartTitleTextSize(50);
        }
    }
}

