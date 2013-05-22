/*******************************************************************************
 * Cyril M. Hansen 2013
 * 
 * Licences :
 * Creative Commons Attribution-ShareAlike 3.0
 * Creative Commons Attribution - Partage dans les Mêmes Conditions 3.0 France
 * 
 * http://creativecommons.org/licenses/by-sa/3.0
 * http://creativecommons.org/licenses/by-sa/3.0/fr/
 * 
 * Sources :
 * https://github.com/cyrilmhansen/DIYGlslLwp
 ******************************************************************************/
package com.softwaresemantics.diyglsllwp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

/**
 * 
 * @author cmh
 * 
 */
public class LiveWallpaperPrefs {

	private static final String TIME_DITHERING_FACTOR = "timeDitheringFactor";
	private static final String TIME_DITHERING = "timeDithering";
	private static final String REDUCTION_FACTOR = "reductionFactor";
	private static final String TOUCH = "touch";
	private static final String DISPLAY_FPS_LWP = "displayFPSLWP";
	private static final String REDUCED_RESOLUTION = "reducedResolution";

	private static final String TIME_LOOP = "timeLoop";
	private static final String TIME_LOOP_60 = "60";
	private static final String TIME_LOOP_6PI = "6pi";
	private static final int TIME_LOOP_6PI_MS = (int) (1000 * 6 * Math.PI);

	private static final String FORCE_MEDIUM_P = "forceMediump";

	private static final String TIME_DITHERING_FACTOR_2 = "2";
	private static final String REDUCTION_FACTOR_8 = "8";

	private Context context;
	private SharedPreferences prefs;

	private void loadLWPPrefs() {
		prefs = PreferenceManager.getDefaultSharedPreferences(context);

	}

	public LiveWallpaperPrefs(Context context) {
		this.context = context;
		loadLWPPrefs();
	}

	public boolean isReductionFactorEnabled() {
		return prefs.getBoolean(REDUCED_RESOLUTION, true);
	}

	public boolean isTouchEnabled() {
		return prefs.getBoolean(TOUCH, false);
	}

	public boolean isDisplayFPSLWP() {
		return prefs.getBoolean(DISPLAY_FPS_LWP, false);
	}

	public int getReductionFactor() {
		return Integer.valueOf(prefs.getString(REDUCTION_FACTOR,
				REDUCTION_FACTOR_8));
	}

	public boolean isTimeDithering() {
		return prefs.getBoolean(TIME_DITHERING, true);
	}

	public boolean isForceMediumP() {
		return prefs.getBoolean(FORCE_MEDIUM_P, true);
	}

	public Integer getTimeLoopPeriod() {
		String timeLoopPref = prefs.getString(TIME_LOOP, TIME_LOOP_60);

		if (TIME_LOOP_6PI.equals(timeLoopPref)) {
			return TIME_LOOP_6PI_MS;
		}

		try {
			return Integer.valueOf(timeLoopPref);
		} catch (Exception ex) {
			return null;
		}

	}

	public Integer getTimeDitheringFactor() {
		return Integer.valueOf(prefs.getString(TIME_DITHERING_FACTOR,
				TIME_DITHERING_FACTOR_2));
	}

	public void registerOnSharedPreferenceChangeListener(
			OnSharedPreferenceChangeListener listener) {
		prefs.registerOnSharedPreferenceChangeListener(listener);

	}

}
