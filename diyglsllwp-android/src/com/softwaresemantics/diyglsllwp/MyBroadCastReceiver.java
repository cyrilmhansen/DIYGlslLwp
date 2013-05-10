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
