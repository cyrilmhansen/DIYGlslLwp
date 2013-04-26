/*******************************************************************************
 * Copyright Cyril M. Hansen 2013
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License
 ******************************************************************************/
package com.softwaresemantics.diyglsllwp;

import android.content.Context;
import android.graphics.Point;
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
	public MeasuredDimension calcMeasures(int arg0, int arg1) {

		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);

		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;

		return new MeasuredDimension(width, height - reservedHeight);
	}
}
