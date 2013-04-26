package com.softwaresemantics.diyglsllwp;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;

public class LivewallpaperSettings extends PreferenceActivity implements
		OnPreferenceChangeListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);

		// Preference touchPref =
		// getPreferenceManager().findPreference("touch");
		//
		// Preference reducedResolutionPref = getPreferenceManager()
		// .findPreference("reducedResolution");
		//
		// Preference resolutionFactorPref = getPreferenceManager()
		// .findPreference("reductionFactor");
	}

	@Override
	public boolean onPreferenceChange(Preference arg0, Object arg1) {
		Log.d("LivewallpaperSettings", arg0.getKey() + " -> " + arg1.toString());

		// TODO Live update of LWP ?

		return true;
	}

}