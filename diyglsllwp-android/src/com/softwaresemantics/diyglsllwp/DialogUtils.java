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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.method.DigitsKeyListener;
import android.widget.EditText;

/**
 * 
 * @author cmh
 * 
 */
public class DialogUtils {

	public static void inputDialog(Context context, String message,
			boolean digitsOnly, final InputDialogCallback callback) {
		AlertDialog.Builder alert = new AlertDialog.Builder(context);

		alert.setTitle(context.getResources().getString(R.string.app_name));
		alert.setMessage(message);

		// Set an EditText view to get user input
		final EditText input = new EditText(context);
		alert.setView(input);
		if (digitsOnly) {
			input.setKeyListener(DigitsKeyListener.getInstance());
		}

		alert.setPositiveButton(context.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				callback.inputValue(value);
			}
		});

		alert.setNegativeButton(context.getResources().getString(R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// do nothing
					}
				});
		alert.show();
	}
}
