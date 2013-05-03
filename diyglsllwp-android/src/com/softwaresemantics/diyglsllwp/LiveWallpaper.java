/*******************************************************************************
 * Copyright Cyril M. Hansen 2013
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License
 * 
 * https://github.com/cyrilmhansen/DIYGlslLwp
 ******************************************************************************/
package com.softwaresemantics.diyglsllwp;

import java.io.File;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.badlogic.gdx.android.AndroidWallpaperListener;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService;
import com.badlogic.gdx.files.FileHandle;

public class LiveWallpaper extends AndroidLiveWallpaperService implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	private AndroidApplicationConfiguration config;
	private String shaderGLSL = null;

	private DIYGslSurface listener;
	private LiveWallpaperPrefs prefs;

	private static LiveWallpaper instance;

	public void onCreateApplication() {
		super.onCreateApplication();

		LiveWallpaper.instance = this;

		config = new AndroidApplicationConfiguration();
		config.useGL20 = true;
		config.depth = 16;
		config.maxSimultaneousSounds = 0;
		config.numSamples = 0;
		config.useAccelerometer = false;
		config.useCompass = false;
		config.useWakelock = false;
		config.stencil = 0;

		PreferenceManager.setDefaultValues(this, R.xml.prefs, false);

		prefs = new LiveWallpaperPrefs(this);
		reloadShader();

		initGDX();

		prefs.registerOnSharedPreferenceChangeListener(this);

		// TODO Add preference for LWP process priority

	}

	protected void initGDX() {
		Log.d("lwp", "initGDX");
		if (listener != null) {
			Log.d("lwp", "listener dispose");
			listener.dispose();
		}

		if (shaderGLSL != null) {
			Log.d("lwp", "new DIYGslSurface");
			listener = new DIYGslSurface(shaderGLSL,
					prefs.isReductionFactorEnabled(),
					prefs.getReductionFactor(), prefs.isTouchEnabled(),
					prefs.isDisplayFPSLWP(), prefs.isTimeDithering(),
					prefs.getTimeDitheringFactor());
		} else {
			// built in default shader
			Log.d("lwp", "new DIYGslSurface default");
			listener = new DIYGslSurface();
		}

		config = new AndroidApplicationConfiguration();
		config.useGL20 = true;

		Log.d("lwp", "initGDX LWP initialize");
		initialize(listener, config);
	}

	// handler for shader change
	protected void reloadShader() {
		// Check if custom shader is defined
		File customShader = ShaderStorage.getCustomShaderLWPFile(this);
		if (customShader != null) {
			Log.d("lwp", "reloadShader");
			shaderGLSL = (new FileHandle(customShader)).readString();
		} else {
			Log.d("lwp", "reloadShader error");
		}

		if (listener != null) {
			listener.updateShader(shaderGLSL);
		}
	}

	public static void reloadShaderIfNeeded() {
		if (instance != null) {
			instance.reloadShader();
		}
	}

	protected void notifyCfgChange() {

		Log.d("lwp", "notifyCfgChange", new Exception());

		if (listener != null) {
			listener.updatePrefs(prefs.isReductionFactorEnabled(),
					prefs.getReductionFactor(), prefs.isTouchEnabled(),
					prefs.isDisplayFPSLWP(), prefs.isTimeDithering(),
					prefs.getTimeDitheringFactor());
		}

	}

	public static void wakeUpNotify() {
		if (instance != null) {
			instance.notifyCfgChange();
			instance.reloadShader();
		}

	}

	// implement AndroidWallpaperListener additionally to ApplicationListener
	// if you want to receive callbacks specific to live wallpapers
	public class MyLiveWallpaperListener implements AndroidWallpaperListener {

		@Override
		public void offsetChange(float xOffset, float yOffset,
				float xOffsetStep, float yOffsetStep, int xPixelOffset,
				int yPixelOffset) {

			// TODO Manage offset (screen changes)

		}

		@Override
		public void previewStateChange(boolean isPreview) {
			// Not sure of what this means
			Log.d("lwp", "previewStateChange " + isPreview, new Exception());
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// Called repeatedly for each preference item
		// changes should be queued until redisplay
		notifyCfgChange();
	}

}
