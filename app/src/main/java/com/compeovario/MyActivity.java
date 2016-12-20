package com.compeovario;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.Manifest;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.Iterator;
//AIzaSyAnxBGS5duVnlerSVLDY9t8hu_vwcDEjH4  - hp
//AIzaSyB91lG4UeCjWxG0aMTxeEjJrl7Lhn87X0Y  - dell
//https://console.developers.google.com/iam-admin/iam/iam-zero

public class MyActivity extends FragmentActivity{

    private MainFragment mainFragment;
    private static final String TAG = "MyActivity";
    private PowerManager.WakeLock wl;
    private LocationManager locManager;
    private LocationListener locListener;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    LocationManager mLocMgr;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.SplashTheme);
        super.onCreate(savedInstanceState);

        sendBroadcast(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MyActivity Tag");
        wl.acquire();

        setContentView(R.layout.activity);

        if (savedInstanceState != null) {
            mainFragment = (MainFragment) getSupportFragmentManager().findFragmentByTag(
                    OTPApp.TAG_FRAGMENT_MAIN_FRAGMENT);//recuperar o tag adecuado e pillar ese fragment

        }

        if (savedInstanceState == null) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager()
                    .beginTransaction();
            mainFragment = new MainFragment();
            fragmentTransaction
                    .replace(R.id.mainFragment, mainFragment, OTPApp.TAG_FRAGMENT_MAIN_FRAGMENT);
            fragmentTransaction.commit();
        }

        //GetCurrentLocation();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onNewIntent(Intent intent) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case OTPApp.SETTINGS_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    boolean changedTileProvider = data
                            .getBooleanExtra(OTPApp.CHANGED_MAP_TILE_PROVIDER_RETURN_KEY, false);

                    if (changedTileProvider) {
                        mainFragment.updateOverlay(null);
                    }

                    break;
                }
        }
    }

    @Override
    protected void onDestroy() {

        mainFragment = null;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        wl.release();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Handle the back button
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.myDialog));
            alertDialogBuilder
                    .setMessage("Are you sure you want exit?")
                    .setCancelable(true)
                    .setPositiveButton("YES",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    mainFragment.exit();
                                }
                            })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //  Action for 'NO' Button
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("My Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();



        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    private void GetCurrentLocation() {
        locListener = new LocationListener() {
            @Override
            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProviderEnabled(String provider) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProviderDisabled(String provider) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onLocationChanged(Location location) {
                // TODO Auto-generated method stub
                mainFragment.updateLocation(location);
            }
        };
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);

        locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locManager.requestLocationUpdates(0, 0, criteria, locListener, null);

        GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener() {
            @Override
            public void onGpsStatusChanged(int event) {
                if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS || event == GpsStatus.GPS_EVENT_FIRST_FIX) {

                    if (Build.VERSION.SDK_INT >= 23 &&
                            ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    GpsStatus status = locManager.getGpsStatus(null);
                    if (status != null) {
                        Iterable<GpsSatellite> satellites = status.getSatellites();
                        Iterator<GpsSatellite> sat = satellites.iterator();
                        int i = 0;
                        while (sat.hasNext()) {
                            GpsSatellite satellite = sat.next();
                            i++;
                            if (satellite.usedInFix()) {

                            } else {

                            }
                        }
                    }
                }
            }
        };
        locManager.addGpsStatusListener(gpsStatusListener);
    }
}