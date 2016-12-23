package com.compeovario;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Process;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.compeovario.maps.CachingUrlTileProvider;
import com.compeovario.util.ArrayUtil;
import com.compeovario.util.BeepThread;
import com.compeovario.util.ConversionUtils;
import com.compeovario.util.FitCircle;
import com.compeovario.util.FlightRadar;
import com.compeovario.util.KalmanFilter;
import com.compeovario.util.LeonardoLiveWriter;
import com.compeovario.util.PositionWriter;
import com.compeovario.util.ThermicCircleUtil;
import com.compeovario.util.WindCalculator;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.maps.android.SphericalUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static android.content.Context.SENSOR_SERVICE;
import static com.google.android.gms.location.LocationServices.API;
import static java.text.DateFormat.getTimeInstance;

public class MainFragment extends Fragment implements
        OnMapReadyCallback,
        LocationListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleMap.OnCameraChangeListener {

    public static final double RADIUS_OF_EARTH_METERS = 6371009;
    final static String DEGREE = "\u00b0";
    private static final String TAG = "MainFragment";
    private static final long INTERVAL = 0;
    private static final long FASTEST_INTERVAL = 0;
    private static final double DEFAULT_RADIUS = 400;
    private static final double SLT_K = 288.15;  // Sea level temperature.
    private static final double TLAPSE_K_PER_M = -0.0065;  // Linear temperature atmospheric lapse rate.
    private static final double G_M_PER_S_PER_S = 9.80665;  // Acceleration from gravity.
    private static final double R_J_PER_KG_PER_K = 287.052;  // Specific gas constant for air, US Standard Atmosphere edition.
    private static final double PA_PER_INHG = 3386;  // Pascals per inch of mercury.
    private static final double KF_VAR_ACCEL = 0.0075;  // Variance of pressure acceleration noise input.
    private static final double KF_VAR_MEASUREMENT = 0.05;  // Variance of pressure measurement noise.
    static boolean loginLW = false, error = false, livetrackenabled = false, logheader = false, logfooter = false;
    static String username, password, serverUrl, errorinfo;
    static int vechiletype = 1, LWcount = 0, type = 0;
    static PositionWriter liveWriter;
    private static LocationManager sLocationManager;
    final List<Double> avgGR = new ArrayList<Double>();
    final List<Double> avgGpsVario = new ArrayList<Double>();
    final List<LatLng> lastLatLng = new ArrayList<LatLng>();
    public ArrayList<ThermalW> tpoints = new ArrayList<ThermalW>();
    int gpspower = 100;
    public static final int PRIORITY_HIGH_ACCURACY = 100;
    public static final int PRIORITY_BALANCED_POWER_ACCURACY = 102;
    public static final int PRIORITY_LOW_POWER = 104;
    public static final int PRIORITY_NO_POWER = 105;

    ThermalW liftps;

    InputStream in_s = null;

    double distToEdge = 0;
    double distToGoal = 0;
    double distToTakeoff = 0;
    double bearingToEdge = 0;
    int hasThermicCount = 0;
    double thermicvariovalue = 2;
    int thermicvariocount = 10;
    int thermicresetavg = 60;
    int tpcount = 0;
    double timereset = 0;

    String taskFileName = "Default.tsk";
    String thermicFileName = "Thermics.txt";
    String wpFileName = "Default.cup";
    String pilotname, glidermodel, glidercertf, civlid, logfilename = null;
    String mLastUpdateTime = null;

    TableLayout textUpperGroup;
    TableLayout textBottomGroup;

    LocationRequest mLocationRequest;
    Location mCurrentLocation = null;
    Location mpreviousGRLocation = null;

    Marker currentMarker = null;
    LatLng currentLatLng = null, takeoffLatLng = null, previousMarkerLatLng = null, previousVarioLatLng = null;
    TextView txt_altgoal, txt_speed, txt_altitude, txt_time, txt_disttakeoff, txt_distgoal, txt_avario, txt_activewp, txt_gravg, txt_varioavg, txt_live;
    RelativeLayout layoutUpperGroup, layoutBottomGroup;
    Boolean barometer = false, gps = false, gpsOn = false, logStarted = false, hasWind = false, hasThermic = false, drawThermic = false;
    int activeWp = 0;
    int logtime = 1000;
    int logcount = 0;

    TaskManager taskmanager = null;
    ThermicCircleUtil thermic_circle = null;
    Button GpsOnOff;
    private Button btnmenusettings;
    private Context mApplicationContext;
    private GoogleApiClient mGoogleApiClient;
    private SharedPreferences mPrefs;
    private MenuItem mGPS;
    private GoogleMap mMap;
    private float mMaxZoomLevel;
    private TileOverlay mSelectedTileOverlay;
    private SensorEventListener mPSensorListener, mTSensorListener;
    private SensorManager mSensorManager;
    private Sensor mPSensor, mTSensor;
    private double slp_inHg_ = 29.92;
    private double pressure_hPa_ = 1013.0912, d_temperature = 0;
    private KalmanFilter pressure_hPa_filter_;
    private double last_measurement_time_;
    private double oldalt = 0, baroaltitude = 0, damping = 30, avgvario = 0, gpsvario = 0;
    private BeepThread beeps = null;
    private WindCalculator windCalculator;
    private double[] wind;
    private double[] windError;
    private double[][] headingArray;
    private Compass compass, needle;
    private ImageView compassImage;
    private ImageView needleImage;
    private Handler variohandler = new Handler();
    private Handler taskhandler = new Handler();
    private Handler loghandler = new Handler();
    private Handler gpshandler = new Handler();

    private Runnable gpsrunnable = new Runnable() {
        @Override
        public void run() {

            gpshandler.postDelayed(this, 1000);
        }
    };

    private Runnable logrunnable = new Runnable() {
        @Override
        public void run() {
            if (mCurrentLocation.hasAccuracy()  && mCurrentLocation.hasAltitude() && logStarted) {

                setigcfile();
            }
            loghandler.postDelayed(this, logtime);
        }
    };
    private Runnable variorunnable = new Runnable() {
        @Override
        public void run() {
            setvario(pressure_hPa_);
            variohandler.postDelayed(this, 1000);
        }
    };
    private Runnable taskrunnable = new Runnable() {
        @Override
        public void run() {

            Calendar rightNow = Calendar.getInstance();
            long offset = rightNow.get(Calendar.ZONE_OFFSET) + rightNow.get(Calendar.DST_OFFSET);

            long currentTime = rightNow.getTimeInMillis() + offset;
            long remainingStartTime = (taskmanager.task_Starttime - currentTime);
            //long remainingEndTime = (taskmanager.task_Endtime - currentTime);

            Date date = new Date(rightNow.getTimeInMillis());
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            String strcurrentTime = formatter.format(date);

            date = new Date(remainingStartTime);
            formatter = new SimpleDateFormat("HH:mm:ss");
            String remaingTimeToStart = formatter.format(date);

            txt_time.setText(strcurrentTime + "\n" + remaingTimeToStart);

            taskhandler.postDelayed(this, 1000);
        }
    };

    private static double hPaToMeter(double slp_inHg, double pressure_hPa) {
        // Algebraically unoptimized computations---let the compiler sort it out.
        double factor_m = SLT_K / TLAPSE_K_PER_M;
        double exponent = -TLAPSE_K_PER_M * R_J_PER_KG_PER_K / G_M_PER_S_PER_S;
        double current_sea_level_pressure_Pa = slp_inHg * PA_PER_INHG;
        double altitude_m =
                factor_m *
                        (Math.pow(100.0 * pressure_hPa / current_sea_level_pressure_Pa, exponent) - 1.0);
        return altitude_m;
    }

    public static void DeleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                DeleteRecursive(child);
            }
        }

        fileOrDirectory.delete();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mApplicationContext);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, getActivity(), 0).show();
            return false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View mainView = inflater.inflate(R.layout.activity_maps, container, false);

        if (mainView != null) {
            return mainView;
        } else {
            Log.e(OTPApp.TAG, "Not possible to obtain main view, UI won't be correctly created");
            return null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(OTPApp.TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        mApplicationContext = getActivity().getApplicationContext();

        PackageManager PM = getActivity().getPackageManager();
        gps = PM.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        barometer = PM.hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER);

        if (beeps == null)
            beeps = new BeepThread(mApplicationContext);
        if (beeps != null)
            beeps.start(mApplicationContext, 2, 3);

        txt_altgoal = (TextView) getActivity().findViewById(R.id.txt_altgoal);
        txt_disttakeoff = (TextView) getActivity().findViewById(R.id.txt_disttakeoff);
        txt_distgoal = (TextView) getActivity().findViewById(R.id.txt_distgoal);
        txt_altitude = (TextView) getActivity().findViewById(R.id.txt_altitude);
        txt_speed = (TextView) getActivity().findViewById(R.id.txt_speed);
        txt_time = (TextView) getActivity().findViewById(R.id.txt_time);
        txt_avario = (TextView) getActivity().findViewById(R.id.txt_vario);
        txt_activewp = (TextView) getActivity().findViewById(R.id.txt_activewp);
        txt_gravg = (TextView) getActivity().findViewById(R.id.txt_gravg);
        txt_varioavg = (TextView) getActivity().findViewById(R.id.txt_varioavg);
        txt_live = (TextView) getActivity().findViewById(R.id.txt_live);

        View decorView = getActivity().getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        GpsOnOff = (Button) getActivity().findViewById(R.id.gpsswitch);
        GpsOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!gpsOn) {
                    gpsOn = true;
                    GpsOnOff.setText("Cam On");
                } else {
                    gpsOn = false;
                    GpsOnOff.setText("Cam Off");
                    if(taskmanager != null)
                    {
                        taskmanager.animCamToTask();
                    }
                }
            }
        });

        btnmenusettings = (Button) getActivity().findViewById(R.id.btnmenusettings);

        btnmenusettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(
                        new Intent(mApplicationContext, MenuActivity.class),
                        OTPApp.MENU_REQUEST_CODE);
            }
        });

        layoutUpperGroup = (RelativeLayout) getActivity().findViewById(R.id.layoutUpperGroup);
        layoutBottomGroup = (RelativeLayout) getActivity().findViewById(R.id.layoutBottomGroup);
        textUpperGroup = (TableLayout) getActivity().findViewById(R.id.textUpperGroup);
        textBottomGroup = (TableLayout) getActivity().findViewById(R.id.textBottomGroup);

        layoutUpperGroup.bringToFront();
        layoutUpperGroup.invalidate();
        layoutBottomGroup.bringToFront();
        layoutBottomGroup.invalidate();

        beeps.beepON(false);
        Toast.makeText(mApplicationContext, "Vario Sound Off", Toast.LENGTH_SHORT).show();

        txt_avario.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                try {
                    if (beeps != null) {
                        if (beeps.getBeepStatus()) {
                            beeps.beepON(false);
                            Toast.makeText(mApplicationContext, "Vario Sound Off", Toast.LENGTH_SHORT).show();
                        } else {
                            beeps.beepON(true);
                            Toast.makeText(mApplicationContext, "Vario Sound On", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                }
            }
        });

        windCalculator = new WindCalculator(16, 0.3, 300);
        wind = new double[3];

        compassImage = (ImageView) getActivity().findViewById(R.id.compass_rose);
        needleImage = (ImageView) getActivity().findViewById(R.id.compass_arrow);
        needleImage.setAlpha(150);
        compassImage.setAlpha(250);

        compass = new Compass(compassImage);
        needle = new Compass(needleImage);

        needle.rotate(0);
        compass.rotate(0);

        sLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);

        SupportMapFragment mapFragment = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map));

        mapFragment.getMapAsync(this);

        if (savedInstanceState == null) {
            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            prefsEditor.commit();
        }
    }

    public void resetViews() {
        txt_altgoal.setText("Alt On Goal\n0 m");
        txt_distgoal.setText("Dist Goal\n0 km");
        txt_disttakeoff.setText("Dist Tkf\n0 km");
        txt_activewp.setText("Active Point");
        txt_altitude.setText("Altitude\n0 m");
        txt_speed.setText("Speed\n0 km");
        txt_varioavg.setText("Vario Avg\n0 m/s");
        txt_avario.setText(String.format("VSpeed\n0 m/s"));
        txt_gravg.setText("Gr Avg\n0");

        if (taskmanager != null) {
            if (!taskmanager.isTaskCreated()) {
                txt_distgoal.setText("Dist Tkf\n0 km");
                txt_altgoal.setText("Wind\n0 km");
            }
        }
    }

    public void showCompass(boolean show) {
        compass.clearAnimation();
        needle.clearAnimation();

        if (show) {
            compassImage.setVisibility(View.VISIBLE);
            needleImage.setVisibility(View.VISIBLE);
            txt_disttakeoff.setVisibility(View.VISIBLE);
            txt_activewp.setVisibility(View.VISIBLE);
        } else {
            compassImage.setVisibility(View.GONE);
            needleImage.setVisibility(View.GONE);
            txt_disttakeoff.setVisibility(View.GONE);
            txt_activewp.setVisibility(View.GONE);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        taskmanager = new TaskManager(mApplicationContext, mMap);
        thermic_circle = new ThermicCircleUtil(mApplicationContext, mMap);

        try {
            File myFile = null;
            File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
            if (!root.exists()) {
                root.mkdirs();
            }

            myFile = new File(root, taskFileName);
            if (!myFile.exists()) {
                myFile.createNewFile();
                taskmanager.createTaskFile("");
            }

            myFile = new File(root, wpFileName);
            if (!myFile.exists()) {
                myFile.createNewFile();
            }

            myFile = new File(root, thermicFileName);
            if (!myFile.exists()) {
                myFile.createNewFile();
            }

        } catch (Exception e) {
        }

        taskmanager.createTask();

        if (taskmanager.isTaskCreated()) {
            String starttime = "13:30:00";
            String endtime = "18:00:00";

            taskmanager.setTaskStartTime(starttime);
            taskmanager.setTaskEndTime(endtime);

            taskmanager.updateTaskTimes(String.valueOf(taskmanager.getTask_Starttime()), String.valueOf(taskmanager.getTask_Endtime()));

            taskhandler.postDelayed(taskrunnable, 1000);
            showCompass(true);
        }

        resetViews();

        if (mMap != null) {

            mMap.setOnCameraChangeListener(this);
            mMap.getUiSettings().setCompassEnabled(true);

            updateOverlay(ConversionUtils.getOverlayString(mApplicationContext));

            addInterfaceListeners();

            if (drawThermic) {
                drawThermals();
            }
        }

       // gpshandler.postDelayed(gpsrunnable, 1000);

    }

    private void addInterfaceListeners() {

        final OnMapClickListener onMapClickListener = new OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latlng) {
                InputMethodManager imm = (InputMethodManager) MainFragment.this.getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);

                final LatLng latLngFinal = latlng;

            }
        };
        mMap.setOnMapClickListener(onMapClickListener);

        OnMapLongClickListener onMapLongClickListener = new OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latlng) {
                InputMethodManager imm = (InputMethodManager) MainFragment.this.getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);

                final LatLng latLngFinal = latlng;
            }
        };
        mMap.setOnMapLongClickListener(onMapLongClickListener);

    }

    @Override
    public void onStart() {
        super.onStart();

        if (!isGooglePlayServicesAvailable()) {
            getActivity().finish();
        }

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        connectLocationClient();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);

        try
        {
            pilotname = preferences.getString("pilotname", "n/a");
            glidermodel = preferences.getString("glidermodel", "n/a");
            glidercertf = preferences.getString("glidercertf", "n/a");
            civlid = preferences.getString("civlid", "n/a");
            String logtimestr = preferences.getString("log_updates_interval", "3000");
            logtime = (int) Integer.parseInt(logtimestr);
            String gpspowerstr = preferences.getString("gps_power", "100");
            gpspower = (int) Integer.parseInt(gpspowerstr);

            thermicvariovalue = Double.parseDouble(preferences.getString("thermicvariovalue", "2"));
            thermicvariocount = Integer.parseInt(preferences.getString("thermicvariocount", "10"));
            thermicresetavg = Integer.parseInt(preferences.getString("thermicresetavg", "60"));

            drawThermic = preferences.getBoolean("thermicdraw", false);
        }
        catch(NumberFormatException nfe)
        {
            Toast.makeText(mApplicationContext,"Error: Check Settings values...",Toast.LENGTH_SHORT).show();
        }

        livetrackenabled = preferences.getBoolean("livetrackenabled", false);

        username = preferences.getString("liveusername", "").trim();
        password = preferences.getString("livepassword", "").trim();

        if (mGoogleApiClient.isConnected()) {
            createLocationRequest();
            stopLocationUpdates();
            startLocationUpdates();
        }

        // serverUrl="http://test.livetrack24.com";
        serverUrl = "http://www.livetrack24.com/";

        showCompass(false);

        if (taskmanager != null) {

            taskmanager.createTask();

            if (taskmanager.isTaskCreated()) {
                taskmanager.updateTaskTimes(String.valueOf(taskmanager.getTask_Starttime()), String.valueOf(taskmanager.getTask_Endtime()));
                taskhandler.postDelayed(taskrunnable, 1000);
                showCompass(true);
            }
        }

        resetViews();

        if (mMap != null) {

            mMap.setOnCameraChangeListener(this);
            mMap.getUiSettings().setCompassEnabled(true);

            updateOverlay(ConversionUtils.getOverlayString(mApplicationContext));

            addInterfaceListeners();

            if (drawThermic) {
                drawThermals();
            }
        }

        if (barometer) {
            startvario();
        }


        if (loginLW && !livetrackenabled) {
            setLivePos emitPos = new setLivePos();
            emitPos.execute(3);
        }
    }

    public void connectLocationClient() {
        if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    public void disconnectLocationClient() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            createLocationRequest();
            stopLocationUpdates();
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        disconnectLocationClient();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu pMenu, MenuInflater inflater) {
        super.onCreateOptionsMenu(pMenu, inflater);
        inflater.inflate(R.menu.activity_main, pMenu);
        mGPS = pMenu.getItem(1);
    }


    @Override
    public void onPrepareOptionsMenu(final Menu pMenu) {
        if (isGPSEnabled()) {
            mGPS.setTitle(R.string.menu_button_disable_gps);
        } else {
            mGPS.setTitle(R.string.menu_button_enable_gps);
        }
        super.onPrepareOptionsMenu(pMenu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem pItem) {
        OTPApp app = ((OTPApp) getActivity().getApplication());
        switch (pItem.getItemId()) {
            case R.id.gps_settings:
                Intent myIntent = new Intent(
                        Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(myIntent);
                break;
            case R.id.settings:
                getActivity().startActivityForResult(
                        new Intent(getActivity(), SettingsActivity.class),
                        OTPApp.SETTINGS_REQUEST_CODE);
                break;
            case R.id.main_menu:
                getActivity().startActivityForResult(
                        new Intent(mApplicationContext, MenuActivity.class),
                        OTPApp.MENU_REQUEST_CODE);
                return true;

            default:
                break;
        }

        return false;
    }

    private Boolean isGPSEnabled() {
        return sLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void zoomToLocation(LatLng latlng) {
        if (latlng != null) {
            mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(latlng, OTPApp.defaultMediumZoomLevel));
        }
    }

    public void zoomToTwoPoints(LatLng pointA, LatLng pointB) {
        if ((pointA.latitude != pointB.latitude) && (pointA.longitude != pointB.longitude)) {
            LatLngBounds.Builder boundsCreator = LatLngBounds.builder();

            boundsCreator.include(pointA);
            boundsCreator.include(pointB);

            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsCreator.build(), getResources().getInteger(R.integer.route_zoom_padding)));
        }
    }

    public void updateOverlay(String overlayString) {

        int tile_width = OTPApp.CUSTOM_MAP_TILE_SMALL_WIDTH;
        int tile_height = OTPApp.CUSTOM_MAP_TILE_SMALL_HEIGHT;

        if (overlayString == null) {
            overlayString = ConversionUtils.getOverlayString(mApplicationContext);

        }
        if (mSelectedTileOverlay != null) {
            mSelectedTileOverlay.remove();
        }
        if (overlayString.startsWith(OTPApp.MAP_TILE_GOOGLE)) {
            int mapType = GoogleMap.MAP_TYPE_NORMAL;

            if (overlayString.equals(OTPApp.MAP_TILE_GOOGLE_HYBRID)) {
                mapType = GoogleMap.MAP_TYPE_HYBRID;
            } else if (overlayString.equals(OTPApp.MAP_TILE_GOOGLE_NORMAL)) {
                mapType = GoogleMap.MAP_TYPE_NORMAL;
            } else if (overlayString.equals(OTPApp.MAP_TILE_GOOGLE_TERRAIN)) {
                mapType = GoogleMap.MAP_TYPE_TERRAIN;
            } else if (overlayString.equals(OTPApp.MAP_TILE_GOOGLE_SATELLITE)) {
                mapType = GoogleMap.MAP_TYPE_SATELLITE;
            }
            mMap.setMapType(mapType);
            mMaxZoomLevel = mMap.getMaxZoomLevel();
        } else {
            if (overlayString.equals(getResources().getString(R.string.tiles_mapnik))) {
                mMaxZoomLevel = getResources().getInteger(R.integer.tiles_mapnik_max_zoom);
            } else {
                mMaxZoomLevel = getResources().getInteger(R.integer.tiles_thunder_max_zoom);
            }

            mMap.setMapType(GoogleMap.MAP_TYPE_NONE);

            final String overlay = overlayString.replace("{z}/{x}/{y}", "%3$s/%1$s/%2$s");

            Log.d(TAG, "Log overlay: " + overlay);

            /*CustomUrlTileProvider mTileProvider = new CustomUrlTileProvider(
                    tile_width,
                    tile_height, overlayString);
            mSelectedTileOverlay = mMap.addTileOverlay(
                    new TileOverlayOptions().tileProvider(mTileProvider)
                            .zIndex(OTPApp.CUSTOM_MAP_TILE_Z_INDEX));
            }*/

            mSelectedTileOverlay = mMap.addTileOverlay(new CachingUrlTileProvider(mApplicationContext, tile_width, tile_height) {
                @Override
                public String getTileUrl(int x, int y, int z) {
                    //return String.format("https://a.tile.openstreetmap.org/%3$s/%1$s/%2$s.png",x,y,z);
                    return String.format(overlay, x, y, z);
                }
            }.createTileOverlayOptions());

            mSelectedTileOverlay.setZIndex(OTPApp.CUSTOM_MAP_TILE_Z_INDEX);

            if (mMap.getCameraPosition().zoom > mMaxZoomLevel) {
                mMap.moveCamera(CameraUpdateFactory.zoomTo(mMaxZoomLevel));
            }
        }
    }

    protected void startLocationUpdates() {

        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(mApplicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mApplicationContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        Log.d(TAG, "Location update started ..............: ");

    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        Log.d(TAG, "Location update stopped .......................");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        createLocationRequest();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onCameraChange(CameraPosition position) {
        if (position.zoom > mMaxZoomLevel) {
            mMap.moveCamera(CameraUpdateFactory.zoomTo(mMaxZoomLevel));
        }
    }

    private void stopdevices() {
        try {

            loghandler.removeCallbacks(logrunnable);
            gpshandler.removeCallbacks(gpsrunnable);
            variohandler.removeCallbacks(variorunnable);
            taskhandler.removeCallbacks(taskrunnable);

            if (beeps != null) {
                beeps.onDestroy();
            }
            if (mPSensor != null) {
                mSensorManager.unregisterListener(mPSensorListener);
            }
            if (mTSensor != null) {
                mSensorManager.unregisterListener(mTSensorListener);
            }
        } catch (Exception e) {
        }
    }

    public void generateIGC_onSD(String filename, String val) {
        File myFile = null;
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
            if (!root.exists()) {
                root.mkdirs();
            }
            myFile = new File(root, filename);
            if (!myFile.exists())
                myFile.createNewFile();
            BufferedWriter buf = new BufferedWriter(new FileWriter(myFile, true));
            buf.append(val);
            buf.newLine();
            buf.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void setigcfile() {

        if (logheader) {
            //B
            java.sql.Date date = new java.sql.Date(mCurrentLocation.getTime());
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String igcgpstime = sdf.format(date);
            String igclat = decimalToDMSLat(mCurrentLocation.getLatitude());
            String igclon = decimalToDMSLon(mCurrentLocation.getLongitude());
            //A
            String igcaltpressure = String.format("%05.0f", baroaltitude);
            String igcaltgps = String.format("%05.0f", mCurrentLocation.getAltitude());
            String igcval = "B" + igcgpstime.replace(":", "") + igclat + igclon + "A" + igcaltpressure + igcaltgps;

            if (!livetrackenabled) {
                txt_live.setText("Log: " + String.valueOf(logcount));
            }

            logcount++;

            generateIGC_onSD(logfilename + ".igc", igcval);
        }
    }

    public String decimalToDMSLat(double coord) {
        try {
            String output, degrees, minutes, hemisphere;
            if (coord < 0) {
                coord = -1 * coord;
                hemisphere = "S";
            } else {
                hemisphere = "N";
            }
            double mod = coord % 1;
            int intPart = (int) coord;
            degrees = String.format("%02d", intPart);
            coord = mod * 60;
            DecimalFormat df = new DecimalFormat("00.000");
            minutes = df.format(coord).replace(".", "");
            minutes = minutes.replace(",", "");
            output = degrees + minutes + hemisphere;
            return output;
        } catch (Exception e) {
            return null;
        }
    }

    public String decimalToDMSLon(double coord) {
        try {
            String output, degrees, minutes, hemisphere;
            if (coord < 0) {
                coord = -1 * coord;
                hemisphere = "W";
            } else {
                hemisphere = "E";
            }
            double mod = coord % 1;
            int intPart = (int) coord;
            degrees = String.format("%03d", intPart);
            coord = mod * 60;
            DecimalFormat df = new DecimalFormat("00.000");
            minutes = df.format(coord).replace(".", "");
            minutes = minutes.replace(",", "");
            output = degrees + minutes + hemisphere;
            return output;
        } catch (Exception e) {
            return null;
        }
    }

    private void checkLog() {

        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
        File file = new File(root, logfilename + ".igc");

        boolean find = false;

        if (file.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("B")) {
                        find = true;
                    }
                }
            } catch (IOException e) {
            }
        }

        if (!find) {
            try {
                delete(file);
            } catch (IOException e) {
            }
        }
    }

    void delete(File f) throws IOException {
        if (!f.delete()) {
            new FileNotFoundException("Failed to delete file: " + f);
        }
    }

    private void preparelogfooter() {
        try {

            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = df.format(c.getTime());

            String value = "LXGD CompeoVario Igc Version 1.00" + "\n";
            value = value + ("LXGD Downloaded " + formattedDate);
            generateIGC_onSD(logfilename + ".igc", value);

            logfooter = true;
            logcount = 0;

        } catch (Exception e) {
        }
    }

    private void preparelogheader() {
        try {

            logcount = 0;
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("ddMMyy");
            String formattedDate = df.format(c.getTime());

            String value = "AXXXABC FLIGHT\n";
            value = value + "HFFXA035\n";
            value = value + "HFDTE" + formattedDate + "\n";
            value = value + "HFPLTPILOTINCHARGE: " + pilotname + "\n";
            value = value + "HFCM2CREW2: " + pilotname + "\n";
            value = value + "HFGTYGLIDERTYPE: " + glidermodel + "\n";
            value = value + "HFGIDGLIDERID: " + civlid + "\n";
            value = value + "HFDTMNNNGPSDATUM: WGS-1984\n";
            value = value + "HFRFWFIRMWAREVERSION: 1\n";
            value = value + "HFRHWHARDWAREVERSION: 1\n";
            value = value + "HFFTYFRTYPE: Turkay Biliyor CompeoVario\n";
            value = value + "HFPRSPRESSALTSENSOR: Phone Sensor\n";
            value = value + "HFCIDCOMPETITIONID: " + civlid + "\n";
            value = value + "HFCCLCOMPETITIONCLASS: " + glidercertf;

            File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
            Toast.makeText(mApplicationContext, "Flight Log Started:\n" + root.toString(), Toast.LENGTH_LONG).show();

            generateIGC_onSD(logfilename + ".igc", value);

            logheader = true;

        } catch (Exception e) {
        }
    }

    public void startvario() {
        try {
            pressure_hPa_filter_ = new KalmanFilter(KF_VAR_ACCEL);
            pressure_hPa_filter_.reset(pressure_hPa_);
            last_measurement_time_ = SystemClock.elapsedRealtime() / 1000.;
            mSensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
            mPSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            mTSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            variohandler.postDelayed(variorunnable, 1000);
            mPSensorListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    pressure_hPa_ = event.values[0];
                }

                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            };
            mTSensorListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    d_temperature = event.values[0];
                }

                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            };
            if (mPSensor != null) {
                mSensorManager.registerListener(mPSensorListener, mPSensor, SensorManager.SENSOR_DELAY_FASTEST);
            }
            if (mTSensor != null) {
                mSensorManager.registerListener(mTSensorListener, mTSensor, SensorManager.SENSOR_DELAY_FASTEST);
            }
        } catch (Exception e) {
        }
    }

    public double calculateThermicRadius(List<LatLng> lastLatLng) {
        int radius = 0;

        if (lastLatLng.size() > 0) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (int i = 0; i < lastLatLng.size(); i++) {

                builder.include(lastLatLng.get(i));
            }

            LatLngBounds bounds = builder.build();
            LatLng neast = bounds.northeast;
            LatLng swest = bounds.southwest;
            double distance = SphericalUtil.computeDistanceBetween(neast, swest);
            radius = (int) distance;
        }

        return radius;
    }

    public void setvario(double value) {
        if (oldalt != 0) {
            try {
                if (!barometer) {
                    baroaltitude = value;
                    baroaltitude = ((damping * baroaltitude / 100) + ((100 - (damping)) * oldalt / 100));
                    avgvario = baroaltitude - oldalt;
                } else {
                    final double curr_measurement_time = SystemClock.elapsedRealtime() / 1000.;
                    final double dt = curr_measurement_time - last_measurement_time_;
                    pressure_hPa_filter_.update(pressure_hPa_, KF_VAR_MEASUREMENT, dt);
                    last_measurement_time_ = curr_measurement_time;
                    baroaltitude = (long) hPaToMeter(slp_inHg_, pressure_hPa_filter_.getXAbs());
                    baroaltitude = ((damping * baroaltitude / 100) + ((100 - (damping)) * oldalt / 100));
                }
                avgvario = baroaltitude - oldalt;

                if (barometer) {

                    if ((avgvario >= thermicvariovalue) && (currentLatLng != previousVarioLatLng)) {
                        lastLatLng.add(currentLatLng);
                        if (hasThermicCount >= thermicvariocount && !hasThermic) {
                            double radius = calculateThermicRadius(lastLatLng);
                            createThermic(currentLatLng, radius);
                        }
                        hasThermicCount++;
                        previousVarioLatLng = currentLatLng;
                    } else {
                        hasThermic = false;
                        hasThermicCount = 0;
                        lastLatLng.clear();
                    }
                }

                oldalt = baroaltitude;
                if (avgvario >= 0) {
                    txt_avario.setTextColor(Color.BLUE);
                } else if (avgvario < 0) {
                    txt_avario.setTextColor(Color.RED);
                }

                txt_avario.setText(String.format("VSpeed\n%.1f m/s", avgvario));
                playsound();

               /* if (mTSensor != null)
                    TempText.setText(String.format("%.1f ",d_temperature) + DEGREE +"C");*/

            } catch (Exception e) {
            }
        } else {
            if (!barometer)
                oldalt = value;
            else
                oldalt = hPaToMeter(slp_inHg_, value);
        }
    }

    void playsound() {
        if (beeps != null)
            beeps.setAvgVario(avgvario);
    }

    private BitmapDescriptor vectorToBitmap(@DrawableRes int id, @ColorInt int color, int width, int height) {

        Drawable vectorDrawable = ResourcesCompat.getDrawable(getResources(), id, null);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        DrawableCompat.setTint(vectorDrawable, color);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void addMarker() {

        MarkerOptions options = new MarkerOptions();
        options.icon(vectorToBitmap(R.drawable.arrow, Color.parseColor("#FF0000"), 35, 35));

        options.position(currentLatLng);
        if (currentMarker != null) {
            currentMarker.remove();
            currentMarker = null;
        }

        if (currentMarker == null) {
            currentMarker = mMap.addMarker(options);
        }
        long atTime = mCurrentLocation.getTime();
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date(atTime));
        currentMarker.setTitle(mLastUpdateTime);

        if (previousMarkerLatLng == null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 13));
        }

        CameraPosition cameraPosition = new CameraPosition.Builder().
                target(currentLatLng).
                tilt(90).
                zoom(mMap.getCameraPosition().zoom).
                bearing(mCurrentLocation.getBearing()).
                build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        if (previousMarkerLatLng != null) {
            Polyline line = mMap.addPolyline(new PolylineOptions()
                    .add(previousMarkerLatLng, currentLatLng)
                    .width(5)
                    .color(Color.GRAY));
        }

        previousMarkerLatLng = currentLatLng;
    }

    public synchronized double getWindSpeed() {
        return Math.sqrt(wind[0] * wind[0] + wind[1] * wind[1]);
    }

    public synchronized double getWindSpeedError() {
        return Math.sqrt(windError[0] * windError[0] + windError[1] * windError[1]);
    }

    public synchronized double getAirSpeed() {
        return wind[2];
    }

    public synchronized double getAirSpeedError() {
        return windError[2];
    }

    public synchronized double[] getWindError() {
        return ArrayUtil.copy(windError);
    }

    public synchronized double[] getWind() {
        return ArrayUtil.copy(wind);
    }

    public synchronized double getWindDirection() {
        return resolveDegrees(Math.toDegrees(Math.atan2(wind[1], wind[0])));
    }

    public double resolveDegrees(double degrees) {
        if (degrees < 0.0) {
            return resolveDegrees(degrees + 360.0);

        }
        if (degrees > 360.0) {
            return resolveDegrees(degrees - 360.0);
        }
        return degrees;
    }

    public void createThermic(LatLng tlatlon, double radius) {
        liftps = new ThermalW();
        liftps.index = tpcount;
        liftps.coordinate = tlatlon;
        liftps.radius = radius;
        liftps.name = "T" + String.valueOf(tpcount);
        tpoints.add(tpcount, liftps);

        DecimalFormat df = new DecimalFormat("#.######");

        if (drawThermic) {
            addThermicCircle(tlatlon, "T" + String.valueOf(tpcount), (int) radius, true, false);
        }

        String tdata = "T" + String.valueOf(tpcount)
                + ";" + df.format(tlatlon.latitude)
                + ";" + df.format(tlatlon.longitude)
                + ";" + String.valueOf(radius)
                + "\n";

        addThermicFile(tdata);
        tpcount++;
        hasThermic = true;
    }

    public void addThermicCircle(LatLng center, String name, double radius, boolean clickable, boolean drawPoint) {

        if (mMap != null) {
            thermic_circle.addCircle(center, name, radius, clickable, drawPoint);
        }
    }

    public void addThermicFile(String data) {

        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
        File myFile = new File(root, thermicFileName);
        StringBuilder newThermic = new StringBuilder();

        if (myFile.exists()) {
            try {

                in_s = new FileInputStream(myFile);

                if (in_s != null) {
                    String str = "";
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in_s));
                        while ((str = reader.readLine()) != null) {
                            newThermic.append(str + "\n");
                        }

                    } finally {
                        try {
                            in_s.close();
                        } catch (Throwable ignore) {
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
        newThermic.append(data);
        createThermicFile(newThermic.toString());
    }

    public void createThermicFile(String data) {

        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
        File myFile = new File(root, thermicFileName);
        StringBuilder newThermic = new StringBuilder();
        newThermic.append(data);

        try {
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(newThermic);

            myOutWriter.close();

            fOut.flush();
            fOut.close();

            //Log.d(TAG, "Log : " + newTAskFile.toString());

        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public void drawThermals() {
        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
        File myFile = new File(root, thermicFileName);

        if (myFile.exists()) {

            try {

                in_s = new FileInputStream(myFile);
                if (in_s != null) {
                    String str = "";
                    StringBuffer buf = new StringBuffer();
                    int i = 0;

                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in_s));
                        while ((str = reader.readLine()) != null) {

                            String[] values = str.toString().split(";");
                            addThermicCircle(new LatLng(Double.valueOf(values[1]), Double.valueOf(values[2])), values[0], Double.valueOf(values[3]), true, false);
                        }

                    } finally {
                        try {
                            in_s.close();
                        } catch (Throwable ignore) {
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    private double calculateAverageGR(List<Double> listavg) {
        Double sum = 0.0;
        for (Double val : listavg) {
            sum += val;
        }
        return sum.doubleValue() / listavg.size();
    }

    private double calculateAverageVario(List<Double> listavg) {
        Double sum = 0.0;
        for (Double val : listavg) {
            sum += val;
        }
        return sum.doubleValue() / listavg.size();
    }

    //The glide ratio is the distance the glider travels through the air divided by the altitude lost.
    public void calculatePerformance(Location location) {

        if (mpreviousGRLocation != null) {

            DecimalFormat df = new DecimalFormat("#.#");
            //double bearing = SphericalUtil.computeHeading(source, destination);
            LatLng fromlatlon = new LatLng(mpreviousGRLocation.getLatitude(), mpreviousGRLocation.getLongitude());
            LatLng tolatlon = new LatLng(location.getLatitude(), location.getLongitude());

            double distance = SphericalUtil.computeDistanceBetween(fromlatlon, tolatlon);
            double altdif = location.getAltitude() - mpreviousGRLocation.getAltitude();
            double timedifsecs = (location.getTime() - mpreviousGRLocation.getTime()) / 1000;

            if (barometer) {
                avgGpsVario.add(avgvario);
            } else {

                gpsvario = 0;

                if (timedifsecs != 0) {
                    gpsvario = altdif / timedifsecs;
                }

                avgGpsVario.add(gpsvario);

                if (gpsvario >= thermicvariovalue) {

                    lastLatLng.add(currentLatLng);

                    if (hasThermicCount >= thermicvariocount && !hasThermic) {
                        double radius = calculateThermicRadius(lastLatLng);
                        createThermic(currentLatLng, radius);
                    }
                    hasThermicCount++;
                } else {
                    hasThermic = false;
                    hasThermicCount = 0;
                    lastLatLng.clear();
                }
            }

            double calcAvgVario = calculateAverageVario(avgGpsVario);

            txt_varioavg.setText("Vario Avg\n" + df.format(calcAvgVario) + " m/s");

            double currentgr = 0;

            if (altdif != 0 && distance > 0) {

                currentgr = distance / altdif;
            }

            avgGR.add(currentgr);

            double calcAvgGr = calculateAverageGR(avgGR);

            txt_gravg.setText("Gr Avg\n" + df.format(calcAvgGr));

            double altovergoal = 0;

            if (taskmanager.isTaskCreated()) {

                if (calcAvgGr != 0) {

                    altovergoal = location.getAltitude() - taskmanager.getGoalAltitude() - (distToGoal / calcAvgGr);
                }

                if(altovergoal < 10000)
                {
                    txt_altgoal.setText("Alt On Goal\n" + df.format(altovergoal / 1000) + " km");
                }
                else
                {
                    txt_altgoal.setText("Alt On Goal\n" + df.format(altovergoal) + " m");
                }
            }

            if (timereset > (int) thermicresetavg) {
                avgGR.clear();
                avgGpsVario.clear();
                timereset = 0;
            }

            timereset = (int) (timereset + timedifsecs);

            /*Log.d(TAG, "LogData: TimeDiff " + String.valueOf((int)timedifsecs)
                    + " Distance: " + String.valueOf((int)distance)
                    + " AltDiff: " + String.valueOf((int)altdif)
                    + " calcAvgGr " + String.valueOf(calcAvgGr)
                    + " AltGoal " + String.valueOf(altovergoal)
                    + " Count " + String.valueOf((int)timereset) + " - " + String.valueOf(thermicresetavg));*/
        }

        mpreviousGRLocation = location;
    }

    protected void createLocationRequest() {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        String gpspowerstr = preferences.getString("gps_power", "100");
        gpspower = (int) Integer.parseInt(gpspowerstr);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(gpspower);
    }

    @Override
    public void onLocationChanged(Location location) {

        if (location.hasAccuracy() == false) {

            txt_live.setText("GPS Waiting");

        } else {

            if(!location.hasAltitude())
            {
                txt_live.setText("Gps Acc " + String.valueOf((int)location.getAccuracy()) + " m");
            }

            updateLocation(location);

            // String radarregion="europe";
            // getFlightData("http://krk.fr24.com/zones/fcgi/"+ radarregion +"_all.json");
        }
    }

    private void getFlightData(String url)
    {
        FlightRadar planeRadar = new FlightRadar(mApplicationContext,mMap,mCurrentLocation);
        planeRadar.execute(url);
    }

    public void updateLocation(Location location) {
        mCurrentLocation = location;

        currentLatLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

        if (livetrackenabled) {
            if (!loginLW) {
                txt_live.setText("Live trying");
                setLivePos emitPos = new setLivePos();
                emitPos.execute(1);
            } else {
                setLivePos emitPos = new setLivePos();
                emitPos.execute(2);
            }
        }

        if (takeoffLatLng == null) {
            takeoffLatLng = currentLatLng;
        }

        if (takeoffLatLng != null && !logStarted) {

            if (!logheader && location.hasAltitude()) {
                Calendar c = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");
                String formattedDate = df.format(c.getTime());
                logfilename = "FlightLog_" + formattedDate.replace(" ", "_");
                preparelogheader();
                logStarted = true;
                loghandler.postDelayed(logrunnable, logtime);
            }
        }

        mLastUpdateTime = getTimeInstance().format(new Date());

        long time = location.getTime();
        Date date = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String strtime = sdf.format(date);
        DecimalFormat df = new DecimalFormat("#.#");

        if (!barometer) {
            setvario(mCurrentLocation.getAltitude());
        }

        txt_altitude.setText("Altitude\n" + df.format(mCurrentLocation.getAltitude()) + " m");
        txt_speed.setText("Speed\n" + df.format(mCurrentLocation.getSpeed() * 3600 / 1000) + " km");

        if (taskmanager.isTaskCreated()) {

            distToEdge = taskmanager.getDistanceToCircle(currentLatLng, activeWp);
            //distToEdge = taskmanager.getDistanceToEdge(currentLatLng);
            distToGoal = taskmanager.getDistanceToGoal(currentLatLng, activeWp);
            distToTakeoff = SphericalUtil.computeDistanceBetween(currentLatLng, takeoffLatLng);
            bearingToEdge = taskmanager.getBearingToEdge(currentLatLng);
            //needle.rotate((int) (-1 * bearingToEdge));
            float H = (int) bearingToEdge;
            float M = mCurrentLocation.getBearing();

            if (M > H) {
                needle.rotate((int) (-1 * (H + (360 - M))));
            } else {
                needle.rotate((int) (-1 * (H - M)));
            }

            /*Log.d(TAG, "LogData: points " + String.valueOf(taskmanager.getTaskPointCount())
                    + " active point " + String.valueOf(activeWp));*/

            if (taskmanager.checkIfInCircle(currentLatLng, activeWp) && activeWp < taskmanager.getTaskPointCount() - 1) {

                activeWp++;

                taskmanager.setActiveEp(activeWp);
            }

            txt_distgoal.setText("Dist Goal\n" + df.format(distToGoal / 1000) + " km");
            txt_disttakeoff.setText("Dist Tkf\n" + df.format(distToTakeoff / 1000) + " km");
            txt_activewp.setText(taskmanager.getActiveEpName() + "\n" + df.format(distToEdge / 1000) + " km");

        } else {
            txt_time.setText(strtime);
            txt_distgoal.setText("Dist Tkf\n" + df.format(distToTakeoff / 1000) + " km");
        }
        //new wind calculation
        windCalculator.addSpeedVector(mCurrentLocation.getBearing(), mCurrentLocation.getSpeed() * 3600 / 1000, mCurrentLocation.getTime() / 1000.0);
        headingArray = windCalculator.getPoints();
        if (headingArray.length > 2) {
            wind = FitCircle.taubinNewton(headingArray);
            windError = FitCircle.getErrors(headingArray, wind);
            hasWind = true;
        } else {
            hasWind = false;
        }
        if (hasWind) {
            double windspeed = getWindSpeed();
            if (!Double.isNaN(windspeed) && !Double.isInfinite(windspeed)) {

                float windBearing = (float) getWindDirection() - mCurrentLocation.getBearing();
                float H = windBearing;
                float M = mCurrentLocation.getBearing();

                if (M > H) {
                    compass.rotate((int) (-1 * (H + (360 - M))));
                } else {
                    compass.rotate((int) (-1 * (H - M)));
                }
                if (taskmanager.isTaskCreated()) {
                    txt_disttakeoff.setText("Wind Speed\n" + df.format(windspeed) + " km");
                } else {
                    txt_altgoal.setText("Wind\n" + df.format(windspeed) + " km");
                }
            }
        }

        calculatePerformance(location);

        if (gpsOn) {
            addMarker();
        }
    }

    public void exit() {

        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }

        stopdevices();

        if (takeoffLatLng != null && logStarted) {
            if (!logfooter) {
                preparelogfooter();
            }
        }

        if (livetrackenabled && loginLW) {
            setLivePos emitPos = new setLivePos();
            emitPos.execute(3);
        }

        checkLog();

        getActivity().finish();
        System.exit(0);

        Process.killProcess(Process.myPid());
    }

    private class setLivePos extends AsyncTask<Object, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            errorinfo = "";
            error = false;
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            try {
                type = (Integer) params[0];
                if (!loginLW && type == 1) {

                    liveWriter = new LeonardoLiveWriter(
                            mApplicationContext,
                            serverUrl,
                            username,
                            password,
                            glidermodel,
                            vechiletype,
                            logtime / 1000);

                    liveWriter.emitProlog();

                } else if (loginLW && type == 2) {

                    if (mCurrentLocation.hasAccuracy() && mCurrentLocation.hasAltitude()) {

                        liveWriter.emitPosition(
                                mCurrentLocation.getTime(),
                                mCurrentLocation.getLatitude(),
                                mCurrentLocation.getLongitude(),
                                (float) mCurrentLocation.getAltitude(),
                                (int) mCurrentLocation.getBearing(),
                                mCurrentLocation.getSpeed());

                        LWcount = liveWriter.getLWCount();
                    }

                } else if (loginLW && type == 3) {
                    liveWriter.emitEpilog();
                }
                return true;
            } catch (Exception e) {
                errorinfo = "Live Connection Error";
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (result) {
                error = false;
                if (type == 1) {
                    loginLW = true;
                } else if (type == 3) {
                    loginLW = false;
                    type = 0;
                    LWcount = 0;

                } else {

                    if (livetrackenabled) {
                        if (LWcount != 0) {
                            txt_live.setText("Live : " + String.valueOf(LWcount));
                        }
                    }
                }

            } else {
                error = true;
                txt_live.setText("Live trying");
            }
        }
    }
}

