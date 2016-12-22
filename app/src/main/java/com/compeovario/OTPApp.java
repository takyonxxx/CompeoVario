/*
 * Copyright 2011 Marcy Gordon
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.compeovario;

import android.app.Application;

public class OTPApp extends Application {

    public static final int CONNECTION_FAILURE_RESOLUTION_REQUEST_CODE = 9000;

    public static final String BUNDLE_KEY_SETTINGS_INTENT = "timepicker minutes";

    public static final int CHECK_GOOGLE_PLAY_REQUEST_CODE = 3;
    public static final int TASK_REQUEST_CODE = 3;
    public static final int SETTINGS_REQUEST_CODE = 2;
    public static final int FILES_REQUEST_CODE = 1;
    public static final int MENU_REQUEST_CODE = 0;
    public static final int LOCATION_REQUEST_CODE = 4;

    public static final String CHANGED_MAP_TILE_PROVIDER_RETURN_KEY = "ChangedMapTileProvider";

    public static final String TAG_FRAGMENT_MAIN_FRAGMENT = "mainFragment";

    public static final String MAP_TILE_GOOGLE = "Google";

    public static final String MAP_TILE_GOOGLE_HYBRID = "Google hybrid";

    public static final String MAP_TILE_GOOGLE_NORMAL = "Google normal";

    public static final String MAP_TILE_GOOGLE_SATELLITE = "Google satellite";

    public static final String MAP_TILE_GOOGLE_TERRAIN = "Google terrain";

    public static final int CUSTOM_MAP_TILE_SMALL_HEIGHT = 256;

    public static final int CUSTOM_MAP_TILE_SMALL_WIDTH = 256;

    public static final int CUSTOM_MAP_TILE_BIG_HEIGHT = 512;

    public static final int CUSTOM_MAP_TILE_BIG_WIDTH = 512;

    public static final int CUSTOM_MAP_TILE_Z_INDEX = -1;

    public static final String BUNDLE_KEY_MAP_FAILED = "Map failed";

    public static final String BUNDLE_KEY_MAP_CAMERA = "Map Camera";

    public static final String PREFERENCE_KEY_MAP_TILE_SOURCE = "map_tile_source";

    public static final String PREFERENCE_KEY_PREFERENCE_SCREEN = "preferences_screen";

    public static final String TAG = "COMPEOVARIO";

    public static final float defaultMediumZoomLevel = 14;

}
