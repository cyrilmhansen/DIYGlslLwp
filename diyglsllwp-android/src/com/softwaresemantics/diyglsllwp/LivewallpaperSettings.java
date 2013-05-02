/*******************************************************************************
 * Copyright Cyril M. Hansen 2013
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License
 ******************************************************************************/
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
		setContentView(R.layout.pref_ad);
		addPreferencesFromResource(R.xml.prefs);
	}

	@Override
	public boolean onPreferenceChange(Preference arg0, Object arg1) {
		Log.d("LivewallpaperSettings", arg0.getKey() + " -> " + arg1.toString());

		// TODO Live update of LWP ? How ?
		return true;
	}

}
