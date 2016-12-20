/*
 * Copyright 2012 University of South Florida
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.compeovario.util;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import java.util.Calendar;


import com.compeovario.OTPApp;
import com.compeovario.R;

public class ConversionUtils {


    public static boolean isToday(Calendar cal) {
        Calendar actualTime = Calendar.getInstance();

        return (actualTime.get(Calendar.ERA) == cal.get(Calendar.ERA) &&
                actualTime.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                actualTime.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR));
    }

    public static boolean isTomorrow(Calendar cal) {
        Calendar tomorrowTime = Calendar.getInstance();
        tomorrowTime.add(Calendar.DAY_OF_YEAR, 1);

        return (tomorrowTime.get(Calendar.ERA) == cal.get(Calendar.ERA) &&
                tomorrowTime.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                tomorrowTime.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR));
    }

    public static String getOverlayString(Context context){
        String defaultOverlay = context.getResources().getString(R.string.tiles_mapnik);
        if (context.getResources().getDisplayMetrics().densityDpi < DisplayMetrics.DENSITY_HIGH){
            defaultOverlay = context.getResources().getString(R.string.tiles_mapnik);
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(OTPApp.PREFERENCE_KEY_MAP_TILE_SOURCE, defaultOverlay);
    }

}
