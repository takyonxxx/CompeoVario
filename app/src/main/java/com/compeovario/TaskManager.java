package com.compeovario;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;

import com.compeovario.util.CircleUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;


public class TaskManager {

    private static final double RADIUS_OF_EARTH_METERS = 6371009;
    private static final double DEFAULT_RADIUS = 400;
    public static long task_Starttime = 0;
    public static long task_Endtime = 0;
    public final List<Marker> mMarkerTask = new ArrayList<Marker>();
    public ArrayList<TP> taskpoints = new ArrayList<TP>();
    public ArrayList<TP> edgepoints = new ArrayList<TP>();
    public ArrayList<WP> wppoints = new ArrayList<WP>();
    GoogleMap mMap = null;

    int id = 0;
    int altitude = 0;
    int radius = 0;
    int lenght = 0;
    int activeWP = 0;
    double latitude = 0.0;
    double longitude = 0.0;
    double startRadius = 0.0;
    double goalAltitude = 0.0;
    InputStream in_s = null;
    String typeStart = "Start";
    String typeTurn = "Turn";
    String typeFinish = "Finish";
    String startString = "<Point type=";
    String endString = "</Point>";
    String altitudeString = "altitude=";
    String commentString = "comment=";
    String idString = "id=";
    String nameString = "name=";
    String latitudeString = "latitude=";
    String longitudeString = "longitude=";
    String lenghtString = "length=";
    String radiusString = "radius=";
    String typeString = "type=";
    String comment = null;
    String name = null;
    String shape = null;
    String startWp = null;
    String goalWp = null;
    String taskFileName = "Default.tsk";
    String wpFileName = "Default.cup";
    TP tps;
    LatLng currentLatLng = null;
    LatLng nextLatLng = null;
    LatLng previousLatLng = null;
    LatLng edgeLatLng = null;
    LatLng startLatLng = null;
    LatLng goalLatLng = null;
    boolean getStartPoint = false;
    boolean getFinishPoint = false;
    WPManager wpmanager = null;
    private Marker edgeMarker = null;
    private List<CircleUtil> mCircles = new ArrayList<CircleUtil>();
    private Context context;

    public TaskManager(Context current, GoogleMap mMap) {

        this.context = current;
        if (mMap != null) {
            this.mMap = mMap;
        }
        wpmanager = new WPManager(context);
    }

    public static long getTask_Starttime() {
        return task_Starttime;
    }

    public static long getTask_Endtime() {
        return task_Endtime;
    }

    public void createTask() {

        getStartPoint = false;
        getFinishPoint = false;
        startLatLng = null;
        goalLatLng = null;
        previousLatLng = null;
        currentLatLng = null;
        startRadius = 0.0;
        goalAltitude = 0.0;
        edgepoints.clear();
        taskpoints.clear();

        if (mMap != null) {
            mMap.clear();
        }

        File myFile = null;
        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");

        myFile = new File(root, wpFileName);
        wppoints = wpmanager.getWPFromFile(myFile.getAbsolutePath());

        if (wppoints.size() != 0 && mMap != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (int i = 0; i < wppoints.size(); i++) {
                try {

                    LatLng center = new LatLng(Double.parseDouble(wppoints.get(i).latitude.replace(",", ".")), Double.parseDouble(wppoints.get(i).longitude.replace(",", ".")));
                    addCircle(context, center, wppoints.get(i).name, 0, true, false);
                    builder.include(center);

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            LatLngBounds bounds = builder.build();

            int width = context.getResources().getDisplayMetrics().widthPixels;
            int height = context.getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (width * 0.10); // offset from edges of the map 12% of screen

            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding),
                    new GoogleMap.CancelableCallback() {
                        @Override
                        public void onFinish() {
                            if (startLatLng != null) {
                                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder(mMap.getCameraPosition())
                                        .target(startLatLng)
                                        .bearing(0)
                                        .tilt(90)
                                        .build()));
                            }
                        }

                        @Override
                        public void onCancel() {
                        }
                    });
        }

        try {

            root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
            myFile = new File(root, taskFileName);

            if (myFile.exists()) {
                in_s = new FileInputStream(myFile);
            } else
                return;

        } catch (Exception e) {
        }

        if (in_s != null) {
            try {

                String str = "";
                StringBuffer buf = new StringBuffer();

                boolean findWp = false;
                int index = 0;

                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in_s));

                    taskpoints.clear();

                    while ((str = reader.readLine()) != null) {

                        if (str.indexOf(startString) != -1) {
                            findWp = true;
                        }

                        if (findWp) {
                            buf.append(str);
                            if (buf.indexOf(endString) != -1) {
                                addToWaypoint(buf.toString(), index);
                                index++;
                                buf.delete(0, buf.length());
                            }
                        }
                    }

                } finally {
                    try {
                        in_s.close();
                    } catch (Throwable ignore) {
                    }
                }

                if (taskpoints.size() > 0) {
                    startTask();
                }

            } catch (Exception e) {
            }
        }
    }

    public void addCircle(Context context, LatLng center, String name, double radius, boolean clickable, boolean drawPoint) {
        if (mMap != null) {
            CircleUtil circle = new CircleUtil(context, mMap, center, name, radius, clickable, drawPoint);
            mCircles.add(circle);
        }
    }

    public void startTask() {

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        previousLatLng = null;
        int j = 0;

        for (int i = 0; i < taskpoints.size(); i++) {

            currentLatLng = new LatLng(taskpoints.get(i).latitude, taskpoints.get(i).longitude);

            builder.include(currentLatLng);

            addCircle(context, currentLatLng, taskpoints.get(i).name, taskpoints.get(i).radius, true, true);

            if (taskpoints.get(i).type.contains("Start")) {
                startLatLng = currentLatLng;
            }

            if (taskpoints.get(i).type.contains("Finish")) {
                goalLatLng = currentLatLng;
                goalAltitude = taskpoints.get(i).altitude;
            }


            if (previousLatLng != null) {

                edgeLatLng = FindEdge(previousLatLng, currentLatLng, taskpoints.get(i).radius);

                tps = new TP();
                tps.index = taskpoints.get(i).index;
                tps.id = taskpoints.get(i).id;
                tps.name = taskpoints.get(i).name;
                tps.comment = taskpoints.get(i).comment;
                tps.type = taskpoints.get(i).type;
                tps.shape = taskpoints.get(i).shape;
                tps.radius = taskpoints.get(i).radius;
                tps.lenght = taskpoints.get(i).lenght;
                tps.altitude = taskpoints.get(i).altitude;
                tps.latitude = edgeLatLng.latitude;
                tps.longitude = edgeLatLng.longitude;

                edgepoints.add(j, tps);
                j++;

                previousLatLng = edgeLatLng;

            } else {

                if (taskpoints.size() > 1) {
                    nextLatLng = new LatLng(taskpoints.get(i + 1).latitude, taskpoints.get(i + 1).longitude);

                    if (taskpoints.get(i).type.contains("Start")) {

                        startLatLng = currentLatLng;
                        startWp = taskpoints.get(i).name;

                        double bearing = SphericalUtil.computeHeading(startLatLng, nextLatLng);
                        double distance = taskpoints.get(i).radius;
                        edgeLatLng = SphericalUtil.computeOffset(startLatLng, distance, bearing);

                        tps = new TP();
                        tps.index = taskpoints.get(i).index;
                        tps.id = taskpoints.get(i).id;
                        tps.name = taskpoints.get(i).name;
                        tps.comment = taskpoints.get(i).comment;
                        tps.type = taskpoints.get(i).type;
                        tps.shape = taskpoints.get(i).shape;
                        tps.radius = taskpoints.get(i).radius;
                        tps.lenght = taskpoints.get(i).lenght;
                        tps.altitude = taskpoints.get(i).altitude;
                        tps.latitude = edgeLatLng.latitude;
                        tps.longitude = edgeLatLng.longitude;

                        edgepoints.add(j, tps);
                        j++;

                    } else if (taskpoints.get(i).type.contains("Finish")) {

                        goalLatLng = currentLatLng;
                        goalWp = taskpoints.get(i).name;

                        double bearing = SphericalUtil.computeHeading(currentLatLng, nextLatLng);
                        double distance = 0;
                        if (taskpoints.get(i).shape.contains("Cylinder")) {
                            distance = taskpoints.get(i).radius;
                        } else {
                            distance = taskpoints.get(i).lenght;
                        }

                        edgeLatLng = SphericalUtil.computeOffset(goalLatLng, distance, bearing);

                        tps = new TP();
                        tps.index = taskpoints.get(i).index;
                        tps.id = taskpoints.get(i).id;
                        tps.name = taskpoints.get(i).name;
                        tps.comment = taskpoints.get(i).comment;
                        tps.type = taskpoints.get(i).type;
                        tps.shape = taskpoints.get(i).shape;
                        tps.radius = taskpoints.get(i).radius;
                        tps.lenght = taskpoints.get(i).lenght;
                        tps.altitude = taskpoints.get(i).altitude;
                        tps.latitude = edgeLatLng.latitude;
                        tps.longitude = edgeLatLng.longitude;

                        edgepoints.add(j, tps);
                        j++;
                    } else {

                        double bearing = SphericalUtil.computeHeading(currentLatLng, nextLatLng);
                        double distance = 0;

                        distance = taskpoints.get(i).radius;

                        edgeLatLng = SphericalUtil.computeOffset(currentLatLng, distance, bearing);

                        tps = new TP();
                        tps.index = taskpoints.get(i).index;
                        tps.id = taskpoints.get(i).id;
                        tps.name = taskpoints.get(i).name;
                        tps.comment = taskpoints.get(i).comment;
                        tps.type = taskpoints.get(i).type;
                        tps.shape = taskpoints.get(i).shape;
                        tps.radius = taskpoints.get(i).radius;
                        tps.lenght = taskpoints.get(i).lenght;
                        tps.altitude = taskpoints.get(i).altitude;
                        tps.latitude = edgeLatLng.latitude;
                        tps.longitude = edgeLatLng.longitude;

                        edgepoints.add(j, tps);
                        j++;
                    }
                } else {

                    double bearing = SphericalUtil.computeHeading(currentLatLng, currentLatLng);
                    double distance = 0;

                    distance = taskpoints.get(i).radius;

                    edgeLatLng = SphericalUtil.computeOffset(currentLatLng, distance, bearing);

                    tps = new TP();
                    tps.index = taskpoints.get(i).index;
                    tps.id = taskpoints.get(i).id;
                    tps.name = taskpoints.get(i).name;
                    tps.comment = taskpoints.get(i).comment;
                    tps.type = taskpoints.get(i).type;
                    tps.shape = taskpoints.get(i).shape;
                    tps.radius = taskpoints.get(i).radius;
                    tps.lenght = taskpoints.get(i).lenght;
                    tps.altitude = taskpoints.get(i).altitude;
                    tps.latitude = edgeLatLng.latitude;
                    tps.longitude = edgeLatLng.longitude;

                    edgepoints.add(j, tps);

                    j++;
                }

                previousLatLng = currentLatLng;
            }
        }

        if (mMap != null) {
            LatLngBounds bounds = builder.build();

            int width = context.getResources().getDisplayMetrics().widthPixels;
            int height = context.getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (width * 0.10); // offset from edges of the map 12% of screen

            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding),
                    new GoogleMap.CancelableCallback() {
                        @Override
                        public void onFinish() {
                            if (startLatLng != null) {
                                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder(mMap.getCameraPosition())
                                        .target(startLatLng)
                                        .bearing(0)
                                        .tilt(90)
                                        .build()));
                            }
                        }

                        @Override
                        public void onCancel() {
                        }
                    });

        }

        drawTask();
    }

    private void drawTask() {
        PolylineOptions polyline_options = new PolylineOptions().color(Color.DKGRAY).width(5);
        polyline_options.add(startLatLng);
        LatLng from = startLatLng;
        LatLng to = null;

        for (int i = 0; i < edgepoints.size(); i++) {

            to = new LatLng(edgepoints.get(i).latitude, edgepoints.get(i).longitude);

            if (i > 0) {
                polyline_options.add(to);
                polyline_options.geodesic(true);
            }

            DrawArrowHead(mMap, from, to);

            if (i == edgepoints.size() - 1) {
                DrawGoalLine(mMap, from, to, edgepoints.get(i).lenght);
            }

            from = to;
        }

        Polyline polyline = mMap.addPolyline(polyline_options);

    }

    public void addToWaypoint(String currentWp, int index) {

        int index_id = currentWp.indexOf(idString);
        int index_name = currentWp.indexOf(nameString);
        int index_comment = currentWp.indexOf(commentString);
        int index_type = currentWp.indexOf(typeString);
        int index_radius = currentWp.indexOf(radiusString);
        int index_lenght = currentWp.indexOf(lenghtString);
        int index_altitude = currentWp.indexOf(altitudeString);
        int index_latitude = currentWp.indexOf(latitudeString);
        int index_longitude = currentWp.indexOf(longitudeString);

        tps = new TP();

        if (currentWp.indexOf(typeStart) != -1) {
            tps.type = typeStart;
        } else if (currentWp.indexOf(typeTurn) != -1) {
            tps.type = typeTurn;
        } else if (currentWp.indexOf(typeFinish) != -1) {
            tps.type = typeFinish;
        }

        id = Integer.parseInt(getWpValue(currentWp, idString, index_id));
        name = getWpValue(currentWp, nameString, index_name);
        comment = getWpValue(currentWp, commentString, index_comment);
        altitude = Integer.parseInt(getWpValue(currentWp, altitudeString, index_altitude));
        latitude = Double.parseDouble(getWpValue(currentWp, latitudeString, index_latitude));
        longitude = Double.parseDouble(getWpValue(currentWp, longitudeString, index_longitude));

        String strTemp = null;
        int ObservationZone = currentWp.indexOf("<ObservationZone");
        if (ObservationZone != -1) {
            radius = 0;
            lenght = 0;
            strTemp = currentWp.substring(ObservationZone, currentWp.length());
            index_type = strTemp.indexOf(typeString);
            shape = getWpValue(strTemp, typeString, index_type);

            if (shape.contains("Cylinder")) {

                radius = Integer.parseInt(getWpValue(currentWp, radiusString, index_radius));
            } else if (shape.contains("Line")) {

                lenght = Integer.parseInt(getWpValue(currentWp, lenghtString, index_lenght));
            }
        }

        tps.index = index;
        tps.id = id;
        tps.name = name;
        tps.comment = comment;
        tps.shape = shape;
        tps.radius = radius;
        tps.lenght = lenght;
        tps.altitude = altitude;
        tps.latitude = latitude;
        tps.longitude = longitude;

        taskpoints.add(index, tps);
    }

    public void deleteTaskFile() {

        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
        File myFile = new File(root, taskFileName);

        if (myFile.exists()) {
            myFile.delete();
            Log.d(TAG, "Log File deleted: " + myFile.getName());
        }
    }

    public void setTaskStartTime(String stime) {
        try {

            Calendar c = Calendar.getInstance();
            SimpleDateFormat dateformatTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            SimpleDateFormat dateformat = new SimpleDateFormat("dd/MM/yyyy");

            String dateString = dateformat.format(c.getTime());
            String dateStringTime = dateString + " " + stime;

            Date date = dateformatTime.parse(dateStringTime);
            task_Starttime = date.getTime();

            /*Calendar rightNow = Calendar.getInstance();
            long offset = rightNow.get(Calendar.ZONE_OFFSET) + rightNow.get(Calendar.DST_OFFSET);
            long sinceMid = (rightNow.getTimeInMillis() + offset) % (24 * 60 * 60 * 1000);*/

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void setTaskEndTime(String stime) {
        try {

            Calendar c = Calendar.getInstance();
            SimpleDateFormat dateformatTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            SimpleDateFormat dateformat = new SimpleDateFormat("dd/MM/yyyy");

            String dateString = dateformat.format(c.getTime());
            String dateStringTime = dateString + " " + stime;

            Date date = dateformatTime.parse(dateStringTime);
            task_Endtime = date.getTime();

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public String createTaskTime(String strtask_Starttime, String strtask_Endtime) {

        //<Task fai_finish="0" finish_min_height_ref="AGL" finish_min_height="0" start_max_height_ref="AGL" start_max_height="0" start_max_speed="0" start_requires_arm="0" aat_min_time="10800" type="RT">

        StringBuilder newTaskRow = new StringBuilder();
        String empty = "\t";
        newTaskRow.append("<Task ");
        newTaskRow.append(
                "starttime=" + "\"" + strtask_Starttime + "\""
                        + " endtime=" + "\"" + strtask_Endtime + "\""
                        + ">\n");

        return newTaskRow.toString();
    }

    public void updateTaskTimes(String strtask_Starttime, String strtask_Endtime) {

        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
        File myFile = new File(root, taskFileName);
        StringBuilder newTAskFile = new StringBuilder();

        boolean editfinish = false;

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
                            //newTAskFile.append(str + "\n");
                            if (i == 0) {
                                if (!editfinish) {
                                    newTAskFile.append(createTaskTime(strtask_Starttime, strtask_Endtime));
                                    editfinish = true;
                                }
                            } else {
                                if (!str.contains("Task")) {
                                    newTAskFile.append(str + "\n");
                                }
                            }
                            i++;
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

        createTaskFile(newTAskFile.toString());
    }

    public void copyTaskFile(String sourceFilePath) {

        try {

            FileChannel source = null;
            FileChannel destination = null;

            File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
            File destFile = new File(root, taskFileName);
            File sourceFile = new File(sourceFilePath);

            if (sourceFile.getAbsolutePath().contains("CompeoVario/Default.tsk")) {
                return;
            }

            try {
                source = new FileInputStream(sourceFile).getChannel();
                destination = new FileOutputStream(destFile).getChannel();
                destination.transferFrom(source, 0, source.size());
                Log.d(TAG, "Log Task File copied: " + sourceFile.getName());

            } finally {
                if (source != null) {
                    source.close();
                }
                if (destination != null) {
                    destination.close();
                }
            }

        } catch (Exception e) {
        }
    }

    public String createTaskPoint(String id, String name, String altitude, String latitude, String longitude, String radius, String length, String pointType, String pointShape) {

        StringBuilder newTaskRow = new StringBuilder();
        String empty = "\t";
        newTaskRow.append(empty + "<Point type=" + "\"" + pointType + "\"" + ">\n");
        newTaskRow.append(empty + empty + "<Waypoint"
                + " altitude=" + "\"" + altitude
                + "\"" + " comment=" + "\"" + name
                + "\"" + " id=" + "\"" + id
                + "\"" + " name=" + "\"" + name
                + "\"" + ">\n");

        newTaskRow.append(empty + empty + empty + "<Location"
                + " latitude=" + "\"" + latitude
                + "\"" + " longitude=" + "\"" + longitude
                + "\"" + "/>\n");

        newTaskRow.append(empty + empty + "</Waypoint>\n");

        if (pointShape.contains("Line")) {
            newTaskRow.append(empty + empty + "<ObservationZone"
                    + " length=" + "\"" + length
                    + "\"" + " type=" + "\"" + pointShape
                    + "\"" + "/>\n");
        } else {
            newTaskRow.append(empty + empty + "<ObservationZone"
                    + " radius=" + "\"" + radius
                    + "\"" + " type=" + "\"" + pointShape
                    + "\"" + "/>\n");
        }

        newTaskRow.append(empty + "</Point>\n");

        return newTaskRow.toString();
    }

    public void createTaskFile(String data) {

        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
        File myFile = new File(root, taskFileName);
        StringBuilder newTAskFile = new StringBuilder();

        if (!data.contains("<Task")) {
            newTAskFile.append("<Task> " + "\n" + data + "</Task>");
        } else {
            newTAskFile.append(data + "</Task>");
        }

        try {
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(newTAskFile);

            myOutWriter.close();

            fOut.flush();
            fOut.close();

            //Log.d(TAG, "Log : " + newTAskFile.toString());

        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public void updateTaskFile(String name, String radius, String length, String pointType, String pointShape, int listindex) {

        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
        File myFile = new File(root, taskFileName);
        StringBuilder newTAskFile = new StringBuilder();

        boolean editfinish = false;

        int place = (listindex * 6) + 1;

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

                            if (place <= i && place + 6 > i) {
                                if (!editfinish) {
                                    newTAskFile.append(
                                            createTaskPoint(String.valueOf(taskpoints.get(listindex).id),
                                                    name,
                                                    String.valueOf(taskpoints.get(listindex).altitude),
                                                    String.valueOf(taskpoints.get(listindex).latitude),
                                                    String.valueOf(taskpoints.get(listindex).longitude),
                                                    radius,
                                                    length,
                                                    pointType,
                                                    pointShape)
                                    );
                                    editfinish = true;
                                }
                            } else {
                                if (!str.contains("Task")) {
                                    newTAskFile.append(str + "\n");
                                }
                            }

                            i++;
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
        if (editfinish) {
            createTaskFile(newTAskFile.toString());
            updateTaskTimes(String.valueOf(task_Starttime), String.valueOf(task_Endtime));
        }
    }

    public void addToTaskFile(String name, String type, String shape, int altitude, int radius, int lenght, double latitude, double longitude) {

        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
        File myFile = new File(root, taskFileName);
        StringBuilder newTAskFile = new StringBuilder();

        if (!myFile.exists()) {
            try {
                myFile.createNewFile();
                createTaskFile("");
                updateTaskTimes(String.valueOf(task_Starttime), String.valueOf(task_Endtime));
            } catch (Exception e) {
            }

            Log.d(TAG, "Log File created: " + myFile.getName());
        }

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
                            if (!str.contains("Task")) {
                                newTAskFile.append(str + "\n");
                                i++;
                            }
                        }

                        newTAskFile.append(
                                createTaskPoint(String.valueOf(i),
                                        String.valueOf(name),
                                        String.valueOf(altitude),
                                        String.valueOf(latitude),
                                        String.valueOf(longitude),
                                        String.valueOf(radius),
                                        String.valueOf(lenght),
                                        String.valueOf(type),
                                        String.valueOf(shape)));

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
        createTaskFile(newTAskFile.toString());
        updateTaskTimes(String.valueOf(task_Starttime), String.valueOf(task_Endtime));
    }

    public void moveTaskPoint(int from, int to) {
        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
        File myFile = new File(root, taskFileName);
        StringBuilder newTAskFile = new StringBuilder();

        boolean movetofinish = false;
        boolean movetorevfinish = false;
        boolean movefromfinish = false;

        //from 5 to 6

        int fromplace = 0;

        int toplace = (to * 6) + 1; //7
        int toplacerev = ((to - 1) * 6) + 1; //7

        fromplace = (from * 6) + 1; //7

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
                            if (toplace <= i && toplace + 6 > i) {
                                if (!movetofinish) {

                                    if (to > from) {

                                        String str1 = createTaskPoint(String.valueOf(taskpoints.get(to - 1).id),
                                                String.valueOf(taskpoints.get(to).name),
                                                String.valueOf(taskpoints.get(to).altitude),
                                                String.valueOf(taskpoints.get(to).latitude),
                                                String.valueOf(taskpoints.get(to).longitude),
                                                String.valueOf(taskpoints.get(to).radius),
                                                String.valueOf(taskpoints.get(to).lenght),
                                                String.valueOf(taskpoints.get(to).type),
                                                String.valueOf(taskpoints.get(to).shape));

                                        newTAskFile.append(str1);
                                    }

                                    String str2 = createTaskPoint(String.valueOf(taskpoints.get(to).id),
                                            String.valueOf(taskpoints.get(from).name),
                                            String.valueOf(taskpoints.get(from).altitude),
                                            String.valueOf(taskpoints.get(from).latitude),
                                            String.valueOf(taskpoints.get(from).longitude),
                                            String.valueOf(taskpoints.get(from).radius),
                                            String.valueOf(taskpoints.get(from).lenght),
                                            String.valueOf(taskpoints.get(from).type),
                                            String.valueOf(taskpoints.get(from).shape));
                                    newTAskFile.append(str2);

                                    if (to < from) {
                                        String str3 = createTaskPoint(String.valueOf(taskpoints.get(to + 1).id),
                                                String.valueOf(taskpoints.get(to).name),
                                                String.valueOf(taskpoints.get(to).altitude),
                                                String.valueOf(taskpoints.get(to).latitude),
                                                String.valueOf(taskpoints.get(to).longitude),
                                                String.valueOf(taskpoints.get(to).radius),
                                                String.valueOf(taskpoints.get(to).lenght),
                                                String.valueOf(taskpoints.get(to).type),
                                                String.valueOf(taskpoints.get(to).shape));
                                        newTAskFile.append(str3);
                                    }

                                    movetofinish = true;
                                }
                            } else if (fromplace <= i && fromplace + 6 > i) {
                                if (!movefromfinish) {
                                    movefromfinish = true;
                                }
                            } else {
                                newTAskFile.append(str + "\n");
                            }
                            i++;
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
        createTaskFile(newTAskFile.toString());
        updateTaskTimes(String.valueOf(task_Starttime), String.valueOf(task_Endtime));
    }

    public String getTaskPoint(int listindex) {

        int i = listindex;
        String taskPoint = null;
        taskPoint = String.valueOf(taskpoints.get(i).name)
                + ";" + String.valueOf(taskpoints.get(i).type)
                + ";" + String.valueOf(taskpoints.get(i).shape)
                + ";" + String.valueOf(taskpoints.get(i).radius)
                + ";" + String.valueOf(taskpoints.get(i).lenght);

        return taskPoint;
    }

    public void deleteTaskPoint(int listindex) {

        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
        File myFile = new File(root, taskFileName);
        StringBuilder newTAskFile = new StringBuilder();

        boolean deletefinish = false;

        int place = (listindex * 6) + 1;

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

                            if (place <= i && place + 6 > i) {
                                deletefinish = true;
                            } else {
                                newTAskFile.append(str + "\n");
                            }

                            i++;
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
        createTaskFile(newTAskFile.toString());
    }

    public String getWpValue(String currentWp, String field, int index) {

        int index_tempStart = index + field.length() + 1;
        String tempString = currentWp.substring(index_tempStart, currentWp.length());
        int index_Quote = tempString.indexOf("\"");
        tempString = tempString.substring(0, index_Quote);
        return tempString;
    }

    public LatLng FindEdge(LatLng source, LatLng destination, double destradius) {

        double bearing = SphericalUtil.computeHeading(source, destination);
        double distance = SphericalUtil.computeDistanceBetween(source, destination) - destradius;

        return SphericalUtil.computeOffset(source, distance, bearing);
    }

    private BitmapDescriptor vectorToBitmap(@DrawableRes int id, @ColorInt int color, int width, int height) {

        Drawable vectorDrawable = ResourcesCompat.getDrawable(context.getResources(), id, null);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        DrawableCompat.setTint(vectorDrawable, color);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void DrawArrowHead(GoogleMap mMap, LatLng from, LatLng to) {
        double bearing = SphericalUtil.computeHeading(from, to);
        double distance = SphericalUtil.computeDistanceBetween(from, to);

        MarkerOptions options = new MarkerOptions();
        options.icon(vectorToBitmap(R.drawable.arrow, Color.parseColor("#0000FF"), 30, 30));
        options.position(SphericalUtil.computeOffset(from, distance / 2, bearing));
        options.rotation((float) bearing);
        options.flat(true);
        edgeMarker = mMap.addMarker(options);
        mMarkerTask.add(edgeMarker);
    }

    private void DrawGoalLine(GoogleMap mMap, LatLng from, LatLng to, int lenght) {

        if (goalLatLng != null) {

            double bearing = SphericalUtil.computeHeading(from, to);
            LatLng goal_left = SphericalUtil.computeOffset(to, lenght / 2, bearing - 90);
            LatLng goal_right = SphericalUtil.computeOffset(to, lenght / 2, bearing + 90);

            Polyline line = mMap.addPolyline(new PolylineOptions()
                    .add(goal_left, goal_right)
                    .width(10)
                    .color(Color.RED));
        }
    }

    public int getActiveEp() {
        return activeWP;
    }

    public void setActiveEp(int index) {
        if (index >= 0 && index < edgepoints.size()) {
            activeWP = index;

        } else
            activeWP = 0;
    }

    public LatLng getActiveEpLocation() {
        for (int i = 0; i < edgepoints.size(); i++) {
            if (i == activeWP) {
                return new LatLng(edgepoints.get(i).latitude, edgepoints.get(i).longitude);
            }
        }
        return null;
    }

    public String getActiveEpName() {
        for (int i = 0; i < edgepoints.size(); i++) {
            if (i == activeWP) {
                return edgepoints.get(i).name;
            }
        }
        return null;
    }

    public double getActiveEpRadius() {
        for (int i = 0; i < edgepoints.size(); i++) {
            if (i == activeWP) {
                return edgepoints.get(i).radius;
            }
        }
        return 0;
    }

    public ArrayList<TP> getTaskTurnPoints() {
        return taskpoints;
    }

    public ArrayList<TP> getTaskEdgePoints() {
        return edgepoints;
    }

    public boolean isTaskCreated() {

        if (edgepoints.size() > 0) {
            return true;
        }
        return false;
    }

    public double getDistanceToEdge(LatLng currentLoc) {
        double edgedistance = 0;
        LatLng activeWpLoc = getActiveEpLocation();
        edgedistance = SphericalUtil.computeDistanceBetween(currentLoc, activeWpLoc);
        return edgedistance;
    }

    public double getBearingToEdge(LatLng currentLoc) {
        double bearing = 0;
        LatLng activeWpLoc = getActiveEpLocation();
        bearing = SphericalUtil.computeHeading(currentLoc, activeWpLoc);
        return bearing;
    }

    public double getDistanceToGoal(LatLng currentLoc, int currentWp) {
        double goaldistance = 0;

        for (int i = 0; i < edgepoints.size(); i++) {
            if (i >= currentWp) {
                LatLng wpLoc = new LatLng(edgepoints.get(i).latitude, edgepoints.get(i).longitude);
                goaldistance = goaldistance + SphericalUtil.computeDistanceBetween(currentLoc, wpLoc);
                currentLoc = wpLoc;
            }
        }

        return goaldistance;
    }

    public double getTotalTaskDistance() {
        double taskdistance = 0;
        if (startLatLng != null) {
            LatLng from = startLatLng;
            LatLng to = null;

            for (int i = 0; i < edgepoints.size(); i++) {

                to = new LatLng(edgepoints.get(i).latitude, edgepoints.get(i).longitude);
                double distance = SphericalUtil.computeDistanceBetween(from, to);
                taskdistance = taskdistance + distance;
                from = to;
            }
        }
        return taskdistance;
    }

    public LatLng getStartLatLng() {
        return startLatLng;
    }

    public void setStartLatLng(LatLng startLatLng) {
        this.startLatLng = startLatLng;
    }

    public LatLng getGoalLatLng() {
        return goalLatLng;
    }

    public void setGoalLatLng(LatLng goalLatLng) {
        this.goalLatLng = goalLatLng;
    }

    public double getGoalAltitude() {
        return goalAltitude;
    }

    public double getDistanceToCircle(LatLng current, int index) {
        return mCircles.get(index).getDistanceToCircle(current);
    }

    public boolean checkIfInCircle(LatLng current, int index) {
        return mCircles.get(index).checkIfInCircle(current);
    }

    public ArrayList<LatLng> getCirclePoints(LatLng centre, double radius) {
        ArrayList<LatLng> points = new ArrayList<LatLng>();

        double EARTH_RADIUS = 6378100.0;
        // Convert to radians.
        double lat = centre.latitude * Math.PI / 180.0;
        double lon = centre.longitude * Math.PI / 180.0;

        for (double t = 0; t <= Math.PI * 2; t += 0.3) {
            // y
            double latPoint = lat + (radius / EARTH_RADIUS) * Math.sin(t);
            // x
            double lonPoint = lon + (radius / EARTH_RADIUS) * Math.cos(t) / Math.cos(lat);

            // saving the location on circle as a LatLng point
            LatLng point = new LatLng(latPoint * 180.0 / Math.PI, lonPoint * 180.0 / Math.PI);

            // now here note that same point(lat/lng) is used for marker as well as saved in the ArrayList
            points.add(point);

        }

        return points;
    }
}