package com.compeovario.util;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;

public class GPSService extends Service implements LocationListener {

	// saving the context for later use
	private final Context mContext;

	// if GPS is enabled
	boolean isGPSEnabled = false;
	// if Network is enabled
	boolean isNetworkEnabled = false;
	// if Location co-ordinates are available using GPS or Network
	public boolean isLocationAvailable = false;

	// Location and co-ordinates coordinates
	Location mLocation;
	double mLatitude;
	double mLongitude;
	double mAccuracy;
	double mAltitude;

	// Minimum time fluctuation for next update (in milliseconds)
	private static final long TIME = 0;
	// Minimum distance fluctuation for next update (in meters)
	private static final long DISTANCE = 0;

	// Declaring a Location Manager
	protected LocationManager mLocationManager;

	public GPSService(Context context) {
		this.mContext = context;
		mLocationManager = (LocationManager) mContext
				.getSystemService(LOCATION_SERVICE);

	}

	/**
	 * Returs the Location
	 * 
	 * @return Location or null if no location is found
	 */
	public Location getLocation() {
		try {

			// Getting GPS status
			isGPSEnabled = mLocationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);

			// If GPS enabled, get latitude/longitude using GPS Services
			if (isGPSEnabled) {
				mLocationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER, TIME, DISTANCE, this);
				if (mLocationManager != null) {
					mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					if (mLocation != null) {
						mLatitude = mLocation.getLatitude();
						mLongitude = mLocation.getLongitude();
						mAccuracy = mLocation.getAccuracy();
						mAltitude = mLocation.getAltitude();
						isLocationAvailable = true; // setting a flag that
													// location is available
						return mLocation;
					}
				}
			}

			// If we are reaching this part, it means GPS was not able to fetch
			// any location
			// Getting network status
			isNetworkEnabled = mLocationManager
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

			if (isNetworkEnabled) {
				mLocationManager.requestLocationUpdates(
						LocationManager.NETWORK_PROVIDER, TIME, DISTANCE, this);
				if (mLocationManager != null) {
					mLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					if (mLocation != null) {
						mLatitude = mLocation.getLatitude();
						mLongitude = mLocation.getLongitude();
						mAccuracy = mLocation.getAccuracy();
						mAltitude = mLocation.getAltitude();
						isLocationAvailable = true; // setting a flag that
													// location is available
						return mLocation;
					}
				}
			}
			// If reaching here means, we were not able to get location neither
			// from GPS not Network,
			if (!isGPSEnabled) {
				// so asking user to open GPS
				//askUserToOpenGPS();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		// if reaching here means, location was not available, so setting the
		// flag as false
		isLocationAvailable = false;
		return null;
	}

	/**
	 * Gives you complete address of the location
	 * 
	 * @return complete address in String
	 */
	public String getLocationAddress() {

		if (isLocationAvailable) {

			Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
			// Get the current location from the input parameter list
			// Create a list to contain the result address
			List<Address> addresses = null;
			try {
				/*
				 * Return 1 address.
				 */
				addresses = geocoder.getFromLocation(mLatitude, mLongitude, 1);
			} catch (IOException e1) {
				e1.printStackTrace();
				return ("IO Exception trying to get address:" + e1);
			} catch (IllegalArgumentException e2) {
				// Error message to post in the log
				String errorString = "Illegal arguments "
						+ Double.toString(mLatitude) + " , "
						+ Double.toString(mLongitude)
						+ " passed to address service";
				e2.printStackTrace();
				return errorString;
			}
			// If the reverse geocode returned an address
			if (addresses != null && addresses.size() > 0) {

				String addressText = getCompleteAddressString(mLatitude,mLongitude);

				return addressText;
			} else {
				return "No address found by the service.";
			}
		} else {
			return "Location Not available";
		}

	}

	private String getCompleteAddressString(double LATITUDE, double LONGITUDE) {
		String strAdd = "";
		Geocoder geocoder = new Geocoder(this, Locale.getDefault());
		try {
			List<Address> addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);
			if (addresses != null) {
				Address returnedAddress = addresses.get(0);
				StringBuilder strReturnedAddress = new StringBuilder("");

				for (int i = 0; i < returnedAddress.getMaxAddressLineIndex(); i++) {
					strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
				}
				strAdd = strReturnedAddress.toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return strAdd;
	}

	/**
	 * get latitude
	 * 
	 * @return latitude in double
	 */
	public double getLatitude() {
		if (mLocation != null) {
			mLatitude = mLocation.getLatitude();
		}
		return mLatitude;
	}

	/**
	 * get longitude
	 * 
	 * @return longitude in double
	 */
	public double getLongitude() {
		if (mLocation != null) {
			mLongitude = mLocation.getLongitude();
		}
		return mLongitude;
	}

	public double getAccuracy() {
		if (mLocation != null) {
			mAccuracy = mLocation.getAccuracy();
		}
		return mAccuracy;
	}

	public double getAltitude() {
		if (mLocation != null) {
			mAltitude = mLocation.getAltitude();
		}
		return mAltitude;
	}
	
	/**
	 * close GPS to save battery
	 */
	public void closeGPS() {
		if (mLocationManager != null) {
			mLocationManager.removeUpdates(GPSService.this);
		}
	}

	/**
	 * show settings to open GPS
	 */
	public void askUserToOpenGPS() {
		AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(mContext);

		// Setting Dialog Title
		mAlertDialog.setTitle("Location not available, Open GPS?")
		.setMessage("Activate GPS to use use location services?")
		.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				mContext.startActivity(intent);
				}
			})
			.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					}
				}).show();
	}

	/** 
	 * Updating the location when location changes
	 */
	@Override
	public void onLocationChanged(Location location) {
		mLatitude = location.getLatitude();
		mLongitude = location.getLongitude();
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
