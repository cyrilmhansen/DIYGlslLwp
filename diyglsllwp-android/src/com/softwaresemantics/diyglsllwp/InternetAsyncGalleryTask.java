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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.os.AsyncTask;
import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;

public class InternetAsyncGalleryTask extends AsyncTask<String, Void, String> {

	private ShaderGalleryActivity parentActivity;
	private int askedPageIndex;


	public InternetAsyncGalleryTask(ShaderGalleryActivity parentActivity,
			int askedPageIndex) {
		
		this.parentActivity = parentActivity;
		this.askedPageIndex = askedPageIndex;
	}

	protected String doInBackground(String... urls) {
		try {
			// URL access...
			String response = HttpRequest.get(urls[0]).body();
			Document doc = Jsoup.parse(response);

			Elements links = doc.getElementsByTag("a");
			int position = 0;

			for (Element link : links) {
				String linkHref = link.attr("href");

				Elements imgs = link.getElementsByTag("img");
				if (imgs.size() == 1) {

					String imgURL = imgs.get(0).attr("src");
				
					// Store 50 results maximum - effective image download will only done if the image is visible
					// thanks to the list adapter
					if (position <= this.parentActivity.getValues().length - 1) {
						String id = linkHref.substring(3);
						this.parentActivity.getValues()[position].setRefId(id);
						this.parentActivity.getValues()[position].setUrl(imgURL);
					}

					position++;
				}

				// Notify in the UI thread to update the GUI
				this.parentActivity
						.runOnUiThread(new UINotifier(parentActivity));
			}

			return doc.text();
		} catch (Exception e) {
			// log this anyway
			Log.e("InternetAsyncGalleryTask", "error during task", e);

			// let's hope it will not crash again
			return null;
		}
	}

	
	class UINotifier implements Runnable {

		private ShaderGalleryActivity parentActivity;

		public UINotifier(ShaderGalleryActivity activity) {
			this.parentActivity = activity;
		}

		public void run() {
			this.parentActivity.galleryUpdated(askedPageIndex);
		}
	}

	protected void onPostExecute(String s) {
	}
}
