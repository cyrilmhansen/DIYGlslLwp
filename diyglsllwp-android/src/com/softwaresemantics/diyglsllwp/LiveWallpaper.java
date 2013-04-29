/*******************************************************************************
 * Copyright Cyril M. Hansen 2013
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License
 ******************************************************************************/
package com.softwaresemantics.diyglsllwp;

import java.io.File;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.android.AndroidWallpaperListener;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService;
import com.badlogic.gdx.files.FileHandle;
import com.softwaresemantics.diyglsllwp.DIYGslSurface;

public class LiveWallpaper extends AndroidLiveWallpaperService {

	private AndroidApplicationConfiguration config;
	private String shaderGLSL = null;

	// TODO improve state management
	private static boolean isRunning = false;

	private static LiveWallpaper instance = null;
	private ApplicationListener listener;
	private LiveWallpaperPrefs prefs;

	public void onCreateApplication() {
		super.onCreateApplication();

		config = new AndroidApplicationConfiguration();
		config.useGL20 = true;

		prefs = new LiveWallpaperPrefs(this);

		reloadShaderAndInitGDX();

		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
	}

	protected void initGDX() {

		if (listener != null) {
			listener.dispose();
		}

		if (shaderGLSL != null) {

			listener = new DIYGslSurface(shaderGLSL,
					prefs.isReductionFactorEnabled(),
					prefs.getReductionFactor(), prefs.isTouchEnabled(),
					prefs.isTimeDithering(), prefs.getTimeDitheringFactor());
		} else {
			// built in default shader
			listener = new DIYGslSurface();
		}

		config = new AndroidApplicationConfiguration();
		config.useGL20 = true;

		initialize(listener, config);
		setRunning(true);

	}

	// handler for shader change
	protected void reloadShader() {
		// Check if custom shader is defined
		File customShader = ShaderGalleryActivity.getCustomShaderLWPFile();
		if (customShader != null) {
			shaderGLSL = (new FileHandle(customShader)).readString();
		}
	}

	protected void reloadShaderAndInitGDX() {
		reloadShader();
		initGDX();
	}

	protected static void notifyShaderChange() {
//		if (isRunning) {
//			instance.reloadShaderAndInitGDX();
//		}
	}

	public boolean isRunning() {
		return isRunning;
	}

	public void setRunning(boolean isRunning) {
		LiveWallpaper.isRunning = isRunning;
		if (isRunning) {
			instance = this;
		} else {
			instance = null;
		}
	}

	// implement AndroidWallpaperListener additionally to ApplicationListener
	// if you want to receive callbacks specific to live wallpapers
	public static class MyLiveWallpaperListener implements
			AndroidWallpaperListener {

		@Override
		public void offsetChange(float xOffset, float yOffset,
				float xOffsetStep, float yOffsetStep, int xPixelOffset,
				int yPixelOffset) {
			Gdx.app.log("LiveWallpaper test", "offsetChange(xOffset:" + xOffset
					+ " yOffset:" + yOffset + " xOffsetSteep:" + xOffsetStep
					+ " yOffsetStep:" + yOffsetStep + " xPixelOffset:"
					+ xPixelOffset + " yPixelOffset:" + yPixelOffset + ")");
		}

		@Override
		public void previewStateChange(boolean isPreview) {
			Gdx.app.log("LiveWallpaper test", "previewStateChange(isPreview:"
					+ isPreview + ")");
		}
	}
}
