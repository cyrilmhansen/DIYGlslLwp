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

import android.graphics.Bitmap;

public class Entry {

	private static final String WAITING_FOR_DOWNLOAD = "waiting for download";
	private String refId;
	private Bitmap bmp;

	private String description;
	
	private boolean selected;

	public Entry(String refId) {
		this.refId = refId;
		this.description = WAITING_FOR_DOWNLOAD;
	}

	public String getRefId() {
		return refId;
	}

	public void setRefId(String refId) {
		this.refId = refId;
	}

	public Bitmap getBmp() {
		return bmp;
	}

	public void setBmp(Bitmap bmp) {
		this.bmp = bmp;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String toString() {
		return refId;

	}

	public void setSelected(boolean b) {
		selected = b;		
	}

	public boolean isSelected() {
		return selected;
	}

	
	
}
