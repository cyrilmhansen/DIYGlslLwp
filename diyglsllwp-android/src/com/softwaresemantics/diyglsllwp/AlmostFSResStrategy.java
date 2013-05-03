/*******************************************************************************
 * Copyright Cyril M. Hansen 2013
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License
 * 
 * https://github.com/cyrilmhansen/DIYGlslLwp
 ******************************************************************************/
package com.softwaresemantics.diyglsllwp;

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.badlogic.gdx.backends.android.surfaceview.ResolutionStrategy;

/**
 * Use almost a fullscreen are for LibGDX, exclude only a rectangle vertically
 * (of height : reservedHeight)
 * 
 */
public class AlmostFSResStrategy implements ResolutionStrategy {

	private Context context;
	private int reservedHeight;

	public AlmostFSResStrategy(Context context, int reservedHeight) {
		this.context = context;
		this.reservedHeight = reservedHeight;
	}

	@Override
	public MeasuredDimension calcMeasures(int ignored0, int ignored1) {

		// Effective window dimensions strategy from
		// https://code.google.com/p/enh/

		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display d = wm.getDefaultDisplay();
		DisplayMetrics metrics = new DisplayMetrics();
		d.getMetrics(metrics);
		// since SDK_INT = 1;
		int widthPixels = metrics.widthPixels;
		int heightPixels = metrics.heightPixels;
		try {
			// used when 17 > SDK_INT >= 14; includes window decorations
			// (statusbar bar/menu bar)
			widthPixels = (Integer) Display.class.getMethod("getRawWidth")
					.invoke(d);
			heightPixels = (Integer) Display.class.getMethod("getRawHeight")
					.invoke(d);
		} catch (Exception ignored) {
		}
		try {
			// used when SDK_INT >= 17; includes window decorations (statusbar
			// bar/menu bar)
			Point realSize = new Point();
			Display.class.getMethod("getRealSize", Point.class).invoke(d,
					realSize);
			widthPixels = realSize.x;
			heightPixels = realSize.y;
		} catch (Exception ignored) {
		}

		return new MeasuredDimension(widthPixels, heightPixels - reservedHeight);
	}
}
