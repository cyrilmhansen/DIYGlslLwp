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

import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;

public class InternetAsyncShaderTask extends AsyncTask<String, Void, String> {

	private static final String CODE = "code";
	private ShaderGalleryActivity parentActivity;
	private int rowIndex;

	public InternetAsyncShaderTask(ShaderGalleryActivity parentActivity, int rowIndex) {
		this.parentActivity = parentActivity;
		this.rowIndex = rowIndex;
	}

	protected String doInBackground(String... urls) {
		try {
			// URL access...
			Log.d("loading shader from", urls[0]);
			String response = HttpRequest.get(urls[0]).body();

			// Log.d("result (shader)", response);

			// It's really json, code has its own attribute...
			JSONObject json = new JSONObject(response);
			String code = json.getString(CODE);
			Log.d("result (shader)", code);

			// notification fin chargment dans le thread UI
			this.parentActivity.runOnUiThread(new ShaderLauncher(
					parentActivity, code, rowIndex));

			return response;
		} catch (Exception e) {
			// log this anyway
			Log.e("InternetAsyncShaderTask", "error during task", e);

			// let's hope it will not crash again
			return null;
		}
	}

	

	class ShaderLauncher implements Runnable {

		private ShaderGalleryActivity parentActivity;
		private String shader;
		private int rowIndex;

		public ShaderLauncher(ShaderGalleryActivity activity, String shader,
				int rowIndex) {
			this.parentActivity = activity;
			this.shader = shader;
			this.rowIndex = rowIndex;
		}

		public void run() {			
			// TODO : put this code in a method in Shader list Activity ?
			
			parentActivity.runShaderinPreview(shader);
			parentActivity.showButtonForSelection(rowIndex);
			
			// dialog has been setup by main activity
			parentActivity.hideProgressDialog();
		}
	}

	protected void onPostExecute(String s) {

	}
}
