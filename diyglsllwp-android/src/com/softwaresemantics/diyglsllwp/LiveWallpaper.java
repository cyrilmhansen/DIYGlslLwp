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

import java.io.File;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.badlogic.gdx.android.AndroidWallpaperListener;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService;
import com.badlogic.gdx.backends.android.CustomAndroidGDXApp;
import com.badlogic.gdx.files.FileHandle;

public class LiveWallpaper extends AndroidLiveWallpaperService implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String RELOAD_SHADER = "RELOAD_SHADER";

	private AndroidApplicationConfiguration config;
	private String shaderGLSL = null;

	private DIYGslSurface listener;
	private LiveWallpaperPrefs prefs;

	static LiveWallpaper instance;
	static ShaderGalleryActivity galleryAppInstance;

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

		if (galleryAppInstance != null) {
			// Second activity (ie Livewallpaper is active and running)
			// dispose ressources

			Toast.makeText(this, "switch gallery -> LWP", Toast.LENGTH_LONG)
					.show();

			galleryAppInstance.mySurface.dispose();
			galleryAppInstance.getGraphics().clearManagedCaches();
			// galleryAppInstance.destroyGraphics();

			// Change lifecycle to recreate surface on resume

			// if (getGraphicsView() instanceof GLSurfaceViewCupcake)
			// ((GLSurfaceViewCupcake) getGraphicsView()).onPause();
			// if (getGraphicsView() instanceof android.opengl.GLSurfaceView)
			// ((android.opengl.GLSurfaceView) getGraphicsView()).onPause();

			// Maybe we should wait and dot the rest of the init in the first
			// render call.. ??
		}

		// Log.d("lwp", "initGDX");
		// if (listener != null) {
		// Log.d("lwp", "listener dispose");
		// listener.dispose();
		// }

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
		try {
			File customShader = ShaderStorage.getCustomShaderLWPFile(this);
			if (customShader != null) {
				Log.d("lwp", "reloadShader");
				shaderGLSL = (new FileHandle(customShader)).readString();
			} else {
				Log.d("lwp", "reloadShader error");
			}

			if (listener != null && shaderGLSL != null) {
				listener.updateShader(shaderGLSL);
			}
		} catch (Exception ex) {
			Log.e("DiyGlslLWP", "reloadShader", ex);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (RELOAD_SHADER.equals(intent.getAction())) {
			// update shader only
			reloadShaderIfNeeded();
		}

		return super.onStartCommand(intent, flags, startId);
	}

	public static void reloadShaderIfNeeded() {
		if (instance != null) {
			instance.reloadShader();
		}
	}

	protected void notifyCfgChange() {

		// Log.d("lwp", "notifyCfgChange", new Exception());

		if (listener != null) {
			listener.updatePrefs(prefs.isReductionFactorEnabled(),
					prefs.getReductionFactor(), prefs.isTouchEnabled(),
					prefs.isDisplayFPSLWP(), prefs.isTimeDithering(),
					prefs.getTimeDitheringFactor());
		}

	}

	// public static void wakeUpNotify() {
	// reloadShaderIfNeeded();
	// }

	// implement AndroidWallpaperListener additionally to ApplicationListener
	// if you want to receive callbacks specific to live wallpapers
	public class MyLiveWallpaperListener implements AndroidWallpaperListener {

		@Override
		public void offsetChange(float xOffset, float yOffset,
				float xOffsetStep, float yOffsetStep, int xPixelOffset,
				int yPixelOffset) {

			// TODO Manage offset (screen changes)
			// this need shader support

		}

		@Override
		public void previewStateChange(boolean isPreview) {
			// Not sure of what this means
			Log.d("lwp", "previewStateChange " + isPreview, new Exception());
			reloadShader();
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// Called repeatedly for each preference item
		// changes should be queued until redisplay
		// wakeUpNotify();
		// reloadShader();
		notifyCfgChange();
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

}
