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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ShaderEntryArrayAdapter extends ArrayAdapter<Entry> {
	private final Context context;
	private final Entry[] values;

	private static ImageDownloader imageDownloader;

	public ShaderEntryArrayAdapter(Context context, Entry[] values) {
		super(context, R.layout.rowlayout, values);
		this.context = context;
		this.values = values;
		
		imageDownloader = new ImageDownloader(context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
		//rowView.setAlpha(1.0f);

		TextView textView = (TextView) rowView.findViewById(R.id.label);
		//textView.setAlpha(1.0f);

		ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
		//imageView.setAlpha(1.0f);

		textView.setText(values[position].toString());

		if (values[position].getUrl() != null) {
			imageDownloader.download(values[position].getUrl(), imageView);
		}


	
		return rowView;
	}
}
