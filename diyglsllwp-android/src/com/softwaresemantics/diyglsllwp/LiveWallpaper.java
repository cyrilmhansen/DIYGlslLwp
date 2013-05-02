/*******************************************************************************
 * Copyright Cyril M. Hansen 2013
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License
 ******************************************************************************/
package com.softwaresemantics.diyglsllwp;

import java.io.File;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.android.AndroidWallpaperListener;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService;
import com.badlogic.gdx.files.FileHandle;

public class LiveWallpaper extends AndroidLiveWallpaperService implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String RELOAD_PREFS = "RELOAD_PREFS";
	private static final String RELOAD_SHADER = "RELOAD_SHADER";
	private AndroidApplicationConfiguration config;
	private String shaderGLSL = null;

	private ApplicationListener listener;
	private LiveWallpaperPrefs prefs;

	public void onCreateApplication() {
		super.onCreateApplication();

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

		reloadAll();

		prefs.registerOnSharedPreferenceChangeListener(this);

		// FIXME Add preference for LWP process priority

		// android.os.Process
		// .setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
		// Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
	}

	// @Override
	// public Engine onCreateEngine() {
	// // hypothesis : equivalent to resume() ??
	// return super.onCreateEngine();
	//
	// }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		if (RELOAD_SHADER.equals(intent.getAction())) {
			// update shader only
			reloadShaderAndInitGDX();
		}

		if (RELOAD_PREFS.equals(intent.getAction())) {
			reloadAll();
		}

		return Service.START_NOT_STICKY;
	}

	private void reloadAll() {
		prefs = new LiveWallpaperPrefs(this);
		reloadShaderAndInitGDX();
	}

	protected void initGDX() {

		if (listener != null) {
			listener.dispose();
		}

		if (shaderGLSL != null) {

			listener = new DIYGslSurface(shaderGLSL,
					prefs.isReductionFactorEnabled(),
					prefs.getReductionFactor(), prefs.isTouchEnabled(),
					prefs.isDisplayFPSLWP(), prefs.isTimeDithering(),
					prefs.getTimeDitheringFactor());
		} else {
			// built in default shader
			listener = new DIYGslSurface();
		}

		config = new AndroidApplicationConfiguration();
		config.useGL20 = true;

		initialize(listener, config);

	}

	// handler for shader change
	protected void reloadShader() {
		// Check if custom shader is defined
		File customShader = ShaderStorage.getCustomShaderLWPFile(this);
		if (customShader != null) {
			shaderGLSL = (new FileHandle(customShader)).readString();
		}
	}

	protected void reloadShaderAndInitGDX() {
		reloadShader();
		initGDX();
	}

	protected static void notifyShaderChange(Context ctx) {
		// LWP is running in a seperate thread
		// the notification uses ComponentName startService (Intent service)

		// First check that the service is running
		if (isLWPRunning(ctx)) {
			Intent intent = new Intent(ctx.getApplicationContext(),
					LiveWallpaper.class);
			intent.setAction(RELOAD_SHADER);
			// intent.set
			ctx.startService(intent);
			// instance.reloadShaderAndInitGDX();
		}
	}

	protected void notifyCfgChange() {

		// First check that the service is running
		if (isLWPRunning(this)) {
			Intent intent = new Intent(getApplicationContext(),
					LiveWallpaper.class);
			intent.setAction(RELOAD_PREFS);
			// intent.set
			this.startService(intent);
			// instance.reloadShaderAndInitGDX();
		}
	}

	/**
	 * from
	 * http://stackoverflow.com/questions/600207/android-check-if-a-service-
	 * is-running
	 * 
	 * @return true if the service is running (in any thread)
	 */
	private static boolean isLWPRunning(Context ctx) {
		ActivityManager manager = (ActivityManager) ctx
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (LiveWallpaper.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	// implement AndroidWallpaperListener additionally to ApplicationListener
	// if you want to receive callbacks specific to live wallpapers
	public class MyLiveWallpaperListener implements AndroidWallpaperListener {

		@Override
		public void offsetChange(float xOffset, float yOffset,
				float xOffsetStep, float yOffsetStep, int xPixelOffset,
				int yPixelOffset) {
			// TODO

		}

		@Override
		public void previewStateChange(boolean isPreview) {
			// Not sure of what this means
			reloadAll();
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		notifyCfgChange();
	}
}
