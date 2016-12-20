package com.compeovario;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by tbiliyor on 01.12.2016.
 */

public class WPManager {
    private Context context;
    public ArrayList<WP> wppoints = new ArrayList<WP>();
    InputStream in_s = null;
    WP wps;
    public WPManager(Context current) {

        this.context = current;
    }

    public ArrayList<WP> getWPFromFile(String sourceFilePath)
    {
        //createWPFromFile(sourceFilePath);

        in_s = null;
        wppoints.clear();

        File myFile = new File(sourceFilePath);
        if (myFile.exists()) {
            try{
                in_s = new FileInputStream(myFile);
            } catch (Exception e) {
        }
        }

        if (in_s != null) {
            String str = "";
            int i = 0;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in_s));
            DecimalFormat df = new DecimalFormat("#.####");

            try{
            while ((str = reader.readLine()) != null) {
                String[] values = str.toString().split(",");

                wps = new WP();

                if(values[0].contains("\""))
                {
                    wps.index = i;
                    wps.name = values[0].replace("\"","");
                    wps.code = values[1];
                    wps.latitude = df.format(ConvertDegreeAngleToDoubleLatitude(values[3]));
                    wps.longitude = df.format(ConvertDegreeAngleToDoubleLongitude(values[4]));
                    wps.altitude = values[5].substring(0,values[5].length() - 3);
                    wppoints.add(i, wps);
                    i++;
                }
            }
            in_s.close();

            } catch (Exception e) {}
        }
        return wppoints;
    }

    public double ConvertDegreeAngleToDoubleLatitude(String point)
    {

        int multiplier = (point.contains("S") || point.contains("W") ? -1 : 1); //handle south and west

        point = point.replaceAll("[^0-9]+", ""); //remove the characters

        double degrees = Double.parseDouble(point.substring(0,2));
        double minutes = Double.parseDouble(point.substring(2,4) + "." + point.substring(4,7)) / 60 ;

        return (degrees + minutes) * multiplier;
    }

    public double ConvertDegreeAngleToDoubleLongitude(String point)
    {

        int multiplier = (point.contains("S") || point.contains("W") ? -1 : 1); //handle south and west

        point = point.replaceAll("[^0-9]+", ""); //remove the characters

        double degrees = Double.parseDouble(point.substring(0,3));
        double minutes = Double.parseDouble(point.substring(3,5) + "." + point.substring(5,8)) / 60;

        return (degrees + minutes ) * multiplier;
    }
}
