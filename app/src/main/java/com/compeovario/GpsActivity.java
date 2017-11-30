package com.compeovario;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.compeovario.util.GPSService;

import java.text.DecimalFormat;

public class GpsActivity extends AppCompatActivity {
	final static String DEGREE = "\u00b0";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gps);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		TextView tvLatLabel,tvLonLabel ;
		TextView tvLat1,tvLat2 ;
		TextView tvLon1,tvLon2 ;
		TextView tvAlt ;
		TextView tvAddress ;


		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_gps, container,false);
			tvLatLabel = (TextView)rootView.findViewById(R.id.tvLatLabel);
			tvLonLabel = (TextView)rootView.findViewById(R.id.tvLonLabel);
			tvLat1 = (TextView)rootView.findViewById(R.id.tvLat1);
			tvLat2 = (TextView)rootView.findViewById(R.id.tvLat2);
			tvLon1 = (TextView)rootView.findViewById(R.id.tvLon1);
			tvLon2 = (TextView)rootView.findViewById(R.id.tvLon2);
			tvAlt = (TextView)rootView.findViewById(R.id.tvAlt);
			tvAddress = (TextView)rootView.findViewById(R.id.tvAddress);

			tvLatLabel.setVisibility(View.GONE);
			tvLonLabel.setVisibility(View.GONE);
			tvLat1.setVisibility(View.GONE);
			tvLat2.setVisibility(View.GONE);
			tvLon1.setVisibility(View.GONE);
			tvLon2.setVisibility(View.GONE);
			tvAlt.setVisibility(View.GONE);
			tvAddress.setVisibility(View.GONE);

			//calculateLocation();

			Button btnGetLocation = (Button)rootView.findViewById(R.id.btnGetLocation);
			
			btnGetLocation.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {

					calculateLocation();
				}
			});
			
			return rootView;
		}

		public void  calculateLocation()
		{
			String address = "";
			GPSService mGPSService = new GPSService(getActivity());
			mGPSService.getLocation();

			if (mGPSService.isLocationAvailable == false) {

				// Here you can ask the user to try again, using return; for that
				Toast.makeText(getActivity(), "Your location is not available, please try again.", Toast.LENGTH_SHORT).show();
				return;

				// Or you can continue without getting the location, remove the return; above and uncomment the line given below
				// address = "Location not available";
			} else {

				DecimalFormat df = new DecimalFormat("#.######");

				// Getting location co-ordinates
				double latitude = mGPSService.getLatitude();
				double longitude = mGPSService.getLongitude();
				double altitude = mGPSService.getAltitude();

				address = mGPSService.getLocationAddress();

				tvLat1.setText(df.format(latitude));
				tvLat2.setText(ConvertDecimalToDegMinSec(latitude) + " " + getHemisphereLat(latitude));
				tvLon1.setText(df.format(longitude));
				tvLon2.setText(ConvertDecimalToDegMinSec(longitude) + " " + getHemisphereLon(longitude));

				df = new DecimalFormat("#.#");

				tvAlt.setText("Altitude\n" + df.format(altitude) + " m");

				tvAddress.setText("Address\n" + address);

				tvLatLabel.setVisibility(View.VISIBLE);
				tvLonLabel.setVisibility(View.VISIBLE);
				tvLat1.setVisibility(View.VISIBLE);
				tvLat2.setVisibility(View.VISIBLE);
				tvLon1.setVisibility(View.VISIBLE);
				tvLon2.setVisibility(View.VISIBLE);
				tvAlt.setVisibility(View.VISIBLE);
				tvAddress.setVisibility(View.VISIBLE);
			}
			// make sure you close the gps after using it. Save user's battery power
			mGPSService.closeGPS();
		}

		static public String decimalToDMSLat(double coord) {
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

		static public String decimalToDMSLon(double coord) {
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

		public static String ConvertDecimalToDegMinSec(double coord) {
			String output, degrees, minutes, seconds;
			double mod = coord % 1;
			int intPart = (int) coord;
			degrees = String.valueOf(intPart);
			coord = mod * 60;
			mod = coord % 1;
			intPart = (int) coord;
			if (intPart < 0) {
				// Convert number to positive if it's negative.
				intPart *= -1;
			}
			minutes = String.format("%02d", intPart);
			coord = mod * 60;
			intPart = (int) coord;
			if (intPart < 0) {
				// Convert number to positive if it's negative.
				intPart *= -1;
			}
			seconds = String.format("%02d", intPart);

			output = degrees + DEGREE + " " + minutes + "' " + seconds + "\"";
			return output;
		}

		public static String getHemisphereLat(double coord) {
			if (coord < 0) {
				return "S";
			} else {
				return "N";
			}
		}

		public static String getHemisphereLon(double coord) {
			if (coord < 0) {
				return "W";
			} else {
				return "E";
			}
		}

	}
}
