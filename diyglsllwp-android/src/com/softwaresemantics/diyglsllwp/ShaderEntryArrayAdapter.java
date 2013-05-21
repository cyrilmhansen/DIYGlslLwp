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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ShaderEntryArrayAdapter extends ArrayAdapter<Entry> {
	private final Context context;
	private final Entry[] values;

	private static final ImageDownloader imageDownloader = new ImageDownloader();

	public ShaderEntryArrayAdapter(Context context, Entry[] values) {
		super(context, R.layout.rowlayout, values);
		this.context = context;
		this.values = values;
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


		// All buttons are hidden initially
		Button buttonViewFS = (Button) rowView.findViewById(R.id.buttonViewFS);
		Button buttonDownload = (Button) rowView
				.findViewById(R.id.buttonDownload);
		Button buttonEdit = (Button) rowView.findViewById(R.id.buttonEdit);

		// beware : lines in ListActivity are recycled, boolean are not always
		// false by default
		int buttonVisibility = values[position].isSelected() ? View.VISIBLE
				: View.INVISIBLE;

		buttonViewFS.setVisibility(buttonVisibility);
		buttonDownload.setVisibility(buttonVisibility);
		buttonEdit.setVisibility(buttonVisibility);

		// Setup click handler unconditionally

		buttonViewFS.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((ShaderGalleryActivity) context).setupFullScreenView();

			}
		});

		buttonEdit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(context, "Launching Editor", Toast.LENGTH_SHORT)
						.show();

				((ShaderGalleryActivity) context)
						.openCurrentSelectedShaderInSystemEditor();

			}

		});

		buttonDownload.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(context, "Processing Save request",
						Toast.LENGTH_SHORT).show();

				((ShaderGalleryActivity) context).saveCurrentSelectedShader();

			}

		});

		return rowView;
	}
}
