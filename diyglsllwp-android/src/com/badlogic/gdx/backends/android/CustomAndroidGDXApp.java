package com.badlogic.gdx.backends.android;

import android.view.View;

public class CustomAndroidGDXApp extends AndroidApplication {
	
	public void destroyGraphics() {
		graphics.destroy();
	}
	
	public AndroidGraphics getGraphics() {
		return (AndroidGraphics) graphics;
	}
	
	public View getGraphicsView() {
		return graphics.view;
	}

}
