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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
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
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.CustomAndroidGDXApp;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;

/**
 * Main activity / view
 * 
 * 
 * TODO : improve data model management, add simple cache between gallery page
 * resize image to smaller size to improve memory footprint
 * 
 */

public class ShaderGalleryActivity extends CustomAndroidGDXApp implements
		ScreenshotProcessor, ClickHandler, NativeCallback {

	private static final String HTTP_GLSL_HEROKU_COM_ITEM = "http://glsl.heroku.com/item/";

	private static final String COM_SOFTWARESEMANTICS_DIYGLSLLWP_PREFS_LWP = "com.softwaresemantics.diyglsllwp.LivewallpaperSettings";

	static final String DIY_GLSL_LWP_DIR_NAME = "DiyGlslLwp";

	private static final String COM_HEROKU_GLSL = "com.heroku.glsl";

	private static final int _200_PX = 200;

	// TODO : dynamic layout
	private static final int FS_BUTTON_HEIGHT = 150;

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

	DIYGslSurface mySurface;

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
			Toast.makeText(this,
					getResources().getString(R.string.networkRequired),
					Toast.LENGTH_LONG).show();
			exit();
		}
	}

	@Override
	protected void onResume() {
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

	@SuppressLint("NewApi")
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		LiveWallpaperService.galleryAppInstance = this;

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

		// Option menu must be accessible, either by key or action bar
		// API level 14
		try {
			if (!ViewConfiguration.get(this).hasPermanentMenuKey()) {
				// Ensure action bar is visible
				getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
				// getActionBar().hide();
			} else {
				requestWindowFeature(Window.FEATURE_NO_TITLE);
			}
		} catch (Error err) {
			// API < 14 ( => java.lang.NoSuchMethodError)
			// Option key is required on this system
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}

		// Try to to keep the UI fluid
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

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
						// TODO What if we are at the last page ??
						// FIXME Theoric CRASH
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
		updateStatusLabel(getResources().getString(R.string.loadingStatus));

	}

	public void updateStatusLabel(String alternateLabel) {
		TextView statusTextView = (TextView) findViewById(R.id.statusLabel);
		if (statusTextView == null) {
			Log.e("diyglsllwp", "updateStatusLabel statusLabel elt missing");
		}
		if (alternateLabel != null) {
			statusTextView.setText(alternateLabel);
		} else {
			statusTextView.setText("http://glsl.heroku.com/?page="
					+ currentPageIndex);
		}
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

		case R.id.goToShader:
			DialogUtils.inputDialog(this,
					getResources().getString(R.string.inputShaderRef), false,
					new InputDialogCallback() {

						@Override
						public void inputValue(String value) {
							try {
								// TODO use button just before preview area
								// for now we use the first line, but the image
								// is not in sync
								new InternetAsyncShaderTask(
										ShaderGalleryActivity.this, 0)
										.execute(HTTP_GLSL_HEROKU_COM_ITEM
												+ value);
							} catch (Exception ignored) {
							}

						}
					});
			return true;

		case R.id.goToPage:
			DialogUtils.inputDialog(this,
					getResources().getString(R.string.inputGalleryPage), true,
					new InputDialogCallback() {

						@Override
						public void inputValue(String value) {
							try {
								askedPageIndex = Integer.valueOf(value);
								currentSelectedIndex = -1;
								updateGallery(askedPageIndex);
							} catch (Exception ignored) {
							}

						}
					});
			return true;

		case R.id.prefsLWP:
			startPrefsLwpActivity();
			return true;

		case R.id.aPropos:
			showAproposDialog();
			return true;

		case R.id.quit:
			Toast.makeText(ShaderGalleryActivity.this,
					getResources().getString(R.string.exiting),
					Toast.LENGTH_SHORT).show();
			moveTaskToBack(true);
			return true;
		}
		return false;
	}

	private void showAproposDialog() {
		AboutDialog about = new AboutDialog(this);
		about.setTitle(getResources().getString(R.string.aPropos));
		about.show();
	}

	private void startPrefsLwpActivity() {
		Intent intent = new Intent(this, LivewallpaperSettings.class);
		intent.setAction(COM_SOFTWARESEMANTICS_DIYGLSLLWP_PREFS_LWP);
		startActivityForResult(intent, REQUEST_SET_LIVE_WALLPAPER);
	}

	private void updateGallery() {
		updateGallery(askedPageIndex);
	}

	private void updateGallery(int pageIndex) {
		if (!isOnline()) {
			Toast.makeText(this,
					getResources().getString(R.string.networkRequired),
					Toast.LENGTH_LONG).show();
			// call finish(); ??
		} else {
			progressDialog.setMessage(getResources().getString(
					R.string.loadingGalleryPage)
					+ pageIndex);
			progressDialog.show();

			// defered loading of gallery
			new InternetAsyncGalleryTask(this, pageIndex)
					.execute("http://glsl.heroku.com/?page=" + pageIndex);
		}
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		Entry item = (Entry) getListAdapter().getItem(position);

		hidePreviousSelectionButton();

		progressDialog.setMessage(getResources().getString(
				R.string.loadingShader)
				+ item.getRefId());
		progressDialog.show();

		// shader download task that will call us back to update UI
		new InternetAsyncShaderTask(this, position)
				.execute(HTTP_GLSL_HEROKU_COM_ITEM + item.getRefId());
	}

	/**
	 * Do what we can to help the user active the builtin Live Wallpaper Code
	 * from stackoverflow... on non standard ROMS, this will fail
	 */
	@SuppressLint("InlinedApi")
	public void setOwnLWP() {
		Intent intent;

		// try the new Jelly Bean direct android wallpaper chooser first
		try {
			ComponentName component = new ComponentName(LiveWallpaperService.class
					.getPackage().getName(),
					LiveWallpaperService.class.getCanonicalName());
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
						getResources().getString(R.string.manualSelectLWP),
						Toast.LENGTH_LONG).show();
			} catch (android.content.ActivityNotFoundException e2) {
				// that failed, let's try the nook intent
				try {
					intent = new Intent();
					intent.setAction("com.bn.nook.CHANGE_WALLPAPER");
					startActivity(intent);
				} catch (android.content.ActivityNotFoundException e) {
					// everything failed, let's notify the user
					Toast.makeText(
							ShaderGalleryActivity.this,
							getResources().getString(
									R.string.errorLaunchingLWPSelector),
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
		updateStatusLabel(null);
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
		LiveWallpaperService.galleryAppInstance = null;

		super.onDestroy();
	}

	@Override
	protected void onPause() {
		progressDialog.dismiss();

		super.onPause();

		// Force complete restart of gallery each time
		exit();
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

	private Button cancelButton;

	private Button setAsLWPButton;

	private AndroidApplicationConfiguration cfg;

	public void runShaderinPreview(String code) {
		if (glslView != null) {
			// Remove previously created view
			glayout.removeView(glslView);
		}
		createGDXPreview(code);
	}

	private void createGDXPreview(String code) {
		cfg = new AndroidApplicationConfiguration();
		cfg.useGL20 = true;
		cfg.resolutionStrategy = new PreviewResStrategy(this, _200_PX);

		// TODO : settings for preview
		DIYGslSurface.setRenderGuard(true);


		mySurface = new DIYGslSurface(code, true, 4, true, true, true, 4);
		mySurface.setScreenshotProc(this);
		
		// impossible to check immediately for GL20 / Surface is created
		// asynchronously we had a callback to get a chance to notify the user
		mySurface.addNativeCallback(this);

		glslView = initializeForView(mySurface, cfg);

		DIYGslSurface.setRenderGuard(false);

		// Add callback for click in preview mode
		mySurface.addClickHandler(this);

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
		String refId = "default";
		if (currentSelectedIndex >= 0) {
			refId = values[currentSelectedIndex].getRefId();
		}
		return saveCurrentSelectedShader(
				ShaderStorage.getDiyGlslLwpSubDir(this, COM_HEROKU_GLSL), refId);
	}

	public File saveCurrentSelectedShaderAsLWP() {
		File lwp = saveCurrentSelectedShader(
				ShaderStorage.getDiyGlslLwpShaderDir(this), ShaderStorage.LWP);

		LiveWallpaperService.reloadShaderIfNeeded();

		return lwp;
	}

	public File saveCurrentSelectedShader(File targetDir, String prefix) {
		// fixme assert

		String shaderPrg = currentFragShaderProgram;
		File targetFile = null;
		try {
			// SD Ext
			targetDir.mkdirs();

			targetFile = new File(targetDir, prefix + ShaderStorage.TXT);
			PrintStream ps = new PrintStream(targetFile);
			ps.print(shaderPrg);
			ps.close();

			screenShotFilename = new File(targetDir, prefix + ShaderStorage.JPG)
					.getAbsolutePath();

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

		// TODO : try to improve encoding handling
		// TODO : Add live editing of fragment shader

		startActivity(intent);
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

		cfg = new AndroidApplicationConfiguration();
		cfg.useGL20 = true;

		// TODO more dynamic layout for this screen
		cfg.resolutionStrategy = new AlmostFSResStrategy(this,
				(int) (2 * FS_BUTTON_HEIGHT * getResources()
						.getDisplayMetrics().density));

		mySurface = new DIYGslSurface(currentFragShaderProgram,
				prefs.isReductionFactorEnabled(), prefs.getReductionFactor(),
				true, true, prefs.isTimeDithering(),
				prefs.getTimeDitheringFactor());

		glslView = initializeForView(mySurface, cfg);

		cancelButton = new Button(this);
		cancelButton.setHeight(FS_BUTTON_HEIGHT);
		cancelButton.setText(getResources().getString(R.string.cancel));

		setAsLWPButton = new Button(this);
		setAsLWPButton.setHeight(FS_BUTTON_HEIGHT);
		setAsLWPButton.setText(getResources().getString(R.string.setAsLWP));

		setAsLWPButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ShaderGalleryActivity.this.setAsLWP();
				backToMainView();
			}

		});

		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				backToMainView();
			}

		});

		glayout.removeView(rootMainView);
		glayout.addView(setAsLWPButton);
		glayout.addView(cancelButton);
		glayout.addView(glslView, 0);
	

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(
				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

	}

	private void backToMainView() {
		glayout.removeView(setAsLWPButton);
		glayout.removeView(cancelButton);
		glayout.removeView(glslView);

		createGDXPreview(currentFragShaderProgram);
		glayout.addView(rootMainView, 1);

		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().setFlags(
				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
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

	@Override
	public void onClick(int x, int y) {
		runOnUiThread(new UIFullViewRunnable(this));
	}

	private void goToFullViewIfPossible() {
		if (currentFragShaderProgram != null) {
			setupFullScreenView();
		}
	}

	class UIFullViewRunnable implements Runnable {

		private ShaderGalleryActivity parentActivity;

		public UIFullViewRunnable(ShaderGalleryActivity activity) {
			this.parentActivity = activity;
		}

		public void run() {
			this.parentActivity.goToFullViewIfPossible();
			;
		}
	}

	@Override
	public void onRequirementFailure(String msg) {
		Toast.makeText(this,
				getResources().getString(R.string.openGLRequirementsFailure),
				Toast.LENGTH_LONG).show();
		exit();

	}

	@Override
	public void onResumeGDX() {
		// do nothing in Gallery
	}

}
