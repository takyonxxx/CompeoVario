package com.compeovario.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;

public class CircleUtil {
    public static final double RADIUS_OF_EARTH_METERS = 6371009;
    private static final double DEFAULT_RADIUS = 400;
    private Circle circletask, circlepoint;
    private double radius = 0;
    private Context context;
    private LatLng center;
    private GoogleMap mMap;

    public CircleUtil(Context context, GoogleMap mMap, LatLng center, String name, double radius, boolean clickable, boolean drawPoint) {

        this.radius = radius;
        this.center = center;
        this.context = context;
        this.mMap = mMap;

        circletask = mMap.addCircle(new CircleOptions()
                .center(center)
                .radius(radius)
                .strokeWidth(3)
                .strokeColor(Color.BLACK)
                .fillColor(Color.argb(50, 255, 255, 0))
                .clickable(clickable));

        if (drawPoint) {
            circlepoint = mMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(10)
                    .strokeWidth(5)
                    .strokeColor(Color.BLACK)
                    .fillColor(Color.argb(100, 0, 0, 0))
                    .clickable(clickable));
        }

        addTextMarker(context, center, name);

    }

    private void addTextMarker(Context context, LatLng center, String name) {

        final TextView textView = new TextView(context);
        textView.setText(name);
        textView.setTextSize(10);
        textView.setTypeface(Typeface.DEFAULT_BOLD);

        final Paint paintText = textView.getPaint();

        final Rect boundsText = new Rect();
        paintText.getTextBounds(name, 0, textView.length(), boundsText);
        paintText.setTextAlign(Paint.Align.CENTER);

        final Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        final Bitmap bmpText = Bitmap.createBitmap(boundsText.width() + 2
                * 1, boundsText.height() + 2 * 1, conf);

        final Canvas canvasText = new Canvas(bmpText);
        paintText.setColor(Color.BLACK);

        canvasText.drawText(name, canvasText.getWidth() / 2,
                canvasText.getHeight() - 1 - boundsText.bottom, paintText);

        final MarkerOptions markerOptions = new MarkerOptions()
                .position(center)
                .icon(BitmapDescriptorFactory.fromBitmap(bmpText))
                .anchor(0.5f, 1);

        mMap.addMarker(markerOptions);
    }

    public double getDistanceToCircle(LatLng current) {
        double distance = 0;

        distance = SphericalUtil.computeDistanceBetween(current, center);

        return distance;
    }

    public boolean checkIfInCircle(LatLng current) {
        LatLngBounds bounds = new LatLngBounds.Builder().
                include(SphericalUtil.computeOffset(center, radius, 0)).
                include(SphericalUtil.computeOffset(center, radius, 90)).
                include(SphericalUtil.computeOffset(center, radius, 180)).
                include(SphericalUtil.computeOffset(center, radius, 270)).build();

        if (bounds.contains(current) && SphericalUtil.computeDistanceBetween(current, center) <= this.radius) {
            return true;
        }
        return false;
    }

}