package com.compeovario.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;

import com.compeovario.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class FlightRadar extends AsyncTask<String, String, String> {

    static JSONObject jObj = null;
    BufferedReader bufferedReader;
    DefaultHttpClient httpclient;
    String jstring;
    JSONArray nameArray;
    List<JSONArray> jasonDatalist = new ArrayList<JSONArray>();
    Context appContext;
    GoogleMap mMap;
    Location currentLoc;

    public FlightRadar(Context context, GoogleMap mMap, Location currentLoc) {
        jstring = null;
        this.appContext = context;
        this.mMap = mMap;
        this.currentLoc = currentLoc;
        jasonDatalist.clear();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... aurl) {

        String url = aurl[0];
        try {

            StringBuilder builder = new StringBuilder();
            httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            httpget.getRequestLine();
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = entity.getContent();
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                for (String line = null; (line = bufferedReader.readLine()) != null; ) {
                    builder.append(line).append("\n");
                }
                jstring = builder.toString();
                jstring = jstring.replace("pd_callback(", "");
                jstring = jstring.replace("fetch_playback_cb(", "");
                jstring = jstring.replace(");", "");

                bufferedReader.close();
                httpclient.getConnectionManager().shutdown();

            }
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        } catch (Exception e) {
        }
        if (jstring != null) {
            try {

                jObj = new JSONObject(jstring);
                nameArray = jObj.names();
                jstring = null;

                for (int flight = 0; flight < nameArray.length(); flight++) {
                    getJsonData(nameArray.getString(flight));
                }

            } catch (JSONException e) {
            } catch (Exception e) {
            }
        }
        return null;
    }

    protected void onProgressUpdate(String... progress) {
    }

    @Override
    protected void onPostExecute(String unused) {
        try {

            if (jasonDatalist.size() == 0) {
                return;
            }
            for (int i = 0; i < jasonDatalist.size(); i++) {
                addPlaneToMap(jasonDatalist.get(i));
            }

        } catch (Exception e) {
        }
    }

    public void addPlaneToMap(JSONArray data) {
        String fligthname;
        double lat, lon, bearing, distance;

        if (data != null) {
            try {
                lat = Double.parseDouble(data.getString(1).toString());
                lon = Double.parseDouble(data.getString(2).toString());

                LatLng locplane = new LatLng(lat, lon);

                int meters = (int) (Double.parseDouble(data.getString(4).toString()) * 0.3048);

                fligthname = data.getString(13).toString();

                if (fligthname.length() < 1) {
                    fligthname = data.getString(16).toString();
                }

                bearing = Double.parseDouble(data.getString(3).toString());
                distance = distancetoplane(locplane);

                String flightInfo = fligthname + "\n" +
                        "Alt:" + String.valueOf(meters) + "m" + "\n" +
                        "Dist:" + String.format("%d", (int) distance / 1000) + "km";

                //Log.d(TAG, "LogData: " + flightInfo);


               /* MarkerOptions options = new MarkerOptions();
                options.icon(vectorToBitmap(R.drawable.airplane, Color.parseColor("#0000FF"), 30, 30));
                options.position(locplane);
                options.rotation((float) bearing);
                options.title(markerLabel);
                options.flat(true);
                Marker planemarker = mMap.addMarker(options);
                */

            } catch (JSONException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }

    public void getJsonData(String id) {
        JSONArray data;

        try {

            data = jObj.getJSONArray(id);
            if (data != null) {
                jasonDatalist.add(data);
            }
        } catch (JSONException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private BitmapDescriptor vectorToBitmap(@DrawableRes int id, @ColorInt int color, int width, int height) {

        Drawable vectorDrawable = ResourcesCompat.getDrawable(appContext.getResources(), id, null);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        DrawableCompat.setTint(vectorDrawable, color);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private double distancetoplane(LatLng locplane) {

        double distance = 0;
        distance = SphericalUtil.computeDistanceBetween(new LatLng(currentLoc.getLatitude(), currentLoc.getLongitude()), locplane);
        return distance;
    }

}