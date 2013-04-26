/*******************************************************************************
 * Copyright Cyril M. Hansen 2013
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License
 ******************************************************************************/
package com.softwaresemantics.diyglsllwp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;

// TODO : Message de rechargement au service LWP lorsque l'on selectionne un autre shader pour le fond d'ecran
// Focus sur selection apres clic initial sur liste principale
// Parametres de performance
// crashs
// le deploiement du package plante le JYG3 si le fond d'ecran est actif

/**
 * Main activity / view
 * 
 * 
 * TODO : improve the data model management
 * 
 * simple cache between gallery page resize image to smaller size to improve
 * memory footprint
 * 
 */

public class ShaderGalleryActivity extends AndroidApplication implements
		ScreenshotProcessor {

	private static final String LWP_TXT = "lwp.txt";

	private static final String LWP_DIR_NAME = "lwp";

	private static final String DIY_GLSL_LWP_DIR_NAME = "DiyGlslLwp";

	private static final String COM_HEROKU_GLSL = "com.heroku.glsl";

	private static final String TXT = ".txt";

	private static final int _200_PX = 200;

	// TODO : dynamic layout
	private static final int FS_BUTTON_HEIGHT = 100;

	private static final int REQUEST_SET_LIVE_WALLPAPER = 101;

	public static String CURRENT_FRAG_SHADER_PROGRAM = "CURRENT_FRAG_SHADER_PROGRAM";
	public static String CURRENT_VERT_SHADER_PROGRAM = "CURRENT_VERT_SHADER_PROGRAM";

	private String currentFragShaderProgram = null;

	private Entry[] values;
	private ShaderEntryArrayAdapter adapter;

	// Gallery page numbering as used by glsl.heroku.com (starts at 0)
	private int currentPageIndex = -1;
	private int askedPageIndex = 0;

	private LinearLayout glayout;
	private View rootMainView;
	private Button visibleButtonViewFS;
	private Button visibleButtonSave;
	private Button visibleButtonEdit;

	private int currentSelectedIndex = -1;

	private DIYGslSurface mySurface;

	private String screenShotFilename;

	private int nbElementParPage;

	// Preference access
	LiveWallpaperPrefs prefs;

	// "GL" view
	private View glslView;

	// To be displayed during download tasks
	private ProgressDialog progressDialog;

	public void onWindowFocusChanged(boolean visible) {
		checkIfOnlineOrExit();
	}

	private void checkIfOnlineOrExit() {
		if (!isOnline()) {
			Toast.makeText(this, "Network access required", Toast.LENGTH_LONG);
			this.exit();
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		checkIfOnlineOrExit();

		if (askedPageIndex != currentPageIndex) {
			// gallery page needs to be downloaded
			updateGallery(askedPageIndex);
		} else {
			if (currentSelectedIndex >= 0) {
				// restore current selection
				mList.setSelection(currentSelectedIndex);
			}
		}
	}

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		if (!Gdx.graphics.isGL20Available()) {
			Toast.makeText(ShaderGalleryActivity.this,
					"Open GL ES 2.0 Required", Toast.LENGTH_LONG).show();
			finish();
		}

		// Create a new Handler that is being associated to the
		// main thread.
		handler = new Handler();

		prefs = new LiveWallpaperPrefs(this);

		// extract shader from intent if any
		Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			currentFragShaderProgram = extras
					.getString(CURRENT_FRAG_SHADER_PROGRAM);
		}

		// Save space on phones
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Try to to keep the UI fluid
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

		nbElementParPage = 50;
		values = new Entry[nbElementParPage];
		for (int i = 0; i < nbElementParPage; i++) {
			values[i] = new Entry("#" + i);
		}

		adapter = new ShaderEntryArrayAdapter(this, values);
		setListAdapter(adapter);

		// TODO even more declarative layout
		glayout = new LinearLayout(this);
		glayout.setOrientation(LinearLayout.VERTICAL);

		// Inflate main layout
		LayoutInflater inflater = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		rootMainView = inflater.inflate(R.layout.main, null, false);
		createGDXPreview(currentFragShaderProgram);

		glayout.addView(rootMainView);
		setContentView(glayout);

		findViewById(R.id.nextPageButton).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {

						// Toast.makeText(ShaderGalleryActivity.this,
						// "Processing next page", Toast.LENGTH_SHORT)
						// .show();
						// TODO What if we are at the last page ??
						// FIXME CRASH
						askedPageIndex = currentPageIndex + 1;
						cancelSelectionIfAny();
						updateGallery();

					}
				});

		findViewById(R.id.prevPageButton).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						// Toast.makeText(ShaderGalleryActivity.this,
						// "Processing previous page", Toast.LENGTH_SHORT)
						// .show();
						if (currentPageIndex > 0) {
							askedPageIndex = currentPageIndex - 1;
							cancelSelectionIfAny();
							updateGallery();
						}
					}
				});

		progressDialog = new ProgressDialog(this);
	}

	/**
	 * Setup of app menu
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// Menu action dispatch
		switch (item.getItemId()) {
		// TODO Access wall paper settings
		// TODO Implement other menu options

		// case R.id.option:
		// Toast.makeText(ShaderGalleryActivity.this, "options",
		// Toast.LENGTH_SHORT).show();
		// return true;
		// case R.id.favoris:
		// Toast.makeText(ShaderGalleryActivity.this, "favoris",
		// Toast.LENGTH_SHORT).show();
		// return true;
		// case R.id.stats:
		// Toast.makeText(ShaderGalleryActivity.this, "stats",
		// Toast.LENGTH_SHORT).show();
		// return true;
		case R.id.quitter:
			Toast.makeText(ShaderGalleryActivity.this, "Exiting",
					Toast.LENGTH_SHORT).show();
			finish();
			return true;
		}
		return false;
	}

	private void updateGallery() {
		updateGallery(askedPageIndex);
	}

	private void updateGallery(int pageIndex) {
		if (!isOnline()) {
			Toast.makeText(this, "Network access required", Toast.LENGTH_LONG);
			// ?? call finish();
		} else {
			progressDialog.setMessage("Loading gallery page #" + pageIndex);
			progressDialog.show();

			// differed loading of gallery
			new InternetAsyncGalleryTask(this, pageIndex)
					.execute("http://glsl.heroku.com/?page=" + pageIndex);
		}
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		Entry item = (Entry) getListAdapter().getItem(position);

		hidePreviousSelectionButton();

		progressDialog.setMessage("Loading shader for " + item.getRefId());
		progressDialog.show();

		// shader download task that will call us back to update UI
		new InternetAsyncShaderTask(this, position)
				.execute("http://glsl.heroku.com/item/" + item.getRefId());
	}

	/**
	 * Do what we can to help the user active the builtin Live Wallpaper Code
	 * from stackoverflow...
	 */
	public void setOwnLWP() {
		Intent intent;

		// try the new Jelly Bean direct android wallpaper chooser first
		try {
			ComponentName component = new ComponentName(LiveWallpaper.class
					.getPackage().getName(),
					LiveWallpaper.class.getCanonicalName());
			intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
			intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
					component);
			startActivityForResult(intent, REQUEST_SET_LIVE_WALLPAPER);

			// Toast.makeText(ShaderGalleryActivity.this,
			// "Live wallpaper activated", Toast.LENGTH_SHORT).show();

		} catch (android.content.ActivityNotFoundException e3) {
			// try the generic android wallpaper chooser next
			try {
				intent = new Intent(
						WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
				startActivityForResult(intent, REQUEST_SET_LIVE_WALLPAPER);

				Toast.makeText(ShaderGalleryActivity.this,
						"Manual action required - Select DIY Glsl LWP",
						Toast.LENGTH_LONG).show();
			} catch (android.content.ActivityNotFoundException e2) {
				// that failed, let's try the nook intent
				try {
					intent = new Intent();
					intent.setAction("com.bn.nook.CHANGE_WALLPAPER");
					startActivity(intent);
				} catch (android.content.ActivityNotFoundException e) {
					// everything failed, let's notify the user
					Toast.makeText(ShaderGalleryActivity.this,
							"Error while launching wallpaper selector",
							Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	public Entry[] getValues() {
		return values;
	}

	public void setValues(Entry[] values) {
		this.values = values;
	}

	public void galleryUpdated(int askedPageIndex) {
		adapter.notifyDataSetChanged();
		currentPageIndex = askedPageIndex;

		progressDialog.dismiss();
	}

	protected ListAdapter mAdapter;

	protected ListView mList;

	private Handler mHandler = new Handler();
	private boolean mFinishedStart = false;

	private Runnable mRequestFocus = new Runnable() {
		public void run() {
			mList.focusableViewAvailable(mList);
		}
	};

	/**
	 * Ensures the list view has been created before Activity restores all of
	 * the view states.
	 * 
	 * @see Activity#onRestoreInstanceState(Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle state) {
		ensureList();
		super.onRestoreInstanceState(state);
	}

	/**
	 * @see Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		mHandler.removeCallbacks(mRequestFocus);
		super.onDestroy();
	}

	/**
	 * Updates the screen state (current list and other views) when the content
	 * changes.
	 * 
	 * @see Activity#onContentChanged()
	 */
	@Override
	public void onContentChanged() {
		super.onContentChanged();
		View emptyView = findViewById(android.R.id.empty);
		mList = (ListView) findViewById(android.R.id.list);
		if (mList == null) {
			throw new RuntimeException(
					"Your content must have a ListView whose id attribute is "
							+ "'android.R.id.list'");
		}
		if (emptyView != null) {
			mList.setEmptyView(emptyView);
		}
		mList.setOnItemClickListener(mOnClickListener);
		if (mFinishedStart) {
			setListAdapter(mAdapter);
		}
		mHandler.post(mRequestFocus);
		mFinishedStart = true;

		setSelection(currentSelectedIndex);
	}

	/**
	 * Provide the cursor for the list view.
	 */
	public void setListAdapter(ListAdapter adapter) {
		synchronized (this) {
			ensureList();
			mAdapter = adapter;
			mList.setAdapter(adapter);
		}
	}

	/**
	 * Set the currently selected list item to the specified position with the
	 * adapter's data
	 * 
	 * @param position
	 */
	public void setSelection(int position) {
		mList.setSelection(position);
	}

	/**
	 * Get the position of the currently selected list item.
	 */
	public int getSelectedItemPosition() {
		return mList.getSelectedItemPosition();
	}

	/**
	 * Get the cursor row ID of the currently selected list item.
	 */
	public long getSelectedItemId() {
		return mList.getSelectedItemId();
	}

	/**
	 * Get the activity's list view widget.
	 */
	public ListView getListView() {
		ensureList();
		return mList;
	}

	/**
	 * Get the ListAdapter associated with this activity's ListView.
	 */
	public ListAdapter getListAdapter() {
		return mAdapter;
	}

	private void ensureList() {
		if (mList != null) {
			return;
		}
		setContentView(android.R.layout.list_content);

	}

	private AdapterView.OnItemClickListener mOnClickListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			onListItemClick((ListView) parent, v, position, id);
		}
	};

	public void runShaderinPreview(String code) {
		if (glslView != null) {
			// Remove previously created view
			glayout.removeView(glslView);
		}
		createGDXPreview(code);
	}

	private void createGDXPreview(String code) {
		AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
		cfg.useGL20 = true;
		cfg.resolutionStrategy = new PreviewResStrategy(this, _200_PX);

		// TODO : settings for preview
		mySurface = new DIYGslSurface(code, true, 4, true, true, 4);
		mySurface.setScreenshotProc(this);
		glslView = initializeForView(mySurface, cfg);

		currentFragShaderProgram = code;

		glayout.addView(glslView, 0);
	}

	public void hidePreviousSelectionButton() {
		if (visibleButtonViewFS != null) {
			visibleButtonViewFS.setVisibility(View.INVISIBLE);
			visibleButtonViewFS = null;
		}
		if (visibleButtonSave != null) {
			visibleButtonSave.setVisibility(View.INVISIBLE);
			visibleButtonSave = null;
		}
		if (visibleButtonEdit != null) {
			visibleButtonEdit.setVisibility(View.INVISIBLE);
			visibleButtonEdit = null;
		}
	}

	public void showButtonForSelection(int rowIndex) {
		cancelSelectionIfAny();

		((Entry) mList.getItemAtPosition(rowIndex)).setSelected(true);
		currentSelectedIndex = rowIndex;

		// method "copied" from ListActivity maybe to be refactored here
		setSelection(rowIndex);

		// Mode changes will be automatically handled bu the UI
		onContentChanged();
	}

	public File saveCurrentSelectedShader() {
		// FIXME assert
		Entry value = values[currentSelectedIndex];
		return saveCurrentSelectedShader(new File(
				getDiyGlslLwpSubDir(COM_HEROKU_GLSL), value.getRefId() + TXT));
	}

	public static File getDiyGlslLwpDir() {
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			Log.e(DIY_GLSL_LWP_DIR_NAME, "Unable to access external storage");

			// To be tested
			Toast.makeText(null, "Unable to access external storage",
					Toast.LENGTH_LONG).show();
			// throw new RuntimeException("Unable to access external storage");

			return null;
		}

		File diyGlslLwpDir = new File(
				Environment.getExternalStorageDirectory(),
				DIY_GLSL_LWP_DIR_NAME);
		return diyGlslLwpDir;
	}

	public static File getDiyGlslLwpSubDir(String subdirName) {
		return new File(getDiyGlslLwpDir(), subdirName);
	}

	public File saveCurrentSelectedShaderAsLWP() {
		File lwp = saveCurrentSelectedShader(getDiyGlslLwpShaderFile());
		// TODO : to be tested
		LiveWallpaper.notifyShaderChange();
		return lwp;
	}

	private static File getDiyGlslLwpShaderFile() {
		return new File(getDiyGlslLwpSubDir(LWP_DIR_NAME), LWP_TXT);
	}

	public static File getCustomShaderLWPFile() {
		// FIXME assert
		File lwpShader = getDiyGlslLwpShaderFile();
		if (lwpShader.exists()) {
			return lwpShader;
		} else {
			return null;
		}
	}

	public File saveCurrentSelectedShader(File targetFile) {
		// fixme assert
		Entry value = values[currentSelectedIndex];
		String shaderPrg = currentFragShaderProgram;

		try {
			// SD Ext
			targetFile.getParentFile().mkdirs();

			PrintStream ps = new PrintStream(targetFile);
			ps.print(shaderPrg);
			ps.close();

			screenShotFilename = new File(targetFile.getParentFile(),
					value.getRefId() + ".jpg").getAbsolutePath();

			// Get callback during next redraw for screenshot
			mySurface.setDoscreenShotRequest(true);

		} catch (Exception ex) {
			Log.e("ShaderGalleryActivity", "saveCurrentShader", ex);
		}

		return targetFile;

	}

	void openCurrentSelectedShaderInSystemEditor() {
		// Save required before anything
		File tmpglsltxt = saveCurrentSelectedShader();

		// Start external editor (system knows which one)
		Intent intent = new Intent(Intent.ACTION_VIEW);
		Uri uri = Uri.fromFile(tmpglsltxt);
		intent.setDataAndType(uri, "text/plain");

		// TODO : Add parameter for content type :
		// ex : application/x-glsl
		// text/x-glsl-es-frag
		// text/x-glsl-frag

		startActivity(intent);

		// todo how to specify encoding in intent ??
	}

	private void cancelSelectionIfAny() {
		// don't trust current selection as it may be corrupted by threading /
		// delay issues in tasks
		for (Entry value : values) {
			value.setSelected(false);
		}

		currentSelectedIndex = -1;
		setSelection(currentSelectedIndex);
	}

	public void saveScreenshot() {
		int height = glslView.getHeight();
		int width = glslView.getWidth();

		Pixmap pixmap = getScreenshot(0, 0, width, height, true);

		// PixmapIO.writePNG(file, pixmap); // too inefficient
		saveAsJpeg(new FileHandle(screenShotFilename), pixmap);
	}

	/**
	 * 
	 * 
	 * @param jpgfile
	 * @param pixmap
	 */
	public void saveAsJpeg(FileHandle jpgfile, Pixmap pixmap) {
		FileOutputStream fos;
		int x = 0, y = 0;
		int xl = 0, yl = 0;
		try {
			Bitmap bmp = Bitmap.createBitmap(pixmap.getWidth(),
					pixmap.getHeight(), Bitmap.Config.ARGB_8888);
			// we need to switch between LibGDX RGBA format to Android ARGB
			// format
			for (x = 0, xl = pixmap.getWidth(); x < xl; x++) {
				for (y = 0, yl = pixmap.getHeight(); y < yl; y++) {
					int color = pixmap.getPixel(x, y);
					// RGBA => ARGB
					int RGB = color >> 8;
					int A = (color & 0x000000ff) << 24;
					int ARGB = A | RGB;
					bmp.setPixel(x, y, ARGB);
				}
			}
			// Finished Color format conversion
			fos = new FileOutputStream(jpgfile.file());
			bmp.compress(CompressFormat.JPEG, 90, fos);
			// Finished Comression to JPEG file
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 *
	 * */
	public Pixmap getScreenshot(int x, int y, int w, int h, boolean flipY) {
		Gdx.gl20.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);

		final Pixmap pixmap = new Pixmap(w, h, Format.RGBA8888);
		ByteBuffer pixels = pixmap.getPixels();

		Gdx.gl20.glReadPixels(x, y, w, h, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE,
				pixels);

		final int numBytes = w * h * 4;
		byte[] lines = new byte[numBytes];
		if (flipY) {
			final int numBytesPerLine = w * 4;
			for (int i = 0; i < h; i++) {
				pixels.position((h - i - 1) * numBytesPerLine);
				pixels.get(lines, i * numBytesPerLine, numBytesPerLine);
			}
			pixels.clear();
			pixels.put(lines);
		} else {
			pixels.clear();
			pixels.get(lines);
		}

		return pixmap;
	}

	/**
	 * setupFullScreenView
	 */
	public void setupFullScreenView() {

		if (glslView != null) {
			// Remove previously created view
			glayout.removeView(glslView);
		}

		AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
		cfg.useGL20 = true;

		// TODO more dynamic layout
		cfg.resolutionStrategy = new AlmostFSResStrategy(this,
				2 * FS_BUTTON_HEIGHT);

		mySurface = new DIYGslSurface(currentFragShaderProgram,
				prefs.isReductionFactorEnabled(), prefs.getReductionFactor(),
				prefs.isTouchEnabled(), prefs.isTimeDithering(),
				prefs.getTimeDitheringFactor());

		glslView = initializeForView(mySurface, cfg);

		// TODO more dynamic layout
		final Button cancelButton = new Button(this);
		cancelButton.setHeight(FS_BUTTON_HEIGHT);
		cancelButton.setText("Annuler");

		final Button setAsLWPButton = new Button(this);
		setAsLWPButton.setHeight(FS_BUTTON_HEIGHT);
		setAsLWPButton.setText("Fond Ecran");

		setAsLWPButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ShaderGalleryActivity.this.setAsLWP();
			}

		});

		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Toast.makeText(ShaderGalleryActivity.this, "Back",
				// Toast.LENGTH_SHORT).show();

				glayout.removeView(cancelButton);
				glayout.removeView(glslView);

				createGDXPreview(currentFragShaderProgram);
				glayout.addView(rootMainView, 1);

				getWindow().clearFlags(
						WindowManager.LayoutParams.FLAG_FULLSCREEN);
				getWindow().setFlags(
						WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
						WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

			}

		});

		glayout.removeView(rootMainView);
		glayout.addView(cancelButton);
		glayout.addView(setAsLWPButton);
		glayout.addView(glslView, 0);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(
				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

	}

	protected void setAsLWP() {
		saveCurrentSelectedShaderAsLWP();
		setOwnLWP();
	}

	@Override
	public void doProcessScreenShot() {
		saveScreenshot();
		screenShotFilename = null;
	}

	public void hideProgressDialog() {
		progressDialog.dismiss();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

}
