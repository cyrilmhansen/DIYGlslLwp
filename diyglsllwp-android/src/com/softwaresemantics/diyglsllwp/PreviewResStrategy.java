/*******************************************************************************
 * Copyright Cyril M. Hansen 2013
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License
 * 
 * https://github.com/cyrilmhansen/DIYGlslLwp
 ******************************************************************************/
package com.softwaresemantics.diyglsllwp;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import com.badlogic.gdx.backends.android.surfaceview.ResolutionStrategy;

/**
 * 
 * @author cmh
 *
 */
public class PreviewResStrategy implements ResolutionStrategy {
	
	
	private Context context;
	private int reservedHeight;

	public PreviewResStrategy(Context context, int reservedHeight) {
		this.context = context;
		this.reservedHeight = reservedHeight;
	}

	
	@Override
	public MeasuredDimension calcMeasures(int ignored0, int ignored1) {
		
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);

		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);

		return new MeasuredDimension(size.x, reservedHeight);
	}
}
