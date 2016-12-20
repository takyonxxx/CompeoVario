package com.compeovario;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.compeovario.util.ConversionUtils;

import java.util.ArrayList;
import java.util.Arrays;

;import static android.content.ContentValues.TAG;

public class SettingsActivity extends PreferenceActivity {

    private ListPreference mapTileProvider;

    private PreferenceScreen preferenceScreen;

    private Intent returnIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        returnIntent = new Intent();

        if (savedInstanceState != null) {
            if ((returnIntent = savedInstanceState.getParcelable(OTPApp.BUNDLE_KEY_SETTINGS_INTENT))
                    != null) {
                setResult(RESULT_OK, returnIntent);
            }
        }

        addPreferencesFromResource(R.xml.preference);

        mapTileProvider = (ListPreference) findPreference(OTPApp.PREFERENCE_KEY_MAP_TILE_SOURCE);

        preferenceScreen = (PreferenceScreen) findPreference(
                OTPApp.PREFERENCE_KEY_PREFERENCE_SCREEN);


        String[] entriesArray = getResources().getStringArray(R.array.map_tiles_servers_names);
        ArrayList<String> entries = new ArrayList<String>(Arrays.asList(entriesArray));
        entries.add(OTPApp.MAP_TILE_GOOGLE_NORMAL);
        entries.add(OTPApp.MAP_TILE_GOOGLE_SATELLITE);
        entries.add(OTPApp.MAP_TILE_GOOGLE_HYBRID);
        entries.add(OTPApp.MAP_TILE_GOOGLE_TERRAIN);
        mapTileProvider.setEntries(entries.toArray(new CharSequence[entries.size()]));

        String[] entriesValuesArray = getResources().getStringArray(R.array.map_tiles_servers_urls);
        ArrayList<String> entriesValues = new ArrayList<String>(Arrays.asList(entriesValuesArray));
        entriesValues.add(OTPApp.MAP_TILE_GOOGLE_NORMAL);
        entriesValues.add(OTPApp.MAP_TILE_GOOGLE_SATELLITE);
        entriesValues.add(OTPApp.MAP_TILE_GOOGLE_HYBRID);
        entriesValues.add(OTPApp.MAP_TILE_GOOGLE_TERRAIN);

        mapTileProvider.setEntryValues(entriesValues.toArray(new CharSequence[entriesValues.size()]));

        if (mapTileProvider.getValue() == null) {
            mapTileProvider.setValue(ConversionUtils.getOverlayString(this));
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        String actualMapTileProvider = prefs.getString(OTPApp.PREFERENCE_KEY_MAP_TILE_SOURCE,getResources().getString(R.string.tiles_mapnik));

        if (actualMapTileProvider.equals(getResources().getString(R.string.tiles_mapnik))) {
            mapTileProvider.setSummary(getResources().getString(R.string.mapnik));
        } else if (actualMapTileProvider.equals(getResources().getString(R.string.tiles_cycle))) {
            mapTileProvider.setSummary(getResources().getString(R.string.cycle));
        } else if (actualMapTileProvider.equals(getResources().getString(R.string.tiles_landscape))) {
            mapTileProvider.setSummary(getResources().getString(R.string.landscape));
        } else if (actualMapTileProvider.equals(getResources().getString(R.string.tiles_outdoor))) {
            mapTileProvider.setSummary(getResources().getString(R.string.outdoors));
        } else if (actualMapTileProvider.equals(OTPApp.MAP_TILE_GOOGLE_NORMAL)) {
            mapTileProvider.setSummary(OTPApp.MAP_TILE_GOOGLE_NORMAL);
        } else if (actualMapTileProvider.equals(OTPApp.MAP_TILE_GOOGLE_HYBRID)) {
            mapTileProvider.setSummary(OTPApp.MAP_TILE_GOOGLE_HYBRID);
        } else if (actualMapTileProvider.equals(OTPApp.MAP_TILE_GOOGLE_SATELLITE)) {
            mapTileProvider.setSummary(OTPApp.MAP_TILE_GOOGLE_SATELLITE);
        } else if (actualMapTileProvider.equals(OTPApp.MAP_TILE_GOOGLE_TERRAIN)) {
            mapTileProvider.setSummary(OTPApp.MAP_TILE_GOOGLE_TERRAIN);
        }

        mapTileProvider.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String value = (String) newValue;

                returnIntent.putExtra(OTPApp.CHANGED_MAP_TILE_PROVIDER_RETURN_KEY, true);
                setResult(RESULT_OK, returnIntent);

                if (value.equals(getResources().getString(R.string.tiles_mapnik))) {
                    mapTileProvider.setSummary(getResources().getString(R.string.mapnik));
                } else if (value.equals(getResources().getString(R.string.tiles_cycle))) {
                    mapTileProvider.setSummary(getResources().getString(R.string.cycle));
                } else if (value.equals(getResources().getString(R.string.tiles_landscape))) {
                    mapTileProvider.setSummary(getResources().getString(R.string.landscape));
                } else if (value.equals(getResources().getString(R.string.tiles_outdoor))) {
                    mapTileProvider.setSummary(getResources().getString(R.string.outdoors));
                } else if (value.equals(OTPApp.MAP_TILE_GOOGLE_NORMAL)) {
                    mapTileProvider.setSummary(OTPApp.MAP_TILE_GOOGLE_NORMAL);
                } else if (value.equals(OTPApp.MAP_TILE_GOOGLE_HYBRID)) {
                    mapTileProvider.setSummary(OTPApp.MAP_TILE_GOOGLE_HYBRID);
                } else if (value.equals(OTPApp.MAP_TILE_GOOGLE_SATELLITE)) {
                    mapTileProvider.setSummary(OTPApp.MAP_TILE_GOOGLE_SATELLITE);
                } else if (value.equals(OTPApp.MAP_TILE_GOOGLE_TERRAIN)) {
                    mapTileProvider.setSummary(OTPApp.MAP_TILE_GOOGLE_TERRAIN);
                }

                return true;
            }

        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (returnIntent != null) {
            outState.putParcelable(OTPApp.BUNDLE_KEY_SETTINGS_INTENT, returnIntent);
        }
    }
}
