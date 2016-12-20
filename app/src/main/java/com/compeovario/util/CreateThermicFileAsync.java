package com.compeovario.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.compeovario.FilesActivity;
import com.compeovario.ThermalW;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CreateThermicFileAsync extends AsyncTask<String, String, String> {

    private ProgressDialog mProgressDialog;
    final List<LatLng> lastLatLng = new ArrayList<LatLng>();

    InputStream in_s = null;
    String thermicFileName = "Thermics.txt";

    Context context;
    String filePath;

    int thermicvariovalue = 0;
    int thermicvariocount = 0;

    String starttime = null,endtime = null,newtime = null,oldtime = null,
            lat = null,lathems = null,firstlathems = null,lastlathems = null,
            lon = null,lonhems = null,firstlonhems = null,lastlonhems = null;
    int count = 0, hasThermicCount = 0, totalwp = 0,thermiccount = 0,maxalt = 0;
    double dlat,dlon,firstlat,firstlon,lastlat,lastlon,alt, oldalt = 0, gpsvario = 0, timedifsecs = 0,altdif = 0;
    boolean hasThermic = false, drawThermic = false;
    LatLng currentLatLng = null;
    StringBuilder newThermic = new StringBuilder();

    public CreateThermicFileAsync(Context context)
    {
        this.context = context;
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setMessage("Creating Thermal Points.\nPlease Wait.");
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setMax(0);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        thermicvariovalue = Integer.parseInt(preferences.getString("thermicvariovalue", "2"));
        thermicvariocount = Integer.parseInt(preferences.getString("thermicvariocount", "10"));
        drawThermic = preferences.getBoolean("thermicdraw", false);

    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        count=0;
        totalwp=0;
        maxalt=0;
        thermiccount=0;
        mProgressDialog.show();
    }
    @Override
    protected String doInBackground(String... aurl) {
        filePath = aurl[0];
        File file = new File(filePath);

        if (file.exists())
        {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    if(line.startsWith("B"))
                    {
                        count++;
                    }
                }
            }
            catch (IOException e) {
            }
            mProgressDialog.setMax(count);
            totalwp = count;
            count = 0;
            file = new File(filePath);
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    try{
                        if(line.startsWith("B")) {
                            lat = line.substring(7, 14);
                            lathems = line.substring(14, 15);
                            lon = line.substring(15, 23);
                            lonhems = line.substring(23, 24);
                            dlat = DDMMmmmToDecimalLat(lat, lathems);
                            dlon = DDMMmmmToDecimalLon(lon, lonhems);
                            String strtrckalt = line.substring(line.indexOf("A") + 6, line.indexOf("A") + 11);
                            alt = Integer.parseInt(strtrckalt);
                            currentLatLng = new LatLng(dlat,dlon);
                            newtime = line.substring(1, 3) + ":" +
                                    line.substring(3, 5) + ":" +
                                    line.substring(5, 7);

                            if (alt > maxalt) {
                                maxalt = (int) alt;
                            }

                            if(oldalt != 0)
                            {
                                altdif = oldalt - alt;
                            }

                            if(oldtime != null) {
                                timedifsecs = getTimeDiff(newtime, oldtime);
                            }

                            if(timedifsecs != 0) {
                                gpsvario = altdif / timedifsecs;

                                if(gpsvario >= thermicvariovalue)
                                {
                                    if(hasThermicCount >= thermicvariocount && !hasThermic) {
                                        double radius = calculateThermicRadius(lastLatLng);
                                        createThermic(currentLatLng, radius);
                                    }
                                    hasThermicCount++;
                                    lastLatLng.add(currentLatLng);
                                }
                                else
                                {
                                    hasThermic = false;
                                    hasThermicCount = 0;
                                    lastLatLng.clear();
                                }
                            }

                            oldalt = alt;
                            oldtime = newtime;

                            if (count == 0) {
                                starttime = line.substring(1, 3) + ":" +
                                        line.substring(3, 5) + ":" +
                                        line.substring(5, 7);
                                firstlat = dlat;
                                firstlathems = lathems;
                                firstlon = dlon;
                                firstlonhems = lonhems;

                            } else if (count == totalwp - 1) {
                                endtime = line.substring(1, 3) + ":" +
                                        line.substring(3, 5) + ":" +
                                        line.substring(5, 7);
                                lastlat = dlat;
                                lastlathems = lathems;
                                lastlon = dlon;
                                lastlonhems = lonhems;
                            }

                            count++;
                            mProgressDialog.setProgress(count);
                        }

                    }catch(Exception e){
                    }
                }
                br.close();
            }
            catch (IOException e) {
            }

        }
        return null;
    }
    protected void onProgressUpdate(String... progress) {
        mProgressDialog.setProgress(Integer.parseInt(progress[0]));
    }
    @Override
    protected void onPostExecute(String unused) {

        mProgressDialog.dismiss();

        createThermicFile(newThermic.toString());

        ((FilesActivity)context).setTxt_filestatus("StartTime: " + starttime + "\nEndTime: " + endtime
                + "\nMax Alt: " + String.valueOf(maxalt) + " m\nTotal Points: " + String.valueOf(thermiccount));
    }
    protected double DDMMmmmToDecimalLat(String coord,String hems)
    {
        String coorddegree=coord.substring(0,2);
        String coordminute=coord.substring(2,7);
        coordminute=coordminute.substring(0,2)+"."+coordminute.substring(2,5);
        double latcoordminute= Double.parseDouble(coordminute);
        latcoordminute=latcoordminute/60;
        DecimalFormat df = new DecimalFormat("0.000000");
        coord=df.format(latcoordminute).substring(2);
        double result=Double.parseDouble(coorddegree + "." + coord);
        if(hems.equals("S"))
            result=-1*result;
        return result;
    }
    protected double DDMMmmmToDecimalLon(String coord,String hems)
    {
        String coorddegree=coord.substring(0,3);
        String coordminute=coord.substring(3,8);
        coordminute=coordminute.substring(0,2)+"."+coordminute.substring(2,5);
        double latcoordminute= Double.parseDouble(coordminute);
        latcoordminute=latcoordminute/60;
        DecimalFormat df = new DecimalFormat("0.000000");
        coord=df.format(latcoordminute).substring(2);
        double result=Double.parseDouble(coorddegree + "." + coord);
        if(hems.equals("W"))
            result=-1*result;
        return result;
    }
    public double getTimeDiff(String timenew, String timeold)
    {
        double diffsecs = 0;
        try
        {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
            Date Date1 = format.parse(timenew);
            Date Date2 = format.parse(timeold);
            long mills = Date1.getTime() - Date2.getTime();

            int Hours = (int) (mills/(1000 * 60 * 60));
            int Mins = (int) (mills/(1000*60)) % 60;
            diffsecs = mills / 1000;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return diffsecs;
    }

    public double calculateThermicRadius(List<LatLng> lastLatLng)
    {
        int radius = 0;

        if(lastLatLng.size() > 0)
        {
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
    public void createThermic(LatLng tlatlon, double radius)
    {
        DecimalFormat df = new DecimalFormat("#.######");
        String tdata = "T" + String.valueOf(thermiccount)
                + ";" + df.format(tlatlon.latitude).replace(",",".")
                + ";" + df.format(tlatlon.longitude).replace(",",".")
                + ";" + String.valueOf(radius)
                + "\n";

        newThermic.append(tdata);
        thermiccount++;
        hasThermic = true;
    }

    public void createThermicFile(String data) {

        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
        File myFile = new File(root, thermicFileName);
        StringBuilder newThermic = new StringBuilder();
        newThermic.append(data);

        if (myFile.exists()) {
            myFile.delete();
        }

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

}
