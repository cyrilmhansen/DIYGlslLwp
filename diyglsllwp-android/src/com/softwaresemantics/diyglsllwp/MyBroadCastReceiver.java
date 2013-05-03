/*******************************************************************************
 * Copyright Cyril M. Hansen 2013
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License
 * 
 * https://github.com/cyrilmhansen/DIYGlslLwp
 ******************************************************************************/
package com.softwaresemantics.diyglsllwp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Added to make some errors go away from LogCat
 * 
 * @author cmh
 * 
 */
public class MyBroadCastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			// Log.i("Check", "Screen went OFF");
			// Toast.makeText(context, "screen OFF",Toast.LENGTH_LONG).show();
		} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
			// Log.i("Check", "Screen went ON");
			// Toast.makeText(context, "screen ON",Toast.LENGTH_LONG).show();

			// All ressources must be newly reallocated for performance reason
			//LiveWallpaper.wakeUpNotify();
		}
	}
}
