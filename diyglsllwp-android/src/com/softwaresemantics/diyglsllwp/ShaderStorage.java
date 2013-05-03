/*******************************************************************************
 * Copyright Cyril M. Hansen 2013
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License
 * 
 * https://github.com/cyrilmhansen/DIYGlslLwp
 ******************************************************************************/
package com.softwaresemantics.diyglsllwp;

import java.io.File;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class ShaderStorage {

	static final String JPG = ".jpg";

	static final String TXT = ".txt";

	static final String LWP = "lwp";

	static final String LWP_TXT = "lwp.txt";

	static final String LWP_DIR_NAME = "lwp";

	static File getDiyGlslLwpShaderDir(Context ctx) {
		return ShaderStorage.getDiyGlslLwpSubDir(ctx, LWP_DIR_NAME);
	}

	static File getDiyGlslLwpShaderFile(Context ctx) {
		return new File(getDiyGlslLwpShaderDir(ctx), LWP_TXT);
	}

	public static File getCustomShaderLWPFile(Context ctx) {
		// TODO assert
		File lwpShader = getDiyGlslLwpShaderFile(ctx);
		if (lwpShader.exists()) {
			return lwpShader;
		} else {
			return null;
		}
	}

	public static File getDiyGlslLwpDir(Context ctx) {
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			Log.e(ShaderGalleryActivity.DIY_GLSL_LWP_DIR_NAME,
					"Unable to access external storage : state = "
							+ Environment.getExternalStorageState());

			// To be tested
			Toast.makeText(
					ctx,
					ctx.getResources().getString(
							R.string.unableAccessExtStorage), Toast.LENGTH_LONG)
					.show();

			return null;
		}

		File diyGlslLwpDir = new File(
				Environment.getExternalStorageDirectory(),
				ShaderGalleryActivity.DIY_GLSL_LWP_DIR_NAME);
		return diyGlslLwpDir;
	}

	public static File getDiyGlslLwpSubDir(Context ctx, String subdirName) {
		return new File(getDiyGlslLwpDir(ctx), subdirName);
	}

}
