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
