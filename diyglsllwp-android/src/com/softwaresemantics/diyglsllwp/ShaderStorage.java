package com.softwaresemantics.diyglsllwp;

import java.io.File;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class ShaderStorage {

	static File getDiyGlslLwpShaderFile(Context ctx) {
		return new File(ShaderStorage.getDiyGlslLwpSubDir(ctx, ShaderGalleryActivity.LWP_DIR_NAME), ShaderGalleryActivity.LWP_TXT);
	}

	public static File getCustomShaderLWPFile(Context ctx) {
		// FIXME assert
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
			Log.e(ShaderGalleryActivity.DIY_GLSL_LWP_DIR_NAME, "Unable to access external storage");
	
			// To be tested
			Toast.makeText(
					null,
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
