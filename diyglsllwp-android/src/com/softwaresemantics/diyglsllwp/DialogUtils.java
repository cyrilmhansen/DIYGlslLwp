/*******************************************************************************
 * Copyright Cyril M. Hansen 2013
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License
 * 
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

		alert.setTitle("Title");
		alert.setMessage(message);

		// Set an EditText view to get user input
		final EditText input = new EditText(context);
		alert.setView(input);
		if (digitsOnly) {
			input.setKeyListener(DigitsKeyListener.getInstance());
		}

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				callback.inputValue(value);
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// do nothing
					}
				});
		alert.show();
	}
}
